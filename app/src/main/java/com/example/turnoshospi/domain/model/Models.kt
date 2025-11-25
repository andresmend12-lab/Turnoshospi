package com.example.turnoshospi.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class GlobalRole { SUPERVISOR, NURSE, AUXILIARY }

enum class ShiftDayType { WORKDAY, WEEKEND, HOLIDAY }

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val globalRole: GlobalRole,
    val currentPlantId: String?,
    val fcmToken: String? = null,
)

data class ShiftType(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Int,
    val isNightShift: Boolean = false,
    val isHalfDay: Boolean = false,
)

data class Plant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val ownerUserId: String,
    val shiftTypes: List<ShiftType>,
    val requiredStaffPerShift: Map<String, Map<String, Int>>, // weekday -> shiftTypeId -> requiredCount
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class StaffCategory { NURSE, AUXILIARY, SUPERVISOR }

data class StaffSlot(
    val id: String = UUID.randomUUID().toString(),
    val plantId: String,
    val name: String,
    val category: StaffCategory,
    val assignedUserId: String? = null,
    val isSupervisor: Boolean = false,
    val isActive: Boolean = true,
)

data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val plantId: String,
    val staffSlotId: String,
    val date: LocalDate,
    val shiftTypeId: String,
    val status: ShiftStatus = ShiftStatus.ASSIGNED,
    val dayType: ShiftDayType = ShiftDayType.WORKDAY,
    val isNightShift: Boolean = false,
    val monthKey: String = "${'$'}{date.year}-${'$'}{"%02d".format(date.monthValue)}",
)

enum class ShiftStatus { ASSIGNED, SWAPPED, CANCELED }

data class Preference(
    val id: String = UUID.randomUUID().toString(),
    val staffSlotId: String,
    val monthKey: String,
    val lookingForChange: List<ShiftRequest>,
    val willingToWork: List<ShiftRequest>,
)

data class ShiftRequest(
    val date: LocalDate,
    val shiftTypeId: String,
)

enum class SwapType { EXCHANGE, GIFT, MULTI_PARTY }

enum class SwapParticipantRole { GIVER, RECEIVER, SWAPPER }

enum class SwapStatus { PENDING, PENDING_USERS, PENDING_SUPERVISOR, APPROVED, REJECTED }

enum class SwapMode { STRICT, FLEXIBLE }

data class SwapParticipant(
    val staffSlotId: String,
    val role: SwapParticipantRole,
)

data class SwapShift(
    val shiftId: String,
    val fromStaffSlotId: String,
    val toStaffSlotId: String,
)

enum class DebtCategory { NIGHT, HOLIDAY, WEEKEND, REGULAR }

data class TurnDebt(
    val id: String = UUID.randomUUID().toString(),
    val creditorStaffSlotId: String,
    val debtorStaffSlotId: String,
    val category: DebtCategory,
    val createdAt: Long = System.currentTimeMillis(),
    val settled: Boolean = false,
    val settledAt: Long? = null,
    val relatedSwapRequestId: String? = null,
    val notes: String? = null,
)

data class SwapRequest(
    val id: String = UUID.randomUUID().toString(),
    val type: SwapType,
    val plantId: String,
    val participants: List<SwapParticipant>,
    val shiftsInvolved: List<SwapShift>,
    val debtsGenerated: List<TurnDebt> = emptyList(),
    val status: SwapStatus = SwapStatus.PENDING_USERS,
    val mode: SwapMode = SwapMode.STRICT,
    val createdByUserId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val targetUserId: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val plantId: String,
    val swapRequestId: String,
    val senderUserId: String,
    val text: String,
    val mentions: List<String> = emptyList(),
    val isSupervisorNote: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val swapRequestId: String,
    val description: String,
    val affectedStaffSlots: List<String>,
    val changedByUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class FeedbackEntry(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val plantId: String?,
    val type: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceInfo: String? = null,
)
