package me.aysh.studynest.data.remote

import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.data.model.AssignmentDraft
import me.aysh.studynest.data.model.DayFocus
import me.aysh.studynest.data.model.FocusSession
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.Profile
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectDraft
import me.aysh.studynest.data.model.SubjectFocus
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.model.SubjectRef
import me.aysh.studynest.data.model.WeekInsights
import java.time.Instant
import java.time.LocalDate

internal fun SubjectRefDto.toModel() =
    SubjectRef(id, name, SubjectColor.fromKey(color), SubjectIcon.fromKey(icon))

internal fun SubjectDto.toModel() = Subject(
    id = id,
    name = name,
    color = SubjectColor.fromKey(color),
    icon = SubjectIcon.fromKey(icon),
    openCount = openCount,
    totalCount = totalCount,
)

internal fun SubjectDraft.toRequest() = SubjectRequest(name = name.trim(), color = color.key, icon = icon.key)

internal fun AssignmentDto.toModel() = Assignment(
    id = id,
    title = title,
    notes = notes,
    dueOn = LocalDate.parse(dueOn),
    priority = Priority.fromKey(priority),
    completedOn = completedOn?.let(LocalDate::parse),
    subject = subject.toModel(),
)

internal fun AssignmentDraft.toRequest() = AssignmentRequest(
    title = title.trim(),
    subjectId = subjectId,
    dueOn = dueOn.toString(),
    priority = priority.key,
    notes = notes.trim(),
)

internal fun FocusSessionDto.toModel() = FocusSession(
    id = id,
    subject = subject?.toModel(),
    startedAt = Instant.parse(startedAt),
    durationSeconds = durationSeconds,
    day = LocalDate.parse(day),
)

internal fun InsightsDto.toModel() = WeekInsights(
    weekStart = LocalDate.parse(weekStart),
    totalSeconds = totalSeconds,
    sessionCount = sessionCount,
    days = days.map { DayFocus(LocalDate.parse(it.date), it.seconds) },
    bySubject = bySubject.map { SubjectFocus(it.subject?.toModel(), it.seconds) },
    assignmentsCompleted = assignmentsCompleted,
    currentStreak = streak.current,
    longestStreak = streak.longest,
    dailyGoalMinutes = dailyGoalMinutes,
)

internal fun ProfileDto.toModel() = Profile(
    id = id,
    name = name,
    email = email,
    dailyGoalMinutes = dailyFocusGoalMinutes,
    memberSince = Instant.parse(createdAt),
)
