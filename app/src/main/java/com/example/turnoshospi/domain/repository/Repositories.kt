package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.model.FeedbackEntry
import com.example.turnoshospi.domain.model.Preference
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.StaffSlot
import com.example.turnoshospi.domain.model.SwapRequest
import com.example.turnoshospi.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signOut()
}

interface PlantRepository {
    suspend fun createPlant(profile: UserProfile, name: String, description: String): String
    suspend fun joinPlant(userId: String, invitationCode: String): String
    suspend fun staffSlots(plantId: String): Flow<List<StaffSlot>>
}

interface ShiftRepository {
    suspend fun shiftsForMonth(plantId: String, monthKey: String): Flow<List<Shift>>
    suspend fun savePreferences(preference: Preference)
}

interface SwapRequestRepository {
    suspend fun createSwapRequest(request: SwapRequest)
    suspend fun swapRequestsForUser(userId: String, plantId: String): Flow<List<SwapRequest>>
    suspend fun postChatMessage(message: ChatMessage)
    fun chatMessages(swapRequestId: String, plantId: String): Flow<List<ChatMessage>>
}

interface FeedbackRepository {
    suspend fun sendFeedback(entry: FeedbackEntry)
    fun feedbackByUser(userId: String): Flow<List<FeedbackEntry>>
}
