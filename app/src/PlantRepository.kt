package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.domain.model.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun observePlant(plantId: String): Flow<Plant?>
    suspend fun createPlant(plant: Plant): Result<String> // Returns plantId
}