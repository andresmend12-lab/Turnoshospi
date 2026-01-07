package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.util.Resource
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    /**
     * Obtiene turnos con estrategia cache-first.
     * Emite primero desde cache local, luego sincroniza con Firebase.
     *
     * @param plantId ID de la planta
     * @return Flow de Resource con lista de turnos
     */
    fun getShifts(plantId: String): Flow<Resource<List<Shift>>>

    /**
     * Observa turnos en tiempo real desde Firebase.
     * Actualiza cache local automaticamente.
     *
     * @param plantId ID de la planta
     * @return Flow de lista de turnos
     */
    fun observeShifts(plantId: String): Flow<List<Shift>>

    /**
     * Solicita cambio de turno.
     *
     * @param shiftId ID del turno
     * @param requesterUserId ID del usuario solicitante
     * @return Result indicando exito o error
     */
    suspend fun requestShiftChange(shiftId: String, requesterUserId: String): Result<Unit>

    /**
     * Obtiene turnos de un usuario especifico con cache-first.
     *
     * @param userId ID del usuario
     * @return Flow de Resource con lista de turnos
     */
    fun getShiftsByUser(userId: String): Flow<Resource<List<Shift>>>

    /**
     * Obtiene turnos por rango de fecha con cache-first.
     *
     * @param plantId ID de la planta
     * @param startDate fecha inicio (YYYY-MM-DD)
     * @param endDate fecha fin (YYYY-MM-DD)
     * @return Flow de Resource con lista de turnos
     */
    fun getShiftsByDateRange(
        plantId: String,
        startDate: String,
        endDate: String
    ): Flow<Resource<List<Shift>>>

    /**
     * Fuerza sincronizacion con Firebase.
     * Util despues de reconectar.
     *
     * @param plantId ID de la planta
     * @return Result indicando exito o error
     */
    suspend fun syncShifts(plantId: String): Result<Unit>
}
