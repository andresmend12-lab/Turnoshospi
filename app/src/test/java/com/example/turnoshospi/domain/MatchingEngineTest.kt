package com.example.turnoshospi.domain

import com.example.turnoshospi.domain.matching.MatchingEngine
import com.example.turnoshospi.domain.model.Preference
import com.example.turnoshospi.domain.model.Shift
import com.example.turnoshospi.domain.model.ShiftRequest
import com.example.turnoshospi.domain.model.StaffCategory
import com.example.turnoshospi.domain.model.StaffSlot
import com.example.turnoshospi.domain.model.SwapMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchingEngineTest {
    private val engine = MatchingEngine()

    @Test
    fun `creates exchange between compatible staff`() {
        val staffA = StaffSlot(id = "a", plantId = "p", name = "Ana", category = StaffCategory.NURSE)
        val staffB = StaffSlot(id = "b", plantId = "p", name = "Bea", category = StaffCategory.NURSE)
        val shifts = listOf(
            shift(staffA.id, LocalDate.of(2024, 10, 1)),
            shift(staffB.id, LocalDate.of(2024, 10, 2)),
        )
        val prefs = listOf(
            Preference("prefA", staffA.id, "2024-10", lookingForChange = listOf(ShiftRequest(LocalDate.of(2024, 10, 1), "m")), willingToWork = emptyList()),
            Preference("prefB", staffB.id, "2024-10", lookingForChange = listOf(ShiftRequest(LocalDate.of(2024, 10, 2), "m")), willingToWork = listOf(ShiftRequest(LocalDate.of(2024, 10, 1), "m")))
        )

        val result = engine.findMatches("p", "2024-10", prefs, shifts, listOf(staffA, staffB), SwapMode.STRICT)
        assertEquals(1, result.size)
    }

    private fun shift(staffSlotId: String, date: LocalDate) = Shift(
        plantId = "p",
        staffSlotId = staffSlotId,
        date = date,
        shiftTypeId = "m"
    )
}
