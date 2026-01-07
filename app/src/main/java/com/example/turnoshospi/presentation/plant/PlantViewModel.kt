package com.example.turnoshospi.presentation.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.Plant
import com.example.turnoshospi.PlantMembership
import com.example.turnoshospi.RegisteredUser
import com.example.turnoshospi.domain.repository.PlantError
import com.example.turnoshospi.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados de UI para operaciones de planta.
 */
sealed class PlantUiState<out T> {
    data object Idle : PlantUiState<Nothing>()
    data object Loading : PlantUiState<Nothing>()
    data class Success<T>(val data: T) : PlantUiState<T>()
    data class Error(val message: String) : PlantUiState<Nothing>()
}

/**
 * Estado completo de la pantalla de planta.
 */
data class PlantScreenState(
    val plant: Plant? = null,
    val membership: PlantMembership? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para la gestion de plantas/unidades hospitalarias.
 * Inyectado por Hilt.
 */
@HiltViewModel
class PlantViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _screenState = MutableStateFlow(PlantScreenState())
    val screenState: StateFlow<PlantScreenState> = _screenState.asStateFlow()

    private val _joinPlantState = MutableStateFlow<PlantUiState<Unit>>(PlantUiState.Idle)
    val joinPlantState: StateFlow<PlantUiState<Unit>> = _joinPlantState.asStateFlow()

    private val _staffOperationState = MutableStateFlow<PlantUiState<Unit>>(PlantUiState.Idle)
    val staffOperationState: StateFlow<PlantUiState<Unit>> = _staffOperationState.asStateFlow()

    /**
     * Carga la planta del usuario actual.
     */
    fun loadUserPlant(userId: String) {
        viewModelScope.launch {
            _screenState.update { it.copy(isLoading = true, error = null) }

            plantRepository.getUserPlantId(userId)
                .onSuccess { plantId ->
                    if (plantId != null) {
                        observePlant(plantId)
                        loadMembership(plantId, userId)
                    } else {
                        _screenState.update { it.copy(isLoading = false, plant = null) }
                    }
                }
                .onFailure { error ->
                    _screenState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Observa cambios en la planta en tiempo real.
     */
    fun observePlant(plantId: String) {
        viewModelScope.launch {
            plantRepository.observePlant(plantId).collect { plant ->
                _screenState.update { it.copy(plant = plant, isLoading = false) }
            }
        }
    }

    /**
     * Carga la membresia del usuario en la planta.
     */
    private fun loadMembership(plantId: String, userId: String) {
        viewModelScope.launch {
            plantRepository.getPlantMembership(plantId, userId)
                .onSuccess { membership ->
                    _screenState.update { it.copy(membership = membership) }
                }
        }
    }

    /**
     * Une al usuario a una planta con codigo de invitacion.
     */
    fun joinPlant(plantId: String, invitationCode: String, userId: String) {
        viewModelScope.launch {
            _joinPlantState.value = PlantUiState.Loading

            plantRepository.joinPlant(plantId, invitationCode, userId)
                .onSuccess {
                    _joinPlantState.value = PlantUiState.Success(Unit)
                    // Recargar la planta despues de unirse
                    loadUserPlant(userId)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is PlantError.PlantNotFound -> "La planta no existe"
                        is PlantError.InvalidInvitationCode -> "Codigo de invitacion invalido"
                        else -> error.message ?: "Error al unirse a la planta"
                    }
                    _joinPlantState.value = PlantUiState.Error(message)
                }
        }
    }

    /**
     * Vincula al usuario con un miembro del personal.
     */
    fun linkUserToStaff(plantId: String, userId: String, staff: RegisteredUser) {
        viewModelScope.launch {
            _staffOperationState.value = PlantUiState.Loading

            plantRepository.linkUserToStaff(plantId, userId, staff)
                .onSuccess {
                    _staffOperationState.value = PlantUiState.Success(Unit)
                    loadMembership(plantId, userId)
                }
                .onFailure { error ->
                    _staffOperationState.value = PlantUiState.Error(error.message ?: "Error al vincular")
                }
        }
    }

    /**
     * Registra un nuevo miembro del personal.
     */
    fun registerStaff(plantId: String, staff: RegisteredUser) {
        viewModelScope.launch {
            _staffOperationState.value = PlantUiState.Loading

            plantRepository.registerStaff(plantId, staff)
                .onSuccess {
                    _staffOperationState.value = PlantUiState.Success(Unit)
                }
                .onFailure { error ->
                    _staffOperationState.value = PlantUiState.Error(error.message ?: "Error al registrar")
                }
        }
    }

    /**
     * Actualiza un miembro del personal.
     */
    fun updateStaff(plantId: String, staff: RegisteredUser) {
        viewModelScope.launch {
            _staffOperationState.value = PlantUiState.Loading

            plantRepository.updateStaff(plantId, staff)
                .onSuccess {
                    _staffOperationState.value = PlantUiState.Success(Unit)
                }
                .onFailure { error ->
                    _staffOperationState.value = PlantUiState.Error(error.message ?: "Error al actualizar")
                }
        }
    }

    /**
     * Elimina un miembro del personal.
     */
    fun deleteStaff(plantId: String, staffId: String) {
        viewModelScope.launch {
            _staffOperationState.value = PlantUiState.Loading

            plantRepository.deleteStaff(plantId, staffId)
                .onSuccess {
                    _staffOperationState.value = PlantUiState.Success(Unit)
                }
                .onFailure { error ->
                    _staffOperationState.value = PlantUiState.Error(error.message ?: "Error al eliminar")
                }
        }
    }

    /**
     * Limpia el estado de union a planta.
     */
    fun clearJoinPlantState() {
        _joinPlantState.value = PlantUiState.Idle
    }

    /**
     * Limpia el estado de operaciones de staff.
     */
    fun clearStaffOperationState() {
        _staffOperationState.value = PlantUiState.Idle
    }

    /**
     * Limpia errores de pantalla.
     */
    fun clearError() {
        _screenState.update { it.copy(error = null) }
    }
}
