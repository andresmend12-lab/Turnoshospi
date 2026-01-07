package com.example.turnoshospi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.turnoshospi.Plant
import com.example.turnoshospi.RegisteredUser
import com.example.turnoshospi.ShiftTime

/**
 * Entidad Room para cachear plantas.
 * Los campos Map se serializan a JSON usando TypeConverters.
 */
@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val unitType: String,
    val hospitalName: String,
    val shiftDuration: String,
    val allowHalfDay: Boolean,
    val staffScope: String,
    val shiftTimesJson: String, // Map<String, ShiftTime> serializado
    val staffRequirementsJson: String, // Map<String, Int> serializado
    val createdAt: Long,
    val accessPassword: String,
    val personalDePlantaJson: String, // Map<String, RegisteredUser> serializado
    // Metadata de sincronizacion
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false
) {
    /**
     * Convierte la entidad Room al modelo de dominio.
     * Requiere TypeConverters para deserializar los JSON.
     */
    fun toDomainModel(
        shiftTimes: Map<String, ShiftTime>,
        staffRequirements: Map<String, Int>,
        personalDePlanta: Map<String, RegisteredUser>
    ): Plant = Plant(
        id = id,
        name = name,
        unitType = unitType,
        hospitalName = hospitalName,
        shiftDuration = shiftDuration,
        allowHalfDay = allowHalfDay,
        staffScope = staffScope,
        shiftTimes = shiftTimes,
        staffRequirements = staffRequirements,
        createdAt = createdAt,
        accessPassword = accessPassword,
        personal_de_planta = personalDePlanta
    )

    companion object {
        /**
         * Crea una entidad desde el modelo de dominio.
         * Los Maps se serializan externamente con TypeConverters.
         */
        fun fromDomainModel(
            plant: Plant,
            shiftTimesJson: String,
            staffRequirementsJson: String,
            personalDePlantaJson: String
        ): PlantEntity = PlantEntity(
            id = plant.id,
            name = plant.name,
            unitType = plant.unitType,
            hospitalName = plant.hospitalName,
            shiftDuration = plant.shiftDuration,
            allowHalfDay = plant.allowHalfDay,
            staffScope = plant.staffScope,
            shiftTimesJson = shiftTimesJson,
            staffRequirementsJson = staffRequirementsJson,
            createdAt = plant.createdAt,
            accessPassword = plant.accessPassword,
            personalDePlantaJson = personalDePlantaJson
        )
    }
}
