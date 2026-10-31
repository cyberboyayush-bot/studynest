package me.aysh.studynest.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.ui.components.ProgressRing
import me.aysh.studynest.ui.navigation.Tab
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import kotlin.math.asin

private val BarHeight = 68.dp
private val BarLip = 4.dp
private val BarRadius = 28.dp
private val BulgeRadius = 42.dp
/** Distance from the bar's top edge down to the Focus button centre (negative = above). */
private val ButtonCenterY = 2.dp
private val FocusSize = 62.dp
/** Room above the bar for the raised button, so it stays inside the bar's bounds (and receives touches). */
private val Overhang = BulgeRadius - ButtonCenterY

private data class TabSpec(val tab: Tab, val label: Int, val selectedIcon: ImageVector, val icon: ImageVector, val tag: String)

private val SideTabs = listOf(
    TabSpec(Tab.Today, R.string.tab_today, Icons.Rounded.WbSunny, Icons.Outlined.WbSunny, "tab_today"),
    TabSpec(Tab.Assignments, R.string.tab_assignments, Icons.Rounded.Checklist, Icons.Outlined.Checklist, "tab_assignments"),
    TabSpec(Tab.Insights, R.string.tab_insights, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "tab_insights"),
    TabSpec(Tab.Profile, R.string.tab_profile, Icons.Rounded.Face, Icons.Outlined.Face, "tab_profile"),
)

/**
 * Floating "tray" bottom bar: warm rounded face with a sticker lip, a bulge that cradles the raised centre
 * Focus button, filled/outlined icon pairs and a lilac selection pill. Shows a thin progress ring around the
 * Focus button while [timer] is Active (dashed track when paused).
 */
@Composable
internal fun NestBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    timer: FocusTimerState,
    modifier: Modifier = Modifier,
) {
    val c = StudyNestTheme.colors
    Box(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(Overhang + BarHeight + BarLip),
    ) {
        // Bar body, drawn by hand so the bulge can merge with the face.
        Canvas(
            Modifier
                .padding(top = Overhang)
                .fillMaxWidth()
                .height(BarHeight + BarLip),
        ) {
            val r = BarRadius.toPx()
            val face = Size(size.width, BarHeight.toPx())
            val lip = BarLip.toPx()
            val hair = 1.dp.toPx()
            val bulgeR = BulgeRadius.toPx()
            val cy = ButtonCenterY.toPx()
            val cx = size.width / 2f
            drawRoundRect(c.surfaceLip, topLeft = Offset(0f, lip), size = face, cornerRadius = CornerRadius(r))
            drawCircle(c.surfaceLip, bulgeR, Offset(cx, cy + lip * 0.5f))
            drawRoundRect(c.surfaceRaised, size = face, cornerRadius = CornerRadius(r))
            drawRoundRect(
                c.hairline,
                topLeft = Offset(hair / 2, hair / 2),
                size = Size(face.width - hair, face.height - hair),
                cornerRadius = CornerRadius(r),
                style = Stroke(hair),
            )
            drawCircle(c.surfaceRaised, bulgeR, Offset(cx, cy))
            // Outline only the part of the bulge that rises above the bar.
            val edge = Math.toDegrees(asin((cy / bulgeR).toDouble())).toFloat()
            drawArc(
                c.hairline,
                startAngle = 180f + edge,
                sweepAngle = 180f - 2 * edge,
                useCenter = false,
                topLeft = Offset(cx - bulgeR + hair / 2, cy - bulgeR + hair / 2),
                size = Size(bulgeR * 2 - hair, bulgeR * 2 - hair),
                style = Stroke(hair),
            )
        }
        Row(
            Modifier
                .padding(top = Overhang)
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SideTabs.take(2).forEach { spec ->
                BarItem(spec, selected == spec.tab, { onSelect(spec.tab) }, Modifier.weight(1f))
            }
            // Label under the raised Focus button (the button itself carries the accessible name).
            Box(
                Modifier
                    .width(BulgeRadius * 2)
                    .fillMaxHeight()
                    .padding(bottom = 10.dp)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    stringResource(R.string.tab_focus),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected == Tab.Focus) c.ink else c.inkMuted,
                )
            }
            SideTabs.drop(2).forEach { spec ->
                BarItem(spec, selected == spec.tab, { onSelect(spec.tab) }, Modifier.weight(1f))
            }
        }
        FocusButton(
            selected = selected == Tab.Focus,
            timer = timer,
            onClick = { onSelect(Tab.Focus) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun BarItem(spec: TabSpec, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = StudyNestTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val pill by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "pill")
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(0.5f, 700f), label = "itemPress")
    val labelColor by animateColorAsState(if (selected) c.ink else c.inkMuted, label = "label")
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .testTag(spec.tag)
            .then(if (focused) Modifier.border(2.dp, c.primary, RoundedCornerShape(20.dp)) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .graphicsLayer {
                    val s = 1f - 0.08f * press
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 0.4f + 0.6f * pill
                        alpha = pill
                    }
                    .clip(NestShapes.Pill)
                    .background(c.lilac.fill),
            )
            Icon(
                if (selected) spec.selectedIcon else spec.icon,
                contentDescription = null,
                tint = if (selected) c.lilac.ink else c.inkMuted,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(spec.label),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun FocusButton(selected: Boolean, timer: FocusTimerState, onClick: () -> Unit, modifier: Modifier) {
    val c = StudyNestTheme.colors
    val active = timer as? FocusTimerState.Active
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.42f, stiffness = if (pressed) 1400f else 480f),
        label = "focusPress",
    )
    val running = active != null && !active.isPaused
    val breatheT = rememberInfiniteTransition(label = "breathe")
    val breathe = breatheT.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "breatheT")
    val description = stringResource(
        when {
            active == null -> R.string.tab_focus
            active.isPaused -> R.string.tab_focus_paused
            else -> R.string.tab_focus_running
        },
    )
    val lip = 4.dp
    Box(
        modifier
            .size(BulgeRadius * 2)
            .clip(NestShapes.Pill)
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .testTag("tab_focus")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (active != null) {
            ProgressRing(
                progress = active.progress,
                modifier = Modifier.size(76.dp),
                strokeWidth = 3.5.dp,
                trackColor = c.primary.copy(alpha = 0.18f),
                progressColor = c.primary,
                dashedTrack = active.isPaused,
                animationSpec = tween(250, easing = LinearEasing),
            )
        }
        Box(
            Modifier
                .size(width = FocusSize, height = FocusSize + lip)
                .graphicsLayer {
                    val s = (1f - 0.05f * press) * (1f + if (running) 0.025f * breathe.value else 0f)
                    scaleX = s
                    scaleY = s
                },
        ) {
            Box(
                Modifier
                    .offset(y = lip)
                    .size(FocusSize)
                    .clip(NestShapes.Pill)
                    .background(c.primaryLip),
            )
            Box(
                Modifier
                    .offset(y = lip * press)
                    .size(FocusSize)
                    .clip(NestShapes.Pill)
                    .background(c.primary)
                    .then(
                        when {
                            focused -> Modifier.border(3.dp, c.ink, NestShapes.Pill)
                            selected -> Modifier.border(3.dp, c.onPrimary.copy(alpha = 0.55f), NestShapes.Pill)
                            else -> Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = active?.isPaused == true,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
                    label = "focusIcon",
                ) { paused ->
                    Icon(
                        if (paused) Icons.Rounded.Pause else Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = c.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
