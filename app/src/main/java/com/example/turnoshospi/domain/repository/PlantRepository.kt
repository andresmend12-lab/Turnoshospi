package com.example.turnoshospi.domain.repository

import com.example.turnoshospi.Plant
import com.example.turnoshospi.PlantMembership
import com.example.turnoshospi.RegisteredUser
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con plantas/unidades hospitalarias.
 * Abstrae la fuente de datos (Firebase Realtime Database).
 */
interface PlantRepository {

    // --- Operaciones de Lectura ---

    /**
     * Obtiene el ID de la planta asociada al usuario.
     * @param userId ID del usuario
     * @return Result con el plantId o null si no tiene planta asignada
     */
    suspend fun getUserPlantId(userId: String): Result<String?>

    /**
     * Obtiene una planta por su ID (lectura unica).
     * @param plantId ID de la planta
     * @return Result con la planta o error
     */
    suspend fun getPlant(plantId: String): Result<Plant?>

    /**
     * Observa cambios en una planta en tiempo real.
     * @param plantId ID de la planta
     * @return Flow que emite la planta actualizada o null
     */
    fun observePlant(plantId: String): Flow<Plant?>

    /**
     * Obtiene la membresia de un usuario en una planta.
     * @param plantId ID de la planta
     * @param userId ID del usuario
     * @return Result con la membresia o null si no existe
     */
    suspend fun getPlantMembership(plantId: String, userId: String): Result<PlantMembership?>

    // --- Operaciones de Union a Planta ---

    /**
     * Une a un usuario a una planta usando codigo de invitacion.
     * Verifica el codigo antes de unir.
     * @param plantId ID de la planta
     * @param invitationCode Codigo de acceso
     * @param userId ID del usuario que se une
     * @return Result.success(Unit) si fue exitoso, Result.failure con error descriptivo
     */
    suspend fun joinPlant(plantId: String, invitationCode: String, userId: String): Result<Unit>

    /**
     * Vincula un usuario autenticado a un miembro del personal de la planta.
     * Actualiza tanto la planta como el perfil del usuario.
     * @param plantId ID de la planta
     * @param userId ID del usuario
     * @param staff Miembro del personal a vincular
     * @return Result indicando exito o fallo
     */
    suspend fun linkUserToStaff(plantId: String, userId: String, staff: RegisteredUser): Result<Unit>

    // --- Operaciones de Personal ---

    /**
     * Registra un nuevo miembro del personal en la planta.
     * @param plantId ID de la planta
     * @param staff Datos del nuevo miembro
     * @return Result indicando exito o fallo
     */
    suspend fun registerStaff(plantId: String, staff: RegisteredUser): Result<Unit>

    /**
     * Actualiza los datos de un miembro del personal.
     * @param plantId ID de la planta
     * @param staff Datos actualizados del miembro
     * @return Result indicando exito o fallo
     */
    suspend fun updateStaff(plantId: String, staff: RegisteredUser): Result<Unit>

    /**
     * Elimina un miembro del personal de la planta.
     * @param plantId ID de la planta
     * @param staffId ID del miembro a eliminar
     * @return Result indicando exito o fallo
     */
    suspend fun deleteStaff(plantId: String, staffId: String): Result<Unit>

    // --- Operaciones de Gestion de Planta ---

    /**
     * Elimina una planta completamente.
     * ADVERTENCIA: Esta operacion es irreversible.
     * @param plantId ID de la planta a eliminar
     * @return Result indicando exito o fallo
     */
    suspend fun deletePlant(plantId: String): Result<Unit>
}

/**
 * Errores especificos de operaciones con plantas.
 */
sealed class PlantError(message: String) : Exception(message) {
    data object PlantNotFound : PlantError("La planta no existe")
    data object InvalidInvitationCode : PlantError("Codigo de invitacion invalido")
    data object UserNotAuthenticated : PlantError("Usuario no autenticado")
    data object InvalidPlantId : PlantError("ID de planta invalido")
    data object InvalidStaffId : PlantError("ID de personal invalido")
    data class DatabaseError(val originalMessage: String?) : PlantError(originalMessage ?: "Error de base de datos")
}
