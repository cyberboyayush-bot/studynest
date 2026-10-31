package me.aysh.studynest.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.LoadingSkeleton
import me.aysh.studynest.ui.components.NestIllustration
import me.aysh.studynest.ui.components.NestTextButton
import me.aysh.studynest.ui.components.PlannerAssignmentRow
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.ProgressRing
import me.aysh.studynest.ui.components.Reveal
import me.aysh.studynest.ui.components.SecondaryButton
import me.aysh.studynest.ui.components.SectionHeader
import me.aysh.studynest.ui.components.SkeletonBlock
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.components.SubjectStamp
import me.aysh.studynest.ui.main.LocalBottomBarPadding
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.NumberStyles
import me.aysh.studynest.ui.theme.Pastel
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.DayPart
import me.aysh.studynest.util.Durations
import me.aysh.studynest.util.dayPart
import me.aysh.studynest.util.longDate

@Composable
fun TodayScreen(
    onStartFocus: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    onNewAssignment: () -> Unit,
    onSeeAllAssignments: () -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val vm = appViewModel { c -> TodayViewModel(c.studyRepository, c.clock, c.focusTimer) }
    val state by vm.state.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    TodayContent(
        state = state,
        timer = timer,
        onRefresh = vm::refresh,
        onStartFocus = onStartFocus,
        onOpenAssignment = onOpenAssignment,
        onNewAssignment = onNewAssignment,
        onSeeAllAssignments = onSeeAllAssignments,
        onOpenSubjects = onOpenSubjects,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    state: TodayUiState,
    timer: FocusTimerState,
    onRefresh: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    onNewAssignment: () -> Unit,
    onSeeAllAssignments: () -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val bar = LocalBottomBarPadding.current
    val pullState = rememberPullToRefreshState()
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("today_screen"),
    ) {
        // Atmosphere behind the greeting only.
        SoftBlob(c.wash(if (c.isDark) c.sky.fill else c.butter.fill, 0.75f), Modifier.size(300.dp).offset(x = maxWidth - 180.dp, y = (-140).dp), seed = 2)
        SoftBlob(c.wash(c.lilac.fill, 0.55f), Modifier.size(170.dp).offset(x = (-96).dp, y = 70.dp), seed = 5)

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                    containerColor = c.surfaceRaised,
                    color = c.primary,
                )
            },
        ) {
            AnimatedContent(
                targetState = state.content,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(320, delayMillis = 60)) togetherWith fadeOut(tween(140)) },
                label = "today",
                modifier = Modifier.fillMaxSize(),
            ) { content ->
                when (content) {
                    Loadable.Loading -> TodaySkeleton(bar)
                    is Loadable.Failed -> Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .padding(bar)
                            .padding(top = 48.dp),
                    ) {
                        ErrorState(message = content.message, onRetry = onRefresh, retryTag = "today_retry")
                    }
                    is Loadable.Ready -> TodayReady(
                        data = content.value,
                        timer = timer,
                        bottomPadding = bar.calculateBottomPadding(),
                        onStartFocus = onStartFocus,
                        onOpenAssignment = onOpenAssignment,
                        onNewAssignment = onNewAssignment,
                        onSeeAllAssignments = onSeeAllAssignments,
                        onOpenSubjects = onOpenSubjects,
                    )
                }
            }
        }

        // Scrolled content melts away under the status bar instead of colliding with the clock.
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusTop + 14.dp)
                .background(Brush.verticalGradient(listOf(c.canvas, c.canvas.copy(alpha = 0.94f), c.canvas.copy(alpha = 0f)))),
        )
    }
}

@Composable
private fun TodayReady(
    data: TodayData,
    timer: FocusTimerState,
    bottomPadding: Dp,
    onStartFocus: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    onNewAssignment: () -> Unit,
    onSeeAllAssignments: () -> Unit,
    onOpenSubjects: () -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = bottomPadding + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            Reveal(0, Modifier.widthIn(max = 640.dp)) { Header(data) }
        }
        if (data.isNewUser) {
            item(key = "empty") {
                Reveal(1, Modifier.widthIn(max = 640.dp)) {
                    NewUserState(onAddSubject = onOpenSubjects, onAddAssignment = onNewAssignment)
                }
            }
        } else {
            item(key = "hero") {
                Reveal(1, Modifier.widthIn(max = 640.dp)) {
                    FocusHero(data = data, timer = timer, onStartFocus = onStartFocus, modifier = Modifier.padding(top = 22.dp))
                }
            }
            item(key = "shortcuts") {
                Reveal(2, Modifier.widthIn(max = 640.dp)) {
                    Shortcuts(
                        subjectCount = data.subjectCount,
                        onOpenSubjects = onOpenSubjects,
                        onNewAssignment = onNewAssignment,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
            }
            item(key = "dueSoon") {
                Reveal(3, Modifier.widthIn(max = 640.dp)) {
                    DueSoon(
                        data = data,
                        onOpenAssignment = onOpenAssignment,
                        onSeeAll = onSeeAllAssignments,
                        onNewAssignment = onNewAssignment,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
            }
        }
    }
}

// ---- Header --------------------------------------------------------------------------------------

@Composable
private fun Header(data: TodayData) {
    val c = StudyNestTheme.colors
    val part = dayPart(data.now)
    val greeting = stringResource(
        when (part) {
            DayPart.Morning -> R.string.planner_greeting_morning
            DayPart.Afternoon -> R.string.planner_greeting_afternoon
            DayPart.Evening -> R.string.planner_greeting_evening
        },
        data.firstName,
    )
    val (icon, pastel) = when (part) {
        DayPart.Morning -> Icons.Rounded.WbSunny to c.butter
        DayPart.Afternoon -> Icons.Rounded.WbTwilight to c.peach
        DayPart.Evening -> Icons.Rounded.Bedtime to c.lilac
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                longDate(data.today),
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(6.dp))
            val nameStart = greeting.lastIndexOf(data.firstName).takeIf { it >= 0 && data.firstName.isNotEmpty() }
            Text(
                buildAnnotatedString {
                    if (nameStart == null) {
                        append(greeting)
                    } else {
                        append(greeting.substring(0, nameStart))
                        withStyle(SpanStyle(color = c.primaryText)) { append(data.firstName) }
                        append(greeting.substring(nameStart + data.firstName.length))
                    }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
                modifier = Modifier
                    .testTag("today_greeting")
                    .semantics { heading() },
            )
        }
        Spacer(Modifier.width(12.dp))
        DayPartStamp(icon, pastel)
    }
}

@Composable
private fun DayPartStamp(icon: ImageVector, pastel: Pastel) {
    Box(Modifier.size(58.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(50.dp)
                .rotate(8f)
                .offset(y = 3.dp)
                .clip(NestShapes.Stamp)
                .background(pastel.lip),
        )
        Box(
            Modifier
                .size(50.dp)
                .rotate(8f)
                .clip(NestShapes.Stamp)
                .background(pastel.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = pastel.ink, modifier = Modifier.size(26.dp).rotate(-8f))
        }
        Sparkle(StudyNestTheme.colors.primary, Modifier.size(14.dp).align(Alignment.TopEnd), delayMillis = 300)
    }
}

// ---- Focus hero ----------------------------------------------------------------------------------

@Composable
private fun FocusHero(data: TodayData, timer: FocusTimerState, onStartFocus: () -> Unit, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val minutes = Durations.wholeMinutes(data.todaySeconds)
    val goal = data.goalMinutes.coerceAtLeast(1)
    val progress = Durations.goalProgress(data.todaySeconds, data.goalMinutes)
    val reached = minutes >= goal
    val ringDescription = stringResource(R.string.planner_today_goal_description, minutes, goal)

    SquishySurface(
        onClick = null,
        modifier = modifier.fillMaxWidth(),
        fill = c.surface,
        lip = c.surfaceLip,
        border = BorderStroke(1.dp, c.hairline),
    ) {
        Box {
            // A pastel wash tucked into the corner, behind the ring.
            SoftBlob(
                c.wash(if (reached) c.mint.fill else c.lilac.fill, 0.8f),
                Modifier
                    .size(220.dp)
                    .offset(x = (-70).dp, y = (-70).dp),
                seed = 3,
            )
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        ProgressRing(
                            progress = progress,
                            modifier = Modifier
                                .size(128.dp)
                                .testTag("today_goal_ring")
                                .semantics(mergeDescendants = true) {
                                    contentDescription = ringDescription
                                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                                },
                            strokeWidth = 14.dp,
                            trackColor = c.surfaceSunken,
                            progressColor = if (reached) c.mint.lip else c.primary,
                            knob = !reached,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$minutes", style = NumberStyles.stat, color = c.ink)
                                Text(
                                    stringResource(R.string.planner_today_goal_of, goal),
                                    style = NumberStyles.mono.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                    color = c.inkMuted,
                                )
                            }
                        }
                        if (reached) {
                            Sparkle(c.butter.lip, Modifier.size(20.dp).align(Alignment.TopEnd), delayMillis = 0)
                            Sparkle(c.mint.lip, Modifier.size(12.dp).align(Alignment.BottomStart), delayMillis = 500)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.planner_today_focus_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.inkMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (reached) {
                                stringResource(R.string.planner_today_goal_reached)
                            } else {
                                pluralStringResource(R.plurals.planner_today_to_go, goal - minutes, goal - minutes)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = c.ink,
                        )
                        if (reached || minutes == 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(if (reached) R.string.planner_today_goal_reached_body else R.string.planner_today_goal_none),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.inkMuted,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        StreakChip(data.currentStreak)
                    }
                }
                Spacer(Modifier.height(20.dp))
                FocusAction(timer, onStartFocus)
            }
        }
    }
}

@Composable
private fun StreakChip(streak: Int) {
    val c = StudyNestTheme.colors
    val active = streak > 0
    Row(
        Modifier
            .clip(NestShapes.Pill)
            .background(if (active) c.butter.fill else c.surfaceSunken)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (active) c.butter.ink else c.inkMuted,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (active) pluralStringResource(R.plurals.planner_streak, streak, streak) else stringResource(R.string.planner_streak_none),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) c.onPastel else c.inkMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun FocusAction(timer: FocusTimerState, onStartFocus: () -> Unit) {
    val c = StudyNestTheme.colors
    AnimatedContent(
        targetState = timer,
        contentKey = { it is FocusTimerState.Idle },
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(120)) },
        label = "focusAction",
    ) { t ->
        if (t is FocusTimerState.Idle) {
            PrimaryButton(
                text = stringResource(R.string.planner_start_focus),
                onClick = onStartFocus,
                leadingIcon = Icons.Rounded.PlayArrow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_start_focus"),
            )
        } else {
            val label = when (t) {
                is FocusTimerState.Active -> stringResource(
                    if (t.isPaused) R.string.planner_session_paused else R.string.planner_session_running,
                    Durations.clock(t.remainingMillis),
                )
                is FocusTimerState.Saving -> stringResource(R.string.planner_session_saving)
                else -> stringResource(R.string.planner_session_finished)
            }
            val progress = (t as? FocusTimerState.Active)?.progress ?: 1f
            SquishySurface(
                onClick = onStartFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_start_focus"),
                fill = c.inverse,
                lip = if (c.isDark) c.lilac.lip else c.primaryLip,
                shape = NestShapes.Pill,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 12.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(
                        progress = progress,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 4.dp,
                        trackColor = c.onInverse.copy(alpha = 0.18f),
                        progressColor = c.inverseAccent,
                        animationSpec = tween(400),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        label,
                        style = NumberStyles.mono.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                        color = c.onInverse,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = c.onInverse, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---- Shortcuts -----------------------------------------------------------------------------------

@Composable
private fun Shortcuts(subjectCount: Int, onOpenSubjects: () -> Unit, onNewAssignment: () -> Unit, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SquishySurface(
            onClick = onOpenSubjects,
            modifier = Modifier
                .weight(1.25f)
                .height(146.dp)
                .testTag("today_subjects"),
            fill = c.peach.fill,
            lip = c.peach.lip,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    // A fanned little stack of stamps.
                    Box(Modifier.width(84.dp).height(48.dp).clearAndSetSemantics { }) {
                        SubjectStamp(SubjectColor.Lilac, SubjectIcon.Book, size = 38.dp, tilt = -8f)
                        SubjectStamp(SubjectColor.Mint, SubjectIcon.Flask, size = 38.dp, tilt = 2f, modifier = Modifier.offset(x = 22.dp, y = 2.dp))
                        SubjectStamp(SubjectColor.Sky, SubjectIcon.Globe, size = 38.dp, tilt = 10f, modifier = Modifier.offset(x = 44.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = c.peach.ink, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.planner_subjects_title), style = MaterialTheme.typography.titleLarge, color = c.onPastel)
                Text(
                    pluralStringResource(R.plurals.planner_subject_count, subjectCount, subjectCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.peach.ink,
                )
            }
        }
        SquishySurface(
            onClick = onNewAssignment,
            modifier = Modifier
                .weight(1f)
                .height(146.dp)
                .testTag("today_new_assignment"),
            fill = c.mint.fill,
            lip = c.mint.lip,
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(NestShapes.Stamp)
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = c.mint.ink, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.planner_quick_add),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.onPastel,
                    maxLines = 2,
                )
                Text(stringResource(R.string.planner_quick_add_body), style = MaterialTheme.typography.bodySmall, color = c.mint.ink)
            }
        }
    }
}

// ---- Due soon ------------------------------------------------------------------------------------

@Composable
private fun DueSoon(
    data: TodayData,
    onOpenAssignment: (String) -> Unit,
    onSeeAll: () -> Unit,
    onNewAssignment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StudyNestTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.planner_due_soon),
            accent = c.peach.lip,
            actionLabel = stringResource(R.string.planner_see_all),
            onAction = onSeeAll,
            actionTag = "today_see_all",
        )
        if (data.overdueCount > 0) {
            Row(
                Modifier
                    .padding(start = 16.dp, top = 2.dp)
                    .clip(NestShapes.Pill)
                    .background(c.overdueFill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = c.overdue, modifier = Modifier.size(16.dp))
                Text(
                    pluralStringResource(R.plurals.planner_overdue_count, data.overdueCount, data.overdueCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.overdue,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("today_due_soon"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (data.dueSoon.isEmpty()) {
                AllClearCard(onNewAssignment)
            } else {
                data.dueSoon.forEach { assignment ->
                    PlannerAssignmentRow(
                        assignment = assignment,
                        today = data.today,
                        onClick = { onOpenAssignment(assignment.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("due_item_${assignment.id}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllClearCard(onNewAssignment: () -> Unit) {
    val c = StudyNestTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(NestShapes.Card)
            .background(c.successFill.copy(alpha = if (c.isDark) 0.7f else 0.55f))
            .padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.planner_due_soon_clear_title), style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(stringResource(R.string.planner_due_soon_clear_body), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
        NestTextButton(
            text = stringResource(R.string.planner_quick_add),
            onClick = onNewAssignment,
            leadingIcon = Icons.Rounded.Add,
            contentPadding = PaddingValues(horizontal = 10.dp),
        )
    }
}

// ---- New user ------------------------------------------------------------------------------------

@Composable
private fun NewUserState(onAddSubject: () -> Unit, onAddAssignment: () -> Unit) {
    val c = StudyNestTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("today_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            SoftBlob(c.wash(c.lilac.fill, 0.7f), Modifier.size(220.dp), seed = 7)
            Sparkle(c.butter.lip, Modifier.size(20.dp).align(Alignment.TopStart).offset(x = 40.dp, y = 24.dp), delayMillis = 200)
            Sparkle(c.rose.lip, Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = (-44).dp, y = (-30).dp), delayMillis = 800)
            NestIllustration(Modifier.size(width = 280.dp, height = 220.dp), books = 1, showPencil = true)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.planner_today_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = c.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.planner_today_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = c.inkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(NestShapes.Card)
                .background(c.surface)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Step(1, stringResource(R.string.planner_today_empty_step_1), c.peach)
            Step(2, stringResource(R.string.planner_today_empty_step_2), c.mint)
            Step(3, stringResource(R.string.planner_today_empty_step_3), c.sky)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.planner_add_first_subject),
            onClick = onAddSubject,
            leadingIcon = Icons.Rounded.Add,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("today_add_subject"),
        )
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            text = stringResource(R.string.planner_add_assignment),
            onClick = onAddAssignment,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("today_add_assignment"),
        )
    }
}

@Composable
private fun Step(number: Int, text: String, pastel: Pastel) {
    val c = StudyNestTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(NestShapes.Pill)
                .background(pastel.fill),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.titleSmall, color = pastel.ink)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.ink)
    }
}

// ---- Loading -------------------------------------------------------------------------------------

@Composable
private fun TodaySkeleton(bar: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bar)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SkeletonBlock(Modifier.width(150.dp).height(14.dp), NestShapes.Pill)
        Spacer(Modifier.height(12.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.75f).height(30.dp), NestShapes.Pill)
        Spacer(Modifier.height(26.dp))
        LoadingSkeleton(rows = 4, heroHeight = 236.dp)
    }
}
