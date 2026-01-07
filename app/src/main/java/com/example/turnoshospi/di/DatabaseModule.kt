package com.example.turnoshospi.di

import android.content.Context
import androidx.room.Room
import com.example.turnoshospi.data.local.AppDatabase
import com.example.turnoshospi.data.local.dao.PlantDao
import com.example.turnoshospi.data.local.dao.ShiftDao
import com.example.turnoshospi.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo Hilt que provee Room Database y DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideShiftDao(database: AppDatabase): ShiftDao {
        return database.shiftDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun providePlantDao(database: AppDatabase): PlantDao {
        return database.plantDao()
    }
}
