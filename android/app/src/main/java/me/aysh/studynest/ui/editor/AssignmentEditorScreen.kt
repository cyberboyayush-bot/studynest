package me.aysh.studynest.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.StudyNestApp
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.BannerTone
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.LoadingSkeleton
import me.aysh.studynest.ui.components.NestAlertDialog
import me.aysh.studynest.ui.components.NestBanner
import me.aysh.studynest.ui.components.NestIconButton
import me.aysh.studynest.ui.components.NestSnackbarHost
import me.aysh.studynest.ui.components.NestTextField
import me.aysh.studynest.ui.components.NestTopBar
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.navigation.LocalAppMessenger
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Validation
import java.time.LocalDate

@Composable
fun AssignmentEditorScreen(assignmentId: String?, presetSubjectId: String?, onClose: () -> Unit, onOpenSubjects: () -> Unit) {
    val vm = appViewModel(key = "editor-${assignmentId ?: "new"}") { c ->
        AssignmentEditorViewModel(assignmentId, presetSubjectId, c.studyRepository, c.clock)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val messenger = LocalAppMessenger.current
    val clock = (LocalContext.current.applicationContext as StudyNestApp).container.clock
    val today = remember(clock) { LocalDate.now(clock) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is EditorEvent.Closed -> {
                    messenger.show(event.message)
                    onClose()
                }
            }
        }
    }

    // Leaving mid-write would cancel the request after the server may already have applied it.
    val busy = state.isSaving || state.isDeleting
    BackHandler(enabled = busy) {}

    EditorContent(
        state = state,
        today = today,
        onClose = onClose,
        onOpenSubjects = onOpenSubjects,
        onRetry = vm::retry,
        onTitleChange = vm::onTitleChange,
        onSubjectChange = vm::onSubjectChange,
        onDueDateChange = vm::onDueDateChange,
        onPriorityChange = vm::onPriorityChange,
        onNotesChange = vm::onNotesChange,
        onSave = vm::save,
        onRequestDelete = vm::requestDelete,
    )

    if (state.confirmDelete) {
        val title = state.title.trim()
        NestAlertDialog(
            title = stringResource(R.string.planner_editor_delete_title),
            message = if (title.isEmpty()) {
                stringResource(R.string.planner_editor_delete_message_untitled)
            } else {
                stringResource(R.string.planner_editor_delete_message, title)
            },
            confirmLabel = stringResource(R.string.planner_editor_delete_confirm),
            onConfirm = vm::confirmDelete,
            onDismiss = vm::cancelDelete,
            confirmTag = "confirm_delete",
            destructive = true,
            icon = Icons.Rounded.DeleteOutline,
        )
    }
}

@Composable
private fun EditorContent(
    state: AssignmentEditorUiState,
    today: LocalDate,
    onClose: () -> Unit,
    onOpenSubjects: () -> Unit,
    onRetry: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onDueDateChange: (LocalDate) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val focusManager = LocalFocusManager.current
    val subject = state.subjects.firstOrNull { it.id == state.subjectId }
    val accent = subject?.let { c.subject(it.color) } ?: c.lilac
    val wash by animateColorAsState(c.wash(accent.fill, 0.75f), tween(500), label = "editorWash")
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val ready = !state.isLoading && state.loadError == null

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("editor_screen"),
    ) {
        // The corner wash follows the chosen subject's colour.
        SoftBlob(wash, Modifier.size(300.dp).offset(x = maxWidth - 170.dp, y = (-150).dp), seed = 6)

        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            NestTopBar(
                title = stringResource(if (state.isNew) R.string.planner_editor_new else R.string.planner_editor_edit),
                navigation = {
                    NestIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        onClick = onClose,
                        enabled = !state.isSaving && !state.isDeleting,
                        modifier = Modifier.testTag("editor_close"),
                    )
                },
                actions = {
                    if (!state.isNew) {
                        if (state.isDeleting) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = c.overdue,
                                    trackColor = c.overdueFill,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        } else {
                            NestIconButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.planner_editor_delete),
                                onClick = onRequestDelete,
                                enabled = state.canEdit,
                                tint = c.overdue,
                                container = c.overdueFill,
                                modifier = Modifier.testTag("editor_delete"),
                            )
                        }
                    }
                },
            )

            AnimatedContent(
                targetState = when {
                    state.isLoading -> EditorPhase.Loading
                    state.loadError != null -> EditorPhase.Error
                    else -> EditorPhase.Form
                },
                transitionSpec = { fadeIn(tween(280, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                label = "editorPhase",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { phase ->
                when (phase) {
                    EditorPhase.Loading -> LoadingSkeleton(
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        rows = 3,
                        heroHeight = 64.dp,
                    )
                    EditorPhase.Error -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ErrorState(message = state.loadError.orEmpty(), onRetry = onRetry, retryTag = "editor_retry")
                    }
                    EditorPhase.Form -> Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(26.dp),
                        ) {
                            if (state.isDone) {
                                NestBanner(
                                    message = stringResource(R.string.planner_editor_done_banner),
                                    tone = BannerTone.Success,
                                    icon = Icons.Rounded.CheckCircle,
                                )
                            }
                            AnimatedVisibility(state.formError != null) {
                                NestBanner(message = state.formError.orEmpty(), modifier = Modifier.testTag("editor_form_error"))
                            }

                            NestTextField(
                                value = state.title,
                                onValueChange = onTitleChange,
                                label = stringResource(R.string.planner_editor_title),
                                placeholder = stringResource(R.string.planner_editor_title_placeholder),
                                error = state.titleError,
                                enabled = state.canEdit,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.testTag("editor_title"),
                            )

                            Column {
                                FieldLabel(stringResource(R.string.planner_editor_subject), isError = state.subjectError != null)
                                if (state.needsSubject) {
                                    NeedsSubjectCard(onAddSubject = onOpenSubjects)
                                } else {
                                    SubjectPicker(
                                        subjects = state.subjects,
                                        selectedId = state.subjectId,
                                        enabled = state.canEdit,
                                        onSelect = {
                                            focusManager.clearFocus()
                                            onSubjectChange(it)
                                        },
                                    )
                                }
                                AnimatedVisibility(state.subjectError != null) {
                                    FieldError(state.subjectError.orEmpty())
                                }
                            }

                            Column {
                                FieldLabel(stringResource(R.string.planner_editor_due))
                                DueDateRow(
                                    dueOn = state.dueOn,
                                    today = today,
                                    accent = accent,
                                    enabled = state.canEdit,
                                    onClick = {
                                        focusManager.clearFocus()
                                        pickingDate = true
                                    },
                                )
                            }

                            Column {
                                FieldLabel(stringResource(R.string.planner_editor_priority))
                                PrioritySegmented(
                                    value = state.priority,
                                    enabled = state.canEdit,
                                    onChange = onPriorityChange,
                                )
                            }

                            NestTextField(
                                value = state.notes,
                                onValueChange = onNotesChange,
                                label = stringResource(R.string.planner_editor_notes),
                                placeholder = stringResource(R.string.planner_editor_notes_placeholder),
                                error = state.notesError,
                                counter = stringResource(R.string.planner_editor_counter, state.notes.length, Validation.MAX_NOTES),
                                enabled = state.canEdit,
                                singleLine = false,
                                minLines = 4,
                                maxLines = 10,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.testTag("editor_notes"),
                            )
                        }
                    }
                }
            }

            // Pinned save bar: rides above the keyboard, fades the form scrolling underneath.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(c.canvas.copy(alpha = 0f), c.canvas, c.canvas)))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                PrimaryButton(
                    text = stringResource(if (state.isNew) R.string.planner_editor_save_new else R.string.planner_editor_save_edit),
                    onClick = {
                        focusManager.clearFocus()
                        onSave()
                    },
                    enabled = ready && !state.isDeleting,
                    loading = state.isSaving,
                    leadingIcon = if (state.isNew) Icons.Rounded.Add else Icons.Rounded.Check,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .testTag("editor_save"),
                )
            }
        }

        NestSnackbarHost(
            LocalAppMessenger.current.host,
            Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 84.dp),
        )
    }

    if (pickingDate) {
        DueDatePickerDialog(
            initial = state.dueOn,
            onPick = onDueDateChange,
            onDismiss = { pickingDate = false },
        )
    }
}

private enum class EditorPhase { Loading, Error, Form }
