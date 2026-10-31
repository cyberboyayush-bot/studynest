package me.aysh.studynest.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.StudyNestApp
import me.aysh.studynest.ui.assignments.AssignmentsScreen
import me.aysh.studynest.ui.components.NestSnackbarHost
import me.aysh.studynest.ui.focus.FocusScreen
import me.aysh.studynest.ui.insights.InsightsScreen
import me.aysh.studynest.ui.navigation.LocalAppMessenger
import me.aysh.studynest.ui.navigation.Tab
import me.aysh.studynest.ui.profile.ProfileScreen
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.ui.today.TodayScreen

/**
 * Space the floating bottom bar occupies (bar + lip + margins + system navigation bar), as bottom padding.
 *
 * HOW TABS PAD THEMSELVES (Today, Assignments, Focus, Insights, Profile):
 * - The tab content is drawn full-screen, edge to edge, UNDER the floating bar (the bar fades the canvas
 *   behind it). Tabs must handle the status bar themselves (`statusBarsPadding()` / [NestTopBar] does it).
 * - Scrolling content: add the bar padding to the list's content padding so the last item can scroll clear:
 *   `val bar = LocalBottomBarPadding.current` →
 *   `LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bar.calculateBottomPadding() + 16.dp))`
 *   (or `Modifier.padding(bar)` at the end of a verticalScroll Column).
 * - Non-scrolling layouts (e.g. Focus) and FABs: `Modifier.padding(bar)` on the root / FAB container.
 * - Do NOT render your own SnackbarHost inside a tab — MainScreen already renders the app snackbar above the bar.
 */
val LocalBottomBarPadding = compositionLocalOf { PaddingValues(0.dp) }

/** Height of the bar's face + lip + bottom margin, excluding the system navigation bar inset. */
internal val BottomBarBlock = 68.dp + 4.dp + 12.dp

/**
 * Signed-in shell: tab content (state kept per tab via a saveable state holder; cross-fades on switch) with a
 * custom floating bottom bar and a raised centre Focus button that shows a mini progress ring while a focus
 * timer is running. Back on any tab other than Today returns to Today. See [LocalBottomBarPadding] for how
 * tabs must pad themselves.
 */
@Composable
fun MainScreen(onOpenAssignment: (String) -> Unit, onNewAssignment: (String?) -> Unit, onOpenSubjects: () -> Unit) {
    val c = StudyNestTheme.colors
    var selected by rememberSaveable { mutableStateOf(Tab.Today) }
    val stateHolder = rememberSaveableStateHolder()
    val container = (LocalContext.current.applicationContext as StudyNestApp).container
    val timer by container.focusTimer.state.collectAsStateWithLifecycle()
    val messenger = LocalAppMessenger.current

    BackHandler(enabled = selected != Tab.Today) { selected = Tab.Today }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barPadding = PaddingValues(bottom = BottomBarBlock + navBottom + 12.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(c.canvas),
    ) {
        CompositionLocalProvider(LocalBottomBarPadding provides barPadding) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(260, delayMillis = 60), initialScale = 0.985f)) togetherWith
                        fadeOut(tween(120))
                },
                label = "tabs",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                stateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        Tab.Today -> TodayScreen(
                            onStartFocus = { selected = Tab.Focus },
                            onOpenAssignment = onOpenAssignment,
                            onNewAssignment = { onNewAssignment(null) },
                            onSeeAllAssignments = { selected = Tab.Assignments },
                            onOpenSubjects = onOpenSubjects,
                        )
                        Tab.Assignments -> AssignmentsScreen(
                            onOpenAssignment = onOpenAssignment,
                            onNewAssignment = { onNewAssignment(null) },
                            onOpenSubjects = onOpenSubjects,
                        )
                        Tab.Focus -> FocusScreen()
                        Tab.Insights -> InsightsScreen()
                        Tab.Profile -> ProfileScreen(onOpenSubjects = onOpenSubjects)
                    }
                }
            }
        }

        // Canvas fade behind the floating bar so scrolled content melts away instead of colliding with it.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BottomBarBlock + navBottom + 28.dp)
                .background(Brush.verticalGradient(listOf(c.canvas.copy(alpha = 0f), c.canvas.copy(alpha = 0.92f), c.canvas))),
        )

        NestSnackbarHost(
            messenger.host,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = BottomBarBlock + navBottom + 34.dp),
        )

        NestBottomBar(
            selected = selected,
            onSelect = { selected = it },
            timer = timer,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
