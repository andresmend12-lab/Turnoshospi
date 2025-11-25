package com.example.turnoshospi.domain.usecase.plants

import com.example.turnoshospi.domain.model.plants.StaffSlot
import java.util.UUID

class CreateInitialStaffSlotsUseCase {
    fun invoke(supervisorName: String, supervisorRole: String = "supervisor"): List<StaffSlot> {
        val supervisorSlot = StaffSlot(
            id = UUID.randomUUID().toString(),
            name = supervisorName.ifBlank { "Supervisor" },
            role = supervisorRole,
            isSupervisor = true
        )
        return listOf(supervisorSlot)
    }
}
