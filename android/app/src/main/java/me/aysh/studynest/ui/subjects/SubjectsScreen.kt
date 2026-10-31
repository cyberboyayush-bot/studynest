package me.aysh.studynest.ui.subjects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.EmptyState
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.NestAlertDialog
import me.aysh.studynest.ui.components.NestIconButton
import me.aysh.studynest.ui.components.NestSnackbarHost
import me.aysh.studynest.ui.components.NestTopBar
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.SkeletonBlock
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.navigation.LocalAppMessenger
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(onBack: () -> Unit, onNewAssignment: (String) -> Unit) {
    val vm = appViewModel { c -> SubjectsViewModel(c.studyRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val messenger = LocalAppMessenger.current

    LaunchedEffect(vm) { vm.messages.collect { messenger.show(it) } }

    // Keep the sheet composed while it animates away after a save/delete, and block swipe-dismiss mid-save.
    val saving by rememberUpdatedState(state.editor?.isSaving == true)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !saving },
    )
    val editor = state.editor
    var shownEditor by remember { mutableStateOf(editor) }
    LaunchedEffect(editor) {
        if (editor != null) {
            shownEditor = editor
        } else if (shownEditor != null) {
            if (sheetState.isVisible) sheetState.hide()
            shownEditor = null
        }
    }

    SubjectsContent(
        subjects = state.subjects,
        onBack = onBack,
        onAdd = vm::openNew,
        onEdit = vm::openEdit,
        onRetry = vm::retry,
    )

    shownEditor?.let { last ->
        val current = editor ?: last
        SubjectEditorSheet(
            editor = current,
            sheetState = sheetState,
            onDismiss = vm::dismissEditor,
            onNameChange = vm::onNameChange,
            onColorChange = vm::onColorChange,
            onIconChange = vm::onIconChange,
            onSave = vm::save,
            onDelete = vm::requestDelete,
            onAddAssignment = { current.id?.let(onNewAssignment) },
        )
    }

    state.confirmDelete?.let { target ->
        val name = target.name.trim()
        NestAlertDialog(
            title = stringResource(R.string.planner_subject_delete_title, name),
            message = if (target.assignmentCount > 0) {
                pluralStringResource(R.plurals.planner_subject_delete_message, target.assignmentCount, target.assignmentCount)
            } else {
                stringResource(R.string.planner_subject_delete_message_none)
            },
            confirmLabel = stringResource(R.string.planner_subject_delete_confirm),
            onConfirm = vm::confirmDelete,
            onDismiss = vm::cancelDelete,
            confirmTag = "confirm_delete_subject",
            destructive = true,
            loading = state.isDeleting,
            icon = Icons.Rounded.DeleteOutline,
        )
    }
}

@Composable
private fun SubjectsContent(
    subjects: Loadable<List<Subject>>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Subject) -> Unit,
    onRetry: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val list = subjects.valueOrNull
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("subjects_screen"),
    ) {
        SoftBlob(c.wash(c.peach.fill, 0.7f), Modifier.size(280.dp).offset(x = maxWidth - 170.dp, y = (-130).dp), seed = 4)

        Column(Modifier.fillMaxSize()) {
            NestTopBar(
                title = stringResource(R.string.planner_subjects_screen_title),
                subtitle = list?.let { pluralStringResource(R.plurals.planner_subject_count, it.size, it.size) },
                navigation = {
                    NestIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                        modifier = Modifier.testTag("subjects_back"),
                    )
                },
                actions = {
                    NestIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.planner_subject_add),
                        onClick = onAdd,
                        modifier = Modifier.testTag("subjects_add"),
                        tint = c.onPrimary,
                        container = c.primary,
                    )
                },
            )
            AnimatedContent(
                targetState = subjects,
                contentKey = { if (it is Loadable.Ready) it.value.isEmpty() else it::class },
                transitionSpec = { fadeIn(tween(300, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                label = "subjects",
                modifier = Modifier.fillMaxSize(),
            ) { content ->
                when (content) {
                    Loadable.Loading -> GridSkeleton()
                    is Loadable.Failed -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ErrorState(message = content.message, onRetry = onRetry, retryTag = "subjects_retry")
                    }
                    is Loadable.Ready -> if (content.value.isEmpty()) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmptyState(
                                title = stringResource(R.string.planner_subjects_empty_title),
                                message = stringResource(R.string.planner_subjects_empty_body),
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .padding(top = 24.dp)
                                    .testTag("subjects_empty"),
                            ) {
                                PrimaryButton(
                                    text = stringResource(R.string.planner_subject_add),
                                    onClick = onAdd,
                                    leadingIcon = Icons.Rounded.Add,
                                    modifier = Modifier.testTag("subjects_empty_add"),
                                )
                            }
                        }
                    } else {
                        SubjectGrid(content.value, onEdit, onAdd)
                    }
                }
            }
        }

        NestSnackbarHost(
            LocalAppMessenger.current.host,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun SubjectGrid(subjects: List<Subject>, onEdit: (Subject) -> Unit, onAdd: () -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = navBottom + 88.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(subjects, key = { it.id }) { subject ->
            val caption = pluralStringResource(R.plurals.planner_open_count, subject.openCount, subject.openCount)
            SubjectTile(
                name = subject.name,
                color = subject.color,
                icon = subject.icon,
                caption = caption,
                onClick = { onEdit(subject) },
                onClickLabel = stringResource(R.string.planner_subject_sheet_edit),
                modifier = Modifier
                    .animateItem()
                    .aspectRatio(0.92f)
                    .testTag("subject_tile_${subject.id}"),
            )
        }
        item(key = "new") {
            NewSubjectTile(
                onClick = onAdd,
                modifier = Modifier
                    .animateItem()
                    .aspectRatio(0.92f)
                    .testTag("subjects_new_tile"),
            )
        }
    }
}

@Composable
private fun GridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
    ) {
        items(6) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f),
                NestShapes.Card,
            )
        }
    }
}
