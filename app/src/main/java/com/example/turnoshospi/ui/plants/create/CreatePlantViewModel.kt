package com.example.turnoshospi.ui.plants.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.data.repository.plants.PlantRepository
import com.example.turnoshospi.domain.model.plants.Plant
import com.example.turnoshospi.domain.model.plants.ShiftType
import com.example.turnoshospi.domain.usecase.plants.CreateInitialStaffSlotsUseCase
import com.example.turnoshospi.domain.usecase.plants.CreatePlantUseCase
import com.example.turnoshospi.domain.usecase.plants.GenerateInviteCodeUseCase
import com.example.turnoshospi.domain.usecase.plants.PlantCreationResult
import com.example.turnoshospi.domain.usecase.plants.ValidateShiftTypesUseCase
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreatePlantViewModel(
    private val createPlantUseCase: CreatePlantUseCase = CreatePlantUseCase(
        plantRepository = PlantRepository(),
        validateShiftTypesUseCase = ValidateShiftTypesUseCase(),
        generateInviteCodeUseCase = GenerateInviteCodeUseCase(PlantRepository()),
        createInitialStaffSlotsUseCase = CreateInitialStaffSlotsUseCase()
    ),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlantUiState())
    val uiState: StateFlow<CreatePlantUiState> = _uiState

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, error = null) }
    }

    fun addShiftType() {
        val newShift = ShiftType(
            id = UUID.randomUUID().toString(),
            label = "",
            startTime = "08:00",
            endTime = "16:00",
            durationMinutes = 480
        )
        _uiState.update { it.copy(shiftTypes = it.shiftTypes + newShift) }
    }

    fun updateShiftType(id: String, updater: (ShiftType) -> ShiftType) {
        _uiState.update {
            it.copy(shiftTypes = it.shiftTypes.map { shift -> if (shift.id == id) updater(shift) else shift })
        }
    }

    fun removeShiftType(id: String) {
        _uiState.update { state ->
            val filtered = state.shiftTypes.filterNot { it.id == id }
            val newRequirements = state.requiredStaff.mapValues { (_, map) -> map.filterKeys { key -> key in filtered.map { it.id } } }
            state.copy(shiftTypes = filtered, requiredStaff = newRequirements)
        }
    }

    fun updateRequiredStaff(dayKey: String, shiftId: String, count: Int) {
        _uiState.update { state ->
            val dayMap = state.requiredStaff[dayKey].orEmpty().toMutableMap()
            dayMap[shiftId] = count.coerceAtLeast(0)
            val newMap = state.requiredStaff.toMutableMap()
            newMap[dayKey] = dayMap
            state.copy(requiredStaff = newMap)
        }
    }

    fun createPlant(supervisorName: String) {
        val state = _uiState.value
        val ownerId = auth.currentUser?.uid ?: ""
        val plant = Plant(
            name = state.name,
            description = state.description,
            ownerUserId = ownerId,
            shiftTypes = state.shiftTypes,
            requiredStaffPerShift = state.requiredStaff
        )

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, success = null) }
            when (val result = createPlantUseCase.invoke(plant, supervisorName)) {
                is PlantCreationResult.Success -> _uiState.update {
                    it.copy(loading = false, success = result)
                }

                is PlantCreationResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }

                PlantCreationResult.Loading -> _uiState.update { it.copy(loading = true) }
            }
        }
    }

    companion object {
        val defaultDays = listOf(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
        )
    }
}

data class CreatePlantUiState(
    val name: String = "",
    val description: String = "",
    val shiftTypes: List<ShiftType> = emptyList(),
    val requiredStaff: Map<String, Map<String, Int>> = defaultRequiredStaffMap(),
    val loading: Boolean = false,
    val success: PlantCreationResult.Success? = null,
    val error: String? = null
)

fun defaultRequiredStaffMap(): Map<String, Map<String, Int>> =
    CreatePlantViewModel.defaultDays.associateWith { emptyMap() }
