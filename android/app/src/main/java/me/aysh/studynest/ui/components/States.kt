package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * Small Fredoka section label with an optional colour "tab" dot and a trailing text action ("See all").
 * Marked as a heading for accessibility. [actionTag] goes on the action button.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = StudyNestTheme.colors.primary,
    titleColor: Color = StudyNestTheme.colors.ink,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionTag: String? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 6.dp, height = 18.dp)
                .clip(NestShapes.Pill)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            NestTextButton(
                text = actionLabel,
                onClick = onAction,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier,
                contentPadding = PaddingValues(horizontal = 12.dp),
            )
        }
    }
}

/**
 * Friendly empty state: illustration slot (defaults to a sparse [NestIllustration]), Fredoka title, muted
 * message and an [actions] column (put PrimaryButton / SecondaryButton there). Put the test tag on [modifier].
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustration: @Composable () -> Unit = {
        NestIllustration(Modifier.size(width = 220.dp, height = 180.dp), books = 1, showPencil = false)
    },
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val c = StudyNestTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { illustration() }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.inkMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = actions,
        )
    }
}

/**
 * Error state with a cloud-off stamp, message and a "Try again" button. [retryTag] goes on the button
 * (e.g. "editor_retry").
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.error_title),
    retryTag: String? = null,
) {
    val c = StudyNestTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(NestShapes.Stamp)
                .background(c.overdueFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = c.overdue, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = c.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(24.dp))
        SecondaryButton(
            text = stringResource(R.string.action_try_again),
            onClick = onRetry,
            leadingIcon = Icons.Rounded.Refresh,
            modifier = if (retryTag != null) Modifier.testTag(retryTag) else Modifier,
        )
    }
}

/** Visual tone of a [NestBanner]. */
enum class BannerTone { Error, Info, Success }

/**
 * Inline banner (form-level error, info hint). A live region, so screen readers announce it.
 * Put the test tag on [modifier] (e.g. "auth_form_error").
 */
@Composable
fun NestBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Error,
    icon: ImageVector = Icons.Rounded.ErrorOutline,
) {
    val c = StudyNestTheme.colors
    val (fill, ink) = when (tone) {
        BannerTone.Error -> c.overdueFill to c.overdue
        BannerTone.Info -> c.lilac.fill to c.lilac.ink
        BannerTone.Success -> c.successFill to c.success
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(NestShapes.Block)
            .background(fill)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = if (tone == BannerTone.Error && !c.isDark) ink else c.ink)
    }
}

/** Animated shimmer fill for skeleton placeholders. */
@Composable
fun Modifier.shimmer(shape: Shape = NestShapes.Block): Modifier {
    val c = StudyNestTheme.colors
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shimmerX")
    val brush = Brush.linearGradient(
        colors = listOf(c.skeleton, c.skeletonHighlight, c.skeleton),
        start = Offset(x * 800f - 400f, 0f),
        end = Offset(x * 800f, 200f),
    )
    return this
        .clip(shape)
        .background(brush)
}

/** A single shimmering placeholder block. Size it with [modifier]. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = NestShapes.Block) {
    Box(modifier.shimmer(shape))
}

/**
 * Generic loading skeleton: a hero block followed by [rows] list-row placeholders (stamp + two lines).
 * Announces "Loading" once for accessibility.
 */
@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier, rows: Int = 4, heroHeight: Dp = 180.dp) {
    val loading = stringResource(R.string.loading)
    Column(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (heroHeight > 0.dp) SkeletonBlock(Modifier.fillMaxWidth().height(heroHeight), NestShapes.Card)
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(44.dp), NestShapes.Stamp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp), NestShapes.Pill)
                    SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(12.dp), NestShapes.Pill)
                }
            }
        }
    }
}

/**
 * Staggered first-appearance animation (fade + gentle rise) for screen sections. [index] sets the delay
 * (~60ms per step). Plays once per composition of this node — use for top-level sections / the first
 * screenful, not for every LazyColumn item (use `Modifier.animateItem()` there).
 */
@Composable
fun Reveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(380, delayMillis = index * 60)) +
            slideInVertically(tween(420, delayMillis = index * 60)) { it / 5 },
    ) { content() }
}
