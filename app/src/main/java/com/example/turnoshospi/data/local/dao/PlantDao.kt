package com.example.turnoshospi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.turnoshospi.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de plantas en Room.
 */
@Dao
interface PlantDao {

    // --- INSERT ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PlantEntity>)

    // --- UPDATE ---

    @Update
    suspend fun update(plant: PlantEntity)

    // --- DELETE ---

    @Delete
    suspend fun delete(plant: PlantEntity)

    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun deleteById(plantId: String)

    @Query("DELETE FROM plants")
    suspend fun deleteAll()

    // --- QUERIES ---

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getById(plantId: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE id = :plantId")
    fun observeById(plantId: String): Flow<PlantEntity?>

    @Query("SELECT * FROM plants ORDER BY name ASC")
    fun getAll(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE hospitalName = :hospitalName")
    fun getByHospital(hospitalName: String): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE isDirty = 1")
    suspend fun getDirtyPlants(): List<PlantEntity>

    @Query("UPDATE plants SET isDirty = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Long = System.currentTimeMillis())
}
