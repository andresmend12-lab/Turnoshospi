package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.domain.model.Shift
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    fun observeShifts(plantId: String): Flow<List<Shift>>
    suspend fun requestShiftChange(shiftId: String, requesterUserId: String): Result<Unit>
}
