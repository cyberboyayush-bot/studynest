package me.aysh.studynest.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import me.aysh.studynest.AppContainer
import me.aysh.studynest.StudyNestApp

/** Creates a ViewModel scoped to the current navigation entry, built from the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as StudyNestApp).container
    return viewModel(key = key) { create(container) }
}
