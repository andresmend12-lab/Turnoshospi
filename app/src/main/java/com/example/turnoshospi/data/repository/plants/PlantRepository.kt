package com.example.turnoshospi.data.repository.plants

import com.example.turnoshospi.domain.model.plants.Plant
import com.example.turnoshospi.domain.model.plants.StaffSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PlantRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun createPlant(plant: Plant): String {
        val ownerId = auth.currentUser?.uid ?: plant.ownerUserId
        val docRef = firestore.collection("plants").document()
        val data = plant.copy(
            id = docRef.id,
            ownerUserId = ownerId
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun createStaffSlots(plantId: String, slots: List<StaffSlot>) {
        val batch = firestore.batch()
        val collection = firestore.collection("plants").document(plantId).collection("staffSlots")
        slots.forEach { slot ->
            val doc = collection.document(slot.id.ifBlank { collection.document().id })
            batch.set(doc, slot.copy(id = doc.id, plantId = plantId))
        }
        batch.commit().await()
    }

    suspend fun updateSupervisorAssignment(plantId: String, staffSlotId: String, userId: String) {
        firestore.collection("plants")
            .document(plantId)
            .collection("staffSlots")
            .document(staffSlotId)
            .update("assignedUserId", userId)
            .await()
    }

    suspend fun generateInviteCode(plantId: String, inviteCode: String): String {
        val inviteLinkBase = "https://turnoshospi.example.com/invite" // TODO replace with dynamic link base if available
        val link = "$inviteLinkBase?plantId=$plantId&code=$inviteCode"
        firestore.collection("plants")
            .document(plantId)
            .update(
                mapOf(
                    "inviteCode" to inviteCode,
                    "inviteLink" to link,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        return link
    }
}
