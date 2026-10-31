package me.aysh.studynest.data.model

import java.time.Instant
import java.time.LocalDate

/** Pastel swatches a subject can use. [key] is the value stored on the server. */
enum class SubjectColor(val key: String) {
    Lilac("lilac"), Peach("peach"), Mint("mint"), Butter("butter"), Sky("sky"), Rose("rose");

    companion object {
        fun fromKey(key: String): SubjectColor = entries.firstOrNull { it.key == key } ?: Lilac
    }
}

enum class SubjectIcon(val key: String) {
    Book("book"),
    Calculator("calculator"),
    Flask("flask"),
    Globe("globe"),
    Palette("palette"),
    Code("code"),
    Music("music"),
    Language("language"),
    History("history"),
    Biology("biology");

    companion object {
        fun fromKey(key: String): SubjectIcon = entries.firstOrNull { it.key == key } ?: Book
    }
}

enum class Priority(val key: String) {
    Low("low"), Medium("medium"), High("high");

    companion object {
        fun fromKey(key: String): Priority = entries.firstOrNull { it.key == key } ?: Medium
    }
}

/** The bits of a subject that other items embed. */
data class SubjectRef(val id: String, val name: String, val color: SubjectColor, val icon: SubjectIcon)

data class Subject(
    val id: String,
    val name: String,
    val color: SubjectColor,
    val icon: SubjectIcon,
    val openCount: Int,
    val totalCount: Int,
) {
    val ref: SubjectRef get() = SubjectRef(id, name, color, icon)
}

data class SubjectDraft(val name: String, val color: SubjectColor, val icon: SubjectIcon)

data class Assignment(
    val id: String,
    val title: String,
    val notes: String,
    val dueOn: LocalDate,
    val priority: Priority,
    val completedOn: LocalDate?,
    val subject: SubjectRef,
) {
    val isDone: Boolean get() = completedOn != null
}

data class AssignmentDraft(
    val title: String,
    val subjectId: String,
    val dueOn: LocalDate,
    val priority: Priority,
    val notes: String,
)

enum class AssignmentStatus(val key: String) { Open("open"), Done("done"), All("all") }

data class FocusSession(
    val id: String,
    val subject: SubjectRef?,
    val startedAt: Instant,
    val durationSeconds: Int,
    val day: LocalDate,
)

data class DayFocus(val date: LocalDate, val seconds: Long)

/** [subject] is null for time logged without a subject (or whose subject was deleted). */
data class SubjectFocus(val subject: SubjectRef?, val seconds: Long)

data class WeekInsights(
    val weekStart: LocalDate,
    val totalSeconds: Long,
    val sessionCount: Int,
    val days: List<DayFocus>,
    val bySubject: List<SubjectFocus>,
    val assignmentsCompleted: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val dailyGoalMinutes: Int,
)

data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val dailyGoalMinutes: Int,
    val memberSince: Instant,
) {
    val firstName: String get() = name.trim().substringBefore(' ').ifEmpty { name }
}
