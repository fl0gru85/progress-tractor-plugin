package com.progresstractor

import kotlin.math.roundToInt

/** Pure layout math for the tractor progress bar (no Swing / platform dependencies). */
object TractorGeometry {

    /** Width-to-height ratio of the tractor sprite. */
    const val TRACTOR_ASPECT = 1.8

    /** Duration of one full left-to-right (or right-to-left) pass in indeterminate mode. */
    const val INDETERMINATE_PASS_MS = 2500L

    fun tractorWidth(barWidth: Int, barHeight: Int): Int =
        (barHeight * TRACTOR_ASPECT).roundToInt().coerceAtLeast(1).coerceAtMost(barWidth.coerceAtLeast(1))

    /** Width of the already plowed (= completed) part of the field. */
    fun plowedWidth(barWidth: Int, fraction: Double): Int =
        (barWidth * clamp(fraction)).roundToInt()

    /** Left x of the tractor for a determinate progress [fraction]; the tractor never leaves the bar. */
    fun determinateTractorX(barWidth: Int, tractorWidth: Int, fraction: Double): Int =
        ((barWidth - tractorWidth).coerceAtLeast(0) * clamp(fraction)).roundToInt()

    data class Pass(val fraction: Double, val movingRight: Boolean)

    /** Ping-pong movement for indeterminate progress, derived purely from a time stamp. */
    fun indeterminatePass(timeMs: Long, passMs: Long = INDETERMINATE_PASS_MS): Pass {
        val cycle = Math.floorMod(timeMs, 2 * passMs)
        return if (cycle < passMs) {
            Pass(cycle.toDouble() / passMs, movingRight = true)
        } else {
            Pass(1.0 - (cycle - passMs).toDouble() / passMs, movingRight = false)
        }
    }

    private fun clamp(fraction: Double): Double =
        if (fraction.isNaN()) 0.0 else fraction.coerceIn(0.0, 1.0)
}
