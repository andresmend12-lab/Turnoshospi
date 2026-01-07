package com.example.turnoshospi.di

import com.example.turnoshospi.data.repository.FirebaseAuthRepository
import com.example.turnoshospi.data.repository.FirebasePlantRepository
import com.example.turnoshospi.data.repository.FirebaseShiftRepository
import com.example.turnoshospi.domain.repository.AuthRepository
import com.example.turnoshospi.domain.repository.PlantRepository
import com.example.turnoshospi.domain.repository.ShiftRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo Hilt que vincula interfaces de repositorios con sus implementaciones.
 * Usa @Binds para mayor eficiencia que @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlantRepository(
        impl: FirebasePlantRepository
    ): PlantRepository

    @Binds
    @Singleton
    abstract fun bindShiftRepository(
        impl: FirebaseShiftRepository
    ): ShiftRepository
}
