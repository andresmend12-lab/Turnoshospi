package com.example.turnoshospi.domain.model.plants

/**
 * Wrapper for minimum required staff per day of week.
 * dayOfWeek -> (shiftTypeId -> requiredCount)
 */
data class RequiredStaffConfig(
    val requirements: Map<String, Map<String, Int>> = emptyMap()
)
