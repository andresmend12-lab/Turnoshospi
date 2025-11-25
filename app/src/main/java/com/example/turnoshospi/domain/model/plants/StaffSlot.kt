package com.example.turnoshospi.domain.model.plants

/**
 * Represents a staffing slot that can be assigned to a user.
 */
data class StaffSlot(
    val id: String = "",
    val plantId: String = "",
    val name: String = "",
    val role: String = "nurse",
    val assignedUserId: String? = null,
    val isSupervisor: Boolean = false
)
