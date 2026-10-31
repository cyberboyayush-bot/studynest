package me.aysh.studynest.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * The StudyNest signature: a "gummy sticker" surface with a solid offset [lip] underneath that compresses
 * with a spring when pressed (face sinks into the lip + scales to ~0.97). Use it for primary actions and
 * for tappable cards/tiles — sparingly, but consistently.
 *
 * Non-clickable when [onClick] is null (still draws the lip). The layout reserves [lipHeight] at the bottom,
 * so the visible face is `height - lipHeight`. Keyboard focus draws a ring in the primary colour.
 * Put `testTag` on [modifier]: it lands on the clickable node.
 */
@Composable
fun SquishySurface(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fill: Color = StudyNestTheme.colors.surface,
    lip: Color = StudyNestTheme.colors.surfaceLip,
    shape: Shape = NestShapes.Card,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    lipHeight: Dp = 4.dp,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = if (pressed) Spring.StiffnessHigh else 520f),
        label = "squish",
    )
    val focusColor = StudyNestTheme.colors.primary

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    } else Modifier

    // propagateMinConstraints: a fillMaxWidth()/fixed-size modifier on the surface stretches the face too.
    Box(modifier.then(clickModifier), propagateMinConstraints = true) {
        // The lip sits behind and peeks out below the face.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = lipHeight)
                .background(lip, shape),
        )
        Box(
            Modifier
                .padding(bottom = lipHeight)
                .graphicsLayer {
                    val s = 1f - 0.03f * press
                    scaleX = s
                    scaleY = s
                    translationY = lipHeight.toPx() * 0.85f * press
                }
                .then(if (focused) Modifier.border(3.dp, focusColor, shape) else Modifier)
                .clip(shape)
                .background(fill)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/** Visual flavours of [NestButton]. */
enum class NestButtonStyle { Primary, Secondary, Danger, Tonal }

/**
 * Pill button with the squishy lip. Shows a spinner and becomes non-interactive while [loading].
 * Prefer the shortcuts [PrimaryButton] / [SecondaryButton].
 */
@Composable
fun NestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NestButtonStyle = NestButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    large: Boolean = true,
) {
    val c = StudyNestTheme.colors
    val (fill, lip, content) = when (style) {
        NestButtonStyle.Primary -> Triple(c.primary, c.primaryLip, c.onPrimary)
        NestButtonStyle.Secondary -> Triple(c.surfaceRaised, c.surfaceLip, c.ink)
        NestButtonStyle.Danger -> Triple(c.overdueFill, c.overdueAccent.copy(alpha = 0.55f), c.overdue)
        NestButtonStyle.Tonal -> Triple(c.lilac.fill, c.lilac.lip, c.onPastel)
    }
    val active = enabled && !loading
    val faceFill = if (enabled) fill else c.hairline
    val faceLip = if (enabled) lip else c.surfaceLip
    val contentColor = if (enabled) content else c.inkMuted
    val border = if (style == NestButtonStyle.Secondary) BorderStroke(1.5.dp, c.hairline) else null

    SquishySurface(
        onClick = onClick,
        modifier = modifier,
        fill = faceFill,
        lip = faceLip,
        shape = NestShapes.Pill,
        enabled = active,
        border = border,
        lipHeight = if (large) 4.dp else 3.dp,
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                Modifier
                    .defaultMinSize(minHeight = if (large) 56.dp else 44.dp)
                    .padding(horizontal = if (large) 28.dp else 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = loading,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
                    label = "buttonLoading",
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.2f),
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, Modifier.size(20.dp))
                            Text(
                                text = text,
                                style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Main call-to-action: lilac pill with a deep lip, white label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = NestButton(text, onClick, modifier, NestButtonStyle.Primary, enabled, loading, leadingIcon)

/** Secondary action: warm-white pill with hairline border and a beige lip. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = NestButton(text, onClick, modifier, NestButtonStyle.Secondary, enabled, loading, leadingIcon)

/**
 * Low-emphasis text action (no lip): primary-tinted label in a pill with a soft pressed tint.
 * Min touch target 48dp.
 */
@Composable
fun NestTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = StudyNestTheme.colors.primaryText,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val tint by animateFloatAsState(if (pressed || focused) 0.14f else 0f, label = "textButtonTint")
    Row(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(NestShapes.Pill)
            .background(color.copy(alpha = tint))
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val c = if (enabled) color else StudyNestTheme.colors.inkMuted
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = c, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = c)
    }
}

/**
 * Round 48dp icon button with a soft filled circle (or transparent when [filled] = false).
 * [contentDescription] is required — this is an icon-only control.
 */
@Composable
fun NestIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    tint: Color = StudyNestTheme.colors.ink,
    container: Color = StudyNestTheme.colors.surfaceRaised,
    enabled: Boolean = true,
    size: Dp = 48.dp,
) {
    val c = StudyNestTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "iconPress",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val s = 1f - 0.08f * press
                scaleX = s
                scaleY = s
            }
            .clip(NestShapes.Pill)
            .then(if (filled) Modifier.background(container) else Modifier.background(c.ink.copy(alpha = 0.08f * press)))
            .then(if (filled) Modifier.border(1.dp, c.hairline, NestShapes.Pill) else Modifier)
            .then(if (focused) Modifier.border(3.dp, c.primary, NestShapes.Pill) else Modifier)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else c.inkMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
    }
}
