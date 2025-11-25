package com.example.turnoshospi.domain.usecase.plants

import com.example.turnoshospi.data.repository.plants.PlantRepository
import java.util.UUID

class GenerateInviteCodeUseCase(
    private val plantRepository: PlantRepository
) {
    suspend fun invoke(plantId: String): InviteData {
        val code = UUID.randomUUID().toString().take(8)
        val link = plantRepository.generateInviteCode(plantId, code)
        return InviteData(code = code, link = link)
    }
}

data class InviteData(val code: String, val link: String)
