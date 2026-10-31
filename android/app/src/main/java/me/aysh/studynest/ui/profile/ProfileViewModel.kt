package me.aysh.studynest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.Profile
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.AuthRepository
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Validation

/** Quick picks offered in the daily goal sheet, in minutes. */
val GOAL_PRESETS = listOf(30, 60, 90, 120, 180)

data class GoalSheetState(
    val input: String,
    val showError: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val inputError: String? get() = if (showError) Validation.dailyGoal(input) else null
}

data class ProfileUiState(
    val profile: Loadable<Profile> = Loadable.Loading,
    val goalSheet: GoalSheetState? = null,
    val confirmSignOut: Boolean = false,
    val isSigningOut: Boolean = false,
)

class ProfileViewModel(
    private val repository: StudyRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val reloads = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(repository.revision, reloads) { a, b -> a to b }.collectLatest {
                val result = try {
                    Loadable.Ready(repository.profile())
                } catch (e: AppException) {
                    Loadable.Failed(e.message)
                }
                _state.update { it.copy(profile = result) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(profile = Loadable.Loading) }
        reloads.update { it + 1 }
    }

    fun openGoalSheet() {
        val profile = _state.value.profile.valueOrNull ?: return
        _state.update { it.copy(goalSheet = GoalSheetState(input = profile.dailyGoalMinutes.toString())) }
    }

    fun onGoalInput(value: String) = updateSheet { it.copy(input = value.filter(Char::isDigit).take(3), error = null) }

    fun dismissGoalSheet() = _state.update { if (it.goalSheet?.isSaving == true) it else it.copy(goalSheet = null) }

    fun saveGoal() {
        val sheet = _state.value.goalSheet ?: return
        if (sheet.isSaving) return
        val minutes = Validation.minutes(sheet.input, Validation.MIN_GOAL_MINUTES, Validation.MAX_GOAL_MINUTES)
        if (minutes == null) {
            updateSheet { it.copy(showError = true) }
            return
        }
        updateSheet { it.copy(isSaving = true, showError = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = repository.updateProfile(dailyGoalMinutes = minutes)
                _state.update { it.copy(goalSheet = null, profile = Loadable.Ready(profile)) }
                _messages.send("Daily goal updated")
            } catch (e: AppException) {
                updateSheet { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun requestSignOut() = _state.update { it.copy(confirmSignOut = true) }

    fun cancelSignOut() = _state.update { if (it.isSigningOut) it else it.copy(confirmSignOut = false) }

    /** The session store emits SignedOut and root navigation returns to Welcome. */
    fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: AppException) {
                _state.update { it.copy(isSigningOut = false, confirmSignOut = false) }
                _messages.send(e.message)
            }
        }
    }

    private fun updateSheet(transform: (GoalSheetState) -> GoalSheetState) =
        _state.update { state -> state.goalSheet?.let { state.copy(goalSheet = transform(it)) } ?: state }
}
