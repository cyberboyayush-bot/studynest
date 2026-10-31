package me.aysh.studynest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.AuthRepository
import me.aysh.studynest.util.Validation

enum class AuthMode { SignIn, SignUp }

data class AuthUiState(
    val mode: AuthMode,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    /** Field errors only appear after the first submit attempt, then update live. */
    val showFieldErrors: Boolean = false,
    val isSubmitting: Boolean = false,
    val formError: String? = null,
) {
    val nameError: String? get() = if (showFieldErrors && mode == AuthMode.SignUp) Validation.name(name) else null
    val emailError: String? get() = if (showFieldErrors) Validation.email(email) else null
    val passwordError: String?
        get() = if (!showFieldErrors) null
        else if (mode == AuthMode.SignUp) Validation.newPassword(password)
        else Validation.passwordPresent(password)

    val passwordStrength: Int get() = Validation.passwordStrength(password)

    internal val isValid: Boolean
        get() = Validation.email(email) == null && when (mode) {
            AuthMode.SignIn -> Validation.passwordPresent(password) == null
            AuthMode.SignUp -> Validation.name(name) == null && Validation.newPassword(password) == null
        }
}

/** On success the session store emits SignedIn and navigation reacts to it; no event needed here. */
class AuthViewModel(mode: AuthMode, private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(mode))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(Validation.MAX_NAME), formError = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value.trim(), formError = null) }
    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value.take(Validation.MAX_PASSWORD), formError = null) }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!current.isValid) {
            _state.update { it.copy(showFieldErrors = true) }
            return
        }
        _state.update { it.copy(isSubmitting = true, showFieldErrors = true, formError = null) }
        viewModelScope.launch {
            try {
                when (current.mode) {
                    AuthMode.SignIn -> auth.signIn(current.email, current.password)
                    AuthMode.SignUp -> auth.signUp(current.name, current.email, current.password)
                }
            } catch (e: AppException) {
                _state.update { it.copy(formError = e.message, isSubmitting = false) }
            }
        }
    }
}
