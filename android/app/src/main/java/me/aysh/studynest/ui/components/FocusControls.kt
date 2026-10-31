package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.NumberStyles
import me.aysh.studynest.ui.theme.StudyNestTheme

/** Colour roles of a [FocusHeroButton]. */
enum class FocusHeroStyle { Primary, Tonal }

/**
 * The big hero pill used for Start / Pause / Resume: a 72dp squishy pill with a round icon "disc" on the
 * left, a chunky label and an optional tabular [trailing] value (e.g. "25 min"). The icon cross-fades when
 * [icon] changes, so Pause ⇄ Resume morphs in place. Put `testTag` on [modifier].
 */
@Composable
fun FocusHeroButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FocusHeroStyle = FocusHeroStyle.Primary,
    trailing: String? = null,
    enabled: Boolean = true,
) {
    val c = StudyNestTheme.colors
    val fill: Color
    val lip: Color
    val ink: Color
    val disc: Color
    val discInk: Color
    when (style) {
        FocusHeroStyle.Primary -> {
            fill = c.primary; lip = c.primaryLip; ink = c.onPrimary; disc = c.onPrimary; discInk = c.primary
        }
        FocusHeroStyle.Tonal -> {
            fill = c.lilac.fill; lip = c.lilac.lip; ink = c.onPastel; disc = c.surfaceRaised; discInk = c.lilac.ink
        }
    }
    SquishySurface(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        fill = fill,
        lip = lip,
        shape = NestShapes.Pill,
        enabled = enabled,
        lipHeight = 5.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(disc),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.5f)) },
                    label = "heroIcon",
                ) { vector ->
                    Icon(vector, contentDescription = null, tint = discInk, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight(800)),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(
                    trailing,
                    style = NumberStyles.mono.copy(fontSize = 16.sp),
                    color = ink.copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A round squishy icon button (gummy disc with a lip) for chunky secondary controls like the timer's Stop.
 * Icon-only, so [contentDescription] is required. Put `testTag` on [modifier].
 */
@Composable
fun FocusRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    fill: Color = StudyNestTheme.colors.surfaceRaised,
    lip: Color = StudyNestTheme.colors.surfaceLip,
    iconTint: Color = StudyNestTheme.colors.ink,
    border: BorderStroke? = BorderStroke(1.5.dp, StudyNestTheme.colors.hairline),
    enabled: Boolean = true,
) {
    SquishySurface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription },
        fill = fill,
        lip = lip,
        shape = CircleShape,
        enabled = enabled,
        border = border,
        lipHeight = 5.dp,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.4f))
    }
}
