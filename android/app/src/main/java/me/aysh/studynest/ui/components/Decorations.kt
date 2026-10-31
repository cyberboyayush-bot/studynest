package me.aysh.studynest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import me.aysh.studynest.ui.theme.StudyNestColors
import me.aysh.studynest.ui.theme.StudyNestTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Paper and pencil-lead tones in illustrations stay the same in both themes. */
private val Paper = Color(0xFFFFFDF8)
private val PaperInk = Color(0xFF2B2340)

// ---- DrawScope primitives (reuse these inside your own Canvas) --------------------------------------

/**
 * A soft organic blob around [center]. [phase] (radians) morphs the outline — animate it for a gentle
 * "breathing" shape. [seed] varies the silhouette between blobs.
 */
fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, phase: Float = 0f, seed: Int = 0) {
    val points = 7
    val pts = List(points) { i ->
        val a = (2 * PI * i / points).toFloat() + seed * 0.7f
        val r = radius * (1f + 0.09f * sin(seed * 1.9f + i * 2.3f + phase) + 0.05f * cos(i * 1.3f - phase * 0.7f))
        Offset(center.x + r * cos(a), center.y + r * sin(a))
    }
    val path = Path()
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val start = mid(pts.last(), pts.first())
    path.moveTo(start.x, start.y)
    for (i in pts.indices) {
        val p = pts[i]
        val m = mid(p, pts[(i + 1) % points])
        path.quadraticTo(p.x, p.y, m.x, m.y)
    }
    path.close()
    drawPath(path, color)
}

/** A four-point sparkle/star with pinched sides, centred on [center]. */
fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val pinch = radius * 0.16f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + pinch, center.y - pinch, center.x + radius, center.y)
        quadraticTo(center.x + pinch, center.y + pinch, center.x, center.y + radius)
        quadraticTo(center.x - pinch, center.y + pinch, center.x - radius, center.y)
        quadraticTo(center.x - pinch, center.y - pinch, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

/** Strokes [path] as a row of round dots ([gap] px apart) — for dotted trails and goal lines. */
fun DrawScope.drawDottedPath(path: Path, color: Color, dotRadius: Float, gap: Float) {
    drawPath(
        path,
        color,
        style = Stroke(
            width = dotRadius * 2,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, gap), 0f),
        ),
    )
}

// ---- Composables ------------------------------------------------------------------------------------

/** A single slowly morphing blob that fills its bounds. Hero backgrounds only. */
@Composable
fun SoftBlob(color: Color, modifier: Modifier = Modifier, seed: Int = 0, animated: Boolean = true) {
    val t = rememberInfiniteTransition(label = "blob")
    // Read only in the draw phase so the morph never triggers recomposition.
    val phase = t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000 + seed * 700, easing = LinearEasing)), label = "blobPhase")
    Canvas(modifier) {
        drawBlob(center, min(size.width, size.height) / 2f * 0.9f, color, if (animated) phase.value else 0f, seed)
    }
}

/** A twinkling sparkle (gentle scale + alpha pulse). Decorative only. */
@Composable
fun Sparkle(color: Color, modifier: Modifier = Modifier, delayMillis: Int = 0) {
    val t = rememberInfiniteTransition(label = "sparkle")
    val pulse by t.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, delayMillis = delayMillis), RepeatMode.Reverse),
        label = "sparklePulse",
    )
    Canvas(modifier) {
        drawSparkle(center, min(size.width, size.height) / 2f * pulse, color.copy(alpha = 0.55f + 0.45f * pulse))
    }
}

/**
 * The StudyNest mark (same motif as the launcher icon): a woven nest cradling two books, with a sparkle.
 * Transparent background — place it on a surface/pastel squircle. Draws into its bounds (keep it square).
 * Pure decoration — give the parent a description if needed.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    Canvas(modifier) {
        // The mark spans roughly x 6..104, y 8..96 in its 100-unit design space.
        val s = min(size.width, size.height) / 100f
        translate((size.width - 100 * s) / 2f - 5f * s, (size.height - 100 * s) / 2f - 2f * s) {
            scale(s, s, pivot = Offset.Zero) { drawMark(c) }
        }
    }
}

private fun DrawScope.drawMark(c: StudyNestColors) {
    // back of nest
    drawOval(c.nestDeep, topLeft = Offset(10f, 52f), size = Size(80f, 18f))
    // books
    rotate(-9f, pivot = Offset(38f, 56f)) {
        drawRoundRect(c.mint.lip, topLeft = Offset(22f, 24f), size = Size(30f, 40f), cornerRadius = CornerRadius(6f))
        drawRoundRect(c.mint.fill, topLeft = Offset(22f, 22f), size = Size(30f, 40f), cornerRadius = CornerRadius(6f))
        drawRoundRect(c.mint.ink.copy(alpha = 0.35f), topLeft = Offset(27f, 22f), size = Size(4f, 40f))
    }
    rotate(8f, pivot = Offset(62f, 56f)) {
        drawRoundRect(c.peach.lip, topLeft = Offset(47f, 18f), size = Size(32f, 44f), cornerRadius = CornerRadius(6f))
        drawRoundRect(c.peach.fill, topLeft = Offset(47f, 16f), size = Size(32f, 44f), cornerRadius = CornerRadius(6f))
        drawRoundRect(c.peach.ink.copy(alpha = 0.3f), topLeft = Offset(53f, 16f), size = Size(4f, 44f))
    }
    // nest bowl
    val bowl = Path().apply {
        moveTo(8f, 58f)
        cubicTo(8f, 82f, 28f, 92f, 50f, 92f)
        cubicTo(72f, 92f, 92f, 82f, 92f, 58f)
        cubicTo(70f, 66f, 30f, 66f, 8f, 58f)
        close()
    }
    drawPath(bowl, c.nest)
    val weave = Stroke(width = 3.2f, cap = StrokeCap.Round)
    listOf(70f, 78f, 86f).forEachIndexed { i, y ->
        val p = Path().apply {
            moveTo(16f + i * 5f, y - 6f)
            quadraticTo(50f, y + 6f, 84f - i * 5f, y - 6f)
        }
        drawPath(p, c.nestDeep.copy(alpha = 0.75f), style = weave)
    }
    // rim
    val rim = Path().apply {
        moveTo(8f, 58f)
        cubicTo(30f, 68f, 70f, 68f, 92f, 58f)
    }
    drawPath(rim, c.nest, style = Stroke(width = 9f, cap = StrokeCap.Round))
    drawPath(rim, c.nestDeep.copy(alpha = 0.5f), style = Stroke(width = 2.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))))
    drawSparkle(Offset(95f, 34f), 8f, c.primary)
}

/**
 * The cosy hero illustration: a woven nest cradling a stack of books, a tiny alarm clock and a pencil,
 * with sparkles. [bob] makes the stack float gently. [books] (0–3), [showClock] and [showPencil] let empty
 * states use a sparser version. Decorative: pass `Modifier.semantics { contentDescription = … }` or keep it hidden.
 */
@Composable
fun NestIllustration(
    modifier: Modifier = Modifier,
    bob: Boolean = true,
    books: Int = 3,
    showClock: Boolean = true,
    showPencil: Boolean = true,
) {
    val c = StudyNestTheme.colors
    val t = rememberInfiniteTransition(label = "nest")
    val waveState = t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "nestWave",
    )
    Canvas(modifier) {
        val wave = waveState.value
        val lift = if (bob) sin(wave) else 0f
        val s = min(size.width / 320f, size.height / 280f)
        translate((size.width - 320 * s) / 2f, (size.height - 280 * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                drawNestScene(c, lift, wave, books.coerceIn(0, 3), showClock, showPencil)
            }
        }
    }
}

private fun DrawScope.drawNestScene(
    c: StudyNestColors,
    lift: Float,
    wave: Float,
    books: Int,
    showClock: Boolean,
    showPencil: Boolean,
) {
    val ink = c.ink
    val bobY = -6f * lift

    // Ground shadow breathes opposite to the bob.
    val shadowW = 210f * (1f - 0.04f * lift)
    drawOval(if (c.isDark) Color.Black.copy(alpha = 0.32f) else ink.copy(alpha = 0.08f), topLeft = Offset(160f - shadowW / 2f, 252f), size = Size(shadowW, 20f))

    // Back sparkles
    drawSparkle(Offset(52f, 70f), 13f * (0.85f + 0.15f * sin(wave * 2f)), c.butter.lip)
    drawSparkle(Offset(276f, 112f), 9f * (0.85f + 0.15f * cos(wave * 2f)), c.lilac.lip)
    drawCircle(c.peach.lip, 4f, Offset(262f, 50f))
    drawCircle(c.mint.lip, 3.5f, Offset(34f, 128f))

    translate(0f, bobY) {
        // Back of the nest
        drawOval(c.nestDeep, topLeft = Offset(44f, 160f), size = Size(232f, 50f))

        if (showPencil) {
            rotate(-24f, pivot = Offset(96f, 150f)) {
                drawRoundRect(c.butter.fill, topLeft = Offset(84f, 52f), size = Size(22f, 120f), cornerRadius = CornerRadius(5f))
                drawRoundRect(c.butter.ink.copy(alpha = 0.25f), topLeft = Offset(98f, 52f), size = Size(8f, 120f), cornerRadius = CornerRadius(4f))
                // eraser + band
                drawRoundRect(c.rose.fill, topLeft = Offset(84f, 160f), size = Size(22f, 18f), cornerRadius = CornerRadius(6f))
                drawRect(c.sky.lip, topLeft = Offset(84f, 152f), size = Size(22f, 9f))
                // tip
                val tip = Path().apply {
                    moveTo(84f, 53f); lineTo(95f, 26f); lineTo(106f, 53f); close()
                }
                drawPath(tip, c.nest)
                val lead = Path().apply {
                    moveTo(91.5f, 35f); lineTo(95f, 26f); lineTo(98.5f, 35f); close()
                }
                drawPath(lead, PaperInk)
            }
        }

        // Stack of books (bottom → top)
        val bookSpecs = listOf(
            Triple(c.lilac, Offset(96f, 142f), -2.5f) to Size(136f, 34f),
            Triple(c.mint, Offset(108f, 112f), 3f) to Size(116f, 32f),
            Triple(c.rose, Offset(100f, 84f), -4f) to Size(108f, 30f),
        ).take(books)
        bookSpecs.forEach { (spec, sz) ->
            val (pastel, topLeft, angle) = spec
            rotate(angle, pivot = Offset(topLeft.x + sz.width / 2f, topLeft.y + sz.height / 2f)) {
                drawRoundRect(pastel.lip, topLeft = topLeft + Offset(0f, 4f), size = sz, cornerRadius = CornerRadius(8f))
                drawRoundRect(pastel.fill, topLeft = topLeft, size = sz, cornerRadius = CornerRadius(8f))
                // page block on the right
                drawRoundRect(
                    Paper,
                    topLeft = Offset(topLeft.x + sz.width - 18f, topLeft.y + 5f),
                    size = Size(12f, sz.height - 10f),
                    cornerRadius = CornerRadius(3f),
                )
                // spine bands + label
                drawRect(pastel.ink.copy(alpha = 0.28f), topLeft = Offset(topLeft.x + 14f, topLeft.y), size = Size(5f, sz.height))
                drawRect(pastel.ink.copy(alpha = 0.28f), topLeft = Offset(topLeft.x + 23f, topLeft.y), size = Size(3f, sz.height))
                drawRoundRect(
                    Paper.copy(alpha = 0.85f),
                    topLeft = Offset(topLeft.x + sz.width * 0.38f, topLeft.y + sz.height * 0.32f),
                    size = Size(sz.width * 0.3f, sz.height * 0.36f),
                    cornerRadius = CornerRadius(4f),
                )
            }
        }

        if (showClock) {
            // Sits on the top book, or inside the nest when there are no books.
            val top = when (books) {
                0 -> 170f
                1 -> 142f
                2 -> 112f
                else -> 84f
            }
            val cy = top - 26f
            val cx = 186f
            // bells
            drawCircle(c.sky.lip, 9f, Offset(cx - 16f, cy - 20f))
            drawCircle(c.sky.lip, 9f, Offset(cx + 16f, cy - 20f))
            // feet
            drawLine(c.sky.ink, Offset(cx - 14f, cy + 20f), Offset(cx - 20f, cy + 28f), 4f, StrokeCap.Round)
            drawLine(c.sky.ink, Offset(cx + 14f, cy + 20f), Offset(cx + 20f, cy + 28f), 4f, StrokeCap.Round)
            drawCircle(c.sky.fill, 26f, Offset(cx, cy))
            drawCircle(Paper, 19f, Offset(cx, cy))
            // ticks
            for (i in 0 until 4) {
                val a = (PI / 2 * i).toFloat()
                drawCircle(c.sky.ink.copy(alpha = 0.6f), 1.8f, Offset(cx + 14f * cos(a), cy + 14f * sin(a)))
            }
            // hands — the minute hand sweeps slowly with the wave
            val m = wave - (PI / 2).toFloat()
            drawLine(PaperInk, Offset(cx, cy), Offset(cx + 12f * cos(m), cy + 12f * sin(m)), 3f, StrokeCap.Round)
            drawLine(PaperInk, Offset(cx, cy), Offset(cx - 6f, cy - 6f), 3.4f, StrokeCap.Round)
            drawCircle(c.overdueAccent, 2.8f, Offset(cx, cy))
        }

        // Front of the nest
        val bowl = Path().apply {
            moveTo(36f, 180f)
            cubicTo(40f, 236f, 100f, 256f, 160f, 256f)
            cubicTo(220f, 256f, 280f, 236f, 284f, 180f)
            cubicTo(230f, 200f, 90f, 200f, 36f, 180f)
            close()
        }
        drawPath(bowl, c.nest)
        val weave = Stroke(width = 5f, cap = StrokeCap.Round)
        listOf(206f, 222f, 238f).forEachIndexed { i, y ->
            val inset = i * 16f
            val p = Path().apply {
                moveTo(52f + inset, y - 12f)
                quadraticTo(160f, y + 18f, 268f - inset, y - 12f)
            }
            drawPath(p, c.nestDeep.copy(alpha = 0.7f), style = weave)
        }
        // criss-cross twig dashes
        for (i in 0 until 7) {
            val x = 78f + i * 28f
            drawLine(c.nestDeep.copy(alpha = 0.45f), Offset(x, 204f), Offset(x + 12f, 226f), 4f, StrokeCap.Round)
        }
        // chunky rim
        val rim = Path().apply {
            moveTo(34f, 180f)
            cubicTo(90f, 202f, 230f, 202f, 286f, 180f)
        }
        drawPath(rim, c.nest, style = Stroke(width = 20f, cap = StrokeCap.Round))
        drawPath(
            rim,
            c.nestDeep.copy(alpha = 0.55f),
            style = Stroke(width = 4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
        )
        // stray twigs
        drawLine(c.nestDeep, Offset(30f, 176f), Offset(14f, 164f), 4f, StrokeCap.Round)
        drawLine(c.nestDeep, Offset(290f, 176f), Offset(306f, 168f), 4f, StrokeCap.Round)
        drawLine(c.nestDeep, Offset(296f, 182f), Offset(310f, 186f), 3.5f, StrokeCap.Round)
    }

    // Front sparkle
    drawSparkle(Offset(250f, 228f + bobY * 0.3f), 8f, c.peach.lip)
}
