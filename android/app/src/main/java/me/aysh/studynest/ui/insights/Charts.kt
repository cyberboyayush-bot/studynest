package me.aysh.studynest.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.components.drawSparkle
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.ui.theme.tabular
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** One day in [WeeklyBarChart]. [label] is the value printed above the bar (blank for zero days). */
@Immutable
data class BarDay(
    val minutes: Int,
    val label: String,
    val dayInitial: String,
    val isToday: Boolean,
    val metGoal: Boolean,
)

/**
 * Seven chunky pill bars (Mon–Sun) on soft "ghost" tracks, a dashed daily-goal line, value labels, a sparkle
 * on days that met the goal and today's bar in the brand colour. Bars spring up one after another on first
 * composition. Purely visual — put the summary contentDescription and test tag on [modifier].
 */
@Composable
fun WeeklyBarChart(days: List<BarDay>, goalMinutes: Int, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.tabular()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1250, easing = LinearEasing)) }

    val barColor = c.lilac.lip
    val todayColor = c.primary
    val track = c.surfaceSunken
    val gloss = Color.White.copy(alpha = if (c.isDark) 0.14f else 0.32f)
    val goalColor = c.primaryText.copy(alpha = 0.7f)
    val sparkle = c.butter.lip
    val labelChip = c.surface

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(184.dp),
        ) {
            val top = 26.dp.toPx()
            val area = size.height - top
            val slot = size.width / 7f
            val barW = min(slot * 0.56f, 34.dp.toPx())
            val radius = CornerRadius(barW / 2f)
            val maxMinutes = max(days.maxOfOrNull { it.minutes } ?: 0, max(goalMinutes, 30)) * 1.12f
            val total = 1250f
            fun progressOf(i: Int) = ((grow.value * total - i * 75f) / 700f).coerceIn(0f, 1f)
            fun barTop(i: Int, day: BarDay): Float {
                val full = max(area * (day.minutes / maxMinutes), barW)
                return top + area - (full * easeOutBack(progressOf(i))).coerceAtLeast(0f)
            }
            // Pass 1: ghost tracks and bars.
            days.forEachIndexed { i, day ->
                val x = slot * i + (slot - barW) / 2f
                drawRoundRect(track, Offset(x, top), Size(barW, area), radius)
                if (day.minutes <= 0) return@forEachIndexed
                val y = barTop(i, day)
                val h = top + area - y
                if (h <= 0.5f) return@forEachIndexed
                drawRoundRect(if (day.isToday) todayColor else barColor, Offset(x, y), Size(barW, h), radius)
                // Gummy gloss stripe.
                if (h > barW) {
                    drawRoundRect(
                        gloss,
                        Offset(x + barW * 0.2f, y + barW * 0.28f),
                        Size(barW * 0.16f, (h - barW * 0.56f).coerceAtLeast(0f)),
                        CornerRadius(barW * 0.08f),
                    )
                }
            }
            // Pass 2: dashed daily-goal line, over the bars so it stays readable.
            if (goalMinutes > 0) {
                val gy = top + area * (1f - goalMinutes / maxMinutes)
                val reveal = ((grow.value * total - 250f) / 600f).coerceIn(0f, 1f)
                drawLine(
                    goalColor,
                    Offset(0f, gy),
                    Offset(size.width * FastOutSlowInEasing.transform(reveal), gy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
                )
            }
            // Pass 3: value labels on little card-coloured chips (the goal line tucks behind them) + sparkles.
            days.forEachIndexed { i, day ->
                if (day.minutes <= 0 || day.label.isEmpty()) return@forEachIndexed
                val alpha = ((progressOf(i) - 0.35f) / 0.65f).coerceIn(0f, 1f)
                if (alpha <= 0f) return@forEachIndexed
                val x = slot * i + (slot - barW) / 2f
                val y = barTop(i, day)
                val style = labelStyle.copy(
                    color = (if (day.isToday) c.primaryText else c.inkMuted).copy(alpha = alpha),
                    fontWeight = if (day.isToday) FontWeight(800) else labelStyle.fontWeight,
                    textAlign = TextAlign.Center,
                )
                val layout = measurer.measure(day.label, style)
                val padX = 4.dp.toPx()
                val tl = Offset(x + barW / 2f - layout.size.width / 2f, y - layout.size.height - 5.dp.toPx())
                drawRoundRect(
                    labelChip.copy(alpha = alpha),
                    Offset(tl.x - padX, tl.y - 1.dp.toPx()),
                    Size(layout.size.width + padX * 2, layout.size.height + 2.dp.toPx()),
                    CornerRadius(layout.size.height / 2f),
                )
                drawText(layout, topLeft = tl)
                if (day.metGoal) {
                    drawSparkle(Offset(x + barW * 0.95f, y + 2.dp.toPx()), 6.dp.toPx() * alpha, sparkle.copy(alpha = alpha))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(NestShapes.Pill)
                            .background(if (day.isToday) c.primary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            day.dayInitial,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (day.isToday) c.onPrimary else c.inkMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** A slice of [SubjectDonut]. */
@Immutable
data class DonutSlice(val fraction: Float, val color: Color)

/**
 * Time-per-subject donut: thick rounded segments separated by small gaps on a soft track, sweeping in
 * clockwise from 12 o'clock with a little twist. [centerContent] is drawn in the hole (e.g. the total).
 * Put the summary contentDescription / test tag on [modifier].
 */
@Composable
fun SubjectDonut(slices: List<DonutSlice>, modifier: Modifier = Modifier, centerContent: @Composable BoxScope.() -> Unit = {}) {
    val c = StudyNestTheme.colors
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val track = c.surfaceSunken
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val d = min(size.width, size.height)
            val stroke = d * 0.16f
            val r = d / 2f - stroke / 2f
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2, r * 2)
            drawCircle(track, r, center, style = Stroke(stroke))
            val reveal = 360f * sweep.value
            val twist = -40f * (1f - sweep.value)
            rotate(twist, center) {
                if (slices.size == 1) {
                    drawArc(slices[0].color, -90f, reveal, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    return@rotate
                }
                val capDeg = Math.toDegrees((stroke / 2f / r).toDouble()).toFloat()
                val gapDeg = 5f
                var cursor = 0f
                slices.forEach { slice ->
                    val span = slice.fraction * 360f
                    val start = cursor + capDeg + gapDeg / 2f
                    val end = cursor + span - capDeg - gapDeg / 2f
                    cursor += span
                    if (start >= reveal) return@forEach
                    if (end <= start) {
                        // Too small for an arc: a single round dot in the middle of its span.
                        val mid = Math.toRadians((cursor - span / 2f - 90f).toDouble())
                        if (cursor - span / 2f <= reveal) {
                            drawCircle(
                                slice.color,
                                stroke / 2f,
                                Offset(center.x + r * cos(mid).toFloat(), center.y + r * sin(mid).toFloat()),
                            )
                        }
                        return@forEach
                    }
                    val visibleEnd = min(end, reveal)
                    drawArc(
                        slice.color,
                        start - 90f,
                        (visibleEnd - start).coerceAtLeast(0.01f),
                        false,
                        topLeft,
                        arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        centerContent()
    }
}

/** Overshooting ease (a soft "boing") for bar growth. */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.45f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}
