package com.example.turnoshospi.data.repository

import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.repository.ShiftRepository
import com.example.turnoshospi.util.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementacion de ShiftRepository usando Firebase Realtime Database.
 */
class FirebaseShiftRepository @Inject constructor(
    private val database: FirebaseDatabase
) : ShiftRepository {

    companion object {
        private const val PATH_SHIFTS = "shifts"
        private const val PATH_SHIFT_REQUESTS = "shift_requests"
    }

    override fun observeShifts(plantId: String): Flow<List<Shift>> = callbackFlow {
        if (plantId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = database.reference
            .child(PATH_SHIFTS)
            .child(plantId)
            .limitToLast(100)

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
        if (shiftId.isBlank() || requesterUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("shiftId y requesterUserId son requeridos"))
        }

        return suspendCancellableCoroutine { continuation ->
            val requestRef = database.reference.child(PATH_SHIFT_REQUESTS).push()
            val requestData = mapOf(
                "shiftId" to shiftId,
                "requesterId" to requesterUserId,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )

            requestRef.setValue(requestData)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }
    }
}
