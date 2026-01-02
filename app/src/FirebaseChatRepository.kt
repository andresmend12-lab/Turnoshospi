package com.example.turnoshospi.data.repository

import com.example.turnoshospi.data.firebase.FirebaseFlowHelpers
import com.example.turnoshospi.data.firebase.FirebaseRefs
import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository : ChatRepository {

    override fun observeGroupMessages(plantId: String): Flow<List<ChatMessage>> {
        val query = FirebaseRefs.getChatsRef().child("group").child(plantId).limitToLast(50)
        // Delegamos al helper seguro
        return FirebaseFlowHelpers.observeList(query, ChatMessage::class.java)
    }

    override fun sendGroupMessage(plantId: String, message: ChatMessage) {
        val ref = FirebaseRefs.getChatsRef().child("group").child(plantId).push()
        val messageWithId = message.copy(id = ref.key ?: "")
        ref.setValue(messageWithId).await()
    }
}