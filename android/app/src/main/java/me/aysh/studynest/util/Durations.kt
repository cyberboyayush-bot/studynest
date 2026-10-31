package me.aysh.studynest.util

import java.util.Locale
import kotlin.math.ceil

object Durations {

    /**
     * Countdown text for the timer: "25:00", "4:05", or "1:02:03" past an hour.
     * Rounds partial seconds up so a fresh 25-minute timer shows 25:00, not 24:59.
     */
    fun clock(millis: Long): String {
        val total = ceil(millis.coerceAtLeast(0) / 1000.0).toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    /** "45 min", "1 h", "1 h 30 min". */
    fun minutes(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** Whole minutes in [seconds], rounded down. */
    fun wholeMinutes(seconds: Long): Int = (seconds.coerceAtLeast(0) / 60).toInt()

    /** Compact hours for charts and stats: "0", "0.5", "4.2", "12". */
    fun hours(seconds: Long): String {
        val tenths = Math.round(seconds.coerceAtLeast(0) / 360.0)
        return if (tenths % 10 == 0L) "${tenths / 10}" else String.format(Locale.ROOT, "%d.%d", tenths / 10, tenths % 10)
    }

    /** Fraction of the daily goal reached, clamped to 0..1. */
    fun goalProgress(seconds: Long, goalMinutes: Int): Float =
        if (goalMinutes <= 0) 0f else (seconds.toFloat() / (goalMinutes * 60f)).coerceIn(0f, 1f)
}
