package com.progresstractor

import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JProgressBar

/**
 * Renders a preview PNG of the progress bar (light and dark theme, several sizes and states).
 * Invoked by the `renderPreview` Gradle task; the single argument is the output file.
 */
object PreviewRenderer {

    private data class Row(val height: Int, val value: Int, val indeterminate: Boolean = false)

    private val rows = listOf(Row(8, 40), Row(12, 60), Row(16, 30), Row(32, 55), Row(32, 0, indeterminate = true))
    private const val BAR_WIDTH = 300
    private const val GAP = 8
    private const val SCALE = 3

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "usage: PreviewRenderer <output.png>" }
        val out = File(args[0])
        out.parentFile?.mkdirs()
        ImageIO.write(render(), "png", out)
        println("Preview written to ${out.absolutePath}")
    }

    fun render(): BufferedImage {
        val colW = BAR_WIDTH + 2 * GAP
        val height = rows.sumOf { it.height + GAP } + GAP
        val image = BufferedImage(colW * 2, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            for ((col, dark) in listOf(0 to false, 1 to true)) {
                JBColor.setDark(dark)
                g.color = if (dark) Color(0x2B2D30) else Color(0xF7F8FA)
                g.fillRect(col * colW, 0, colW, height)
                var y = GAP
                for (row in rows) {
                    val bar = JProgressBar(0, 100).apply {
                        setUI(TractorProgressBarUI().also { it.clock = { 700L } })
                        border = null
                        value = row.value
                        isIndeterminate = row.indeterminate
                        setSize(BAR_WIDTH, row.height)
                    }
                    val bg = g.create(col * colW + GAP, y, BAR_WIDTH, row.height) as Graphics2D
                    try {
                        bar.ui.paint(bg, bar)
                    } finally {
                        bg.dispose()
                    }
                    y += row.height + GAP
                }
            }
        } finally {
            JBColor.setDark(false)
            g.dispose()
        }

        val scaled = BufferedImage(image.width * SCALE, image.height * SCALE, BufferedImage.TYPE_INT_ARGB)
        val sg = scaled.createGraphics()
        try {
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            sg.drawImage(image, 0, 0, scaled.width, scaled.height, null)
        } finally {
            sg.dispose()
        }
        return scaled
    }
}
