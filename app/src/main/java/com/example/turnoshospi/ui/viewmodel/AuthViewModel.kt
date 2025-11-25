package com.example.turnoshospi.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.UserProfile
import com.example.turnoshospi.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val user: StateFlow<UserProfile?> = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null
    )

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            runCatching { authRepository.signInWithEmail(email, password) }
                .onFailure { errorMessage = it.message }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            runCatching { authRepository.signInWithGoogle(idToken) }
                .onFailure { errorMessage = it.message }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
