package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * Multi-select filter chip (checkbox semantics). Selected = solid ink "sticker" pill with a check;
 * unselected = warm-white pill with hairline border. Optional [dotColor] shows a subject colour dot.
 * Put `testTag` on [modifier].
 */
@Composable
fun NestFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    leadingIcon: ImageVector? = null,
    showCheck: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    ChipFace(
        selected = selected,
        label = label,
        dotColor = dotColor,
        leadingIcon = leadingIcon,
        showCheck = showCheck,
        source = source,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = selected, interactionSource = source, indication = null, role = Role.Checkbox) { onClick() },
    )
}

/**
 * Single-select choice chip (radio semantics) — e.g. durations, subject pickers. Same look as
 * [NestFilterChip] but without the check by default. Put `testTag` on [modifier].
 */
@Composable
fun NestChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    leadingIcon: ImageVector? = null,
    showCheck: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    ChipFace(
        selected = selected,
        label = label,
        dotColor = dotColor,
        leadingIcon = leadingIcon,
        showCheck = showCheck,
        source = source,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun ChipFace(
    selected: Boolean,
    label: String,
    dotColor: Color?,
    leadingIcon: ImageVector?,
    showCheck: Boolean,
    source: MutableInteractionSource,
    modifier: Modifier,
) {
    val c = StudyNestTheme.colors
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(0.45f, 700f), label = "chipPress")
    val fill by animateColorAsState(if (selected) c.inverse else c.surfaceRaised, label = "chipFill")
    val content by animateColorAsState(if (selected) c.onInverse else c.ink, label = "chipContent")
    val borderColor = when {
        focused -> c.primary
        selected -> c.inverse
        else -> c.hairline
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .heightIn(min = 40.dp)
                .graphicsLayer {
                    val s = 1f - 0.05f * press
                    scaleX = s
                    scaleY = s
                }
                .clip(NestShapes.Pill)
                .background(fill)
                .border(if (focused) 2.5.dp else 1.5.dp, borderColor, NestShapes.Pill)
                .padding(start = if (dotColor != null || leadingIcon != null || (showCheck && selected)) 12.dp else 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(
                visible = showCheck && selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
            if (dotColor != null) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(NestShapes.Pill)
                        .background(dotColor)
                        .border(1.5.dp, if (selected) c.onInverse.copy(alpha = 0.5f) else c.ink.copy(alpha = 0.12f), NestShapes.Pill),
                )
            }
            if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}
