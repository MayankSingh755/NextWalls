package com.ionic.nextwalls.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ionic.nextwalls.ui.components.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        val currentUser = auth.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.Authenticated(currentUser)
        } else {
            AuthState.Unauthenticated
        }
    }

    fun signInWithGoogle(idToken: String?) {
        if (idToken == null) {
            _authState.value = AuthState.Error("Failed to get Google ID token")
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()

                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error("Sign-in failed: User is null")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-out failed: ${e.message}")
            }
        }
    }
}
