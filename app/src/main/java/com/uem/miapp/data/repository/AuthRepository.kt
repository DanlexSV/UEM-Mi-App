package com.uem.miapp.data.repository

interface AuthRepository {
    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun register(email: String, password: String, onResult: (Result<Unit>) -> Unit)
    fun signOut()
    fun currentUserId(): String?
}
