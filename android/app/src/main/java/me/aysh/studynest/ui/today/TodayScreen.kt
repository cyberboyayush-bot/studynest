package me.aysh.studynest.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.aysh.studynest.R
import me.aysh.studynest.ui.main.TabPlaceholder

/** Stub — replaced in a later phase. Pad for the bottom bar with LocalBottomBarPadding (see MainScreen). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun TodayScreen(onStartFocus: () -> Unit, onOpenAssignment: (String) -> Unit, onNewAssignment: () -> Unit, onSeeAllAssignments: () -> Unit, onOpenSubjects: () -> Unit) {
    TabPlaceholder(stringResource(R.string.tab_today), "today_screen")
}
