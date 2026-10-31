package me.aysh.studynest.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.data.model.WeekInsights
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.EmptyState
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.NestIconButton
import me.aysh.studynest.ui.components.NestIllustration
import me.aysh.studynest.ui.components.Reveal
import me.aysh.studynest.ui.components.SkeletonBlock
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.main.LocalBottomBarPadding
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.NumberStyles
import me.aysh.studynest.ui.theme.Pastel
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Durations
import me.aysh.studynest.util.Weeks
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsScreen() {
    val vm = appViewModel { c -> InsightsViewModel(c.studyRepository, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    InsightsContent(state, onPrev = vm::previousWeek, onNext = vm::nextWeek, onRetry = vm::retry)
}

/** Identity of what's on screen: switching weeks slides, loading → ready cross-fades. */
private data class InsightsKey(val week: LocalDate, val kind: Int)

private fun Loadable<*>.kind(): Int = when (this) {
    Loadable.Loading -> 0
    is Loadable.Failed -> 1
    is Loadable.Ready -> 2
}

@Composable
private fun InsightsContent(state: InsightsUiState, onPrev: () -> Unit, onNext: () -> Unit, onRetry: () -> Unit) {
    val c = StudyNestTheme.colors
    val bar = LocalBottomBarPadding.current
    Box(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("insights_screen"),
    ) {
        SoftBlob(c.wash(c.sky.fill, 0.6f), Modifier.size(300.dp).offset(x = (-150).dp, y = (-110).dp), seed = 6)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                Text(
                    stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.ink,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { heading() },
                )
                Text(
                    stringResource(R.string.insights_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
                Spacer(Modifier.height(16.dp))
                WeekSwitcher(state, onPrev, onNext)
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = state,
                    contentKey = { InsightsKey(it.weekStart, it.content.kind()) },
                    transitionSpec = {
                        val weekChanged = initialState.weekStart != targetState.weekStart
                        if (weekChanged) {
                            val forward = targetState.weekStart > initialState.weekStart
                            (fadeIn(tween(300)) + slideInHorizontally(tween(340)) { w -> if (forward) w / 6 else -w / 6 }) togetherWith
                                (fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { w -> if (forward) -w / 8 else w / 8 })
                        } else {
                            fadeIn(tween(320)) togetherWith fadeOut(tween(160))
                        }
                    },
                    label = "insightsWeek",
                    modifier = Modifier.fillMaxSize(),
                ) { s ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .padding(bar),
                    ) {
                        when (val content = s.content) {
                            Loadable.Loading -> InsightsSkeleton()
                            is Loadable.Failed -> ErrorState(content.message, onRetry = onRetry)
                            is Loadable.Ready -> InsightsBody(content.value, s)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                // Soft fade so cards melt under the pinned week switcher instead of being sliced off.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Brush.verticalGradient(listOf(c.canvas, c.canvas.copy(alpha = 0f)))),
                )
            }
        }
    }
}

// ---- Week switcher ------------------------------------------------------------------------------

@Composable
private fun WeekSwitcher(state: InsightsUiState, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = StudyNestTheme.colors
    val weeksAgo = ChronoUnit.WEEKS.between(state.weekStart, state.currentWeekStart).toInt()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(NestShapes.Pill)
            .background(c.surfaceRaised)
            .border(1.5.dp, c.hairline, NestShapes.Pill)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NestIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.insights_prev_week),
            onClick = onPrev,
            container = c.surfaceSunken,
            modifier = Modifier.testTag("insights_prev_week"),
        )
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                state.rangeLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("insights_week_label"),
            )
            Spacer(Modifier.height(3.dp))
            if (state.isCurrentWeek) {
                Row(
                    Modifier
                        .clip(NestShapes.Pill)
                        .background(c.mint.fill)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(c.mint.ink),
                    )
                    Text(stringResource(R.string.insights_this_week), style = MaterialTheme.typography.labelSmall, color = c.onPastel)
                }
            } else {
                Text(
                    if (weeksAgo == 1) stringResource(R.string.insights_last_week)
                    else pluralStringResource(R.plurals.insights_weeks_ago, weeksAgo, weeksAgo),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkMuted,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        NestIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.insights_next_week),
            onClick = onNext,
            enabled = state.canGoNext,
            container = c.surfaceSunken,
            modifier = Modifier.testTag("insights_next_week"),
        )
    }
}

// ---- Body ---------------------------------------------------------------------------------------

@Composable
private fun InsightsBody(w: WeekInsights, state: InsightsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Reveal(0) { HeroTotal(w, state.isCurrentWeek) }
        if (w.totalSeconds <= 0L) {
            Reveal(1) { EmptyWeek() }
            Reveal(2) { StatGrid(w) }
        } else {
            Reveal(1) { StatGrid(w) }
            Reveal(2) { DailyChartCard(w, state.today) }
            Reveal(3) { SubjectCard(w) }
        }
    }
}

@Composable
private fun HeroTotal(w: WeekInsights, current: Boolean) {
    val c = StudyNestTheme.colors
    val hours = Durations.hours(w.totalSeconds)
    val hoursQuantity = if (hours == "1") 1 else 2
    val sessions = pluralStringResource(R.plurals.insights_sessions, w.sessionCount, w.sessionCount)
    val description = stringResource(R.string.insights_total_description, hours, sessions)
    SquishySurface(
        onClick = null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_total")
            .clearAndSetSemantics { contentDescription = description },
        fill = c.lilac.fill,
        lip = c.lilac.lip,
        lipHeight = 5.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Decorative corner: a soft blob, a big sparkle and a couple of dots.
            SoftBlob(c.lilac.lip.copy(alpha = 0.45f), Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-40).dp), seed = 3)
            Sparkle(c.surfaceRaised, Modifier.size(34.dp).align(Alignment.TopEnd).offset(x = (-30).dp, y = 26.dp))
            Sparkle(c.butter.lip, Modifier.size(16.dp).align(Alignment.TopEnd).offset(x = (-72).dp, y = 70.dp), delayMillis = 600)
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c.surfaceRaised.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = c.lilac.ink, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (current) R.string.insights_hero_label else R.string.insights_hero_label_past),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.onPastel,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        hours,
                        style = NumberStyles.stat.copy(fontSize = 64.sp, lineHeight = 64.sp),
                        color = c.onPastel,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pluralStringResource(R.plurals.insights_hours_unit, hoursQuantity),
                        style = MaterialTheme.typography.titleLarge,
                        color = c.onPastel,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroPill(sessions)
                    if (w.totalSeconds > 0L) {
                        HeroPill(stringResource(R.string.insights_daily_avg, Durations.minutes(Durations.wholeMinutes(w.totalSeconds / 7))))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    val c = StudyNestTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = c.onPastel,
        maxLines = 1,
        modifier = Modifier
            .clip(NestShapes.Pill)
            .background(c.surfaceRaised.copy(alpha = if (c.isDark) 0.35f else 0.65f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatGrid(w: WeekInsights) {
    val c = StudyNestTheme.colors
    val goalDays = Weeks.daysMeetingGoal(w.days.map { it.seconds }, w.dailyGoalMinutes)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                title = stringResource(R.string.insights_streak_title),
                value = w.currentStreak.toString(),
                unit = stringResource(R.string.insights_streak_label),
                icon = Icons.Rounded.LocalFireDepartment,
                pastel = c.peach,
                filled = true,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .testTag("insights_streak"),
            )
            StatTile(
                title = stringResource(R.string.insights_longest_title),
                value = w.longestStreak.toString(),
                unit = pluralStringResource(R.plurals.insights_days_unit, w.longestStreak),
                icon = Icons.Rounded.EmojiEvents,
                pastel = c.butter,
                filled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                title = stringResource(R.string.insights_completed_title),
                value = w.assignmentsCompleted.toString(),
                unit = pluralStringResource(R.plurals.insights_completed_unit, w.assignmentsCompleted),
                icon = Icons.Rounded.TaskAlt,
                pastel = c.sky,
                filled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("insights_completed"),
            )
            StatTile(
                title = stringResource(R.string.insights_goal_title),
                value = stringResource(R.string.insights_goal_value, goalDays),
                unit = stringResource(R.string.insights_goal_unit),
                icon = Icons.Rounded.Flag,
                pastel = c.mint,
                filled = true,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    pastel: Pastel,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = StudyNestTheme.colors
    val ink = if (filled) c.onPastel else c.ink
    val muted = if (filled) c.onPastel.copy(alpha = 0.78f) else c.inkMuted
    val description = stringResource(R.string.insights_stat_description, title, value, unit)
    SquishySurface(
        onClick = null,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        fill = if (filled) pastel.fill else c.surface,
        lip = if (filled) pastel.lip else c.surfaceLip,
        border = if (filled) null else BorderStroke(1.5.dp, c.hairline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(NestShapes.Stamp)
                        .background(if (filled) c.surfaceRaised.copy(alpha = if (c.isDark) 0.4f else 0.7f) else pastel.fill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = pastel.ink, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(14.dp))
            Text(value, style = NumberStyles.stat.copy(fontSize = 34.sp, lineHeight = 36.sp), color = ink, maxLines = 1)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 2)
        }
    }
}

@Composable
private fun ChartCard(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val c = StudyNestTheme.colors
    SquishySurface(
        onClick = null,
        modifier = modifier.fillMaxWidth(),
        fill = c.surface,
        lip = c.surfaceLip,
        border = BorderStroke(1.5.dp, c.hairline),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 6.dp, height = 18.dp)
                        .clip(NestShapes.Pill)
                        .background(c.primary),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                trailing()
            }
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun DailyChartCard(w: WeekInsights, today: LocalDate) {
    val c = StudyNestTheme.colors
    val locale = Locale.getDefault()
    val goalText = Durations.minutes(w.dailyGoalMinutes)
    val minutesFmt = stringResource(R.string.insights_chart_minutes)
    val hoursFmt = stringResource(R.string.insights_chart_hours)
    val dayFmt = stringResource(R.string.insights_bar_day)
    val bars = Weeks.days(w.weekStart).map { date ->
        val seconds = w.days.firstOrNull { it.date == date }?.seconds ?: 0L
        val minutes = Durations.wholeMinutes(seconds)
        BarDay(
            minutes = minutes,
            label = when {
                minutes <= 0 -> ""
                minutes < 60 -> String.format(locale, minutesFmt, minutes)
                else -> String.format(locale, hoursFmt, Durations.hours(seconds))
            },
            dayInitial = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            isToday = date == today,
            metGoal = w.dailyGoalMinutes > 0 && seconds >= w.dailyGoalMinutes * 60L,
        )
    }
    val summary = Weeks.days(w.weekStart).zip(bars).joinToString(", ") { (date, bar) ->
        String.format(locale, dayFmt, date.dayOfWeek.getDisplayName(TextStyle.FULL, locale), Durations.minutes(bar.minutes))
    }
    val description = stringResource(R.string.insights_bar_description, summary, goalText)
    ChartCard(
        title = stringResource(R.string.insights_daily_title),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { }) {
                val dash = c.primaryText.copy(alpha = 0.7f)
                Canvas(Modifier.size(width = 18.dp, height = 4.dp)) {
                    drawLine(
                        dash, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(), StrokeCap.Round,
                        PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.insights_goal_legend, goalText), style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
            }
        },
    ) {
        WeeklyBarChart(
            days = bars,
            goalMinutes = w.dailyGoalMinutes,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("insights_bar_chart")
                .clearAndSetSemantics { contentDescription = description },
        )
    }
}

@Composable
private fun SubjectCard(w: WeekInsights) {
    val c = StudyNestTheme.colors
    val noSubject = stringResource(R.string.insights_no_subject)
    val entryFmt = stringResource(R.string.insights_donut_entry)
    val total = w.bySubject.sumOf { it.seconds }.coerceAtLeast(1L)
    val rows = w.bySubject.filter { it.seconds > 0 }.sortedByDescending { it.seconds }.map { s ->
        val color = s.subject?.let { c.subject(it.color).let { p -> if (c.isDark) p.fill else p.lip } }
            ?: c.inkMuted.copy(alpha = 0.5f)
        LegendRow(
            name = s.subject?.name ?: noSubject,
            hours = Durations.hours(s.seconds),
            percent = (s.seconds * 100.0 / total).roundToInt(),
            color = color,
            fraction = s.seconds.toFloat() / total,
        )
    }
    val description = stringResource(
        R.string.insights_donut_description,
        rows.joinToString(", ") { String.format(Locale.getDefault(), entryFmt, it.name, it.hours, it.percent) },
    )
    ChartCard(title = stringResource(R.string.insights_subject_title)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            SubjectDonut(
                slices = rows.map { DonutSlice(it.fraction, it.color) },
                modifier = Modifier
                    .size(196.dp)
                    .testTag("insights_donut")
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Durations.hours(w.totalSeconds), style = NumberStyles.stat, color = c.ink)
                    Text(stringResource(R.string.insights_donut_total_unit), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { row -> Legend(row) }
            }
        }
    }
}

private data class LegendRow(val name: String, val hours: String, val percent: Int, val color: Color, val fraction: Float)

@Composable
private fun Legend(row: LegendRow) {
    val c = StudyNestTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(NestShapes.Block)
            .background(c.surfaceSunken.copy(alpha = if (c.isDark) 0.6f else 0.7f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(row.color),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            row.name,
            style = MaterialTheme.typography.bodyLarge,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.insights_legend_hours, row.hours),
            style = NumberStyles.mono,
            color = c.ink,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .widthIn(min = 48.dp)
                .clip(NestShapes.Pill)
                .background(c.surfaceRaised)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.insights_legend_percent, row.percent), style = NumberStyles.mono.copy(fontSize = 12.sp), color = c.inkMuted)
        }
    }
}

@Composable
private fun EmptyWeek() {
    val c = StudyNestTheme.colors
    SquishySurface(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        fill = c.surface,
        lip = c.surfaceLip,
        border = BorderStroke(1.5.dp, c.hairline),
    ) {
        EmptyState(
            title = stringResource(R.string.insights_empty_title),
            message = stringResource(R.string.insights_empty_body),
            illustration = {
                NestIllustration(Modifier.size(width = 200.dp, height = 160.dp), books = 0, showPencil = false)
            },
        )
    }
}

@Composable
private fun InsightsSkeleton() {
    val loading = stringResource(R.string.loading)
    Column(
        Modifier.clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(168.dp), NestShapes.Card)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1.2f).height(118.dp), NestShapes.Card)
            SkeletonBlock(Modifier.weight(1f).height(118.dp), NestShapes.Card)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1f).height(118.dp), NestShapes.Card)
            SkeletonBlock(Modifier.weight(1.2f).height(118.dp), NestShapes.Card)
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(280.dp), NestShapes.Card)
    }
}
