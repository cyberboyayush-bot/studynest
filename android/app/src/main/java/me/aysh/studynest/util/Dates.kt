package me.aysh.studynest.util

import me.aysh.studynest.data.model.Assignment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Weeks run Monday to Sunday, matching the server's insights. */
object Weeks {

    fun startOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun endOf(date: LocalDate): LocalDate = startOf(date).plusDays(6)

    fun days(weekStart: LocalDate): List<LocalDate> = (0L until 7L).map { weekStart.plusDays(it) }

    /** "12 – 18 Oct", or "28 Sep – 4 Oct" across months, or with years when they differ. */
    fun rangeLabel(weekStart: LocalDate, locale: Locale = Locale.getDefault()): String {
        val end = weekStart.plusDays(6)
        val full = DateTimeFormatter.ofPattern("d MMM", locale)
        return when {
            weekStart.year != end.year -> {
                val withYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
                "${weekStart.format(withYear)} – ${end.format(withYear)}"
            }
            weekStart.month == end.month -> "${weekStart.dayOfMonth} – ${end.format(full)}"
            else -> "${weekStart.format(full)} – ${end.format(full)}"
        }
    }

    /** Number of days in the week whose total met the daily goal. */
    fun daysMeetingGoal(secondsPerDay: List<Long>, goalMinutes: Int): Int =
        if (goalMinutes <= 0) 0 else secondsPerDay.count { it >= goalMinutes * 60L }
}

/** Groups used by the assignments list, in display order. */
enum class DueBucket { Overdue, Today, ThisWeek, Later, Done }

object Due {

    fun bucket(assignment: Assignment, today: LocalDate): DueBucket = when {
        assignment.isDone -> DueBucket.Done
        assignment.dueOn < today -> DueBucket.Overdue
        assignment.dueOn == today -> DueBucket.Today
        assignment.dueOn <= Weeks.endOf(today) -> DueBucket.ThisWeek
        else -> DueBucket.Later
    }

    /** Buckets in display order, skipping empty ones. Items keep their incoming order. */
    fun group(assignments: List<Assignment>, today: LocalDate): List<Pair<DueBucket, List<Assignment>>> {
        val grouped = assignments.groupBy { bucket(it, today) }
        return DueBucket.entries.mapNotNull { bucket -> grouped[bucket]?.let { bucket to it } }
    }

    fun isOverdue(assignment: Assignment, today: LocalDate): Boolean = !assignment.isDone && assignment.dueOn < today

    /** Short, friendly due label: "2 days late", "Today", "Tomorrow", "Fri", "12 Nov". */
    fun label(dueOn: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String {
        val days = ChronoUnit.DAYS.between(today, dueOn)
        return when {
            days < -1 -> "${-days} days late"
            days == -1L -> "Yesterday"
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days < 7 -> dueOn.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            dueOn.year == today.year -> dueOn.format(DateTimeFormatter.ofPattern("d MMM", locale))
            else -> dueOn.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
        }
    }
}

enum class DayPart { Morning, Afternoon, Evening }

fun dayPart(time: LocalTime): DayPart = when (time.hour) {
    in 5..11 -> DayPart.Morning
    in 12..16 -> DayPart.Afternoon
    else -> DayPart.Evening
}

/** "Saturday, 31 October" */
fun longDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
