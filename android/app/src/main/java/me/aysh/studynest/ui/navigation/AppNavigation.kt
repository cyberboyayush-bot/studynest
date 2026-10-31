package me.aysh.studynest.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.aysh.studynest.ui.auth.AuthMode
import me.aysh.studynest.ui.auth.AuthScreen
import me.aysh.studynest.ui.auth.WelcomeScreen
import me.aysh.studynest.ui.editor.AssignmentEditorScreen
import me.aysh.studynest.ui.main.MainScreen
import me.aysh.studynest.ui.subjects.SubjectsScreen

/**
 * One snackbar host for the signed-in app, so a message raised by a screen that is closing
 * (e.g. "Assignment added" from the editor) still shows on the screen underneath.
 */
val LocalAppSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No snackbar host provided") }

/**
 * Root navigation. The back stack is reset whenever the user signs in or out, so the
 * auth screens and the app screens never mix and Back can't return to a stale session.
 */
@Composable
fun AppNavigation(signedIn: Boolean) {
    val root: NavKey = if (signedIn) Main else Welcome
    val backStack = rememberNavBackStack(root)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(signedIn) {
        if (backStack.firstOrNull() != root) {
            backStack.clear()
            backStack.add(root)
        }
    }

    CompositionLocalProvider(LocalAppSnackbar provides snackbar) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = NavTransitions.push,
            popTransitionSpec = NavTransitions.pop,
            predictivePopTransitionSpec = NavTransitions.predictivePop,
            entryProvider = entryProvider {
                entry<Welcome> {
                    WelcomeScreen(
                        onGetStarted = { backStack.pushIfTop(Welcome, SignUp) },
                        onSignIn = { backStack.pushIfTop(Welcome, SignIn) },
                    )
                }
                entry<SignIn> { key ->
                    AuthScreen(
                        mode = AuthMode.SignIn,
                        onBack = { backStack.popIfTop(key) },
                        onSwitchMode = { backStack.replaceTop(key, SignUp) },
                    )
                }
                entry<SignUp> { key ->
                    AuthScreen(
                        mode = AuthMode.SignUp,
                        onBack = { backStack.popIfTop(key) },
                        onSwitchMode = { backStack.replaceTop(key, SignIn) },
                    )
                }
                entry<Main> {
                    MainScreen(
                        onOpenAssignment = { id -> backStack.pushIfTop(Main, AssignmentEditor(assignmentId = id)) },
                        onNewAssignment = { subjectId -> backStack.pushIfTop(Main, AssignmentEditor(subjectId = subjectId)) },
                        onOpenSubjects = { backStack.pushIfTop(Main, Subjects) },
                    )
                }
                entry<Subjects> { key ->
                    SubjectsScreen(
                        onBack = { backStack.popIfTop(key) },
                        onNewAssignment = { subjectId -> backStack.pushIfTop(key, AssignmentEditor(subjectId = subjectId)) },
                    )
                }
                entry<AssignmentEditor> { key ->
                    AssignmentEditorScreen(
                        assignmentId = key.assignmentId,
                        presetSubjectId = key.subjectId,
                        onClose = { backStack.popIfTop(key) },
                        onOpenSubjects = { backStack.pushIfTop(key, Subjects) },
                    )
                }
            },
        )
    }
}

/** Pops [key] only if it's still on top, so double taps during a transition can't empty the stack. */
private fun MutableList<NavKey>.popIfTop(key: NavKey) {
    if (size > 1 && last() == key) removeAt(lastIndex)
}

/** Pushes [next] only from [current], so a double tap can't open the same screen twice. */
private fun MutableList<NavKey>.pushIfTop(current: NavKey, next: NavKey) {
    if (lastOrNull() == current) add(next)
}

private fun MutableList<NavKey>.replaceTop(current: NavKey, next: NavKey) {
    if (lastOrNull() != current) return
    removeAt(lastIndex)
    add(next)
}
