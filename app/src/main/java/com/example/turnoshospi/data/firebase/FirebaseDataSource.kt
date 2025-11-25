package com.example.turnoshospi.data.firebase

import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.domain.model.FeedbackEntry
import com.example.turnoshospi.domain.model.Preference
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.StaffSlot
import com.example.turnoshospi.domain.model.SwapRequest
import com.example.turnoshospi.domain.model.UserProfile
import com.example.turnoshospi.domain.repository.AuthRepository
import com.example.turnoshospi.domain.repository.FeedbackRepository
import com.example.turnoshospi.domain.repository.PlantRepository
import com.example.turnoshospi.domain.repository.ShiftRepository
import com.example.turnoshospi.domain.repository.SwapRequestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {
    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).addSnapshotListener { snapshot, _ ->
                    trySend(snapshot?.toObject<UserProfile>())
                }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

class FirestorePlantRepository(private val firestore: FirebaseFirestore) : PlantRepository {
    override suspend fun createPlant(profile: UserProfile, name: String, description: String): String {
        val plantId = firestore.collection("plants").document().id
        val payload = mapOf(
            "name" to name,
            "description" to description,
            "ownerUserId" to profile.id,
            "shiftTypes" to emptyList<Any>(),
            "requiredStaffPerShift" to emptyMap<String, Any>()
        )
        firestore.collection("plants").document(plantId).set(payload).await()
        firestore.collection("users").document(profile.id)
            .set(mapOf("currentPlantId" to plantId), SetOptions.merge()).await()
        return plantId
    }

    override suspend fun joinPlant(userId: String, invitationCode: String): String {
        // TODO: validate invitation codes stored in Firestore
        firestore.collection("users").document(userId)
            .set(mapOf("currentPlantId" to invitationCode), SetOptions.merge()).await()
        return invitationCode
    }

    override suspend fun staffSlots(plantId: String): Flow<List<StaffSlot>> = callbackFlow {
        val registration = firestore.collection("plants")
            .document(plantId)
            .collection("staffSlots")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<StaffSlot>()?.copy(id = doc.id)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }
}

class FirestoreShiftRepository(private val firestore: FirebaseFirestore) : ShiftRepository {
    override suspend fun shiftsForMonth(plantId: String, monthKey: String): Flow<List<Shift>> = callbackFlow {
        val registration = firestore.collection("plants").document(plantId)
            .collection("shifts")
            .whereEqualTo("monthKey", monthKey)
            .addSnapshotListener { snapshot, _ ->
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    val date = LocalDate.parse(map["date"] as String, formatter)
                    val shift = doc.toObject<Shift>()?.copy(id = doc.id, date = date)
                    shift
                }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun savePreferences(preference: Preference) {
        val doc = firestore.collection("plants").document(preference.staffSlotId)
            .collection("preferences").document(preference.monthKey)
        doc.set(preference).await()
    }
}

class FirestoreSwapRequestRepository(private val firestore: FirebaseFirestore) : SwapRequestRepository {
    override suspend fun createSwapRequest(request: SwapRequest) {
        val ref = firestore.collection("plants").document(request.plantId)
            .collection("swapRequests").document(request.id)
        ref.set(request).await()
    }

    override suspend fun swapRequestsForUser(userId: String, plantId: String): Flow<List<SwapRequest>> = callbackFlow {
        val registration = firestore.collection("plants").document(plantId)
            .collection("swapRequests")
            .whereArrayContains("participants", mapOf("staffSlotId" to userId))
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { it.toObject<SwapRequest>() } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun postChatMessage(message: ChatMessage) {
        firestore.collection("plants").document(message.plantId)
            .collection("chatMessages")
            .add(message).await()
    }

    override fun chatMessages(swapRequestId: String, plantId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = firestore.collection("plants").document(plantId)
            .collection("chatMessages")
            .whereEqualTo("swapRequestId", swapRequestId)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { it.toObject<ChatMessage>() } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }
}

class FirestoreFeedbackRepository(private val firestore: FirebaseFirestore) : FeedbackRepository {
    override suspend fun sendFeedback(entry: FeedbackEntry) {
        firestore.collection("feedback").document(entry.id).set(entry).await()
    }

    override fun feedbackByUser(userId: String): Flow<List<FeedbackEntry>> = callbackFlow {
        val registration = firestore.collection("feedback")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { it.toObject<FeedbackEntry>() } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }
}
