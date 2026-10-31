package me.aysh.studynest.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.aysh.studynest.data.model.SubjectColor

/**
 * A pastel swatch: [fill] is the soft block colour, [ink] a deep tone of the same hue for icons/text drawn
 * on top of the fill (>= 4.5:1), and [lip] a slightly deeper tone used for the "gummy sticker" offset shadow.
 */
@Immutable
data class Pastel(val fill: Color, val ink: Color, val lip: Color)

/**
 * StudyNest's extra design tokens on top of [MaterialTheme.colorScheme]. Read them with
 * `StudyNestTheme.colors` (or `LocalStudyNestColors.current`).
 *
 * Rule for pastel fills in BOTH themes: pastels always carry DARK ink text ([onPastel] or the pastel's own
 * [Pastel.ink]). In dark mode the pastels are slightly deeper / desaturated so they don't glare.
 */
@Immutable
data class StudyNestColors(
    val isDark: Boolean,
    /** App canvas (cream / deep plum). */
    val canvas: Color,
    /** Raised card surface (warm white / plum). */
    val surface: Color,
    /** Sunken / secondary card surface. */
    val surfaceSunken: Color,
    /** Highest surface, e.g. input fill or bottom bar. */
    val surfaceRaised: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text, captions. */
    val inkMuted: Color,
    /** Hairlines, dividers, input borders at rest. */
    val hairline: Color,
    /** Lip (offset shadow) under surface-coloured squishy elements. */
    val surfaceLip: Color,
    /** Primary brand fill for buttons and its deeper lip. */
    val primary: Color,
    val onPrimary: Color,
    val primaryLip: Color,
    /** Primary tone safe for small text/icons on the canvas. */
    val primaryText: Color,
    /** Dark text used on any pastel fill. */
    val onPastel: Color,
    val lilac: Pastel,
    val peach: Pastel,
    val mint: Pastel,
    val butter: Pastel,
    val sky: Pastel,
    val rose: Pastel,
    /** Woven nest tones used by illustrations. */
    val nest: Color,
    val nestDeep: Color,
    /** Success (done / completed) text-safe tone + soft fill. */
    val success: Color,
    val successFill: Color,
    /** Overdue / danger text-safe tone, a bright accent for icons/bars, and a soft fill. */
    val overdue: Color,
    val overdueAccent: Color,
    val overdueFill: Color,
    /** Inverted pill (snackbars, selected chips). */
    val inverse: Color,
    val onInverse: Color,
    /** Highlight colour used for snackbar actions on [inverse]. */
    val inverseAccent: Color,
    /** Skeleton loading base + shimmer highlight. */
    val skeleton: Color,
    val skeletonHighlight: Color,
) {
    /** The pastel swatch for a subject colour. */
    fun subject(color: SubjectColor): Pastel = when (color) {
        SubjectColor.Lilac -> lilac
        SubjectColor.Peach -> peach
        SubjectColor.Mint -> mint
        SubjectColor.Butter -> butter
        SubjectColor.Sky -> sky
        SubjectColor.Rose -> rose
    }

    /**
     * Pastel for big decorative washes (hero blobs): full strength on cream, a soft translucent glow on plum
     * so it never glares in dark mode.
     */
    fun wash(color: Color, strength: Float = 1f): Color =
        color.copy(alpha = (if (isDark) 0.24f else 1f) * strength)

    /** All pastels in [SubjectColor] order, handy for decorations and charts. */
    val pastels: List<Pastel> get() = listOf(lilac, peach, mint, butter, sky, rose)
}

// ---- Raw palette -------------------------------------------------------------------------------

private val Cream = Color(0xFFFFF8EE)
private val WarmWhite = Color(0xFFFFFDF8)
private val Sunken = Color(0xFFFFF1DF)
private val Ink = Color(0xFF2B2340)
private val InkMuted = Color(0xFF6E6585)

private val Plum = Color(0xFF1B1430)
private val PlumSurface = Color(0xFF251C3E)
private val PlumRaised = Color(0xFF2F2550)
private val Mist = Color(0xFFF4EFFF)
private val MistMuted = Color(0xFFB8AED6)

val LightStudyNestColors = StudyNestColors(
    isDark = false,
    canvas = Cream,
    surface = WarmWhite,
    surfaceSunken = Sunken,
    surfaceRaised = Color(0xFFFFFFFF),
    ink = Ink,
    inkMuted = InkMuted,
    hairline = Color(0xFFEADCC9),
    surfaceLip = Color(0xFFEBDCC6),
    primary = Color(0xFF6556E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryLip = Color(0xFF4437BE),
    primaryText = Color(0xFF5B4FD6),
    onPastel = Ink,
    lilac = Pastel(fill = Color(0xFFD9D4FF), ink = Color(0xFF4639BF), lip = Color(0xFFB8AFF7)),
    peach = Pastel(fill = Color(0xFFFFC9A9), ink = Color(0xFF8F3D10), lip = Color(0xFFF2A780)),
    mint = Pastel(fill = Color(0xFFBDEBD2), ink = Color(0xFF1C6542), lip = Color(0xFF8FD3AF)),
    butter = Pastel(fill = Color(0xFFFFE59A), ink = Color(0xFF6E5200), lip = Color(0xFFF0C95A)),
    sky = Pastel(fill = Color(0xFFC2E1FF), ink = Color(0xFF1D5793), lip = Color(0xFF94C3F0)),
    rose = Pastel(fill = Color(0xFFFFC6D9), ink = Color(0xFF9C2757), lip = Color(0xFFF29DBD)),
    nest = Color(0xFFF3C58E),
    nestDeep = Color(0xFFC98A4E),
    success = Color(0xFF1C6542),
    successFill = Color(0xFFBDEBD2),
    overdue = Color(0xFFC62F36),
    overdueAccent = Color(0xFFE5484D),
    overdueFill = Color(0xFFFFE1DD),
    inverse = Ink,
    onInverse = Color(0xFFFFF8EE),
    inverseAccent = Color(0xFFFFE59A),
    skeleton = Color(0xFFF6E8D5),
    skeletonHighlight = Color(0xFFFFF6EA),
)

val DarkStudyNestColors = StudyNestColors(
    isDark = true,
    canvas = Plum,
    surface = PlumSurface,
    surfaceSunken = Color(0xFF211839),
    surfaceRaised = PlumRaised,
    ink = Mist,
    inkMuted = MistMuted,
    hairline = Color(0xFF3D3263),
    surfaceLip = Color(0xFF130E24),
    primary = Color(0xFFA99DFF),
    onPrimary = Plum,
    primaryLip = Color(0xFF6A5BD9),
    primaryText = Color(0xFFB9AFFF),
    onPastel = Plum,
    lilac = Pastel(fill = Color(0xFFADA4F2), ink = Color(0xFF2B2180), lip = Color(0xFF7469C9)),
    peach = Pastel(fill = Color(0xFFEDB290), ink = Color(0xFF5A2508), lip = Color(0xFFB57E60)),
    mint = Pastel(fill = Color(0xFF96D2B2), ink = Color(0xFF0D3F27), lip = Color(0xFF649D80)),
    butter = Pastel(fill = Color(0xFFE8CF7A), ink = Color(0xFF453300), lip = Color(0xFFB09A4C)),
    sky = Pastel(fill = Color(0xFF97C2EC), ink = Color(0xFF0E365C), lip = Color(0xFF6690BA)),
    rose = Pastel(fill = Color(0xFFEBA7C0), ink = Color(0xFF5C1232), lip = Color(0xFFB57690)),
    nest = Color(0xFFD9A877),
    nestDeep = Color(0xFF9C6C40),
    success = Color(0xFF8EDDB3),
    successFill = Color(0xFF1E4636),
    overdue = Color(0xFFFF8F8A),
    overdueAccent = Color(0xFFFF6B6B),
    overdueFill = Color(0xFF4A2236),
    inverse = Mist,
    onInverse = Plum,
    inverseAccent = Color(0xFF5B4FD6),
    skeleton = Color(0xFF2B2148),
    skeletonHighlight = Color(0xFF3A2E5F),
)

val LocalStudyNestColors = staticCompositionLocalOf { LightStudyNestColors }

internal val LightColors: ColorScheme = lightColorScheme(
    primary = LightStudyNestColors.primary,
    onPrimary = LightStudyNestColors.onPrimary,
    primaryContainer = LightStudyNestColors.lilac.fill,
    onPrimaryContainer = Color(0xFF241A75),
    inversePrimary = Color(0xFFC3BBFF),
    secondary = Color(0xFF9A4A1C),
    onSecondary = Color.White,
    secondaryContainer = LightStudyNestColors.peach.fill,
    onSecondaryContainer = Color(0xFF3D1A05),
    tertiary = Color(0xFF1C6542),
    onTertiary = Color.White,
    tertiaryContainer = LightStudyNestColors.mint.fill,
    onTertiaryContainer = Color(0xFF06311D),
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF6E9D6),
    onSurfaceVariant = InkMuted,
    surfaceTint = Cream,
    inverseSurface = Ink,
    inverseOnSurface = Cream,
    error = LightStudyNestColors.overdue,
    onError = Color.White,
    errorContainer = LightStudyNestColors.overdueFill,
    onErrorContainer = Color(0xFF5C0F14),
    outline = Color(0xFF9D8F80),
    outlineVariant = LightStudyNestColors.hairline,
    scrim = Color(0xFF1B1430),
    surfaceBright = WarmWhite,
    surfaceDim = Color(0xFFF3E6D3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmWhite,
    surfaceContainer = Color(0xFFFFF5E8),
    surfaceContainerHigh = Sunken,
    surfaceContainerHighest = Color(0xFFFBE8D2),
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = DarkStudyNestColors.primary,
    onPrimary = DarkStudyNestColors.onPrimary,
    primaryContainer = Color(0xFF3E3380),
    onPrimaryContainer = Color(0xFFE6E1FF),
    inversePrimary = Color(0xFF5B4FD6),
    secondary = Color(0xFFFFB98F),
    onSecondary = Color(0xFF3D1A05),
    secondaryContainer = Color(0xFF5A3320),
    onSecondaryContainer = Color(0xFFFFE1CF),
    tertiary = Color(0xFF8EDDB3),
    onTertiary = Color(0xFF06311D),
    tertiaryContainer = Color(0xFF1F4A37),
    onTertiaryContainer = Color(0xFFCFF5E1),
    background = Plum,
    onBackground = Mist,
    surface = Plum,
    onSurface = Mist,
    surfaceVariant = PlumRaised,
    onSurfaceVariant = MistMuted,
    surfaceTint = Plum,
    inverseSurface = Mist,
    inverseOnSurface = Ink,
    error = DarkStudyNestColors.overdue,
    onError = Color(0xFF4A0B10),
    errorContainer = DarkStudyNestColors.overdueFill,
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A7FAA),
    outlineVariant = DarkStudyNestColors.hairline,
    scrim = Color(0xFF07040F),
    surfaceBright = Color(0xFF3A2E5F),
    surfaceDim = Plum,
    surfaceContainerLowest = Color(0xFF150F26),
    surfaceContainerLow = Color(0xFF211839),
    surfaceContainer = PlumSurface,
    surfaceContainerHigh = PlumRaised,
    surfaceContainerHighest = Color(0xFF392E5E),
)

/** Shortcut: the pastel swatch for [color] in the current theme. */
@Composable
@ReadOnlyComposable
fun subjectPastel(color: SubjectColor): Pastel = LocalStudyNestColors.current.subject(color)
