package com.example.turnoshospi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.turnoshospi.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de turnos en Room.
 */
@Dao
interface ShiftDao {

    // --- INSERT ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftEntity>)

    // --- UPDATE ---

    @Update
    suspend fun update(shift: ShiftEntity)

    // --- DELETE ---

    @Delete
    suspend fun delete(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :shiftId")
    suspend fun deleteById(shiftId: String)

    @Query("DELETE FROM shifts WHERE plantId = :plantId")
    suspend fun deleteByPlantId(plantId: String)

    @Query("DELETE FROM shifts")
    suspend fun deleteAll()

    // --- QUERIES ---

    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    suspend fun getById(shiftId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE plantId = :plantId ORDER BY date DESC")
    fun getByPlantId(plantId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE userId = :userId ORDER BY date DESC")
    fun getByUserId(userId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE plantId = :plantId AND date = :date")
    suspend fun getByPlantAndDate(plantId: String, date: String): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE plantId = :plantId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByPlantAndDateRange(
        plantId: String,
        startDate: String,
        endDate: String
    ): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE isDirty = 1")
    suspend fun getDirtyShifts(): List<ShiftEntity>

    @Query("UPDATE shifts SET isDirty = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Long = System.currentTimeMillis())
}
