package me.aysh.studynest.ui.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.model.SubjectRef
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.FocusHeroButton
import me.aysh.studynest.ui.components.FocusHeroStyle
import me.aysh.studynest.ui.components.FocusRoundButton
import me.aysh.studynest.ui.components.NestChoiceChip
import me.aysh.studynest.ui.components.NestTextButton
import me.aysh.studynest.ui.components.NestTextField
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.SecondaryButton
import me.aysh.studynest.ui.components.SkeletonBlock
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.components.StatefulNestSheet
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.components.SubjectStamp
import me.aysh.studynest.ui.main.LocalBottomBarPadding
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.NumberStyles
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Durations
import me.aysh.studynest.util.Validation

/** The bottom bar's centre button rises ~40dp above the space reserved by LocalBottomBarPadding. */
private val BulgeClearance = 30.dp

private enum class FocusPhase { Setup, Active, Saving, Finished }

private fun FocusTimerState.phase(): FocusPhase = when (this) {
    FocusTimerState.Idle -> FocusPhase.Setup
    is FocusTimerState.Active -> FocusPhase.Active
    is FocusTimerState.Saving -> FocusPhase.Saving
    is FocusTimerState.Finished -> FocusPhase.Finished
}

/** Everything the Focus UI can ask the ViewModel to do. */
private class FocusActions(
    val selectSubject: (String?) -> Unit,
    val selectPreset: (Int) -> Unit,
    val openCustom: () -> Unit,
    val onCustomInput: (String) -> Unit,
    val confirmCustom: () -> Unit,
    val dismissCustom: () -> Unit,
    val retrySubjects: () -> Unit,
    val start: () -> Unit,
    val pause: () -> Unit,
    val resume: () -> Unit,
    val stop: () -> Unit,
    val retrySave: () -> Unit,
    val dismissResult: () -> Unit,
)

@Composable
fun FocusScreen() {
    val vm = appViewModel { c -> FocusViewModel(c.studyRepository, c.focusTimer) }
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = remember(vm) {
        FocusActions(
            selectSubject = vm::selectSubject,
            selectPreset = vm::selectPreset,
            openCustom = vm::openCustom,
            onCustomInput = vm::onCustomInput,
            confirmCustom = vm::confirmCustom,
            dismissCustom = vm::dismissCustom,
            retrySubjects = vm::retry,
            start = vm::start,
            pause = vm::pause,
            resume = vm::resume,
            stop = vm::stop,
            retrySave = vm::retrySave,
            dismissResult = vm::dismissResult,
        )
    }
    FocusContent(state, actions)
}

@Composable
private fun FocusContent(state: FocusUiState, actions: FocusActions) {
    val c = StudyNestTheme.colors
    val bar = LocalBottomBarPadding.current
    val phase = state.timer.phase()
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("focus_screen"),
    ) {
        // Atmosphere: two slow blobs, warmer while a session runs.
        SoftBlob(c.wash(c.lilac.fill, 0.75f), Modifier.size(360.dp).offset(x = maxWidth - 200.dp, y = (-90).dp), seed = 2)
        SoftBlob(
            c.wash(if (phase == FocusPhase.Setup) c.peach.fill else c.mint.fill, 0.55f),
            Modifier.size(240.dp).offset(x = (-120).dp, y = maxHeight * 0.48f),
            seed = 4,
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bar),
        ) {
            FocusHeader(state.timer)
            AnimatedContent(
                targetState = state.timer,
                contentKey = { it.phase() },
                transitionSpec = {
                    (fadeIn(tween(320, delayMillis = 80)) + scaleIn(tween(380, delayMillis = 80), initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.03f))
                },
                label = "focusPhase",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { timer ->
                when (timer) {
                    FocusTimerState.Idle -> SetupContent(state.setup, actions)
                    is FocusTimerState.Active -> ActiveContent(timer, actions)
                    is FocusTimerState.Saving -> SavingContent(timer)
                    is FocusTimerState.Finished -> FinishedContent(timer, actions)
                }
            }
        }
    }

    CustomDurationSheet(state.setup.customSheet?.takeIf { phase == FocusPhase.Setup }, actions)
}

// ---- Header -------------------------------------------------------------------------------------

@Composable
private fun FocusHeader(timer: FocusTimerState) {
    val c = StudyNestTheme.colors
    val active = timer as? FocusTimerState.Active
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 24.dp, end = 20.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.focus_title),
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(2.dp))
            AnimatedContent(
                targetState = when {
                    timer is FocusTimerState.Idle -> R.string.focus_subtitle_setup
                    active?.isPaused == true -> R.string.focus_subtitle_paused
                    active != null -> R.string.focus_subtitle_running
                    timer is FocusTimerState.Saving -> R.string.focus_subtitle_saving
                    else -> R.string.focus_subtitle_finished
                },
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "focusSubtitle",
            ) { res ->
                Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
            }
        }
        if (active != null) {
            Spacer(Modifier.width(12.dp))
            StatusPill(paused = active.isPaused)
        }
    }
}

@Composable
private fun StatusPill(paused: Boolean) {
    val c = StudyNestTheme.colors
    val fill by animateColorAsState(if (paused) c.butter.fill else c.mint.fill, label = "pillFill")
    val dot by animateColorAsState(if (paused) c.butter.ink else c.mint.ink, label = "pillDot")
    val t = rememberInfiniteTransition(label = "statusDot")
    val beat = t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "beat")
    Row(
        Modifier
            .clip(NestShapes.Pill)
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer {
                    val s = if (paused) 1f else 0.75f + 0.35f * beat.value
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            stringResource(if (paused) R.string.focus_status_paused else R.string.focus_status_running),
            style = MaterialTheme.typography.labelMedium,
            color = c.onPastel,
        )
    }
}

// ---- Setup --------------------------------------------------------------------------------------

@Composable
private fun SetupContent(setup: FocusSetupState, actions: FocusActions) {
    val c = StudyNestTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dialSize = (maxHeight - 380.dp).coerceIn(168.dp, 300.dp).coerceAtMost(maxWidth - 64.dp)
        val minutesText = Durations.minutes(setup.minutes)
        val previewDescription = stringResource(R.string.focus_preview_description, minutesText)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                FocusDial(
                    progress = (setup.minutes / 60f).coerceAtMost(1f),
                    mode = DialMode.Preview,
                    modifier = Modifier
                        .size(dialSize)
                        .clearAndSetSemantics { contentDescription = previewDescription },
                ) {
                    PreviewCenter(setup.minutes, dialSize)
                }
                Sparkle(c.butter.lip, Modifier.size(20.dp).align(Alignment.TopStart).offset(x = 40.dp, y = 18.dp), delayMillis = 300)
                Sparkle(c.rose.lip, Modifier.size(13.dp).align(Alignment.BottomEnd).offset(x = (-44).dp, y = (-26).dp), delayMillis = 900)
            }

            Column(Modifier.fillMaxWidth()) {
                FocusLabel(stringResource(R.string.focus_section_subject), Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(6.dp))
                SubjectPicker(setup.subjects, setup.selectedSubjectId, actions)
                Spacer(Modifier.height(14.dp))
                FocusLabel(stringResource(R.string.focus_section_duration), Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(10.dp))
                DurationPicker(setup, actions, Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(22.dp))
                FocusHeroButton(
                    text = stringResource(R.string.focus_start),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = actions.start,
                    trailing = minutesText,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .testTag("focus_start"),
                )
                // Clearance for the bar's raised Focus button, which bulges above the bar block.
                Spacer(Modifier.height(BulgeClearance))
            }
        }
    }
}

@Composable
private fun PreviewCenter(minutes: Int, dialSize: Dp) {
    val c = StudyNestTheme.colors
    val density = LocalDensity.current
    val numberSize = with(density) { (dialSize * 0.25f).toSp() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = minutes,
            transitionSpec = {
                val up = targetState > initialState
                (fadeIn(tween(220)) + slideInVertically(tween(260)) { if (up) it / 2 else -it / 2 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(200)) { if (up) -it / 2 else it / 2 })
            },
            label = "previewMinutes",
        ) { value ->
            Text(
                value.toString(),
                style = NumberStyles.stat.copy(fontSize = numberSize, lineHeight = numberSize),
                color = c.ink,
            )
        }
        Text(
            stringResource(R.string.focus_preview_unit),
            style = MaterialTheme.typography.labelLarge,
            color = c.primaryText,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.focus_preview_caption),
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
    }
}

@Composable
private fun FocusLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = StudyNestTheme.colors.ink,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun SubjectPicker(subjects: Loadable<List<Subject>>, selectedId: String?, actions: FocusActions) {
    val c = StudyNestTheme.colors
    when (subjects) {
        Loadable.Loading -> Row(
            Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(104.dp, 88.dp, 120.dp).forEach { w -> SkeletonBlock(Modifier.size(width = w, height = 40.dp), NestShapes.Pill) }
        }
        is Loadable.Failed -> Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.focus_subjects_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = c.overdue,
                modifier = Modifier.weight(1f),
            )
            NestTextButton(stringResource(R.string.action_try_again), onClick = actions.retrySubjects, leadingIcon = Icons.Rounded.Refresh)
        }
        is Loadable.Ready -> Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NestChoiceChip(
                selected = selectedId == null,
                onClick = { actions.selectSubject(null) },
                label = stringResource(R.string.focus_no_subject),
                modifier = Modifier.testTag("focus_subject_none"),
            )
            subjects.value.forEach { subject ->
                NestChoiceChip(
                    selected = selectedId == subject.id,
                    onClick = { actions.selectSubject(subject.id) },
                    label = subject.name,
                    dotColor = c.subject(subject.color).lip,
                    modifier = Modifier.testTag("focus_subject_${subject.id}"),
                )
            }
        }
    }
}

@Composable
private fun DurationPicker(setup: FocusSetupState, actions: FocusActions, modifier: Modifier = Modifier) {
    val unit = stringResource(R.string.focus_minutes_unit)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FOCUS_PRESETS.forEach { minutes ->
            val description = stringResource(R.string.focus_duration_description, Durations.minutes(minutes))
            DurationTile(
                selected = !setup.isCustom && setup.minutes == minutes,
                onClick = { actions.selectPreset(minutes) },
                description = description,
                modifier = Modifier
                    .weight(1f)
                    .testTag("focus_duration_$minutes"),
            ) { ink ->
                TileNumber(minutes.toString(), unit, ink)
            }
        }
        val customDescription = if (setup.isCustom) {
            stringResource(R.string.focus_custom_selected_description, Durations.minutes(setup.minutes))
        } else stringResource(R.string.focus_custom_description)
        DurationTile(
            selected = setup.isCustom,
            onClick = actions.openCustom,
            description = customDescription,
            modifier = Modifier
                .weight(1.25f)
                .testTag("focus_duration_custom"),
        ) { ink ->
            if (setup.isCustom) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TileNumber(setup.minutes.toString(), unit, ink)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.focus_custom), style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TileNumber(number: String, unit: String, ink: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, style = NumberStyles.statSmall.copy(fontSize = 22.sp, lineHeight = 24.sp), color = ink, maxLines = 1)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.72f), maxLines = 1)
    }
}

@Composable
private fun DurationTile(
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val c = StudyNestTheme.colors
    val fill by animateColorAsState(if (selected) c.inverse else c.surfaceRaised, label = "tileFill")
    val lip by animateColorAsState(if (selected) (if (c.isDark) c.lilac.lip else c.primaryLip) else c.surfaceLip, label = "tileLip")
    val ink by animateColorAsState(if (selected) c.onInverse else c.ink, label = "tileInk")
    SquishySurface(
        onClick = onClick,
        modifier = modifier
            .height(70.dp)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = description
            },
        fill = fill,
        lip = lip,
        shape = NestShapes.Block,
        border = if (selected) null else BorderStroke(1.5.dp, c.hairline),
        role = Role.RadioButton,
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) { content(ink) }
    }
}

// ---- Active -------------------------------------------------------------------------------------

@Composable
private fun ActiveContent(active: FocusTimerState.Active, actions: FocusActions) {
    val c = StudyNestTheme.colors
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dialSize = minOf(maxWidth - 40.dp, maxHeight - 170.dp, 340.dp).coerceAtLeast(190.dp)
        val remaining = Durations.clock(active.remainingMillis)
        val total = Durations.minutes((active.run.plannedMillis / 60_000L).toInt())
        val description = stringResource(
            if (active.isPaused) R.string.focus_ring_paused_description else R.string.focus_ring_description,
            remaining,
            total,
        )
        val blink = rememberInfiniteTransition(label = "pausedBlink")
        val blinkAlpha = blink.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "blinkAlpha")
        val digitScale = if (remaining.length > 5) 0.14f else 0.18f
        val digitSize = with(density) { (dialSize * digitScale).toSp() }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 20.dp, end = 20.dp, bottom = BulgeClearance),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            SubjectBadge(active.run.subject)
            FocusDial(
                progress = active.progress,
                mode = if (active.isPaused) DialMode.Paused else DialMode.Running,
                modifier = Modifier
                    .size(dialSize)
                    .testTag("focus_ring")
                    .semantics {
                        contentDescription = description
                        progressBarRangeInfo = ProgressBarRangeInfo(active.progress.coerceIn(0f, 1f), 0f..1f)
                    },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(if (active.isPaused) R.string.focus_paused_label else R.string.focus_remaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active.isPaused) c.inkMuted else c.primaryText,
                    )
                    Text(
                        remaining,
                        style = NumberStyles.timer.copy(fontSize = digitSize, lineHeight = digitSize * 1.05f),
                        color = c.ink,
                        maxLines = 1,
                        modifier = Modifier
                            .graphicsLayer { alpha = if (active.isPaused) blinkAlpha.value else 1f }
                            .testTag("focus_time"),
                    )
                    Text(
                        stringResource(R.string.focus_of_total, total),
                        style = NumberStyles.mono,
                        color = c.inkMuted,
                    )
                }
            }
            Row(
                Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FocusRoundButton(
                    icon = Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.focus_stop_description),
                    onClick = actions.stop,
                    iconTint = c.overdue,
                    modifier = Modifier.testTag("focus_stop"),
                )
                if (active.isPaused) {
                    FocusHeroButton(
                        text = stringResource(R.string.focus_resume),
                        icon = Icons.Rounded.PlayArrow,
                        onClick = actions.resume,
                        style = FocusHeroStyle.Primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("focus_resume"),
                    )
                } else {
                    FocusHeroButton(
                        text = stringResource(R.string.focus_pause),
                        icon = Icons.Rounded.Pause,
                        onClick = actions.pause,
                        style = FocusHeroStyle.Tonal,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("focus_pause"),
                    )
                }
            }
        }
    }
}

/** Subject stamp + name in a soft pill; "No subject" gets a neutral sparkle stamp. */
@Composable
private fun SubjectBadge(subject: SubjectRef?) {
    val c = StudyNestTheme.colors
    Row(
        Modifier
            .clip(NestShapes.Pill)
            .background(c.surfaceRaised)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (subject != null) {
            SubjectStamp(subject.color, subject.icon, size = 30.dp, tilt = -4f)
        } else {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(NestShapes.Stamp)
                    .background(c.lilac.fill),
                contentAlignment = Alignment.Center,
            ) {
                Sparkle(c.lilac.ink, Modifier.size(14.dp))
            }
        }
        Text(
            subject?.name ?: stringResource(R.string.focus_no_subject),
            style = MaterialTheme.typography.labelLarge,
            color = c.ink,
            maxLines = 1,
        )
    }
}

// ---- Saving -------------------------------------------------------------------------------------

@Composable
private fun SavingContent(saving: FocusTimerState.Saving) {
    val c = StudyNestTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val dialSize = minOf(maxWidth - 40.dp, maxHeight - 170.dp, 340.dp).coerceAtLeast(190.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SubjectBadge(saving.subject)
            Spacer(Modifier.height(24.dp))
            FocusDial(
                progress = 0f,
                mode = DialMode.Saving,
                modifier = Modifier
                    .size(dialSize)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Durations.minutes(Durations.wholeMinutes(saving.durationSeconds.toLong())),
                        style = NumberStyles.stat,
                        color = c.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.focus_saving), style = MaterialTheme.typography.labelLarge, color = c.primaryText)
                }
            }
        }
    }
}

// ---- Finished -----------------------------------------------------------------------------------

@Composable
private fun FinishedContent(result: FocusTimerState.Finished, actions: FocusActions) {
    val c = StudyNestTheme.colors
    val minutesText = Durations.minutes(Durations.wholeMinutes(result.durationSeconds.toLong()))
    val failed = !result.saved && result.error != null
    val (title, message) = when {
        result.saved && result.completed -> stringResource(R.string.focus_result_complete_title) to stringResource(R.string.focus_result_saved, minutesText)
        result.saved -> stringResource(R.string.focus_result_saved_title) to stringResource(R.string.focus_result_saved, minutesText)
        result.tooShort -> stringResource(R.string.focus_result_short_title) to stringResource(R.string.focus_result_too_short)
        else -> stringResource(R.string.focus_result_error_title) to (result.error ?: stringResource(R.string.focus_result_error_hint))
    }
    val pastel = when {
        result.saved -> c.mint
        result.tooShort -> c.butter
        else -> null
    }
    val medalFill = pastel?.fill ?: c.overdueFill
    val medalLip = pastel?.lip ?: c.overdueAccent.copy(alpha = 0.5f)
    val medalInk = pastel?.ink ?: c.overdue
    val medalIcon = when {
        result.saved -> Icons.Rounded.Check
        result.tooShort -> Icons.Rounded.TimerOff
        else -> Icons.Rounded.CloudOff
    }

    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(width = 320.dp, height = 260.dp), contentAlignment = Alignment.Center) {
            if (result.saved) {
                ConfettiBurst(Modifier.fillMaxSize(), intensity = if (result.completed) 1f else 0.55f)
                Sparkle(c.butter.lip, Modifier.size(22.dp).align(Alignment.TopEnd).offset(x = (-58).dp, y = 40.dp), delayMillis = 400)
                Sparkle(c.lilac.lip, Modifier.size(16.dp).align(Alignment.BottomStart).offset(x = 64.dp, y = (-46).dp), delayMillis = 900)
                Sparkle(c.peach.lip, Modifier.size(12.dp).align(Alignment.TopStart).offset(x = 70.dp, y = 56.dp), delayMillis = 0)
            }
            SquishySurface(
                onClick = null,
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        rotationZ = (1f - pop.value) * -24f
                    },
                fill = medalFill,
                lip = medalLip,
                shape = CircleShape,
                lipHeight = 6.dp,
                contentAlignment = Alignment.Center,
            ) {
                Icon(medalIcon, contentDescription = null, tint = medalInk, modifier = Modifier.size(64.dp))
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("focus_result")
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (failed) c.overdue else c.inkMuted,
                textAlign = TextAlign.Center,
            )
            if (failed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.focus_result_error_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (result.subject != null) {
            Spacer(Modifier.height(16.dp))
            SubjectBadge(result.subject)
        }
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (failed) {
                PrimaryButton(
                    text = stringResource(R.string.focus_retry),
                    onClick = actions.retrySave,
                    leadingIcon = Icons.Rounded.Refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_retry"),
                )
                SecondaryButton(
                    text = stringResource(R.string.focus_done),
                    onClick = actions.dismissResult,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_done"),
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.focus_done),
                    onClick = actions.dismissResult,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_done"),
                )
            }
        }
        Spacer(Modifier.height(BulgeClearance))
    }
}

// ---- Custom duration sheet ----------------------------------------------------------------------

@Composable
private fun CustomDurationSheet(sheet: CustomDurationState?, actions: FocusActions) {
    val c = StudyNestTheme.colors
    StatefulNestSheet(
        value = sheet,
        onDismissRequest = actions.dismissCustom,
        title = stringResource(R.string.focus_custom_title),
    ) { custom ->
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        val parsed = Validation.minutes(custom.input, Validation.MIN_CUSTOM_FOCUS_MINUTES, Validation.MAX_CUSTOM_FOCUS_MINUTES)
        Text(stringResource(R.string.focus_custom_body), style = MaterialTheme.typography.bodyLarge, color = c.inkMuted)
        Spacer(Modifier.height(20.dp))
        NestTextField(
            value = custom.input,
            onValueChange = actions.onCustomInput,
            label = stringResource(R.string.focus_custom_label),
            placeholder = stringResource(R.string.focus_custom_placeholder),
            error = custom.error,
            supportingText = parsed?.let { stringResource(R.string.focus_custom_preview, Durations.minutes(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.confirmCustom() }),
            trailing = {
                Text(
                    stringResource(R.string.focus_minutes_unit),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.inkMuted,
                    modifier = Modifier.padding(end = 14.dp),
                )
            },
            modifier = Modifier
                .focusRequester(focus)
                .testTag("custom_minutes_field"),
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.focus_custom_save),
            onClick = actions.confirmCustom,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_minutes_save"),
        )
    }
}
