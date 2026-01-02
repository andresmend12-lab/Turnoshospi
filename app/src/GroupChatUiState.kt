package com.example.turnoshospi.presentation.chat

import com.example.turnoshospi.domain.model.ChatMessage

data class GroupChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)