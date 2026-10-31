package me.aysh.studynest.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.ui.components.DueTone
import me.aysh.studynest.ui.components.NestButton
import me.aysh.studynest.ui.components.NestButtonStyle
import me.aysh.studynest.ui.components.NestTextButton
import me.aysh.studynest.ui.components.PriorityIndicator
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.components.dueTone
import me.aysh.studynest.ui.components.exposeTestTags
import me.aysh.studynest.ui.components.subjectIconVector
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.Pastel
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Due
import me.aysh.studynest.util.longDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Small label above an editor section, aligned with NestTextField's own label. */
@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    val c = StudyNestTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (isError) c.overdue else c.ink,
        modifier = modifier.padding(start = 6.dp, bottom = 10.dp),
    )
}

/** Coral inline error with an icon, matching NestTextField's error line. */
@Composable
internal fun FieldError(message: String, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    Row(modifier.padding(start = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = c.overdue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = c.overdue)
    }
}

// ---- Subject picker -------------------------------------------------------------------------------

/** Wrapping row of subject pills in their own pastels. Each pill is tagged `editor_subject_<id>`. */
@Composable
internal fun SubjectPicker(
    subjects: List<Subject>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subjects.forEach { subject ->
            SubjectPill(
                subject = subject,
                selected = subject.id == selectedId,
                enabled = enabled,
                onClick = { onSelect(subject.id) },
            )
        }
    }
}

@Composable
private fun SubjectPill(subject: Subject, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = StudyNestTheme.colors
    val pastel = c.subject(subject.color)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(0.45f, 700f), label = "pillPress")
    val fill by animateColorAsState(if (selected) pastel.fill else c.surfaceRaised, label = "pillFill")
    val border by animateColorAsState(if (selected) pastel.ink.copy(alpha = 0.6f) else c.hairline, label = "pillBorder")
    val text by animateColorAsState(if (selected) c.onPastel else c.ink, label = "pillText")
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("editor_subject_${subject.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .graphicsLayer {
                    val s = 1f - 0.05f * press
                    scaleX = s
                    scaleY = s
                }
                .clip(NestShapes.Pill)
                .background(fill)
                .border(if (selected) 2.dp else 1.5.dp, border, NestShapes.Pill)
                .padding(start = 6.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(NestShapes.Pill)
                    .background(if (selected) Color.White.copy(alpha = 0.6f) else pastel.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(subjectIconVector(subject.icon), contentDescription = null, tint = pastel.ink, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(subject.name, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = pastel.ink,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

/** Shown instead of the picker when the student has no subjects yet. */
@Composable
internal fun NeedsSubjectCard(onAddSubject: () -> Unit) {
    val c = StudyNestTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(NestShapes.Block)
            .background(c.butter.fill)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(NestShapes.Stamp)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = c.butter.ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.planner_editor_needs_subject_title),
                style = MaterialTheme.typography.titleMedium,
                color = c.onPastel,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.planner_editor_needs_subject_body),
            style = MaterialTheme.typography.bodyMedium,
            color = c.onPastel,
        )
        Spacer(Modifier.height(14.dp))
        NestButton(
            text = stringResource(R.string.planner_editor_add_subject),
            onClick = onAddSubject,
            style = NestButtonStyle.Secondary,
            leadingIcon = Icons.Rounded.Add,
            large = false,
            modifier = Modifier.testTag("editor_add_subject"),
        )
    }
}

// ---- Due date ------------------------------------------------------------------------------------

/** Tappable due-date block with a tear-off calendar stamp. Tagged `editor_due_date`. */
@Composable
internal fun DueDateRow(
    dueOn: LocalDate,
    today: LocalDate,
    accent: Pastel,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val tone = dueTone(dueOn, today, done = false)
    val relative = Due.label(dueOn, today)
    SquishySurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_due_date"),
        fill = c.surfaceRaised,
        lip = c.surfaceLip,
        shape = NestShapes.Block,
        border = BorderStroke(1.5.dp, c.hairline),
        lipHeight = 3.dp,
        onClickLabel = stringResource(R.string.planner_editor_due_change),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarStamp(dueOn, accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(longDate(dueOn), style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    relative,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tone == DueTone.Overdue) c.overdue else c.inkMuted,
                )
            }
            Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = c.primaryText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CalendarStamp(date: LocalDate, accent: Pastel) {
    val c = StudyNestTheme.colors
    val month = date.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())).uppercase(Locale.getDefault())
    Box(Modifier.size(width = 50.dp, height = 56.dp).clearAndSetSemantics { }) {
        Box(
            Modifier
                .offset(y = 3.dp)
                .size(width = 50.dp, height = 53.dp)
                .clip(RoundedStamp)
                .background(accent.lip),
        )
        Column(
            Modifier
                .size(width = 50.dp, height = 53.dp)
                .clip(RoundedStamp)
                .background(c.surfaceSunken),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(accent.fill),
                contentAlignment = Alignment.Center,
            ) {
                Text(month, style = MaterialTheme.typography.labelSmall, color = c.onPastel)
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    color = c.ink,
                )
            }
        }
    }
}

private val RoundedStamp = RoundedCornerShape(14.dp)

/**
 * Material3 date picker in StudyNest colours. Dates cross the picker as UTC-midnight millis, so the
 * conversion never shifts a day in any time zone. Content exposes test tags (`date_confirm`, `date_cancel`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDatePickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = StudyNestTheme.colors
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    val colors = pickerColors()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags(),
        shape = NestShapes.Dialog,
        colors = colors,
        confirmButton = {
            NestTextButton(
                text = stringResource(R.string.planner_date_confirm),
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("date_confirm"),
            )
        },
        dismissButton = {
            NestTextButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                color = c.inkMuted,
                modifier = Modifier.testTag("date_cancel"),
            )
        },
    ) {
        DatePicker(state = pickerState, colors = colors)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pickerColors(): DatePickerColors {
    val c = StudyNestTheme.colors
    return DatePickerDefaults.colors(
        containerColor = c.surface,
        titleContentColor = c.inkMuted,
        headlineContentColor = c.ink,
        weekdayContentColor = c.inkMuted,
        subheadContentColor = c.inkMuted,
        navigationContentColor = c.ink,
        yearContentColor = c.ink,
        disabledYearContentColor = c.inkMuted.copy(alpha = 0.4f),
        currentYearContentColor = c.primaryText,
        selectedYearContentColor = c.onPrimary,
        selectedYearContainerColor = c.primary,
        dayContentColor = c.ink,
        disabledDayContentColor = c.inkMuted.copy(alpha = 0.4f),
        selectedDayContentColor = c.onPrimary,
        selectedDayContainerColor = c.primary,
        todayContentColor = c.primaryText,
        todayDateBorderColor = c.primary,
        dayInSelectionRangeContentColor = c.onPastel,
        dayInSelectionRangeContainerColor = c.lilac.fill,
        dividerColor = c.hairline,
    )
}

// ---- Priority -------------------------------------------------------------------------------------

/**
 * Three-way segmented control with a sliding, springy thumb. Each segment is a radio button tagged
 * `editor_priority_low|medium|high` and shows the priority's bar signal next to its label.
 */
@Composable
internal fun PrioritySegmented(value: Priority, enabled: Boolean, onChange: (Priority) -> Unit) {
    val c = StudyNestTheme.colors
    val index = Priority.entries.indexOf(value)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(NestShapes.Pill)
            .background(c.surfaceSunken)
            .border(1.5.dp, c.hairline, NestShapes.Pill)
            .padding(5.dp),
    ) {
        val segment = maxWidth / 3
        val offset by animateDpAsState(segment * index, spring(dampingRatio = 0.68f, stiffness = 420f), label = "thumb")
        // Thumb with a little lip.
        Box(
            Modifier
                .offset(x = offset)
                .size(width = segment, height = maxHeight),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 3.dp)
                    .clip(NestShapes.Pill)
                    .background(c.surfaceLip),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 3.dp)
                    .clip(NestShapes.Pill)
                    .background(c.surfaceRaised),
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            Priority.entries.forEach { priority ->
                val selected = priority == value
                val label = stringResource(
                    when (priority) {
                        Priority.Low -> R.string.priority_low
                        Priority.Medium -> R.string.priority_medium
                        Priority.High -> R.string.priority_high
                    },
                )
                val description = stringResource(
                    when (priority) {
                        Priority.Low -> R.string.priority_low_description
                        Priority.Medium -> R.string.priority_medium_description
                        Priority.High -> R.string.priority_high_description
                    },
                )
                val text by animateColorAsState(if (selected) c.ink else c.inkMuted, label = "segText")
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(NestShapes.Pill)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onChange(priority) },
                        )
                        .testTag("editor_priority_${priority.key}")
                        .semantics(mergeDescendants = true) { contentDescription = description },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PriorityIndicator(priority)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1)
                }
            }
        }
    }
}
