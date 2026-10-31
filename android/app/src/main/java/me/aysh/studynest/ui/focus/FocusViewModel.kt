package me.aysh.studynest.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.focus.FocusTimer
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Validation

/** Durations offered as one-tap presets. */
val FOCUS_PRESETS = listOf(15, 25, 45, 60)

data class CustomDurationState(val input: String = "", val showError: Boolean = false) {
    val error: String? get() = if (showError) Validation.customFocus(input) else null
}

data class FocusSetupState(
    val subjects: Loadable<List<Subject>> = Loadable.Loading,
    val selectedSubjectId: String? = null,
    val minutes: Int = 25,
    val isCustom: Boolean = false,
    /** Non-null while the custom duration sheet is open. */
    val customSheet: CustomDurationState? = null,
)

data class FocusUiState(val setup: FocusSetupState, val timer: FocusTimerState)

class FocusViewModel(
    private val repository: StudyRepository,
    private val timer: FocusTimer,
) : ViewModel() {

    private val setup = MutableStateFlow(FocusSetupState())
    private val reloads = MutableStateFlow(0)

    val state: StateFlow<FocusUiState> = combine(setup, timer.state, ::FocusUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState(setup.value, timer.state.value))

    init {
        viewModelScope.launch {
            combine(repository.revision, reloads) { a, b -> a to b }.collectLatest {
                val result = try {
                    Loadable.Ready(repository.subjects())
                } catch (e: AppException) {
                    Loadable.Failed(e.message)
                }
                setup.update { s ->
                    val list = result.valueOrNull
                    // Drop a selection whose subject no longer exists.
                    val selected = s.selectedSubjectId?.takeIf { id -> list == null || list.any { it.id == id } }
                    s.copy(subjects = result, selectedSubjectId = selected)
                }
            }
        }
    }

    fun retry() = reloads.update { it + 1 }

    /** Pass null for "no particular subject". */
    fun selectSubject(id: String?) = setup.update { it.copy(selectedSubjectId = id) }

    /** Pre-selects a subject, e.g. when arriving from a subject's quick-start. */
    fun preselect(id: String) = selectSubject(id)

    fun selectPreset(minutes: Int) = setup.update { it.copy(minutes = minutes, isCustom = false) }

    fun openCustom() = setup.update {
        it.copy(customSheet = CustomDurationState(input = if (it.isCustom) it.minutes.toString() else ""))
    }

    fun onCustomInput(value: String) = setup.update { s ->
        s.customSheet?.let { s.copy(customSheet = it.copy(input = value.filter(Char::isDigit).take(3))) } ?: s
    }

    fun confirmCustom() = setup.update { s ->
        val sheet = s.customSheet ?: return@update s
        val minutes = Validation.minutes(
            sheet.input,
            Validation.MIN_CUSTOM_FOCUS_MINUTES,
            Validation.MAX_CUSTOM_FOCUS_MINUTES,
        )
        if (minutes == null) s.copy(customSheet = sheet.copy(showError = true))
        else s.copy(minutes = minutes, isCustom = true, customSheet = null)
    }

    fun dismissCustom() = setup.update { it.copy(customSheet = null) }

    fun start() {
        val s = setup.value
        val subject = s.subjects.valueOrNull?.firstOrNull { it.id == s.selectedSubjectId }?.ref
        timer.start(subject, s.minutes)
    }

    fun pause() = timer.pause()
    fun resume() = timer.resume()
    fun stop() = timer.stop()
    fun retrySave() = timer.retrySave()
    fun dismissResult() = timer.dismiss()
}
