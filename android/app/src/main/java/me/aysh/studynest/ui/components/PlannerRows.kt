package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Due
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How urgent a due date is, which decides the [PlannerDuePill] colours. */
enum class DueTone { Overdue, Today, Tomorrow, Soon, Later, Done }

/** Tone for an assignment due on [dueOn], seen from [today]. */
fun dueTone(dueOn: LocalDate, today: LocalDate, done: Boolean): DueTone {
    if (done) return DueTone.Done
    val days = ChronoUnit.DAYS.between(today, dueOn)
    return when {
        days < 0 -> DueTone.Overdue
        days == 0L -> DueTone.Today
        days == 1L -> DueTone.Tomorrow
        days < 7 -> DueTone.Soon
        else -> DueTone.Later
    }
}

/** A tiny playful tilt (-3..3 degrees) derived from an id, so stamps feel hand-placed but stay stable. */
fun plannerTilt(id: String): Float = (Math.floorMod(id.hashCode(), 5) - 2) * 1.5f

/**
 * Small pill carrying a due label ("2 days late", "Today", "Fri"). Overdue is a solid coral pill; Today is
 * butter, Tomorrow peach, everything else a quiet sunken pill. Pastels always carry dark ink.
 */
@Composable
fun PlannerDuePill(label: String, tone: DueTone, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val (fill, ink) = when (tone) {
        DueTone.Overdue -> c.overdue to (if (c.isDark) c.onPastel else Color.White)
        DueTone.Today -> c.butter.fill to c.onPastel
        DueTone.Tomorrow -> c.peach.fill to c.onPastel
        DueTone.Soon, DueTone.Later -> c.surfaceSunken to c.ink
        DueTone.Done -> c.successFill to c.success
    }
    Row(
        modifier
            .heightIn(min = 26.dp)
            .clip(NestShapes.Pill)
            .background(fill)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (tone == DueTone.Done) Icon(Icons.Rounded.Check, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1)
    }
}

/**
 * The planner's assignment row: a sticker-like block with the subject stamp, title, subject name, a due pill
 * and the priority signal. Overdue rows turn coral; done rows are struck through and quieter.
 *
 * [modifier] lands on the clickable node (put `testTag` there). [onToggleDone] adds an accessibility action
 * so screen-reader users can complete a row without swiping.
 */
@Composable
fun PlannerAssignmentRow(
    assignment: Assignment,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDone: (() -> Unit)? = null,
) {
    val c = StudyNestTheme.colors
    val done = assignment.isDone
    val tone = dueTone(assignment.dueOn, today, done)
    val overdue = tone == DueTone.Overdue
    val dueLabel = if (done) stringResource(R.string.planner_row_done) else Due.label(assignment.dueOn, today)
    val toggleLabel = stringResource(if (done) R.string.planner_swipe_reopen else R.string.planner_swipe_mark_done)
    val a11y = if (onToggleDone != null) {
        Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(toggleLabel) { onToggleDone(); true })
        }
    } else Modifier

    SquishySurface(
        onClick = onClick,
        modifier = modifier.then(a11y),
        fill = when {
            overdue -> c.overdueFill
            done -> c.surfaceSunken
            else -> c.surface
        },
        lip = when {
            overdue -> c.overdueAccent.copy(alpha = if (c.isDark) 0.45f else 0.38f)
            else -> c.surfaceLip
        },
        shape = NestShapes.Block,
        border = if (overdue || done) null else BorderStroke(1.dp, c.hairline),
        lipHeight = 3.dp,
        onClickLabel = stringResource(R.string.planner_action_open),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubjectStamp(
                color = assignment.subject.color,
                icon = assignment.subject.icon,
                size = 44.dp,
                tilt = plannerTilt(assignment.id),
                modifier = Modifier.alpha(if (done) 0.55f else 1f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    assignment.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (done) c.inkMuted else c.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    assignment.subject.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) c.overdue else c.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlannerDuePill(dueLabel, tone)
                if (!done) PriorityIndicator(assignment.priority)
            }
        }
    }
}

/**
 * Pill-shaped search input with a leading magnifier and an animated clear button. [modifier] goes on the
 * editable node (put the test tag there).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = StudyNestTheme.colors
    val focusManager = LocalFocusManager.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) c.primary else c.hairline, label = "searchBorder")
    val borderWidth by animateDpAsState(if (focused) 2.dp else 1.5.dp, label = "searchBorderWidth")
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink)
    // Hiding the keyboard (Back) also ends the search edit, so no stray cursor keeps blinking.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) { if (!imeVisible && focused) focusManager.clearFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(c.primary),
        interactionSource = source,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(NestShapes.Pill)
                    .background(c.surfaceRaised)
                    .border(borderWidth, border, NestShapes.Pill)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (focused) c.primaryText else c.inkMuted,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = textStyle, color = c.inkMuted.copy(alpha = 0.8f), maxLines = 1)
                    }
                    inner()
                }
                AnimatedVisibility(
                    visible = value.isNotEmpty(),
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                ) {
                    NestIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.planner_search_clear),
                        onClick = { onValueChange("") },
                        filled = false,
                        tint = c.inkMuted,
                        size = 44.dp,
                    )
                }
                if (value.isEmpty()) Spacer(Modifier.width(12.dp))
            }
        },
    )
}
