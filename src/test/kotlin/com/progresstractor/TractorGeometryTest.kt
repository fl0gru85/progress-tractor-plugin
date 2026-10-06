package com.progresstractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TractorGeometryTest {

    @Test
    fun `tractor width follows aspect ratio but never exceeds the bar`() {
        assertEquals(36, TractorGeometry.tractorWidth(200, 20))
        assertEquals(10, TractorGeometry.tractorWidth(10, 20))
        assertEquals(1, TractorGeometry.tractorWidth(0, 0))
    }

    @Test
    fun `plowed width is proportional to progress and clamped`() {
        assertEquals(0, TractorGeometry.plowedWidth(200, 0.0))
        assertEquals(100, TractorGeometry.plowedWidth(200, 0.5))
        assertEquals(200, TractorGeometry.plowedWidth(200, 1.0))
        assertEquals(200, TractorGeometry.plowedWidth(200, 1.7))
        assertEquals(0, TractorGeometry.plowedWidth(200, -0.3))
        assertEquals(0, TractorGeometry.plowedWidth(200, Double.NaN))
    }

    @Test
    fun `tractor stays inside the bar`() {
        assertEquals(0, TractorGeometry.determinateTractorX(200, 36, 0.0))
        assertEquals(82, TractorGeometry.determinateTractorX(200, 36, 0.5))
        assertEquals(164, TractorGeometry.determinateTractorX(200, 36, 1.0))
        assertEquals(0, TractorGeometry.determinateTractorX(20, 36, 1.0))
    }

    @Test
    fun `indeterminate pass goes right then back left`() {
        val pass = 1000L
        assertEquals(TractorGeometry.Pass(0.0, true), TractorGeometry.indeterminatePass(0, pass))
        assertEquals(TractorGeometry.Pass(0.5, true), TractorGeometry.indeterminatePass(500, pass))
        val back = TractorGeometry.indeterminatePass(1500, pass)
        assertEquals(0.5, back.fraction, 1e-9)
        assertFalse(back.movingRight)
        assertEquals(TractorGeometry.Pass(0.0, true), TractorGeometry.indeterminatePass(2000, pass))
        assertTrue(TractorGeometry.indeterminatePass(-250, pass).fraction in 0.0..1.0)
    }
}
