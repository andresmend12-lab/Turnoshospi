package com.example.turnoshospi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.turnoshospi.UserProfile
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Entidad Room para cachear perfiles de usuario.
 * Permite funcionamiento offline.
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val gender: String,
    val email: String,
    val plantId: String?,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    // Metadata de sincronizacion
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false // true si hay cambios locales sin sincronizar
) {
    /**
     * Convierte la entidad Room al modelo de dominio.
     */
    fun toDomainModel(): UserProfile = UserProfile(
        firstName = firstName,
        lastName = lastName,
        role = role,
        gender = gender,
        email = email,
        plantId = plantId,
        createdAt = createdAtMillis?.let { Timestamp(Date(it)) },
        updatedAt = updatedAtMillis?.let { Timestamp(Date(it)) }
    )

    companion object {
        /**
         * Crea una entidad desde el modelo de dominio.
         */
        fun fromDomainModel(userId: String, profile: UserProfile): UserProfileEntity =
            UserProfileEntity(
                userId = userId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                role = profile.role,
                gender = profile.gender,
                email = profile.email,
                plantId = profile.plantId,
                createdAtMillis = profile.createdAt?.toDate()?.time,
                updatedAtMillis = profile.updatedAt?.toDate()?.time
            )
    }
}
