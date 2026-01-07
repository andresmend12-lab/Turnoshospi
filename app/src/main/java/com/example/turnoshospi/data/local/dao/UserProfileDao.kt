package com.example.turnoshospi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.turnoshospi.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de perfiles de usuario en Room.
 */
@Dao
interface UserProfileDao {

    // --- INSERT ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<UserProfileEntity>)

    // --- UPDATE ---

    @Update
    suspend fun update(profile: UserProfileEntity)

    // --- DELETE ---

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAll()

    // --- QUERIES ---

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getById(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun observeById(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE plantId = :plantId")
    fun getByPlantId(plantId: String): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE email = :email")
    suspend fun getByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE isDirty = 1")
    suspend fun getDirtyProfiles(): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET isDirty = 0, lastSyncedAt = :syncTime WHERE userId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Long = System.currentTimeMillis())
}
