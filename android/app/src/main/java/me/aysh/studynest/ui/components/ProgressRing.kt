package me.aysh.studynest.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.theme.StudyNestTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws a ring (track + progress arc with rounded caps, starting at 12 o'clock) into this DrawScope.
 * Use inside your own Canvas when you need extra layers (glow, ticks, confetti).
 *
 * @param progress 0..1 (values > 1 are clamped)
 * @param dashedTrack draws the track as soft dashes (e.g. paused timer)
 * @param glowAlpha > 0 draws a soft wide halo under the arc (running timer pulse)
 * @param knob draws a small dot at the arc's leading end
 */
fun DrawScope.drawProgressRing(
    progress: Float,
    strokeWidth: Float,
    trackColor: Color,
    progressColor: Color,
    dashedTrack: Boolean = false,
    glowAlpha: Float = 0f,
    knob: Boolean = false,
    knobColor: Color = Color.White,
) {
    val p = progress.coerceIn(0f, 1f)
    val inset = strokeWidth / 2f + if (glowAlpha > 0f) strokeWidth * 0.6f else 0f
    val d = min(size.width, size.height) - inset * 2
    val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
    val arcSize = Size(d, d)
    val track = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        pathEffect = if (dashedTrack) PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 0.2f, strokeWidth * 1.4f)) else null,
    )
    drawArc(trackColor, -90f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = track)
    if (p <= 0f) return
    val sweep = 360f * p
    if (glowAlpha > 0f) {
        drawArc(
            progressColor.copy(alpha = 0.22f * glowAlpha),
            -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize,
            style = Stroke(width = strokeWidth * 2.1f, cap = StrokeCap.Round),
        )
    }
    drawArc(progressColor, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    if (knob && p < 1f) {
        val a = Math.toRadians((sweep - 90f).toDouble())
        val r = d / 2f
        val c = Offset(topLeft.x + r + (r * cos(a)).toFloat(), topLeft.y + r + (r * sin(a)).toFloat())
        drawCircle(knobColor, strokeWidth * 0.28f, c)
    }
}

/**
 * Animated progress ring with centred content slot. Reused by the Today goal ring, the Focus timer, the
 * Focus preview, and the bottom-bar mini ring.
 *
 * The arc animates from 0 on first composition ("sweeps in") and then tweens to each new [progress] with
 * [animationSpec]. For a ticking timer pass a short linear tween (e.g. `tween(250, easing = LinearEasing)`)
 * so it moves continuously. Add semantics (contentDescription / progressBarRangeInfo) on [modifier].
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = StudyNestTheme.colors.surfaceSunken,
    progressColor: Color = StudyNestTheme.colors.primary,
    dashedTrack: Boolean = false,
    glowAlpha: Float = 0f,
    knob: Boolean = false,
    animationSpec: AnimationSpec<Float> = tween(900, easing = FastOutSlowInEasing),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) { animated.animateTo(progress.coerceIn(0f, 1f), animationSpec) }
    val knobColor = StudyNestTheme.colors.surfaceRaised
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawProgressRing(
                progress = animated.value,
                strokeWidth = strokeWidth.toPx(),
                trackColor = trackColor,
                progressColor = progressColor,
                dashedTrack = dashedTrack,
                glowAlpha = glowAlpha,
                knob = knob,
                knobColor = knobColor,
            )
        }
        content()
    }
}
