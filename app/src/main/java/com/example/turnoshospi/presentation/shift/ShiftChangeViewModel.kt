package com.example.turnoshospi.presentation.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShiftChangeUiState(
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val requestSuccessMessage: String? = null
)

/**
 * ViewModel para la pantalla de cambio de turnos.
 * Usa SavedStateHandle para recibir parametros de navegacion.
 */
@HiltViewModel
class ShiftChangeViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Parametros de navegacion
    private val plantId: String = savedStateHandle.get<String>("plantId") ?: ""
    private val currentUserId: String = savedStateHandle.get<String>("currentUserId") ?: ""

    private val _uiState = MutableStateFlow(ShiftChangeUiState())
    val uiState: StateFlow<ShiftChangeUiState> = _uiState.asStateFlow()

    init {
        if (plantId.isNotBlank()) {
            observeShifts()
        }
    }

    /**
     * Inicializa el ViewModel con parametros si no se pasaron via navegacion.
     * Util para uso desde pantallas que no usan Navigation Component.
     */
    fun initialize(plantId: String, userId: String) {
        if (this.plantId.isBlank() && plantId.isNotBlank()) {
            observeShiftsForPlant(plantId, userId)
        }
    }

    private fun observeShifts() {
        observeShiftsForPlant(plantId, currentUserId)
    }

    private fun observeShiftsForPlant(plantId: String, userId: String) {
        viewModelScope.launch {
            shiftRepository.observeShifts(plantId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { shifts ->
                    _uiState.update { it.copy(shifts = shifts, isLoading = false) }
                }
        }
    }

    fun onRequestChange(shift: Shift) {
        val userId = currentUserId.ifBlank { return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = shiftRepository.requestShiftChange(shift.id, userId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, requestSuccessMessage = "Solicitud enviada") }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error al solicitar cambio") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, requestSuccessMessage = null) }
    }
}
