package me.aysh.studynest.util

import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.model.SubjectRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class DurationsTest {

    @Test
    fun `clock formats minutes and hours`() {
        assertEquals("25:00", Durations.clock(25 * 60_000L))
        assertEquals("00:59", Durations.clock(59_000L))
        assertEquals("1:02:03", Durations.clock((3600 + 120 + 3) * 1000L))
        assertEquals("00:00", Durations.clock(-5L))
    }

    @Test
    fun `clock rounds partial seconds up`() {
        assertEquals("25:00", Durations.clock(25 * 60_000L - 300))
        assertEquals("00:01", Durations.clock(1L))
    }

    @Test
    fun `minutes reads naturally`() {
        assertEquals("0 min", Durations.minutes(0))
        assertEquals("45 min", Durations.minutes(45))
        assertEquals("1 h", Durations.minutes(60))
        assertEquals("1 h 30 min", Durations.minutes(90))
    }

    @Test
    fun `hours keeps one decimal only when needed`() {
        assertEquals("0", Durations.hours(0))
        assertEquals("0.5", Durations.hours(1800))
        assertEquals("2", Durations.hours(7200))
        assertEquals("4.2", Durations.hours(15_000))
    }

    @Test
    fun `goal progress is clamped`() {
        assertEquals(0.5f, Durations.goalProgress(1800, 60), 0.0001f)
        assertEquals(1f, Durations.goalProgress(9000, 60), 0.0001f)
        assertEquals(0f, Durations.goalProgress(500, 0), 0.0001f)
        assertEquals(1, Durations.wholeMinutes(119))
    }
}

class ValidationTest {

    @Test
    fun `email rules`() {
        assertNull(Validation.email("a@b.co"))
        assertNull(Validation.email("  first.last+tag@uni.edu  "))
        assertEquals("Enter your email", Validation.email(" "))
        assertEquals("Enter a valid email address", Validation.email("a@b"))
    }

    @Test
    fun `name and password rules`() {
        assertEquals("Enter your name", Validation.name("   "))
        assertEquals("Name is too long", Validation.name("x".repeat(61)))
        assertEquals("Use at least 8 characters", Validation.newPassword("short"))
        assertNull(Validation.newPassword("long-enough"))
        assertEquals("Enter your password", Validation.passwordPresent(""))
    }

    @Test
    fun `password strength grows with variety and length`() {
        assertEquals(0, Validation.passwordStrength(""))
        assertEquals(1, Validation.passwordStrength("abc"))
        assertEquals(1, Validation.passwordStrength("abcdefgh"))
        assertEquals(2, Validation.passwordStrength("abcdefgh12"))
        assertEquals(4, Validation.passwordStrength("Study#Nest2026"))
    }

    @Test
    fun `subject title and notes rules`() {
        assertEquals("Give the subject a name", Validation.subjectName(" "))
        assertEquals("Keep it under 40 characters", Validation.subjectName("x".repeat(41)))
        assertNull(Validation.subjectName("Physics"))
        assertEquals("Give it a title", Validation.title(""))
        assertNull(Validation.title("Essay"))
        assertEquals("Notes can be up to 500 characters", Validation.notes("x".repeat(501)))
    }

    @Test
    fun `minutes parsing for goals and custom timers`() {
        assertEquals(90, Validation.minutes(" 90 ", 5, 720))
        assertNull(Validation.minutes("4", 5, 720))
        assertNull(Validation.minutes("1.5", 1, 240))
        assertNull(Validation.minutes("-3", 1, 240))
        assertNull(Validation.minutes("99999", 1, 240))
        assertNull(Validation.dailyGoal("120"))
        assertEquals("Choose between 5 and 720 minutes", Validation.dailyGoal("721"))
        assertNull(Validation.customFocus("1"))
        assertEquals("Enter a number of minutes", Validation.customFocus(""))
    }
}

class DatesTest {

    private val wednesday = LocalDate.of(2026, 10, 14)
    private val subject = SubjectRef("s", "Maths", SubjectColor.Sky, SubjectIcon.Calculator)

    private fun assignment(due: LocalDate, done: LocalDate? = null, id: String = due.toString()) =
        Assignment(id, "T", "", due, Priority.Medium, done, subject)

    @Test
    fun `weeks start on monday`() {
        assertEquals(LocalDate.of(2026, 10, 12), Weeks.startOf(wednesday))
        assertEquals(LocalDate.of(2026, 10, 12), Weeks.startOf(LocalDate.of(2026, 10, 12)))
        assertEquals(LocalDate.of(2026, 10, 12), Weeks.startOf(LocalDate.of(2026, 10, 18)))
        assertEquals(LocalDate.of(2026, 10, 18), Weeks.endOf(wednesday))
        assertEquals(LocalDate.of(2025, 12, 29), Weeks.startOf(LocalDate.of(2026, 1, 1)))
        assertEquals(7, Weeks.days(LocalDate.of(2026, 10, 12)).size)
    }

    @Test
    fun `week labels handle month and year boundaries`() {
        assertEquals("12 – 18 Oct", Weeks.rangeLabel(LocalDate.of(2026, 10, 12), Locale.UK))
        assertEquals("28 Sept – 4 Oct", Weeks.rangeLabel(LocalDate.of(2026, 9, 28), Locale.UK))
        assertEquals("29 Dec 2025 – 4 Jan 2026", Weeks.rangeLabel(LocalDate.of(2025, 12, 29), Locale.UK))
    }

    @Test
    fun `days meeting the goal`() {
        assertEquals(2, Weeks.daysMeetingGoal(listOf(3600, 1800, 5400, 0), 60))
        assertEquals(0, Weeks.daysMeetingGoal(listOf(3600), 0))
    }

    @Test
    fun `assignments fall into due buckets`() {
        assertEquals(DueBucket.Overdue, Due.bucket(assignment(wednesday.minusDays(1)), wednesday))
        assertEquals(DueBucket.Today, Due.bucket(assignment(wednesday), wednesday))
        assertEquals(DueBucket.ThisWeek, Due.bucket(assignment(LocalDate.of(2026, 10, 18)), wednesday))
        assertEquals(DueBucket.Later, Due.bucket(assignment(LocalDate.of(2026, 10, 19)), wednesday))
        assertEquals(DueBucket.Done, Due.bucket(assignment(wednesday.minusDays(3), done = wednesday), wednesday))
        assertTrue(Due.isOverdue(assignment(wednesday.minusDays(1)), wednesday))
        assertFalse(Due.isOverdue(assignment(wednesday.minusDays(1), done = wednesday), wednesday))
    }

    @Test
    fun `grouping keeps display order and skips empty buckets`() {
        val items = listOf(
            assignment(LocalDate.of(2026, 10, 30), id = "later"),
            assignment(wednesday.minusDays(2), id = "late"),
            assignment(wednesday, done = wednesday, id = "done"),
            assignment(wednesday, id = "today"),
        )
        val groups = Due.group(items, wednesday)
        assertEquals(listOf(DueBucket.Overdue, DueBucket.Today, DueBucket.Later, DueBucket.Done), groups.map { it.first })
        assertEquals(listOf("today"), groups[1].second.map { it.id })
    }

    @Test
    fun `due labels`() {
        val uk = Locale.UK
        assertEquals("3 days late", Due.label(wednesday.minusDays(3), wednesday, uk))
        assertEquals("Yesterday", Due.label(wednesday.minusDays(1), wednesday, uk))
        assertEquals("Today", Due.label(wednesday, wednesday, uk))
        assertEquals("Tomorrow", Due.label(wednesday.plusDays(1), wednesday, uk))
        assertEquals("Sat", Due.label(wednesday.plusDays(3), wednesday, uk))
        assertEquals("30 Oct", Due.label(LocalDate.of(2026, 10, 30), wednesday, uk))
        assertEquals("5 Jan 2027", Due.label(LocalDate.of(2027, 1, 5), wednesday, uk))
    }

    @Test
    fun `day parts`() {
        assertEquals(DayPart.Morning, dayPart(LocalTime.of(7, 0)))
        assertEquals(DayPart.Afternoon, dayPart(LocalTime.of(13, 0)))
        assertEquals(DayPart.Evening, dayPart(LocalTime.of(22, 0)))
        assertEquals(DayPart.Evening, dayPart(LocalTime.of(2, 0)))
    }
}
