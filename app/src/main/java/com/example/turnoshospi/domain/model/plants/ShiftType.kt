package com.example.turnoshospi.domain.model.plants

/**
 * Shift definition that will be associated to a plant.
 */
data class ShiftType(
    val id: String = "",
    val label: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: Int = 0,
    val isNightShift: Boolean = false,
    val isHalfDay: Boolean = false
)
