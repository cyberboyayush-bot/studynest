package me.aysh.studynest.ui.assignments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.EmptyState
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.LoadingSkeleton
import me.aysh.studynest.ui.components.NestChoiceChip
import me.aysh.studynest.ui.components.NestIconButton
import me.aysh.studynest.ui.components.NestIllustration
import me.aysh.studynest.ui.components.PlannerAssignmentRow
import me.aysh.studynest.ui.components.PlannerSearchField
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.SecondaryButton
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.main.LocalBottomBarPadding
import me.aysh.studynest.ui.navigation.LocalAppMessenger
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.DueBucket
import java.time.LocalDate

@Composable
fun AssignmentsScreen(onOpenAssignment: (String) -> Unit, onNewAssignment: () -> Unit, onOpenSubjects: () -> Unit) {
    val vm = appViewModel { c -> AssignmentsViewModel(c.studyRepository, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    val messenger = LocalAppMessenger.current
    val resources = LocalResources.current

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is AssignmentsEvent.Changed -> launch {
                    val change = event.change
                    val message = resources.getString(
                        if (change.done) R.string.planner_marked_done else R.string.planner_marked_open,
                        change.title,
                    )
                    messenger.host.currentSnackbarData?.dismiss()
                    val result = messenger.host.showSnackbar(
                        message = message,
                        actionLabel = resources.getString(R.string.planner_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.undo(change)
                }
                is AssignmentsEvent.Message -> messenger.show(event.text)
            }
        }
    }

    AssignmentsContent(
        state = state,
        onQueryChange = vm::onQueryChange,
        onSubjectFilter = vm::onSubjectFilter,
        onShowCompletedChange = vm::onShowCompletedChange,
        onClearFilters = vm::clearFilters,
        onRetry = vm::retry,
        onToggleDone = vm::toggleDone,
        onOpenAssignment = onOpenAssignment,
        onNewAssignment = onNewAssignment,
        onOpenSubjects = onOpenSubjects,
    )
}

@Composable
private fun AssignmentsContent(
    state: AssignmentsUiState,
    onQueryChange: (String) -> Unit,
    onSubjectFilter: (String?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onToggleDone: (Assignment) -> Unit,
    onOpenAssignment: (String) -> Unit,
    onNewAssignment: () -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val bar = LocalBottomBarPadding.current
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward || listState.lastScrolledBackward } }
    val groups = state.groups.valueOrNull
    // The app snackbar floats just above the bar; lift the FAB out of its way while one is showing.
    val snackbarShowing = LocalAppMessenger.current.host.currentSnackbarData != null
    val fabLift by animateDpAsState(if (snackbarShowing) 68.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 400f), label = "fabLift")
    val openCount = groups?.filter { it.bucket != DueBucket.Done }?.sumOf { it.items.size }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("assignments_screen"),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            TitleBar(openCount = openCount, onOpenSubjects = onOpenSubjects)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(top = 4.dp, bottom = bar.calculateBottomPadding() + 120.dp),
            ) {
                item(key = "search", contentType = "search") {
                    PlannerSearchField(
                        value = state.filters.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.planner_search_placeholder),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .testTag("assignments_search"),
                    )
                }
                item(key = "chips", contentType = "chips") {
                    SubjectChips(state, onSubjectFilter)
                }
                item(key = "toggle", contentType = "toggle") {
                    CompletedToggle(state.filters.showCompleted, onShowCompletedChange)
                }
                when (val content = state.groups) {
                    Loadable.Loading -> item(key = "loading", contentType = "loading") {
                        LoadingSkeleton(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), rows = 6, heroHeight = 0.dp)
                    }
                    is Loadable.Failed -> item(key = "error", contentType = "error") {
                        ErrorState(message = content.message, onRetry = onRetry, retryTag = "assignments_retry")
                    }
                    is Loadable.Ready -> if (content.value.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            EmptyAssignments(
                                filtering = state.filters.isFiltering,
                                hasSubjects = state.subjects.isNotEmpty(),
                                onClearFilters = onClearFilters,
                                onNewAssignment = onNewAssignment,
                                onOpenSubjects = onOpenSubjects,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    } else {
                        content.value.forEach { group ->
                            stickyHeader(key = "group_${group.bucket}", contentType = "header") {
                                GroupHeader(group.bucket, group.items.size)
                            }
                            items(group.items, key = { it.id }, contentType = { "row" }) { assignment ->
                                SwipeRow(
                                    assignment = assignment,
                                    today = state.today,
                                    onToggle = { onToggleDone(assignment) },
                                    onOpen = { onOpenAssignment(assignment.id) },
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 20.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // The "nothing at all" empty state carries its own big call to action.
        AnimatedVisibility(
            visible = !(state.isEmpty && !state.filters.isFiltering),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bar)
                .padding(end = 20.dp, bottom = 36.dp)
                .offset { IntOffset(0, -fabLift.roundToPx()) },
        ) {
            NewAssignmentFab(expanded = fabExpanded, onClick = onNewAssignment)
        }
    }
}

@Composable
private fun TitleBar(openCount: Int?, onOpenSubjects: () -> Unit) {
    val c = StudyNestTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.planner_assignments_title),
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
                modifier = Modifier.semantics { heading() },
            )
            AnimatedVisibility(openCount != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    pluralStringResource(R.plurals.planner_open_count, openCount ?: 0, openCount ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                )
            }
        }
        NestIconButton(
            icon = Icons.Rounded.Category,
            contentDescription = stringResource(R.string.planner_open_subjects),
            onClick = onOpenSubjects,
            modifier = Modifier.testTag("assignments_subjects"),
            tint = c.primaryText,
        )
    }
}

@Composable
private fun SubjectChips(state: AssignmentsUiState, onSubjectFilter: (String?) -> Unit) {
    val c = StudyNestTheme.colors
    LazyRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            NestChoiceChip(
                selected = state.filters.subjectId == null,
                onClick = { onSubjectFilter(null) },
                label = stringResource(R.string.planner_filter_all),
                modifier = Modifier.testTag("chip_all"),
            )
        }
        items(state.subjects, key = { it.id }) { subject ->
            NestChoiceChip(
                selected = state.filters.subjectId == subject.id,
                onClick = { onSubjectFilter(subject.id) },
                label = subject.name,
                dotColor = c.subject(subject.color).lip,
                modifier = Modifier.testTag("chip_subject_${subject.id}"),
            )
        }
    }
}

@Composable
private fun CompletedToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = StudyNestTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 14.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.SwipeRight, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.planner_swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier
                .clip(NestShapes.Pill)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .testTag("assignments_show_completed")
                .heightIn(min = 48.dp)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.planner_show_completed), style = MaterialTheme.typography.labelMedium, color = c.ink)
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.82f
                    scaleY = 0.82f
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onPrimary,
                    checkedTrackColor = c.primary,
                    checkedBorderColor = c.primary,
                    checkedIconColor = c.primary,
                    uncheckedThumbColor = c.inkMuted,
                    uncheckedTrackColor = c.surfaceSunken,
                    uncheckedBorderColor = c.hairline,
                ),
            )
        }
    }
}

@Composable
private fun GroupHeader(bucket: DueBucket, count: Int) {
    val c = StudyNestTheme.colors
    val (label, accent, titleColor) = when (bucket) {
        DueBucket.Overdue -> Triple(R.string.planner_group_overdue, c.overdueAccent, c.overdue)
        DueBucket.Today -> Triple(R.string.planner_group_today, c.butter.lip, c.ink)
        DueBucket.ThisWeek -> Triple(R.string.planner_group_this_week, c.sky.lip, c.ink)
        DueBucket.Later -> Triple(R.string.planner_group_later, c.lilac.lip, c.ink)
        DueBucket.Done -> Triple(R.string.planner_group_done, c.mint.lip, c.ink)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.canvas)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            .testTag("group_${bucket.name.lowercase()}")
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 6.dp, height = 18.dp)
                .clip(NestShapes.Pill)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(stringResource(label), style = MaterialTheme.typography.titleLarge, color = titleColor)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .heightIn(min = 22.dp)
                .widthIn(min = 22.dp)
                .clip(NestShapes.Pill)
                .background(if (bucket == DueBucket.Overdue) c.overdueFill else c.surfaceSunken)
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = if (bucket == DueBucket.Overdue) c.overdue else c.inkMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(
    assignment: Assignment,
    today: LocalDate,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deliberately NOT rememberSaveable: a row that leaves the list after a swipe (and comes back on Undo) must
    // start settled, otherwise the restored "dismissed" state would fire onDismiss again.
    val swipe = remember(assignment.id, assignment.isDone) {
        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { distance -> distance * 0.3f })
    }
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        backgroundContent = { SwipeBackground(swipe, assignment.isDone) },
        modifier = modifier.testTag("assignment_row_${assignment.id}"),
        onDismiss = {
            onToggle()
            scope.launch { swipe.reset() }
        },
    ) {
        PlannerAssignmentRow(
            assignment = assignment,
            today = today,
            onClick = onOpen,
            onToggleDone = onToggle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, done: Boolean) {
    val c = StudyNestTheme.colors
    val direction = state.dismissDirection
    val armed = state.targetValue != SwipeToDismissBoxValue.Settled
    val pastel = if (done) c.butter else c.mint
    val fill by animateColorAsState(if (armed) pastel.lip else pastel.fill, label = "swipeFill")
    val iconScale by animateFloatAsState(if (armed) 1.15f else 0.85f, label = "swipeIcon")
    Row(
        Modifier
            .fillMaxSize()
            .padding(bottom = 3.dp)
            .clip(NestShapes.Block)
            .background(if (direction == SwipeToDismissBoxValue.Settled) c.canvas else fill)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (direction == SwipeToDismissBoxValue.EndToStart) Arrangement.End else Arrangement.Start,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(
                if (done) Icons.Rounded.Replay else Icons.Rounded.Check,
                contentDescription = null,
                tint = pastel.ink,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (done) R.string.planner_swipe_reopen else R.string.planner_swipe_mark_done),
                style = MaterialTheme.typography.labelLarge,
                color = c.onPastel,
            )
        }
    }
}

@Composable
private fun EmptyAssignments(
    filtering: Boolean,
    hasSubjects: Boolean,
    onClearFilters: () -> Unit,
    onNewAssignment: () -> Unit,
    onOpenSubjects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filtering) {
        EmptyState(
            title = stringResource(R.string.planner_assignments_no_matches_title),
            message = stringResource(R.string.planner_assignments_no_matches_body),
            modifier = modifier.testTag("assignments_empty"),
            illustration = { NestIllustration(Modifier.size(width = 200.dp, height = 160.dp), books = 0, showPencil = false) },
        ) {
            SecondaryButton(
                text = stringResource(R.string.planner_clear_filters),
                onClick = onClearFilters,
                leadingIcon = Icons.Rounded.FilterAltOff,
                modifier = Modifier.testTag("assignments_clear_filters"),
            )
        }
    } else {
        EmptyState(
            title = stringResource(R.string.planner_assignments_empty_title),
            message = stringResource(
                if (hasSubjects) R.string.planner_assignments_empty_body else R.string.planner_assignments_empty_no_subjects,
            ),
            modifier = modifier.testTag("assignments_empty"),
            illustration = { NestIllustration(Modifier.size(width = 220.dp, height = 180.dp), books = 2, showPencil = true) },
        ) {
            if (hasSubjects) {
                PrimaryButton(
                    text = stringResource(R.string.planner_new_assignment),
                    onClick = onNewAssignment,
                    leadingIcon = Icons.Rounded.Add,
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.planner_assignments_add_subject),
                    onClick = onOpenSubjects,
                    leadingIcon = Icons.Rounded.Add,
                )
            }
        }
    }
}

@Composable
private fun NewAssignmentFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val label = stringResource(R.string.planner_new_assignment)
    SquishySurface(
        onClick = onClick,
        modifier = modifier
            .testTag("assignments_add")
            .semantics { contentDescription = label },
        fill = c.primary,
        lip = c.primaryLip,
        shape = NestShapes.Pill,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(58.dp)
                .padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(24.dp))
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 4.dp)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}
