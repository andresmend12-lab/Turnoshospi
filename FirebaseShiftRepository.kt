package com.example.turnoshospi.data.repository

import com.example.turnoshospi.data.firebase.DbPaths
import com.example.turnoshospi.data.firebase.FirebaseRefs
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.repository.ShiftRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseShiftRepository : ShiftRepository {

    override fun observeShifts(plantId: String): Flow<List<Shift>> = callbackFlow {
        val query = FirebaseRefs.getRootRef().child(DbPaths.SHIFTS).child(plantId).limitToLast(100)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shifts = snapshot.children.mapNotNull { it.getValue(Shift::class.java) }
                trySend(shifts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun requestShiftChange(shiftId: String, requesterUserId: String): Result<Unit> {
        return try {
            // Lógica simplificada: Crear una entrada en shift_requests
            val requestRef = FirebaseRefs.getRootRef().child(DbPaths.SHIFT_REQUESTS).push()
            val requestData = mapOf(
                "shiftId" to shiftId,
                "requesterId" to requesterUserId,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            requestRef.setValue(requestData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
