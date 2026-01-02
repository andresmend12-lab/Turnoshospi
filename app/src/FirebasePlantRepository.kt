package com.example.turnoshospi.data.repository

import com.example.turnoshospi.data.firebase.DbPaths
import com.example.turnoshospi.data.firebase.FirebaseRefs
import com.example.turnoshospi.domain.model.Plant
import com.example.turnoshospi.domain.repository.PlantRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePlantRepository : PlantRepository {

    override fun observePlant(plantId: String): Flow<Plant?> = callbackFlow {
        val ref = FirebaseRefs.getRootRef().child(DbPaths.PLANTS).child(plantId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Plant::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createPlant(plant: Plant): Result<String> {
        return try {
            val ref = FirebaseRefs.getRootRef().child(DbPaths.PLANTS).push()
            val plantId = ref.key ?: throw Exception("Could not generate key")
            val newPlant = plant.copy(id = plantId)
            ref.setValue(newPlant).await()
            Result.success(plantId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}