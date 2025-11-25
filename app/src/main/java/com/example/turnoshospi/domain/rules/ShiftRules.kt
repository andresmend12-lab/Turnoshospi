package com.example.turnoshospi.domain.rules

import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.StaffSlot
import java.time.LocalDate

class ShiftRules {
    data class ValidationResult(val valid: Boolean, val reason: String? = null)

    fun validateSingleDay(shifts: List<Shift>, newShift: Shift): ValidationResult {
        val sameDay = shifts.any { it.staffSlotId == newShift.staffSlotId && it.date == newShift.date }
        return if (sameDay) ValidationResult(false, "No se permiten dos turnos el mismo día")
        else ValidationResult(true)
    }

    fun validateNightRest(shifts: List<Shift>, newShift: Shift): ValidationResult {
        val prevDay = newShift.date.minusDays(1)
        val hadNight = shifts.any { it.staffSlotId == newShift.staffSlotId && it.date == prevDay && it.isNightShift }
        return if (hadNight) ValidationResult(false, "Hay día saliente después de noche") else ValidationResult(true)
    }

    fun validateConsecutiveLimit(shifts: List<Shift>, staffSlotId: String, maxConsecutive: Int = 6): ValidationResult {
        val dates = shifts.filter { it.staffSlotId == staffSlotId }
            .map { it.date }
            .toSet()
        var longest = 0
        val sorted = dates.sorted()
        var streak = 0
        var prev: LocalDate? = null
        for (date in sorted) {
            if (prev == null || date.minusDays(1) == prev) {
                streak++
            } else {
                streak = 1
            }
            longest = maxOf(longest, streak)
            prev = date
        }
        return if (longest > maxConsecutive) {
            ValidationResult(false, "Se superan ${'$'}maxConsecutive días consecutivos")
        } else ValidationResult(true)
    }

    fun validateSwap(shifts: List<Shift>, updatedShifts: List<Shift>, staffSlots: List<StaffSlot>): ValidationResult {
        val combined = shifts + updatedShifts
        val ids = updatedShifts.map { it.staffSlotId }.distinct()
        ids.forEach { id ->
            val byPerson = combined.filter { it.staffSlotId == id }
            byPerson.forEach { shift ->
                val singleDay = validateSingleDay(byPerson, shift)
                if (!singleDay.valid) return singleDay
                val night = validateNightRest(byPerson, shift)
                if (!night.valid) return night
            }
            val streaks = validateConsecutiveLimit(combined, id)
            if (!streaks.valid) return streaks
        }

        updatedShifts.forEach { shift ->
            val staffSlot = staffSlots.firstOrNull { it.id == shift.staffSlotId }
                ?: return ValidationResult(false, "StaffSlot no encontrado")
            val original = staffSlots.firstOrNull { it.id == shift.staffSlotId }
            if (staffSlot.category != original?.category) {
                return ValidationResult(false, "Los intercambios deben ser entre el mismo rol")
            }
        }

        return ValidationResult(true)
    }
}
