package com.example.turnoshospi.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.FeedbackEntry
import com.example.turnoshospi.domain.repository.FeedbackRepository
import kotlinx.coroutines.launch

class FeedbackViewModel(private val repository: FeedbackRepository) : ViewModel() {
    val isSending = mutableStateOf(false)
    val success = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun sendFeedback(entry: FeedbackEntry) {
        isSending.value = true
        viewModelScope.launch {
            runCatching { repository.sendFeedback(entry) }
                .onSuccess { success.value = true }
                .onFailure { error.value = it.message }
                .also { isSending.value = false }
        }
    }
}
