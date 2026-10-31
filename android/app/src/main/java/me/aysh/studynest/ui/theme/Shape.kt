package me.aysh.studynest.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val StudyNestShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Named shapes used across StudyNest so radii stay consistent. */
object NestShapes {
    /** Fully round pill: buttons, chips, snackbars. */
    val Pill = CircleShape
    /** Chunky cards and tiles. */
    val Card = RoundedCornerShape(26.dp)
    /** Smaller inner blocks (rows, banners). */
    val Block = RoundedCornerShape(20.dp)
    /** Text inputs. */
    val Field = RoundedCornerShape(18.dp)
    /** Subject stamps / icon squircles. */
    val Stamp = RoundedCornerShape(30)
    /** Bottom sheets: only the top corners are rounded. */
    val Sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    /** Dialogs. */
    val Dialog = RoundedCornerShape(30.dp)
}
