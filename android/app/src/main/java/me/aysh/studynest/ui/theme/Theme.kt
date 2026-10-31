package me.aysh.studynest.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * StudyNest theme: cream/plum canvas, pastel accents, Fredoka + Figtree. Dynamic colour is intentionally off.
 * Extra tokens live in [StudyNestTheme.colors]; number styles in [NumberStyles]; named shapes in [NestShapes].
 */
@Composable
fun StudyNestTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val nestColors = if (dark) DarkStudyNestColors else LightStudyNestColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalStudyNestColors provides nestColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = StudyNestTypography,
            shapes = StudyNestShapes,
            content = content,
        )
    }
}

object StudyNestTheme {
    /** StudyNest's extra colour tokens for the current theme. */
    val colors: StudyNestColors
        @Composable @ReadOnlyComposable get() = LocalStudyNestColors.current
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
