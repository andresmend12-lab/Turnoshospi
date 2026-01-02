package com.example.turnoshospi.data.repository

import com.example.turnoshospi.data.firebase.FirebaseRefs
import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.repository.ChatRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository : ChatRepository {

    override fun observeGroupMessages(plantId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = FirebaseRefs.getChatsRef().child("group").child(plantId).limitToLast(50)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun sendGroupMessage(plantId: String, message: ChatMessage) {
        val ref = FirebaseRefs.getChatsRef().child("group").child(plantId).push()
        val messageWithId = message.copy(id = ref.key ?: "")
        ref.setValue(messageWithId).await()
    }
}