package com.uem.miapp.ui.login

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
