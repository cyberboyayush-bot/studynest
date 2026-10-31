package me.aysh.studynest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.aysh.studynest.R

private fun fredoka(weight: Int) = Font(
    R.font.fredoka,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(100f)),
)

private fun figtree(weight: Int) = Font(
    R.font.figtree,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Rounded, friendly display face: headlines, titles, big numbers. */
val Fredoka = FontFamily(fredoka(400), fredoka(500), fredoka(600), fredoka(700))

/** Clean UI/body face. Supports tabular figures (`tnum`). */
val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700), figtree(800))

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun display(size: Int, line: Int, weight: Int = 600, tracking: Double = -0.01) = TextStyle(
    fontFamily = Fredoka,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
)

private fun body(size: Int, line: Int, weight: Int = 400, tracking: Double = 0.0) = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
)

val StudyNestTypography = Typography(
    displayLarge = display(54, 60),
    displayMedium = display(44, 50),
    displaySmall = display(36, 42),
    headlineLarge = display(32, 38),
    headlineMedium = display(28, 34),
    headlineSmall = display(24, 30, weight = 500),
    titleLarge = display(21, 27, weight = 500, tracking = 0.0),
    titleMedium = body(16, 22, weight = 700),
    titleSmall = body(14, 20, weight = 700),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16, weight = 500),
    labelLarge = body(15, 20, weight = 700, tracking = 0.005),
    labelMedium = body(13, 16, weight = 600, tracking = 0.01),
    labelSmall = body(11, 14, weight = 700, tracking = 0.04),
)

/** Returns this style with tabular (fixed-width) digits so changing numbers don't jiggle. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * Number styles that aren't part of M3 [Typography]. All use tabular digits.
 * - [timer]: the huge Focus countdown.
 * - [stat]: hero stat numbers ("12.5 h").
 * - [statSmall]: numbers in stat tiles / rings.
 * - [mono]: small inline counters ("45 of 90 min").
 */
object NumberStyles {
    val timer = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight(700),
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    )
    val stat = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight(600),
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    )
    val statSmall = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight(600),
        fontSize = 26.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    )
    val mono = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    )
}
