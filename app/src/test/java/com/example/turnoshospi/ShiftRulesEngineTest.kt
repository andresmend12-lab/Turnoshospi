package com.example.turnoshospi

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ShiftRulesEngine - Motor de Reglas de Negocio")
class ShiftRulesEngineTest {

    // =========================================================================
    // 1. calculateShiftHardness() - Cálculo de dureza del turno
    // =========================================================================
    @Nested
    @DisplayName("calculateShiftHardness()")
    inner class CalculateShiftHardnessTests {

        @Test
        fun `turno de Noche debe retornar Hardness NIGHT`() {
            val date = LocalDate.of(2024, 1, 15) // Lunes

            assertEquals(
                ShiftRulesEngine.Hardness.NIGHT,
                ShiftRulesEngine.calculateShiftHardness(date, "Noche")
            )
        }

        @Test
        fun `turno de noche en minusculas debe retornar Hardness NIGHT`() {
            val date = LocalDate.of(2024, 1, 15)

            assertEquals(
                ShiftRulesEngine.Hardness.NIGHT,
                ShiftRulesEngine.calculateShiftHardness(date, "noche")
            )
        }

        @Test
        fun `turno con palabra noche incluida debe retornar Hardness NIGHT`() {
            val date = LocalDate.of(2024, 1, 15)

            assertEquals(
                ShiftRulesEngine.Hardness.NIGHT,
                ShiftRulesEngine.calculateShiftHardness(date, "Turno Noche Largo")
            )
        }

        @Test
        fun `turno en sabado debe retornar Hardness WEEKEND`() {
            val saturday = LocalDate.of(2024, 1, 13) // Sábado

            assertEquals(
                ShiftRulesEngine.Hardness.WEEKEND,
                ShiftRulesEngine.calculateShiftHardness(saturday, "Mañana")
            )
        }

        @Test
        fun `turno en domingo debe retornar Hardness WEEKEND`() {
            val sunday = LocalDate.of(2024, 1, 14) // Domingo

            assertEquals(
                ShiftRulesEngine.Hardness.WEEKEND,
                ShiftRulesEngine.calculateShiftHardness(sunday, "Tarde")
            )
        }

        @Test
        fun `turno normal en dia laborable debe retornar Hardness NORMAL`() {
            val wednesday = LocalDate.of(2024, 1, 17) // Miércoles

            assertEquals(
                ShiftRulesEngine.Hardness.NORMAL,
                ShiftRulesEngine.calculateShiftHardness(wednesday, "Mañana")
            )
        }

        @Test
        fun `turno de noche en fin de semana prioriza NIGHT sobre WEEKEND`() {
            val saturday = LocalDate.of(2024, 1, 13) // Sábado

            // La noche tiene prioridad sobre el fin de semana
            assertEquals(
                ShiftRulesEngine.Hardness.NIGHT,
                ShiftRulesEngine.calculateShiftHardness(saturday, "Noche")
            )
        }
    }

    // =========================================================================
    // 2. canUserParticipate() - Validación de rol para participar
    // =========================================================================
    @Nested
    @DisplayName("canUserParticipate()")
    inner class CanUserParticipateTests {

        @Test
        fun `Supervisor no puede participar en cambios`() {
            assertFalse(ShiftRulesEngine.canUserParticipate("Supervisor"))
        }

        @Test
        fun `Supervisora no puede participar en cambios`() {
            assertFalse(ShiftRulesEngine.canUserParticipate("Supervisora"))
        }

        @Test
        fun `Supervisor con texto adicional no puede participar`() {
            assertFalse(ShiftRulesEngine.canUserParticipate("Supervisor de Planta"))
        }

        @Test
        fun `Enfermero puede participar en cambios`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("Enfermero"))
        }

        @Test
        fun `Enfermera puede participar en cambios`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("Enfermera"))
        }

        @Test
        fun `Enfermero con especialidad puede participar`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("Enfermero UCI"))
        }

        @Test
        fun `Auxiliar puede participar en cambios`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("Auxiliar"))
        }

        @Test
        fun `TCAE puede participar en cambios`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("TCAE"))
        }

        @Test
        fun `Auxiliar de enfermeria puede participar`() {
            assertTrue(ShiftRulesEngine.canUserParticipate("Auxiliar de Enfermería"))
        }

        @Test
        fun `Rol desconocido no puede participar`() {
            assertFalse(ShiftRulesEngine.canUserParticipate("Medico"))
        }

        @Test
        fun `Rol vacio no puede participar`() {
            assertFalse(ShiftRulesEngine.canUserParticipate(""))
        }
    }

    // =========================================================================
    // 3. areRolesCompatible() - Compatibilidad de roles para intercambio
    // =========================================================================
    @Nested
    @DisplayName("areRolesCompatible()")
    inner class AreRolesCompatibleTests {

        @Test
        fun `Enfermero y Enfermera son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("Enfermero", "Enfermera"))
        }

        @Test
        fun `Enfermera y Enfermero son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("Enfermera", "Enfermero"))
        }

        @Test
        fun `Dos Enfermeros son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("Enfermero", "Enfermero"))
        }

        @Test
        fun `Auxiliar y TCAE son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("Auxiliar", "TCAE"))
        }

        @Test
        fun `TCAE y Auxiliar son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("TCAE", "Auxiliar"))
        }

        @Test
        fun `Dos Auxiliares son compatibles`() {
            assertTrue(ShiftRulesEngine.areRolesCompatible("Auxiliar", "Auxiliar"))
        }

        @Test
        fun `Enfermero y Auxiliar NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Enfermero", "Auxiliar"))
        }

        @Test
        fun `Auxiliar y Enfermera NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Auxiliar", "Enfermera"))
        }

        @Test
        fun `Supervisor y Enfermero NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Supervisor", "Enfermero"))
        }

        @Test
        fun `Enfermero y Supervisor NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Enfermero", "Supervisor"))
        }

        @Test
        fun `Supervisor y Auxiliar NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Supervisor", "Auxiliar"))
        }

        @Test
        fun `Dos Supervisores NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Supervisor", "Supervisora"))
        }

        @Test
        fun `Roles desconocidos NO son compatibles`() {
            assertFalse(ShiftRulesEngine.areRolesCompatible("Medico", "Celador"))
        }
    }

    // =========================================================================
    // 4. validateWorkRules() - Validación de reglas laborales
    // =========================================================================
    @Nested
    @DisplayName("validateWorkRules()")
    inner class ValidateWorkRulesTests {

        @Test
        fun `usuario ya tiene turno ese dia debe retornar error`() {
            val targetDate = LocalDate.of(2024, 1, 15)
            val schedule = mapOf(
                targetDate to "Mañana"
            )

            val result = ShiftRulesEngine.validateWorkRules(targetDate, "Tarde", schedule)

            assertNotNull(result)
            assertTrue(result!!.contains("Ya tienes un turno"))
        }

        @Test
        fun `dia despues de noche (saliente) debe retornar error`() {
            val nightDate = LocalDate.of(2024, 1, 14)
            val dayAfterNight = LocalDate.of(2024, 1, 15)
            val schedule = mapOf(
                nightDate to "Noche"
            )

            val result = ShiftRulesEngine.validateWorkRules(dayAfterNight, "Mañana", schedule)

            assertNotNull(result)
            assertTrue(result!!.contains("Saliente") || result.contains("noche"))
        }

        @Test
        fun `noche cuando manana ya tiene turno debe retornar error`() {
            val nightDate = LocalDate.of(2024, 1, 15)
            val nextDay = LocalDate.of(2024, 1, 16)
            val schedule = mapOf(
                nextDay to "Mañana"
            )

            val result = ShiftRulesEngine.validateWorkRules(nightDate, "Noche", schedule)

            assertNotNull(result)
            assertTrue(result!!.contains("mañana") || result.contains("librar"))
        }

        @Test
        fun `mas de 6 dias consecutivos debe retornar error`() {
            val targetDate = LocalDate.of(2024, 1, 17)
            // Crear 6 días consecutivos antes del target
            val schedule = mapOf(
                LocalDate.of(2024, 1, 11) to "Mañana",
                LocalDate.of(2024, 1, 12) to "Mañana",
                LocalDate.of(2024, 1, 13) to "Mañana",
                LocalDate.of(2024, 1, 14) to "Mañana",
                LocalDate.of(2024, 1, 15) to "Mañana",
                LocalDate.of(2024, 1, 16) to "Mañana"
            )

            val result = ShiftRulesEngine.validateWorkRules(targetDate, "Mañana", schedule)

            assertNotNull(result)
            assertTrue(result!!.contains("6 días"))
        }

        @Test
        fun `caso valido sin conflictos debe retornar null`() {
            val targetDate = LocalDate.of(2024, 1, 17)
            val schedule = mapOf(
                LocalDate.of(2024, 1, 15) to "Mañana"
                // Día 16 libre, día 17 es el target
            )

            val result = ShiftRulesEngine.validateWorkRules(targetDate, "Mañana", schedule)

            assertNull(result)
        }

        @Test
        fun `horario vacio es valido`() {
            val targetDate = LocalDate.of(2024, 1, 17)
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.validateWorkRules(targetDate, "Mañana", emptySchedule)

            assertNull(result)
        }

        @Test
        fun `exactamente 6 dias consecutivos es valido`() {
            val targetDate = LocalDate.of(2024, 1, 16)
            // 5 días antes del target = 6 total con el target
            val schedule = mapOf(
                LocalDate.of(2024, 1, 11) to "Mañana",
                LocalDate.of(2024, 1, 12) to "Mañana",
                LocalDate.of(2024, 1, 13) to "Mañana",
                LocalDate.of(2024, 1, 14) to "Mañana",
                LocalDate.of(2024, 1, 15) to "Mañana"
            )

            val result = ShiftRulesEngine.validateWorkRules(targetDate, "Mañana", schedule)

            assertNull(result)
        }
    }

    // =========================================================================
    // 5. checkMatch() - Verificación de match entre solicitudes
    // =========================================================================
    @Nested
    @DisplayName("checkMatch()")
    inner class CheckMatchTests {

        private fun createRequest(
            role: String,
            shiftDate: String,
            shiftName: String = "Mañana",
            offeredDates: List<String> = emptyList()
        ): ShiftChangeRequest {
            return ShiftChangeRequest(
                id = "test-${System.nanoTime()}",
                requesterRole = role,
                requesterShiftDate = shiftDate,
                requesterShiftName = shiftName,
                offeredDates = offeredDates
            )
        }

        @Test
        fun `roles incompatibles retorna false`() {
            val requesterRequest = createRequest("Enfermero", "2024-01-15")
            val candidateRequest = createRequest("Auxiliar", "2024-01-16")
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertFalse(result)
        }

        @Test
        fun `match valido con fechas correctas retorna true`() {
            val requesterRequest = createRequest("Enfermero", "2024-01-15")
            val candidateRequest = createRequest("Enfermera", "2024-01-16")
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertTrue(result)
        }

        @Test
        fun `match valido entre auxiliares retorna true`() {
            val requesterRequest = createRequest("Auxiliar", "2024-01-15")
            val candidateRequest = createRequest("TCAE", "2024-01-16")
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertTrue(result)
        }

        @Test
        fun `match falla cuando requester tiene conflicto laboral`() {
            val requesterRequest = createRequest("Enfermero", "2024-01-15")
            val candidateRequest = createRequest("Enfermera", "2024-01-16")

            // El requester ya tiene turno el día 16 (día del candidate)
            val requesterSchedule = mapOf(
                LocalDate.of(2024, 1, 16) to "Tarde"
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                requesterSchedule,
                emptySchedule
            )

            assertFalse(result)
        }

        @Test
        fun `match falla cuando candidate tiene conflicto laboral`() {
            val requesterRequest = createRequest("Enfermero", "2024-01-15")
            val candidateRequest = createRequest("Enfermera", "2024-01-16")

            // El candidate ya tiene turno el día 15 (día del requester)
            val candidateSchedule = mapOf(
                LocalDate.of(2024, 1, 15) to "Tarde"
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                candidateSchedule
            )

            assertFalse(result)
        }

        @Test
        fun `match con offeredDates especificas que coinciden retorna true`() {
            val requesterRequest = createRequest(
                role = "Enfermero",
                shiftDate = "2024-01-15",
                offeredDates = listOf("2024-01-16", "2024-01-17")
            )
            val candidateRequest = createRequest(
                role = "Enfermera",
                shiftDate = "2024-01-16",
                offeredDates = listOf("2024-01-15", "2024-01-18")
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertTrue(result)
        }

        @Test
        fun `match con offeredDates que no coinciden retorna false`() {
            val requesterRequest = createRequest(
                role = "Enfermero",
                shiftDate = "2024-01-15",
                offeredDates = listOf("2024-01-20", "2024-01-21") // No incluye 2024-01-16
            )
            val candidateRequest = createRequest(
                role = "Enfermera",
                shiftDate = "2024-01-16",
                offeredDates = listOf("2024-01-15")
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertFalse(result)
        }

        @Test
        fun `match con offeredDates vacias (comodin) es flexible`() {
            val requesterRequest = createRequest(
                role = "Enfermero",
                shiftDate = "2024-01-15",
                offeredDates = emptyList() // Acepta cualquier fecha
            )
            val candidateRequest = createRequest(
                role = "Enfermera",
                shiftDate = "2024-01-16",
                offeredDates = emptyList() // Acepta cualquier fecha
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                emptySchedule,
                emptySchedule
            )

            assertTrue(result)
        }

        @Test
        fun `match falla por regla de saliente despues de noche`() {
            val requesterRequest = createRequest(
                role = "Enfermero",
                shiftDate = "2024-01-15",
                shiftName = "Mañana"
            )
            val candidateRequest = createRequest(
                role = "Enfermera",
                shiftDate = "2024-01-16",
                shiftName = "Mañana"
            )

            // El requester trabajó noche el 15, así que el 16 es saliente
            val requesterSchedule = mapOf(
                LocalDate.of(2024, 1, 15) to "Noche"
            )
            val emptySchedule = emptyMap<LocalDate, String>()

            val result = ShiftRulesEngine.checkMatch(
                requesterRequest,
                candidateRequest,
                requesterSchedule,
                emptySchedule
            )

            assertFalse(result)
        }
    }
}
