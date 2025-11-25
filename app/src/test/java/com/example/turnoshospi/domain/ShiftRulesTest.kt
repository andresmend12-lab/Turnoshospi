package com.example.turnoshospi.domain

import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.StaffSlot
import com.example.turnoshospi.domain.model.StaffCategory
import com.example.turnoshospi.domain.rules.ShiftRules
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftRulesTest {
    private val rules = ShiftRules()
    private val staffSlot = StaffSlot(id = "a", plantId = "p", name = "Ana", category = StaffCategory.NURSE)

    @Test
    fun `prevent two shifts same day`() {
        val existing = listOf(shift(LocalDate.of(2024, 10, 1)))
        val result = rules.validateSingleDay(existing, shift(LocalDate.of(2024, 10, 1)))
        assertFalse(result.valid)
    }

    @Test
    fun `night shift blocks next day`() {
        val existing = listOf(shift(LocalDate.of(2024, 10, 1), isNight = true))
        val result = rules.validateNightRest(existing, shift(LocalDate.of(2024, 10, 2)))
        assertFalse(result.valid)
    }

    @Test
    fun `max six consecutive days`() {
        val existing = (1..7).map { shift(LocalDate.of(2024, 10, it.toLong())) }
        val result = rules.validateConsecutiveLimit(existing, staffSlot.id)
        assertFalse(result.valid)
    }

    private fun shift(date: LocalDate, isNight: Boolean = false) = Shift(
        plantId = "p",
        staffSlotId = staffSlot.id,
        date = date,
        shiftTypeId = "morning",
        isNightShift = isNight
    )
}
