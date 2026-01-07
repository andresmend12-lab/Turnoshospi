package com.example.turnoshospi.data.repository

import android.util.Log
import com.example.turnoshospi.data.local.dao.ShiftDao
import com.example.turnoshospi.data.local.entity.ShiftEntity
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.repository.ShiftRepository
import com.example.turnoshospi.util.NetworkChecker
import com.example.turnoshospi.util.Resource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementacion de ShiftRepository con estrategia cache-first.
 *
 * Patron de sincronizacion:
 * 1. Emitir Loading
 * 2. Emitir datos de cache local (rapido)
 * 3. Si hay conexion, sincronizar con Firebase
 * 4. Actualizar cache y emitir datos frescos
 */
class FirebaseShiftRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val shiftDao: ShiftDao,
    private val networkChecker: NetworkChecker
) : ShiftRepository {

    companion object {
        private const val TAG = "ShiftRepository"
        private const val PATH_SHIFTS = "shifts"
        private const val PATH_SHIFT_REQUESTS = "shift_requests"
    }

    override fun getShifts(plantId: String): Flow<Resource<List<Shift>>> = flow {
        if (plantId.isBlank()) {
            emit(Resource.Error("plantId es requerido"))
            return@flow
        }

        emit(Resource.Loading())

        // 1. Emitir cache primero (respuesta inmediata)
        val cachedShifts = shiftDao.getByPlantId(plantId).first()
        val cachedDomainList = cachedShifts.map { it.toDomainModel() }

        if (cachedShifts.isNotEmpty()) {
            emit(Resource.Success(cachedDomainList, fromCache = true))
        }

        // 2. Intentar sincronizar con Firebase si hay conexion
        if (networkChecker.isConnected()) {
            try {
                val remoteShifts = fetchShiftsFromFirebase(plantId)
                // Actualizar cache
                shiftDao.deleteByPlantId(plantId)
                shiftDao.insertAll(remoteShifts.map { ShiftEntity.fromDomainModel(it) })
                // Emitir datos frescos
                emit(Resource.Success(remoteShifts, fromCache = false))
            } catch (e: Exception) {
                Log.e(TAG, "Error sincronizando shifts: ${e.message}")
                // Si no hay cache, emitir error
                if (cachedShifts.isEmpty()) {
                    emit(Resource.Error(e.message ?: "Error de sincronizacion"))
                }
                // Si hay cache, ya lo emitimos, solo logueamos
            }
        } else {
            // Sin conexion
            if (cachedShifts.isEmpty()) {
                emit(Resource.Error("Sin conexion y sin datos en cache"))
            }
            // Si hay cache, ya lo emitimos
        }
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
                val shifts = snapshot.children.mapNotNull { child ->
                    child.getValue(Shift::class.java)?.copy(id = child.key ?: "")
                }
                // Actualizar cache en background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        shiftDao.deleteByPlantId(plantId)
                        shiftDao.insertAll(shifts.map { ShiftEntity.fromDomainModel(it) })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error actualizando cache: ${e.message}")
                    }
                }
                trySend(shifts)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun getShiftsByUser(userId: String): Flow<Resource<List<Shift>>> = flow {
        if (userId.isBlank()) {
            emit(Resource.Error("userId es requerido"))
            return@flow
        }

        emit(Resource.Loading())

        // Emitir cache
        val cachedShifts = shiftDao.getByUserId(userId).first()
        val cachedDomainList = cachedShifts.map { it.toDomainModel() }

        if (cachedShifts.isNotEmpty()) {
            emit(Resource.Success(cachedDomainList, fromCache = true))
        } else if (!networkChecker.isConnected()) {
            emit(Resource.Error("Sin conexion y sin datos en cache"))
        }

        // Firebase no tiene indice por userId directamente,
        // los datos se obtienen via observeShifts de la planta
    }

    override fun getShiftsByDateRange(
        plantId: String,
        startDate: String,
        endDate: String
    ): Flow<Resource<List<Shift>>> = flow {
        if (plantId.isBlank()) {
            emit(Resource.Error("plantId es requerido"))
            return@flow
        }

        emit(Resource.Loading())

        // Emitir cache por rango de fechas
        val cachedShifts = shiftDao.getByPlantAndDateRange(plantId, startDate, endDate).first()
        val cachedDomainList = cachedShifts.map { it.toDomainModel() }

        if (cachedShifts.isNotEmpty()) {
            emit(Resource.Success(cachedDomainList, fromCache = true))
        }

        // Sincronizar si hay conexion
        if (networkChecker.isConnected()) {
            try {
                val remoteShifts = fetchShiftsFromFirebase(plantId)
                    .filter { it.date >= startDate && it.date <= endDate }

                // Actualizar cache (solo este rango)
                shiftDao.insertAll(remoteShifts.map { ShiftEntity.fromDomainModel(it) })

                emit(Resource.Success(remoteShifts, fromCache = false))
            } catch (e: Exception) {
                Log.e(TAG, "Error sincronizando shifts por fecha: ${e.message}")
                if (cachedShifts.isEmpty()) {
                    emit(Resource.Error(e.message ?: "Error de sincronizacion"))
                }
            }
        } else if (cachedShifts.isEmpty()) {
            emit(Resource.Error("Sin conexion y sin datos en cache"))
        }
    }

    override suspend fun syncShifts(plantId: String): Result<Unit> {
        if (plantId.isBlank()) {
            return Result.failure(IllegalArgumentException("plantId es requerido"))
        }

        if (!networkChecker.isConnected()) {
            return Result.failure(Exception("Sin conexion a internet"))
        }

        return try {
            val remoteShifts = fetchShiftsFromFirebase(plantId)
            shiftDao.deleteByPlantId(plantId)
            shiftDao.insertAll(remoteShifts.map { ShiftEntity.fromDomainModel(it) })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en syncShifts: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun requestShiftChange(shiftId: String, requesterUserId: String): Result<Unit> {
        if (shiftId.isBlank() || requesterUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("shiftId y requesterUserId son requeridos"))
        }

        if (!networkChecker.isConnected()) {
            return Result.failure(Exception("Sin conexion a internet"))
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

    /**
     * Obtiene turnos desde Firebase de forma suspendida.
     */
    private suspend fun fetchShiftsFromFirebase(plantId: String): List<Shift> =
        suspendCancellableCoroutine { continuation ->
            database.reference
                .child(PATH_SHIFTS)
                .child(plantId)
                .limitToLast(100)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val shifts = snapshot.children.mapNotNull { child ->
                            child.getValue(Shift::class.java)?.copy(id = child.key ?: "")
                        }
                        continuation.resume(shifts)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(emptyList())
                    }
                })
        }
}
