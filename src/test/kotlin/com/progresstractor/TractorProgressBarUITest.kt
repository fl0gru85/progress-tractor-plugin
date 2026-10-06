package com.progresstractor

import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.JProgressBar

class TractorProgressBarUITest {

    private fun render(bar: JProgressBar): BufferedImage {
        val image = BufferedImage(bar.width, bar.height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            bar.ui.paint(g, bar)
        } finally {
            g.dispose()
        }
        return image
    }

    private fun bar(width: Int, height: Int, value: Int, indeterminate: Boolean = false) =
        JProgressBar(0, 100).apply {
            setUI(TractorProgressBarUI().also { it.clock = { 1234L } })
            border = null
            this.value = value
            isIndeterminate = indeterminate
            setSize(width, height)
        }

    private fun isSoil(c: Color) = c.red > c.green && c.red > c.blue
    private fun isCrop(c: Color) = c.green > c.red && c.green > c.blue

    private val groundY = 18 // inside the ground strip of a 20 px bar
    private val skyY = 1

    @Test
    fun `completed part of the ground is plowed, the rest is grass`() {
        val image = render(bar(200, 20, 50))
        assertTrue("left half should be soil", isSoil(Color(image.getRGB(60, groundY), true)))
        assertTrue("right half should be grass", isCrop(Color(image.getRGB(180, groundY), true)))
    }

    @Test
    fun `full progress plows the whole ground`() {
        val image = render(bar(200, 20, 100))
        assertTrue(isSoil(Color(image.getRGB(10, groundY), true)))
        assertTrue(isSoil(Color(image.getRGB(150, groundY), true)))
    }

    @Test
    fun `upper part is sky, neither soil nor grass`() {
        val image = render(bar(200, 20, 50))
        for (x in listOf(30, 180)) {
            val c = Color(image.getRGB(x, skyY), true)
            assertTrue("x=$x should be sky but was $c", !isSoil(c) && !isCrop(c))
        }
    }

    @Test
    fun `renders all sizes and modes without failing`() {
        for (height in listOf(1, 2, 4, 8, 12, 20, 40)) {
            for (width in listOf(1, 10, 60, 300)) {
                for (value in listOf(0, 33, 100)) {
                    render(bar(width, height, value))
                }
                render(bar(width, height, 0, indeterminate = true))
            }
        }
    }

    @Test
    fun `installer makes new progress bars use the tractor UI`() {
        TractorUiInstaller.install()
        assertTrue(JProgressBar().ui is TractorProgressBarUI)
    }

    @Test
    fun `preferred height is large enough for the tractor`() {
        val bar = JProgressBar().apply { setUI(TractorProgressBarUI()); border = null }
        assertTrue(bar.preferredSize.height >= 16)
    }
}
