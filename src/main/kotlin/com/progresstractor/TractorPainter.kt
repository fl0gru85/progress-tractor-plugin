package com.progresstractor

import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Draws the field and the tractor with plain Java2D – no image assets. */
object TractorPainter {

    enum class Status { NORMAL, WARNING, ERROR }

    private val SKY = JBColor(Color(0xDDEFFA), Color(0xC2DDF0))
    private val GRASS = JBColor(Color(0xA5D46A), Color(0x7FAF4A))
    private val GRASS_BLADE = JBColor(Color(0x8BC34A), Color(0x6A9A3C))
    private val SOIL = JBColor(Color(0x8D6E63), Color(0x7A5A4C))
    private val SOIL_WARNING = JBColor(Color(0xC98A3C), Color(0x9E6B2A))
    private val SOIL_ERROR = JBColor(Color(0xB0453A), Color(0x8F3830))
    private val FURROW = JBColor(Color(0x5D4037), Color(0x4E342E))

    // Approximation of Fendt's dark "nature green" – not an official color value.
    private val BODY = JBColor(Color(0x2F5F2C), Color(0x2F5F2C))
    private val BODY_DARK = JBColor(Color(0x1E3F1D), Color(0x1E3F1D))
    private val CHASSIS = Color(0x2B2B2B)
    private val WINDOW = JBColor(Color(0xB3E5FC), Color(0x9FD3EE))
    private val RIM = JBColor(Color(0xB8BEC4), Color(0xC9CED3))
    private val TIRE = Color(0x1C1C1C)
    private val HUB = Color(0x4A4F54)
    // Dark smoke so it stands out against the light sky in both themes.
    private val SMOKE = Color(0x6B7075)
    private val SMOKE_EDGE = Color(0x45494D)

    /** Height of the ground strip at the bottom of the bar; the rest above it is sky. */
    fun groundHeight(h: Int): Int = max(min(3, h), (h * GROUND_RATIO).roundToInt())

    /**
     * Paints sky on top, a low grass strip at the bottom and plowed soil in that strip between
     * [plowedFrom] and [plowedTo] (absolute x).
     */
    fun paintField(g: Graphics2D, x: Int, y: Int, w: Int, h: Int, plowedFrom: Int, plowedTo: Int, status: Status) {
        val groundH = groundHeight(h)
        val groundY = y + h - groundH
        g.color = SKY
        g.fillRect(x, y, w, h - groundH)

        g.color = GRASS
        g.fillRect(x, groundY, w, groundH)
        if (groundH >= 4) {
            val bladeGap = max(3, JBUIScale.scale(4))
            val bladeTop = groundY - max(1, groundH / 4)
            g.color = GRASS_BLADE
            var bx = x + bladeGap / 2
            while (bx < x + w) {
                g.drawLine(bx, bladeTop, bx, groundY + groundH / 2)
                bx += bladeGap
            }
        }

        val from = plowedFrom.coerceIn(x, x + w)
        val to = plowedTo.coerceIn(x, x + w)
        if (to <= from) return
        g.color = when (status) {
            Status.NORMAL -> SOIL
            Status.WARNING -> SOIL_WARNING
            Status.ERROR -> SOIL_ERROR
        }
        g.fillRect(from, groundY, to - from, groundH)
        if (groundH >= 4) {
            g.color = FURROW
            val furrowGap = max(2, groundH / 3)
            var fy = groundY + furrowGap - 1
            while (fy < y + h) {
                g.drawLine(from, fy, to - 1, fy)
                fy += furrowGap
            }
        }
    }

    /**
     * Paints the tractor into the box ([x],[y],[w],[h]). The sprite is drawn facing right and mirrored
     * when [facingRight] is false. [timeMs] drives the wheel rotation and the exhaust smoke.
     */
    fun paintTractor(g: Graphics2D, x: Int, y: Int, w: Int, h: Int, facingRight: Boolean, timeMs: Long) {
        if (w <= 0 || h <= 0) return
        val g2 = g.create() as Graphics2D
        try {
            if (facingRight) {
                g2.translate(x, y)
            } else {
                g2.translate(x + w, y)
                g2.scale(-1.0, 1.0)
            }
            drawTractor(g2, w.toDouble(), h.toDouble(), timeMs)
        } finally {
            g2.dispose()
        }
    }

    private fun drawTractor(g: Graphics2D, w: Double, h: Double, timeMs: Long) {
        val detailed = h >= 10
        val rearR = h * 0.36
        val rearCx = rearR + w * 0.03
        val rearCy = h - rearR
        val frontR = h * 0.22
        val frontCx = w - frontR - w * 0.03
        val frontCy = h - frontR

        // Exhaust pipe and smoke (behind the hood).
        val pipeW = max(1.0, w * 0.045)
        val pipeX = w * 0.70
        val pipeTop = h * 0.10
        if (detailed) drawSmoke(g, pipeX + pipeW / 2, pipeTop, w, h, timeMs)
        g.color = CHASSIS
        g.fill(RoundRectangle2D.Double(pipeX, pipeTop, pipeW, h * 0.32, pipeW, pipeW))

        // Chassis and hood.
        g.color = CHASSIS
        g.fill(RoundRectangle2D.Double(rearCx, h * 0.58, frontCx - rearCx, h * 0.16, h * 0.08, h * 0.08))
        g.color = BODY
        val hoodX = rearCx
        val hoodW = frontCx + frontR * 0.8 - hoodX
        g.fill(RoundRectangle2D.Double(hoodX, h * 0.36, hoodW, h * 0.28, h * 0.12, h * 0.12))
        if (detailed) {
            g.color = BODY_DARK
            g.stroke = BasicStroke(max(1f, (h / 24).toFloat()))
            var gx = hoodX + hoodW * 0.55
            while (gx < hoodX + hoodW - h * 0.06) {
                g.draw(Line2D.Double(gx, h * 0.41, gx, h * 0.58))
                gx += max(2.0, h * 0.09)
            }
        }

        // Cabin.
        val cabX = w * 0.07
        val cabW = w * 0.40
        val cabY = h * 0.02
        val cabH = h * 0.46
        g.color = BODY
        g.fill(RoundRectangle2D.Double(cabX, cabY, cabW, cabH, h * 0.1, h * 0.1))
        if (detailed) {
            val inset = max(1.0, h * 0.07)
            g.color = WINDOW
            g.fill(RoundRectangle2D.Double(cabX + inset, cabY + inset * 1.6, cabW - 2 * inset, cabH - inset * 2.6, inset, inset))
        }

        drawWheel(g, rearCx, rearCy, rearR, timeMs, detailed)
        drawWheel(g, frontCx, frontCy, frontR, timeMs, detailed)
    }

    private fun drawWheel(g: Graphics2D, cx: Double, cy: Double, r: Double, timeMs: Long, detailed: Boolean) {
        g.color = TIRE
        g.fill(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
        val rimR = r * 0.58
        g.color = RIM
        g.fill(Ellipse2D.Double(cx - rimR, cy - rimR, 2 * rimR, 2 * rimR))
        if (!detailed) return

        // Same ground speed for both wheels -> smaller wheel spins faster.
        val angle = (timeMs / 1000.0) * (WHEEL_GROUND_SPEED / r)
        g.color = HUB
        g.stroke = BasicStroke(max(1f, (r / 8).toFloat()))
        for (i in 0 until 3) {
            val a = angle + i * PI / 3
            val dx = cos(a) * rimR
            val dy = sin(a) * rimR
            g.draw(Line2D.Double(cx - dx, cy - dy, cx + dx, cy + dy))
        }
        val hubR = max(1.0, r * 0.15)
        g.fill(Ellipse2D.Double(cx - hubR, cy - hubR, 2 * hubR, 2 * hubR))
    }

    private fun drawSmoke(g: Graphics2D, pipeCx: Double, pipeTop: Double, w: Double, h: Double, timeMs: Long) {
        val oldComposite = g.composite
        try {
            val phase = Math.floorMod(timeMs, SMOKE_CYCLE_MS).toDouble() / SMOKE_CYCLE_MS
            for (i in 0 until SMOKE_PUFFS) {
                val p = (phase + i.toDouble() / SMOKE_PUFFS) % 1.0
                val r = h * (0.07 + 0.10 * p)
                // Drift only slightly backwards so the puffs stay in front of the sky, not over the cabin.
                val cx = pipeCx - p * w * 0.16
                val cy = pipeTop + r * 0.2 - p * h * 0.05
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (0.9 * (1 - p * 0.8)).toFloat())
                g.color = SMOKE
                g.fill(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
                g.color = SMOKE_EDGE
                g.stroke = BasicStroke(max(1f, (h / 32).toFloat()))
                g.draw(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
            }
        } finally {
            g.composite = oldComposite
        }
    }

    private const val WHEEL_GROUND_SPEED = 40.0
    private const val SMOKE_CYCLE_MS = 1200L
    private const val SMOKE_PUFFS = 3
    private const val GROUND_RATIO = 0.4
}
