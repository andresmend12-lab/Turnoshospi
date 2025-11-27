package com.example.turnoshospi

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ShiftRulesEngine {

    // --- Definición de Tipos de Turno ---
    enum class ShiftType { MORNING, AFTERNOON, NIGHT, OFF }

    // --- REGLA 1: Reglas de Rol ---
    fun canUserParticipate(userRole: String): Boolean {
        val normalized = userRole.trim().lowercase()
        // Los supervisores no pueden participar
        if (normalized.contains("supervisor")) return false
        // Solo enfermeros y auxiliares
        return normalized.contains("enfermer") || normalized.contains("auxiliar")
    }

    fun areRolesCompatible(roleA: String, roleB: String): Boolean {
        val rA = roleA.trim().lowercase()
        val rB = roleB.trim().lowercase()

        return when {
            rA.contains("enfermer") && rB.contains("enfermer") -> true
            rA.contains("auxiliar") && rB.contains("auxiliar") -> true
            else -> false
        }
    }

    // --- REGLA 2 y 4: Validación de Solicitud Inicial ---
    fun validateCreateRequest(
        requestDate: LocalDate,
        hasShiftThatDay: Boolean,
        isShiftBlocked: Boolean,
        offeredDates: List<String>
    ): String? {
        val today = LocalDate.now()

        if (!hasShiftThatDay) return "No tienes un turno asignado este día para cambiar."
        if (requestDate.isBefore(today)) return "No puedes cambiar un turno pasado."
        if (isShiftBlocked) return "Este turno está bloqueado o cerrado por el supervisor."
        // Nota: La validación de offeredDates se hace en la UI al confirmar el diálogo

        return null // Todo OK
    }

    // --- REGLA 3: Reglas Laborales Imprescindibles ---

    private fun getShiftType(shiftName: String): ShiftType {
        val name = shiftName.lowercase()
        return when {
            name.contains("noche") -> ShiftType.NIGHT
            name.contains("mañana") -> ShiftType.MORNING
            name.contains("tarde") -> ShiftType.AFTERNOON
            else -> ShiftType.OFF
        }
    }

    // Valida si un usuario puede aceptar un turno específico en una fecha
    fun validateWorkRules(
        targetDate: LocalDate,
        targetShiftName: String,
        userSchedule: Map<LocalDate, String> // Mapa Fecha -> NombreTurno
    ): String? {
        val targetType = getShiftType(targetShiftName)

        // 1. Un usuario no puede trabajar dos turnos el mismo día
        if (userSchedule.containsKey(targetDate)) {
            return "Ya tienes un turno asignado el ${targetDate}."
        }

        // 2. Prohibidos encadenamientos incompatibles y Regla de Saliente
        val yesterday = targetDate.minusDays(1)
        val tomorrow = targetDate.plusDays(1)

        val shiftYesterday = userSchedule[yesterday]?.let { getShiftType(it) } ?: ShiftType.OFF
        val shiftTomorrow = userSchedule[tomorrow]?.let { getShiftType(it) } ?: ShiftType.OFF

        // Si ayer fue noche, hoy es SALIENTE. No se puede trabajar NADA.
        if (shiftYesterday == ShiftType.NIGHT) {
            return "Vienes de una noche (Saliente). Debes descansar hoy."
        }

        // Si hoy trabajo NOCHE, mañana es SALIENTE.
        if (targetType == ShiftType.NIGHT && shiftTomorrow != ShiftType.OFF) {
            return "Si trabajas noche hoy, mañana debes librar (tienes turno asignado)."
        }

        // 3. No superar 6 días seguidos (Regla del 7º día)
        if (calculateConsecutiveDays(targetDate, userSchedule) >= 6) {
            return "Este cambio generaría 7 o más días seguidos de trabajo."
        }

        return null // Cumple reglas laborales
    }

    private fun calculateConsecutiveDays(targetDate: LocalDate, schedule: Map<LocalDate, String>): Int {
        var consecutive = 0

        // Hacia atrás
        var current = targetDate.minusDays(1)
        while (schedule.containsKey(current)) {
            consecutive++
            current = current.minusDays(1)
        }

        // Hacia adelante
        current = targetDate.plusDays(1)
        while (schedule.containsKey(current)) {
            consecutive++
            current = current.plusDays(1)
        }

        return consecutive + 1 // +1 por el día objetivo
    }

    // --- REGLA 5: Matching ---
    fun findMatch(
        myRequest: ShiftChangeRequest,
        otherRequest: ShiftChangeRequest,
        mySchedule: Map<LocalDate, String>,
        otherSchedule: Map<LocalDate, String>
    ): Boolean {
        // 1. Roles iguales
        if (!areRolesCompatible(myRequest.requesterRole, otherRequest.requesterRole)) return false

        // 2. Cruce de fechas (Simplificado: Swap directo)
        // Yo tomo su turno (fecha de su request), Él toma mi turno (fecha de mi request)
        val dateIWillWork = LocalDate.parse(otherRequest.originalDate)
        val dateHeWillWork = LocalDate.parse(myRequest.originalDate)

        // Verificar si la fecha que yo suelto está en sus ofrecidas y viceversa
        val isSwapDesired = myRequest.offeredDates.contains(otherRequest.originalDate) &&
                otherRequest.offeredDates.contains(myRequest.originalDate)

        if (!isSwapDesired) return false

        // 3. Validar reglas laborales
        val validForMe = validateWorkRules(dateIWillWork, otherRequest.originalShift, mySchedule) == null
        val validForHim = validateWorkRules(dateHeWillWork, myRequest.originalShift, otherSchedule) == null

        return validForMe && validForHim
    }
}