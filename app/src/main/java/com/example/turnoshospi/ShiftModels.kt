package com.example.turnoshospi

import java.time.LocalDate

// --- ENUMS ---
enum class RequestType { COVERAGE, SWAP }
enum class RequestMode { STRICT, FLEXIBLE }
enum class RequestStatus { DRAFT, SEARCHING, PENDING_PARTNER, AWAITING_SUPERVISOR, APPROVED, REJECTED }

// --- DATA CLASSES ---

// Solicitud de cambio/cobertura
data class ShiftChangeRequest(
    val id: String = "",
    val type: RequestType = RequestType.SWAP,
    val status: RequestStatus = RequestStatus.SEARCHING,
    val mode: RequestMode = RequestMode.FLEXIBLE,
    val hardnessLevel: ShiftRulesEngine.Hardness = ShiftRulesEngine.Hardness.NORMAL,
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterRole: String = "",
    val requesterShiftDate: String = "",
    val requesterShiftName: String = "",
    val offeredDates: List<String> = emptyList(),

    // Target fields added for swap logic
    val targetUserId: String? = null,
    val targetUserName: String? = null,
    val targetShiftDate: String? = null,
    val targetShiftName: String? = null,

    // NUEVO CAMPO: Lista de IDs de supervisores que deben aprobar esto
    val supervisorIds: List<String> = emptyList(),

    val timestamp: Long = System.currentTimeMillis()
)

// ... (El resto del archivo FavorTransaction, MyShiftDisplay, PlantShift sigue igual)
data class FavorTransaction(
    val id: String = "",
    val covererId: String = "",
    val covererName: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val date: String = "",
    val shiftName: String = "",
    val timestamp: Long = 0
)

data class MyShiftDisplay(
    val date: String,
    val shiftName: String,
    val fullDate: LocalDate,
    val isHalfDay: Boolean = false
)

data class PlantShift(
    val userId: String,
    val userName: String,
    val userRole: String,
    val date: LocalDate,
    val shiftName: String
)