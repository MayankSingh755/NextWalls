package com.ionic.nextwalls.ui.components

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}