package com.example.turnoshospi.domain.usecase.plants

import com.example.turnoshospi.data.repository.plants.PlantRepository
import com.example.turnoshospi.domain.model.plants.Plant
import com.example.turnoshospi.domain.model.plants.StaffSlot

class CreatePlantUseCase(
    private val plantRepository: PlantRepository,
    private val validateShiftTypesUseCase: ValidateShiftTypesUseCase,
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase,
    private val createInitialStaffSlotsUseCase: CreateInitialStaffSlotsUseCase
) {
    suspend fun invoke(
        plant: Plant,
        supervisorName: String,
        extraStaffSlots: List<StaffSlot> = emptyList()
    ): PlantCreationResult {
        if (plant.name.isBlank()) {
            return PlantCreationResult.Error("El nombre no puede estar vacío")
        }

        val validation = validateShiftTypesUseCase.invoke(plant.shiftTypes)
        if (!validation.isValid) {
            return PlantCreationResult.Error(validation.errorMessage ?: "Datos de turno inválidos")
        }

        val plantId = plantRepository.createPlant(plant)
        val supervisorSlots = createInitialStaffSlotsUseCase.invoke(supervisorName)
        val slotsToCreate = (supervisorSlots + extraStaffSlots).map { it.copy(plantId = plantId) }
        plantRepository.createStaffSlots(plantId, slotsToCreate)
        supervisorSlots.firstOrNull()?.let { slot ->
            plantRepository.updateSupervisorAssignment(plantId, slot.id, plant.ownerUserId)
        }

        val invite = generateInviteCodeUseCase.invoke(plantId)
        return PlantCreationResult.Success(
            plantId = plantId,
            inviteCode = invite.code,
            inviteLink = invite.link
        )
    }
}

sealed class PlantCreationResult {
    data class Success(val plantId: String, val inviteCode: String, val inviteLink: String) : PlantCreationResult()
    data class Error(val message: String) : PlantCreationResult()
    object Loading : PlantCreationResult()
}
