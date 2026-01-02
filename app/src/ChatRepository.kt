package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeGroupMessages(plantId: String): Flow<List<ChatMessage>>
    suspend fun sendGroupMessage(plantId: String, message: ChatMessage)
}