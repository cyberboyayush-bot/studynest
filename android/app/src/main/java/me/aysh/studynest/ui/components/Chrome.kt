package me.aysh.studynest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.aysh.studynest.R
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * Exposes test tags as resource ids for this subtree. REQUIRED as the outermost content modifier inside every
 * separate window (Dialog, AlertDialog, DatePickerDialog `modifier`, ModalBottomSheet content) — the root flag
 * set in MainActivity does not cross window boundaries. [NestDialog]/[NestAlertDialog]/[NestBottomSheet] apply it.
 */
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }

/**
 * StudyNest top bar (not Material TopAppBar): status-bar inset, 64dp row with optional [navigation] slot
 * (use [NestIconButton]), a Fredoka [title] (+ optional muted [subtitle]) and trailing [actions].
 * Pass `windowInsets = WindowInsets(0)` when the parent already handles the status bar.
 */
@Composable
fun NestTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = StudyNestTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigation != null) {
            navigation()
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The app's snackbar look: an ink "sticker" pill with cream text, a little sparkle dot and the action in
 * butter (light) / lilac (dark). Render it with `NestSnackbarHost(LocalAppMessenger.current.host)`.
 */
@Composable
fun NestSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> NestSnackbar(data) }
}

@Composable
private fun NestSnackbar(data: SnackbarData) {
    val c = StudyNestTheme.colors
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .clip(NestShapes.Block)
                .background(if (c.isDark) c.lilac.lip else c.primaryLip),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(NestShapes.Block)
                .background(c.inverse)
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(NestShapes.Pill)
                    .background(c.inverseAccent),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = c.onInverse,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
            val action = data.visuals.actionLabel
            if (action != null) {
                NestTextButton(
                    text = action,
                    onClick = { data.performAction() },
                    color = c.inverseAccent,
                    modifier = Modifier.testTag("snackbar_action"),
                )
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

/**
 * Generic StudyNest dialog window: rounded warm surface with a lip, content wrapped in [exposeTestTags].
 * [modifier] applies to the content column inside the window (put a test tag here if needed).
 */
@Composable
fun NestDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = StudyNestTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
    ) {
        Box(Modifier.exposeTestTags().padding(horizontal = 8.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 5.dp)
                    .clip(NestShapes.Dialog)
                    .background(c.surfaceLip),
            )
            Column(
                modifier
                    .fillMaxWidth()
                    .clip(NestShapes.Dialog)
                    .background(c.surface)
                    .border(1.dp, c.hairline, NestShapes.Dialog)
                    .padding(24.dp),
                content = content,
            )
        }
    }
}

/**
 * Confirm dialog (replace Material AlertDialog with this). Confirm is a squishy button ([destructive] makes
 * it coral); Cancel is a text button tagged [dismissTag] (default "dialog_cancel"). While [loading] the confirm
 * button spins and the dialog can't be dismissed.
 */
@Composable
fun NestAlertDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmTag: String,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(R.string.action_cancel),
    dismissTag: String = "dialog_cancel",
    destructive: Boolean = false,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val c = StudyNestTheme.colors
    NestDialog(onDismissRequest = { if (!loading) onDismiss() }, modifier = modifier, dismissible = !loading) {
        if (icon != null) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(NestShapes.Stamp)
                    .background(if (destructive) c.overdueFill else c.lilac.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (destructive) c.overdue else c.lilac.ink, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = c.ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.inkMuted)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NestTextButton(
                text = dismissLabel,
                onClick = onDismiss,
                enabled = !loading,
                color = c.inkMuted,
                modifier = Modifier.testTag(dismissTag),
            )
            NestButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) NestButtonStyle.Danger else NestButtonStyle.Primary,
                loading = loading,
                large = false,
                modifier = Modifier.testTag(confirmTag),
            )
        }
    }
}

/**
 * StudyNest bottom sheet: 32dp top corners, warm surface, chunky drag handle, content padded 24dp and wrapped
 * in [exposeTestTags]. [modifier] lands on the content column INSIDE the sheet window — put the sheet's test tag
 * there (e.g. "subject_sheet"). Opens fully expanded by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = StudyNestTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = NestShapes.Sheet,
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.inverse.copy(alpha = if (c.isDark) 0.6f else 0.32f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 6.dp)
                    .clip(NestShapes.Pill)
                    .background(c.hairline),
            )
        },
    ) {
        Column(
            modifier
                .exposeTestTags()
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.ink,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 20.dp)
                        .semantics { heading() },
                )
            }
            content()
        }
    }
}
