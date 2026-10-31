package me.aysh.studynest.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import me.aysh.studynest.ui.components.drawSparkle
import me.aysh.studynest.ui.theme.StudyNestTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Immutable
private data class Piece(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val kind: Int,
    val color: Color,
    val spin: Float,
    val delay: Float,
)

/**
 * A one-shot confetti burst drawn from the centre of its bounds: pastel dots, gummy rectangles and sparkles
 * fly out, drift down with a little gravity and fade, while two soft shock-rings expand. Decorative only.
 * [intensity] (0..1) scales the number of pieces — stopped-early saves get a smaller pop.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, intensity: Float = 1f, seed: Int = 7) {
    val c = StudyNestTheme.colors
    val palette = listOf(c.lilac.lip, c.peach.lip, c.mint.lip, c.butter.lip, c.sky.lip, c.rose.lip, c.primary)
    val pieces = remember(seed, intensity, palette) {
        val r = Random(seed)
        List((44 * intensity).toInt().coerceAtLeast(12)) {
            Piece(
                angle = r.nextFloat() * 360f,
                speed = 0.45f + r.nextFloat() * 0.55f,
                size = 0.6f + r.nextFloat() * 0.6f,
                kind = r.nextInt(3),
                color = palette[r.nextInt(palette.size)],
                spin = (r.nextFloat() - 0.5f) * 720f,
                delay = r.nextFloat() * 0.12f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2200, easing = LinearEasing)) }
    val ringColor = c.mint.lip
    val ringColor2 = c.lilac.lip

    Canvas(modifier) {
        val time = t.value
        if (time >= 1f) return@Canvas
        val maxR = min(size.width, size.height) * 0.5f
        val unit = maxR / 22f
        // Shock rings.
        listOf(0f to ringColor, 0.12f to ringColor2).forEach { (delay, color) ->
            val local = ((time - delay) / 0.45f).coerceIn(0f, 1f)
            if (local in 0.001f..0.999f) {
                val e = 1f - (1f - local).pow(3)
                drawCircle(color.copy(alpha = (1f - local) * 0.7f), maxR * (0.35f + 0.75f * e), center, style = Stroke(unit * (1.6f - local)))
            }
        }
        pieces.forEach { p ->
            val local = ((time - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val ease = 1f - (1f - local).pow(3)
            val dist = maxR * (0.3f + 0.95f * p.speed) * ease
            val a = Math.toRadians(p.angle.toDouble())
            val x = center.x + cos(a).toFloat() * dist
            val y = center.y + sin(a).toFloat() * dist + maxR * 0.45f * local * local
            val pop = (local / 0.08f).coerceAtMost(1f)
            val alpha = if (local < 0.65f) 1f else (1f - (local - 0.65f) / 0.35f).coerceIn(0f, 1f)
            val s = unit * p.size * pop
            val color = p.color.copy(alpha = alpha)
            val pos = Offset(x, y)
            when (p.kind) {
                0 -> drawCircle(color, s * 0.75f, pos)
                1 -> rotate(p.spin * local, pos) {
                    drawRoundRect(color, Offset(x - s * 1.1f, y - s * 0.45f), Size(s * 2.2f, s * 0.9f), CornerRadius(s * 0.45f))
                }
                else -> drawSparkle(pos, s * 1.5f, color)
            }
        }
    }
}
