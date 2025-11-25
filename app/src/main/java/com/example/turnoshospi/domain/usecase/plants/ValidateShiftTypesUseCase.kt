package com.example.turnoshospi.domain.usecase.plants

import com.example.turnoshospi.domain.model.plants.ShiftType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ValidateShiftTypesUseCase {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    fun invoke(shiftTypes: List<ShiftType>): ValidationResult {
        if (shiftTypes.isEmpty()) {
            return ValidationResult(false, "Debe haber al menos un tipo de turno")
        }

        shiftTypes.forEach { shift ->
            if (shift.label.isBlank()) {
                return ValidationResult(false, "La etiqueta del turno no puede estar vacía")
            }
            val start = parseTime(shift.startTime)
                ?: return ValidationResult(false, "Formato de hora de inicio inválido")
            val end = parseTime(shift.endTime)
                ?: return ValidationResult(false, "Formato de hora de fin inválido")

            val startsBeforeEnd = start.isBefore(end)
            if (!startsBeforeEnd && !shift.isNightShift) {
                return ValidationResult(false, "La hora de inicio debe ser menor a la hora de fin")
            }

            if (shift.durationMinutes <= 0) {
                return ValidationResult(false, "La duración debe ser mayor que 0")
            }
        }

        return ValidationResult(true, null)
    }

    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value, formatter)
    } catch (e: DateTimeParseException) {
        null
    }
}

data class ValidationResult(val isValid: Boolean, val errorMessage: String?)
