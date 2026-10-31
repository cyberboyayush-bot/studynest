package me.aysh.studynest.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.components.drawSparkle
import me.aysh.studynest.ui.theme.StudyNestColors
import me.aysh.studynest.ui.theme.StudyNestTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** What the dial is showing. */
enum class DialMode { Preview, Running, Paused, Saving }

/**
 * The Focus "kitchen timer": a raised warm disc with a sticker lip, 60 minute ticks, a thick rounded track and a
 * gradient progress arc with a knob at its leading end.
 *
 * - [DialMode.Preview]: [progress] is the chosen length as a fraction of an hour; springs when it changes.
 * - [DialMode.Running]: the arc fills continuously (short linear tweens between ticks) under a breathing glow.
 * - [DialMode.Paused]: calmer — muted arc, dashed track, no glow.
 * - [DialMode.Saving]: a soft arc orbits the track.
 *
 * The arc sweeps in from zero the first time the dial appears. Add semantics on [modifier].
 */
@Composable
fun FocusDial(
    progress: Float,
    mode: DialMode,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = StudyNestTheme.colors
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

    val shown = remember { Animatable(progress.coerceIn(0f, 1f)) }
    LaunchedEffect(progress, mode) {
        val target = progress.coerceIn(0f, 1f)
        if (mode == DialMode.Preview) {
            shown.animateTo(target, spring(dampingRatio = 0.72f, stiffness = 170f))
        } else {
            shown.animateTo(target, tween(260, easing = LinearEasing))
        }
    }

    val calm by animateFloatAsState(if (mode == DialMode.Paused) 1f else 0f, tween(500), label = "calm")
    val glow by animateFloatAsState(if (mode == DialMode.Running) 1f else 0f, tween(600), label = "glow")

    val loop = rememberInfiniteTransition(label = "dialLoop")
    val pulse = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val spin = loop.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "spin")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawDial(
                c = c,
                progress = if (mode == DialMode.Saving) 0f else shown.value * intro.value,
                calm = calm,
                glow = glow * (0.55f + 0.45f * pulse.value),
                spinAngle = if (mode == DialMode.Saving) spin.value else null,
            )
        }
        content()
    }
}

private fun DrawScope.drawDial(
    c: StudyNestColors,
    progress: Float,
    calm: Float,
    glow: Float,
    spinAngle: Float?,
) {
    val d = min(size.width, size.height)
    val lipH = d * 0.022f
    val cx = size.width / 2f
    val cy = size.height / 2f - lipH / 2f
    val discR = d / 2f - lipH / 2f
    val stroke = d * 0.082f
    val pad = d * 0.05f
    val arcR = discR - pad - stroke / 2f
    val center = Offset(cx, cy)

    // Gummy disc: lip, face, hairline.
    drawCircle(c.surfaceLip, discR, Offset(cx, cy + lipH))
    drawCircle(c.surfaceRaised, discR, center)
    drawCircle(c.hairline, discR - 0.5.dp.toPx(), center, style = Stroke(1.dp.toPx()))

    // Track: soft lilac wash, dashed while paused.
    val trackColor = lerp(c.lilac.fill.copy(alpha = if (c.isDark) 0.2f else 0.55f), c.hairline, calm * 0.6f)
    val arcTopLeft = Offset(cx - arcR, cy - arcR)
    val arcSize = Size(arcR * 2, arcR * 2)
    drawArc(trackColor, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(stroke))
    if (calm > 0.01f) {
        // Paused: a fine dotted "stitch" runs along the track, like a timer on hold.
        val dot = stroke * 0.11f
        val circumference = (2 * Math.PI * arcR).toFloat()
        val gap = circumference / (circumference / (stroke * 0.62f)).toInt().coerceAtLeast(1)
        drawArc(
            c.inkMuted.copy(alpha = 0.32f * calm), 0f, 360f, false, arcTopLeft, arcSize,
            style = Stroke(dot * 2, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, gap))),
        )
    }

    val p = progress.coerceIn(0f, 1f)
    val sweep = 360f * p

    // Minute ticks inside the track; those already "passed" pick up the brand colour.
    val tickOuter = arcR - stroke / 2f - d * 0.035f
    val passedTick = lerp(c.primary, c.inkMuted, calm).copy(alpha = 0.85f)
    for (i in 0 until 60) {
        val deg = i * 6f
        val a = Math.toRadians((deg - 90f).toDouble())
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val passed = p > 0f && deg <= sweep + 0.01f
        if (i % 5 == 0) {
            val len = d * 0.032f
            drawLine(
                if (passed) passedTick else c.inkMuted.copy(alpha = 0.38f),
                Offset(cx + ca * tickOuter, cy + sa * tickOuter),
                Offset(cx + ca * (tickOuter - len), cy + sa * (tickOuter - len)),
                strokeWidth = d * 0.009f,
                cap = StrokeCap.Round,
            )
        } else {
            drawCircle(
                if (passed) passedTick.copy(alpha = 0.6f) else c.inkMuted.copy(alpha = 0.2f),
                d * 0.0045f,
                Offset(cx + ca * (tickOuter - d * 0.006f), cy + sa * (tickOuter - d * 0.006f)),
            )
        }
    }

    if (spinAngle != null) {
        rotate(spinAngle, center) {
            drawArc(
                Brush.sweepGradient(listOf(c.primary.copy(alpha = 0f), c.primary), center),
                -90f, 110f, false, arcTopLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        return
    }
    if (p <= 0f) return

    // Breathing glow: a few wide translucent strokes under the arc.
    if (glow > 0.01f) {
        listOf(2.05f to 0.05f, 1.7f to 0.07f, 1.4f to 0.1f, 1.18f to 0.12f).forEach { (w, a) ->
            drawArc(
                c.primary.copy(alpha = a * glow * (if (c.isDark) 1.3f else 1f)),
                -90f, sweep, false, arcTopLeft, arcSize,
                style = Stroke(stroke * w, cap = StrokeCap.Round),
            )
        }
    }

    // Progress arc: light lilac at the start deepening to brand at the leading edge. The gradient wraps
    // back to the start colour after the head so the start cap never shows a seam.
    val start = lerp(c.lilac.lip, c.hairline, calm * 0.5f)
    val head = lerp(c.primary, c.lilac.lip, calm)
    val brush = if (p < 0.999f) {
        Brush.sweepGradient(
            0f to start,
            (p * 0.999f).coerceAtLeast(0.001f) to head,
            1f to start,
            center = center,
        )
    } else Brush.sweepGradient(listOf(start, head, start), center)
    rotate(-90f, center) {
        drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }

    // Knob at the leading edge.
    if (p < 0.999f) {
        val a = Math.toRadians((sweep - 90f).toDouble())
        val k = Offset(cx + arcR * cos(a).toFloat(), cy + arcR * sin(a).toFloat())
        if (glow > 0.01f) drawCircle(c.surfaceRaised.copy(alpha = 0.35f * glow), stroke * 0.62f, k)
        drawCircle(c.surfaceRaised, stroke * 0.3f, k)
        if (calm > 0.01f) drawCircle(head.copy(alpha = calm), stroke * 0.14f, k)
    }

    // Tiny sparkle riding just outside the head while running.
    if (glow > 0.01f && p < 0.999f) {
        val a = Math.toRadians((sweep - 90f + 8f).toDouble())
        val r = arcR + stroke * 0.95f
        val s = Offset(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
        drawSparkle(s, stroke * 0.32f * (0.7f + 0.3f * glow), c.butter.lip.copy(alpha = glow))
    }
}
