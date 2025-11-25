package com.example.turnoshospi.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.model.SwapRequest
import com.example.turnoshospi.domain.repository.SwapRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SwapViewModel(private val swapRepository: SwapRequestRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    val isSending = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun observeChat(swapId: String, plantId: String) {
        viewModelScope.launch {
            swapRepository.chatMessages(swapId, plantId).collect { _messages.value = it }
        }
    }

    fun sendMessage(message: ChatMessage) {
        isSending.value = true
        viewModelScope.launch {
            runCatching { swapRepository.postChatMessage(message) }
                .onFailure { error.value = it.message }
                .also { isSending.value = false }
        }
    }

    fun createSwapRequest(request: SwapRequest) {
        viewModelScope.launch { swapRepository.createSwapRequest(request) }
    }
}
