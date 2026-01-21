package com.uem.miapp.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.uem.miapp.data.repository.AuthRepository
import com.uem.miapp.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val repo: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value.trim(), error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null) }
    }

    fun submit() {
        val email = _uiState.value.email
        val pass = _uiState.value.password

        val validationError = validate(email, pass)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        _uiState.update { it.copy(loading = true, error = null) }

        val callback: (Result<Unit>) -> Unit = { result ->
            result.onSuccess {
                _uiState.update { it.copy(loading = false, success = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(loading = false, error = firebaseErrorToHuman(e)) }
            }
        }

        if (_uiState.value.isLoginMode) {
            repo.signIn(email, pass, callback)
        } else {
            repo.register(email, pass, callback)
        }
    }

    private fun validate(email: String, password: String): String? {
        if (email.isBlank()) return "El email no puede estar vacío."
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Email inválido."
        if (password.isBlank()) return "La contraseña no puede estar vacía."
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres."
        return null
    }

    private fun firebaseErrorToHuman(e: Throwable): String {
        val msg = e.message?.lowercase().orEmpty()

        return when {
            "password" in msg && "invalid" in msg -> "Contraseña incorrecta."
            "no user record" in msg || "user not found" in msg -> "No existe una cuenta con ese email."
            "already in use" in msg -> "Ese email ya está registrado."
            "network" in msg -> "Error de red. Revisa tu conexión."
            else -> e.message ?: "Error desconocido."
        }
    }
}
