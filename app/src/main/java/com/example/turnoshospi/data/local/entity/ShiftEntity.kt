package com.example.turnoshospi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.turnoshospi.domain.model.Shift

/**
 * Entidad Room para cachear turnos.
 * Indexada por plantId para consultas rapidas.
 */
@Entity(
    tableName = "shifts",
    indices = [
        Index(value = ["plantId"]),
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class ShiftEntity(
    @PrimaryKey
    val id: String,
    val plantId: String,
    val userId: String,
    val date: String, // Formato YYYY-MM-DD
    val type: String, // "M", "T", "N", etc.
    val isCovered: Boolean,
    // Metadata de sincronizacion
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false
) {
    /**
     * Convierte la entidad Room al modelo de dominio.
     */
    fun toDomainModel(): Shift = Shift(
        id = id,
        plantId = plantId,
        userId = userId,
        date = date,
        type = type,
        isCovered = isCovered
    )

    companion object {
        /**
         * Crea una entidad desde el modelo de dominio.
         */
        fun fromDomainModel(shift: Shift): ShiftEntity = ShiftEntity(
            id = shift.id,
            plantId = shift.plantId,
            userId = shift.userId,
            date = shift.date,
            type = shift.type,
            isCovered = shift.isCovered
        )
    }
}
