package com.example.turnoshospi.domain.model.plants

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a hospital plant that can be managed by supervisors.
 */
data class Plant(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUserId: String = "",
    val shiftTypes: List<ShiftType> = emptyList(),
    val requiredStaffPerShift: Map<String, Map<String, Int>> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val inviteCode: String = "",
    val inviteLink: String = ""
)
