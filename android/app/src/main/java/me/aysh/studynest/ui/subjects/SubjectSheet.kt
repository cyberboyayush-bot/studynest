package me.aysh.studynest.ui.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.ui.components.BannerTone
import me.aysh.studynest.ui.components.NestBanner
import me.aysh.studynest.ui.components.NestBottomSheet
import me.aysh.studynest.ui.components.NestButton
import me.aysh.studynest.ui.components.NestButtonStyle
import me.aysh.studynest.ui.components.NestTextField
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.components.subjectIconVector
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Validation

/** Add / edit subject bottom sheet with a live preview tile. Tagged `subject_sheet`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubjectEditorSheet(
    editor: SubjectEditorState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (SubjectColor) -> Unit,
    onIconChange: (SubjectIcon) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onAddAssignment: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val focusManager = LocalFocusManager.current
    NestBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("subject_sheet"),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(if (editor.isNew) R.string.planner_subject_sheet_new else R.string.planner_subject_sheet_edit),
                style = MaterialTheme.typography.headlineSmall,
                color = c.ink,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            // Live preview
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Sparkle(c.subject(editor.color).lip, Modifier.size(22.dp).offset(x = (-104).dp, y = (-50).dp), delayMillis = 100)
                Sparkle(c.butter.lip, Modifier.size(14.dp).offset(x = 100.dp, y = 56.dp), delayMillis = 700)
                SubjectTile(
                    name = editor.name.trim().ifEmpty { stringResource(R.string.planner_subject_preview_name) },
                    color = editor.color,
                    icon = editor.icon,
                    caption = null,
                    onClick = null,
                    modifier = Modifier
                        .size(width = 156.dp, height = 156.dp)
                        .rotate(-3f),
                )
            }
            Spacer(Modifier.height(12.dp))

            NestTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.planner_subject_name),
                placeholder = stringResource(R.string.planner_subject_name_placeholder),
                error = editor.nameError,
                counter = "${editor.name.length}/${Validation.MAX_SUBJECT_NAME}",
                enabled = !editor.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.testTag("subject_name"),
            )

            SheetLabel(stringResource(R.string.planner_subject_colour))
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SubjectColor.entries.forEach { color ->
                    ColorSwatch(color = color, selected = editor.color == color, onClick = { onColorChange(color) })
                }
            }

            SheetLabel(stringResource(R.string.planner_subject_icon))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectIcon.entries.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { icon ->
                            IconCell(
                                icon = icon,
                                color = editor.color,
                                selected = editor.icon == icon,
                                onClick = { onIconChange(icon) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(editor.error != null) {
                NestBanner(
                    message = editor.error.orEmpty(),
                    tone = BannerTone.Error,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = stringResource(if (editor.isNew) R.string.planner_subject_save_new else R.string.planner_subject_save_edit),
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                loading = editor.isSaving,
                leadingIcon = if (editor.isNew) Icons.Rounded.Add else Icons.Rounded.Check,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_save"),
            )
            if (!editor.isNew) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NestButton(
                        text = stringResource(R.string.planner_subject_add_assignment),
                        onClick = onAddAssignment,
                        style = NestButtonStyle.Tonal,
                        leadingIcon = Icons.Rounded.Add,
                        enabled = !editor.isSaving,
                        large = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_add_assignment"),
                    )
                    NestButton(
                        text = stringResource(R.string.planner_subject_delete),
                        onClick = onDelete,
                        style = NestButtonStyle.Danger,
                        leadingIcon = Icons.Rounded.DeleteOutline,
                        enabled = !editor.isSaving,
                        large = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_delete"),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = StudyNestTheme.colors.ink,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
private fun ColorSwatch(color: SubjectColor, selected: Boolean, onClick: () -> Unit) {
    val c = StudyNestTheme.colors
    val pastel = c.subject(color)
    val name = colorName(color)
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, spring(0.5f, 500f), label = "swatchRing")
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, spring(0.45f, 500f), label = "swatchScale")
    Box(
        Modifier
            .size(52.dp)
            .clip(NestShapes.Pill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("subject_color_${color.key}")
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(50.dp)
                .border(ring, c.ink, NestShapes.Pill),
        )
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(NestShapes.Pill)
                .background(pastel.fill)
                .border(2.dp, pastel.lip, NestShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(selected, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = pastel.ink, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun IconCell(icon: SubjectIcon, color: SubjectColor, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val pastel = c.subject(color)
    val name = iconName(icon)
    val fill by animateColorAsState(if (selected) pastel.fill else c.surfaceSunken, label = "iconFill")
    val tint by animateColorAsState(if (selected) pastel.ink else c.inkMuted, label = "iconTint")
    val border by animateColorAsState(if (selected) pastel.ink.copy(alpha = 0.55f) else c.hairline.copy(alpha = 0f), label = "iconBorder")
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, spring(0.4f, 500f), label = "iconScale")
    Box(
        modifier
            .widthIn(max = 64.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(NestShapes.Stamp)
            .background(fill)
            .border(2.dp, border, NestShapes.Stamp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("subject_icon_${icon.key}")
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Icon(subjectIconVector(icon), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}
