package com.example.turnoshospi.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupChatViewModel(
    private val chatRepository: ChatRepository,
    private val plantId: String,
    private val currentUserId: String,
    private val currentUserName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeGroupMessages(plantId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun onSend(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = ChatMessage(
                senderId = currentUserId,
                senderName = currentUserName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            try {
                chatRepository.sendGroupMessage(plantId, message)
            } catch (e: Exception) {
                // Handle send error
            }
        }
    }
}