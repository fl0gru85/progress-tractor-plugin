package com.progresstractor

import com.intellij.ui.scale.JBUIScale
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.Timer
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Progress bar UI that shows a tractor plowing a field instead of a plain bar.
 * Swing instantiates it through the static `createUI` in [TractorProgressBarUIFactory].
 */
open class TractorProgressBarUI : BasicProgressBarUI() {

    internal var clock: () -> Long = System::currentTimeMillis

    private var repaintTimer: Timer? = null

    private val hierarchyListener = HierarchyListener { e ->
        if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) updateRepaintTimer()
    }

    override fun installListeners() {
        super.installListeners()
        progressBar.addHierarchyListener(hierarchyListener)
        updateRepaintTimer()
    }

    override fun uninstallListeners() {
        progressBar.removeHierarchyListener(hierarchyListener)
        stopRepaintTimer()
        super.uninstallListeners()
    }

    // The timer only runs while the bar is on screen, so hidden/discarded bars are never kept alive.
    private fun updateRepaintTimer() {
        val bar = progressBar ?: return
        if (bar.isShowing) {
            if (repaintTimer == null) {
                repaintTimer = Timer(FRAME_MS) { bar.repaint() }.apply { start() }
            }
        } else {
            stopRepaintTimer()
        }
    }

    private fun stopRepaintTimer() {
        repaintTimer?.stop()
        repaintTimer = null
    }

    override fun getPreferredSize(c: JComponent): Dimension {
        val size = super.getPreferredSize(c)
        if (progressBar.orientation == SwingConstants.HORIZONTAL) {
            val insets = progressBar.insets
            size.height = max(size.height, JBUIScale.scale(PREFERRED_HEIGHT) + insets.top + insets.bottom)
        }
        return size
    }

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        if (progressBar.orientation != SwingConstants.HORIZONTAL) return super.paintDeterminate(g, c)
        val r = contentRect() ?: return
        val fraction = progressBar.percentComplete
        val plowed = TractorGeometry.plowedWidth(r.width, fraction)
        val tractorW = TractorGeometry.tractorWidth(r.width, r.height)
        val tractorX = r.x + TractorGeometry.determinateTractorX(r.width, tractorW, fraction)

        paintScene(g, r) { g2 ->
            TractorPainter.paintField(g2, r.x, r.y, r.width, r.height, r.x, r.x + plowed, status())
            TractorPainter.paintTractor(g2, tractorX, r.y, tractorW, r.height, facingRight = true, timeMs = clock())
        }
        if (progressBar.isStringPainted) paintString(g, r.x, r.y, r.width, r.height, plowed, progressBar.insets)
    }

    override fun paintIndeterminate(g: Graphics, c: JComponent) {
        if (progressBar.orientation != SwingConstants.HORIZONTAL) return super.paintIndeterminate(g, c)
        val r = contentRect() ?: return
        val now = clock()
        val pass = TractorGeometry.indeterminatePass(now)
        val tractorW = TractorGeometry.tractorWidth(r.width, r.height)
        val tractorX = r.x + TractorGeometry.determinateTractorX(r.width, tractorW, pass.fraction)
        // Short strip of fresh soil behind the tractor.
        val (trailFrom, trailTo) = if (pass.movingRight) {
            tractorX - TRAIL_LENGTH * tractorW to tractorX + (tractorW * 0.3).roundToInt()
        } else {
            tractorX + (tractorW * 0.7).roundToInt() to tractorX + (TRAIL_LENGTH + 1) * tractorW
        }

        paintScene(g, r) { g2 ->
            TractorPainter.paintField(g2, r.x, r.y, r.width, r.height, trailFrom, trailTo, status())
            TractorPainter.paintTractor(g2, tractorX, r.y, tractorW, r.height, pass.movingRight, now)
        }
        if (progressBar.isStringPainted) paintString(g, r.x, r.y, r.width, r.height, 0, progressBar.insets)
    }

    private inline fun paintScene(g: Graphics, r: Rectangle, block: (Graphics2D) -> Unit) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            val arc = minOf(r.height, JBUIScale.scale(CORNER_ARC)).toDouble()
            g2.clip(RoundRectangle2D.Double(r.x.toDouble(), r.y.toDouble(), r.width.toDouble(), r.height.toDouble(), arc, arc))
            block(g2)
        } finally {
            g2.dispose()
        }
    }

    private fun contentRect(): Rectangle? {
        val i = progressBar.insets
        val r = Rectangle(i.left, i.top, progressBar.width - i.left - i.right, progressBar.height - i.top - i.bottom)
        return if (r.width > 0 && r.height > 0) r else null
    }

    /** Honors the status IntelliJ sets on progress bars (e.g. failed tests in the test runner). */
    private fun status(): TractorPainter.Status =
        when (progressBar.getClientProperty(STATUS_PROPERTY)) {
            "error" -> TractorPainter.Status.ERROR
            "warning" -> TractorPainter.Status.WARNING
            else -> TractorPainter.Status.NORMAL
        }

    companion object {
        private const val FRAME_MS = 40
        private const val PREFERRED_HEIGHT = 16
        private const val CORNER_ARC = 6
        private const val TRAIL_LENGTH = 2
        private const val STATUS_PROPERTY = "ProgressBar.status"
    }
}
