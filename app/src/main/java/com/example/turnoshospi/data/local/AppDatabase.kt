package com.example.turnoshospi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.turnoshospi.data.local.converter.RoomTypeConverters
import com.example.turnoshospi.data.local.dao.PlantDao
import com.example.turnoshospi.data.local.dao.ShiftDao
import com.example.turnoshospi.data.local.dao.UserProfileDao
import com.example.turnoshospi.data.local.entity.PlantEntity
import com.example.turnoshospi.data.local.entity.ShiftEntity
import com.example.turnoshospi.data.local.entity.UserProfileEntity

/**
 * Base de datos Room para cache offline.
 *
 * Estrategia de sincronizacion:
 * - Los datos se cargan primero desde cache local
 * - Se actualizan desde Firebase en background
 * - isDirty marca cambios locales pendientes de sincronizar
 * - lastSyncedAt marca la ultima sincronizacion exitosa
 */
@Database(
    entities = [
        ShiftEntity::class,
        UserProfileEntity::class,
        PlantEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shiftDao(): ShiftDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun plantDao(): PlantDao

    companion object {
        const val DATABASE_NAME = "turnoshospi_db"
    }
}
