package me.aysh.studynest.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.components.subjectIconVector
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

/** Glassy white tint used for small badges sitting on a pastel fill. */
internal val OnPastelBadge = Color.White.copy(alpha = 0.55f)

/**
 * Chunky subject tile: the subject's pastel with an icon badge, a big faint watermark of the icon peeking
 * from the corner, the name and an optional [caption] pill ("3 open").
 *
 * The caption is exposed as the tile's state description (not as text), so the tile's text is exactly the
 * subject name. Put `testTag` on [modifier]; [onClick] null renders a static preview.
 */
@Composable
fun SubjectTile(
    name: String,
    color: SubjectColor,
    icon: SubjectIcon,
    caption: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
) {
    val c = StudyNestTheme.colors
    val pastel = c.subject(color)
    val vector = subjectIconVector(icon)
    SquishySurface(
        onClick = onClick,
        modifier = modifier.then(if (caption != null) Modifier.semantics { stateDescription = caption } else Modifier),
        fill = pastel.fill,
        lip = pastel.lip,
        onClickLabel = onClickLabel,
    ) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                vector,
                contentDescription = null,
                tint = pastel.ink.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 26.dp, y = 24.dp)
                    .size(118.dp)
                    .rotate(-14f),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(NestShapes.Stamp)
                        .background(OnPastelBadge),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(vector, contentDescription = null, tint = pastel.ink, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        color = c.onPastel,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (caption != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            caption,
                            style = MaterialTheme.typography.labelMedium,
                            color = pastel.ink,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(NestShapes.Pill)
                                .background(OnPastelBadge)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
    }
}

/** Dashed "New subject" tile that sits at the end of the grid. */
@Composable
fun NewSubjectTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(0.45f, 600f), label = "newTilePress")
    val dash = c.primaryText.copy(alpha = 0.55f)
    Box(
        modifier
            .padding(bottom = 4.dp)
            .graphicsLayer {
                val s = 1f - 0.03f * press
                scaleX = s
                scaleY = s
            }
            .clip(NestShapes.Card)
            .background(c.primaryText.copy(alpha = 0.05f + 0.06f * press))
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = dash,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(26.dp.toPx() - stroke / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))),
                )
            }
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(NestShapes.Pill)
                    .background(c.lilac.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = c.lilac.ink, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.planner_subject_new_tile), style = MaterialTheme.typography.labelLarge, color = c.primaryText)
        }
    }
}

/** Human name of a subject colour, for content descriptions. */
@Composable
internal fun colorName(color: SubjectColor): String = stringResource(
    when (color) {
        SubjectColor.Lilac -> R.string.planner_color_lilac
        SubjectColor.Peach -> R.string.planner_color_peach
        SubjectColor.Mint -> R.string.planner_color_mint
        SubjectColor.Butter -> R.string.planner_color_butter
        SubjectColor.Sky -> R.string.planner_color_sky
        SubjectColor.Rose -> R.string.planner_color_rose
    },
)

/** Human name of a subject icon, for content descriptions. */
@Composable
internal fun iconName(icon: SubjectIcon): String = stringResource(
    when (icon) {
        SubjectIcon.Book -> R.string.planner_icon_book
        SubjectIcon.Calculator -> R.string.planner_icon_calculator
        SubjectIcon.Flask -> R.string.planner_icon_flask
        SubjectIcon.Globe -> R.string.planner_icon_globe
        SubjectIcon.Palette -> R.string.planner_icon_palette
        SubjectIcon.Code -> R.string.planner_icon_code
        SubjectIcon.Music -> R.string.planner_icon_music
        SubjectIcon.Language -> R.string.planner_icon_language
        SubjectIcon.History -> R.string.planner_icon_history
        SubjectIcon.Biology -> R.string.planner_icon_biology
    },
)
