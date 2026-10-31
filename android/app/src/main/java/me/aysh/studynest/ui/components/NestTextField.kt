package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * StudyNest text input: Figtree label above a chunky rounded field, optional leading icon and trailing slot,
 * focus ring in primary, coral treatment + error message (a real [Text] node, with an icon) below.
 *
 * IMPORTANT: [modifier] is applied to the editable node itself — put `testTag` / `semantics { contentType = … }`
 * there. The surrounding Column (label + field + message) always fills the available width.
 *
 * [supportingText] shows when there's no [error]; [counter] (e.g. "12/500") is right-aligned on the same line.
 */
@Composable
fun NestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    error: String? = null,
    supportingText: String? = null,
    counter: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val c = StudyNestTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val isError = error != null

    val borderColor by animateColorAsState(
        when {
            isError -> c.overdueAccent
            focused -> c.primary
            else -> c.hairline
        },
        label = "fieldBorder",
    )
    val borderWidth by animateDpAsState(if (focused || isError) 2.dp else 1.5.dp, label = "fieldBorderWidth")
    val fill by animateColorAsState(
        when {
            !enabled -> c.surfaceSunken
            isError -> c.overdueFill.copy(alpha = if (c.isDark) 0.55f else 0.6f)
            else -> c.surfaceRaised
        },
        label = "fieldFill",
    )
    val iconTint by animateColorAsState(
        when {
            isError -> c.overdue
            focused -> c.primaryText
            else -> c.inkMuted
        },
        label = "fieldIcon",
    )
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) c.ink else c.inkMuted)

    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) c.overdue else c.ink,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .semantics { if (error != null) error(error) },
            enabled = enabled,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            interactionSource = source,
            cursorBrush = SolidColor(c.primary),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(NestShapes.Field)
                        .background(fill)
                        .border(borderWidth, borderColor, NestShapes.Field)
                        .padding(start = if (leadingIcon != null) 14.dp else 18.dp, end = if (trailing != null) 4.dp else 18.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .padding(top = if (singleLine) 0.dp else 16.dp)
                                .size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = c.inkMuted.copy(alpha = 0.75f), maxLines = maxLines)
                        }
                        inner()
                    }
                    if (trailing != null) trailing()
                }
            },
        )
        val message = error ?: supportingText
        AnimatedVisibility(
            visible = message != null || counter != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (error != null) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = c.overdue, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) c.overdue else c.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                if (counter != null) {
                    Text(counter, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                }
            }
        }
    }
}
