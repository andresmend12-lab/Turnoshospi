package com.example.turnoshospi.presentation.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShiftChangeUiState(
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val requestSuccessMessage: String? = null
)

class ShiftChangeViewModel(
    private val shiftRepository: ShiftRepository,
    private val plantId: String,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftChangeUiState())
    val uiState: StateFlow<ShiftChangeUiState> = _uiState.asStateFlow()

    init {
        observeShifts()
    }

    private fun observeShifts() {
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = shiftRepository.requestShiftChange(shift.id, currentUserId)
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
