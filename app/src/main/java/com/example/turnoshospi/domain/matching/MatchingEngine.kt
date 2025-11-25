package com.example.turnoshospi.domain.matching

import com.example.turnoshospi.domain.model.Preference
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.StaffCategory
import com.example.turnoshospi.domain.model.StaffSlot
import com.example.turnoshospi.domain.model.SwapMode
import com.example.turnoshospi.domain.model.SwapParticipant
import com.example.turnoshospi.domain.model.SwapParticipantRole
import com.example.turnoshospi.domain.model.SwapRequest
import com.example.turnoshospi.domain.model.SwapShift
import com.example.turnoshospi.domain.model.SwapStatus
import com.example.turnoshospi.domain.model.SwapType
import com.example.turnoshospi.domain.rules.ShiftRules

class MatchingEngine(private val rules: ShiftRules = ShiftRules()) {
    data class MatchCandidate(
        val request: SwapRequest,
        val score: Int,
    )

    fun findMatches(
        plantId: String,
        monthKey: String,
        preferences: List<Preference>,
        shifts: List<Shift>,
        staffSlots: List<StaffSlot>,
        mode: SwapMode,
    ): List<MatchCandidate> {
        val candidates = mutableListOf<MatchCandidate>()
        preferences.forEach { pref ->
            pref.lookingForChange.forEach { wanted ->
                val ownedShift = shifts.firstOrNull { it.staffSlotId == pref.staffSlotId && it.date == wanted.date && it.shiftTypeId == wanted.shiftTypeId }
                    ?: return@forEach
                val sameCategorySlots = staffSlots.filter { slot ->
                    slot.category == staffSlots.firstOrNull { it.id == pref.staffSlotId }?.category
                }
                preferences.filter { it.staffSlotId != pref.staffSlotId }.forEach { otherPref ->
                    val otherSlot = staffSlots.firstOrNull { it.id == otherPref.staffSlotId } ?: return@forEach
                    if (otherSlot !in sameCategorySlots) return@forEach
                    val willingShift = shifts.firstOrNull { it.staffSlotId == otherPref.staffSlotId &&
                        otherPref.willingToWork.any { willing -> willing.date == it.date && willing.shiftTypeId == it.shiftTypeId }
                    }
                    if (willingShift != null) {
                        val exchange = buildExchange(
                            plantId,
                            ownedShift.id,
                            willingShift.id,
                            pref.staffSlotId,
                            otherPref.staffSlotId,
                            mode
                        )
                        val validation = rules.validateSwap(shifts, listOf(
                            ownedShift.copy(staffSlotId = otherPref.staffSlotId),
                            willingShift.copy(staffSlotId = pref.staffSlotId)
                        ), staffSlots)
                        if (validation.valid) {
                            candidates += MatchCandidate(exchange, score = 100)
                        }
                    } else if (mode == SwapMode.FLEXIBLE) {
                        val gift = buildGift(plantId, ownedShift.id, pref.staffSlotId, otherPref.staffSlotId, mode)
                        val validation = rules.validateSwap(
                            shifts,
                            listOf(ownedShift.copy(staffSlotId = otherPref.staffSlotId)),
                            staffSlots
                        )
                        if (validation.valid) {
                            candidates += MatchCandidate(gift, score = 50)
                        }
                    }
                }
            }
        }
        return candidates.sortedByDescending { it.score }
    }

    private fun buildExchange(
        plantId: String,
        shiftA: String,
        shiftB: String,
        staffA: String,
        staffB: String,
        mode: SwapMode,
    ): SwapRequest {
        return SwapRequest(
            type = SwapType.EXCHANGE,
            plantId = plantId,
            participants = listOf(
                SwapParticipant(staffA, SwapParticipantRole.SWAPPER),
                SwapParticipant(staffB, SwapParticipantRole.SWAPPER),
            ),
            shiftsInvolved = listOf(
                SwapShift(shiftA, staffA, staffB),
                SwapShift(shiftB, staffB, staffA),
            ),
            status = SwapStatus.PENDING_USERS,
            mode = mode,
            createdByUserId = staffA,
        )
    }

    private fun buildGift(
        plantId: String,
        shiftId: String,
        from: String,
        to: String,
        mode: SwapMode,
    ): SwapRequest {
        return SwapRequest(
            type = SwapType.GIFT,
            plantId = plantId,
            participants = listOf(
                SwapParticipant(from, SwapParticipantRole.GIVER),
                SwapParticipant(to, SwapParticipantRole.RECEIVER),
            ),
            shiftsInvolved = listOf(
                SwapShift(shiftId, from, to),
            ),
            status = SwapStatus.PENDING_USERS,
            mode = mode,
            createdByUserId = from,
        )
    }
}
