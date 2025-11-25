package com.example.turnoshospi.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.model.Preference
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.SwapRequest
import com.example.turnoshospi.domain.repository.PlantRepository
import com.example.turnoshospi.domain.repository.ShiftRepository
import com.example.turnoshospi.domain.repository.SwapRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val plantRepository: PlantRepository,
    private val shiftRepository: ShiftRepository,
    private val swapRequestRepository: SwapRequestRepository,
) : ViewModel() {
    private val _shifts = MutableStateFlow<List<Shift>>(emptyList())
    val shifts: StateFlow<List<Shift>> = _shifts

    val pendingRequests = MutableStateFlow<List<SwapRequest>>(emptyList())
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun observeMonth(plantId: String, monthKey: String) {
        viewModelScope.launch {
            shiftRepository.shiftsForMonth(plantId, monthKey).collect {
                _shifts.value = it
            }
        }
    }

    fun createPlant(name: String, description: String) {
        // delegate to repository and show progress
        isLoading.value = true
        viewModelScope.launch {
            runCatching { /* TODO supply profile */ }
                .onFailure { error.value = it.message }
                .also { isLoading.value = false }
        }
    }

    fun joinPlant(code: String) {
        isLoading.value = true
        viewModelScope.launch {
            runCatching { /* TODO supply profile */ }
                .onFailure { error.value = it.message }
                .also { isLoading.value = false }
        }
    }

    fun savePreferences(preference: Preference) {
        viewModelScope.launch { shiftRepository.savePreferences(preference) }
    }

    fun loadSwapRequests(userId: String, plantId: String) {
        viewModelScope.launch {
            swapRequestRepository.swapRequestsForUser(userId, plantId).collect { pendingRequests.value = it }
        }
    }
}
