package com.example.turnoshospi.data.local

import kotlinx.coroutines.flow.Flow

interface OfflineStore {
    // Retorna un mapa de "YYYY-MM-DD" a "ShiftID"
    val shiftsMap: Flow<Map<String, String>>
    suspend fun saveShift(date: String, shiftId: String)
    suspend fun clear()
}
