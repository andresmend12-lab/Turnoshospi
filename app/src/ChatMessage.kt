package com.example.turnoshospi.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)