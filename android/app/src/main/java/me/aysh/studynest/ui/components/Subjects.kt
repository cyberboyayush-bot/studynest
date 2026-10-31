package me.aysh.studynest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/** Rounded material icon for a [SubjectIcon]. */
fun subjectIconVector(icon: SubjectIcon): ImageVector = when (icon) {
    SubjectIcon.Book -> Icons.AutoMirrored.Rounded.MenuBook
    SubjectIcon.Calculator -> Icons.Rounded.Calculate
    SubjectIcon.Flask -> Icons.Rounded.Science
    SubjectIcon.Globe -> Icons.Rounded.Public
    SubjectIcon.Palette -> Icons.Rounded.Palette
    SubjectIcon.Code -> Icons.Rounded.Code
    SubjectIcon.Music -> Icons.Rounded.MusicNote
    SubjectIcon.Language -> Icons.Rounded.Translate
    SubjectIcon.History -> Icons.Rounded.HistoryEdu
    SubjectIcon.Biology -> Icons.Rounded.Eco
}

/**
 * A subject "stamp": pastel squircle in the subject colour with its icon in the deep ink tone, plus a small
 * lip below so it reads like a sticker. [tilt] (degrees) adds a playful rotation; keep it small (±4).
 * Decorative by default (no content description) — rows should merge semantics and describe the subject in text.
 */
@Composable
fun SubjectStamp(
    color: SubjectColor,
    icon: SubjectIcon,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tilt: Float = 0f,
) {
    val pastel = StudyNestTheme.colors.subject(color)
    val lip = (size.value * 0.07f).coerceIn(2f, 4f).dp
    Box(
        modifier
            .size(width = size, height = size + lip)
            .rotate(tilt),
    ) {
        Box(
            Modifier
                .offset(y = lip)
                .size(size)
                .clip(NestShapes.Stamp)
                .background(pastel.lip),
        )
        Box(
            Modifier
                .size(size)
                .clip(NestShapes.Stamp)
                .background(pastel.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(subjectIconVector(icon), contentDescription = null, tint = pastel.ink, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Small round colour dot for a subject (legends, chips). [size] defaults to 10dp. */
@Composable
fun SubjectDot(color: SubjectColor, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val pastel = StudyNestTheme.colors.subject(color)
    Box(
        modifier
            .size(size)
            .clip(NestShapes.Pill)
            .background(pastel.lip),
    )
}

/**
 * Priority as 1–3 ascending bars (signal style) — shape, not only colour, carries the meaning.
 * Low = 1 muted bar, Medium = 2 butter-ink bars, High = 3 coral bars. Announces "High priority" etc.
 */
@Composable
fun PriorityIndicator(priority: Priority, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val (level, color) = when (priority) {
        Priority.Low -> 1 to c.inkMuted
        Priority.Medium -> 2 to (if (c.isDark) c.butter.fill else c.butter.ink)
        Priority.High -> 3 to c.overdueAccent
    }
    val description = stringResource(
        when (priority) {
            Priority.Low -> R.string.priority_low_description
            Priority.Medium -> R.string.priority_medium_description
            Priority.High -> R.string.priority_high_description
        },
    )
    val empty = c.hairline
    Canvas(
        modifier
            .size(width = 16.dp, height = 14.dp)
            .semantics { contentDescription = description },
    ) {
        val barW = size.width / 4.2f
        val gap = (size.width - barW * 3) / 2f
        for (i in 0 until 3) {
            val h = size.height * (0.45f + 0.275f * i)
            drawRoundRect(
                color = if (i < level) color else empty,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}

/** Priority indicator + label in a soft pill ("High"), for detail views. */
@Composable
fun PriorityPill(priority: Priority, label: String, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    Row(
        modifier
            .clip(NestShapes.Pill)
            .background(c.surfaceSunken)
            .padding(horizontal = 10.dp)
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PriorityIndicator(priority)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.ink)
    }
}
