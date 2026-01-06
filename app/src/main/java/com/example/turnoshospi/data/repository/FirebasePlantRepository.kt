package com.example.turnoshospi.data.repository

import com.example.turnoshospi.Plant
import com.example.turnoshospi.PlantMembership
import com.example.turnoshospi.RegisteredUser
import com.example.turnoshospi.ShiftTime
import com.example.turnoshospi.domain.repository.PlantError
import com.example.turnoshospi.domain.repository.PlantRepository
import com.example.turnoshospi.util.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementacion de PlantRepository usando Firebase Realtime Database.
 */
class FirebasePlantRepository(
    private val database: FirebaseDatabase = FirebaseConfig.getDatabaseInstance()
) : PlantRepository {

    // --- Operaciones de Lectura ---

    override suspend fun getUserPlantId(userId: String): Result<String?> {
        if (userId.isBlank()) {
            return Result.failure(PlantError.UserNotAuthenticated)
        }

        return suspendCancellableCoroutine { continuation ->
            database.getReference("users")
                .child(userId)
                .child("plantId")
                .get()
                .addOnSuccessListener { snapshot ->
                    val plantId = snapshot.getValue(String::class.java)
                    continuation.resume(Result.success(plantId))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    override suspend fun getPlant(plantId: String): Result<Plant?> {
        if (plantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }

        return suspendCancellableCoroutine { continuation ->
            database.getReference("plants")
                .child(plantId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val plant = snapshot.toPlant()
                    continuation.resume(Result.success(plant))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    override fun observePlant(plantId: String): Flow<Plant?> = callbackFlow {
        if (plantId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val reference = database.getReference("plants").child(plantId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plant = snapshot.toPlant()
                trySend(plant)
            }

            override fun onCancelled(error: DatabaseError) {
                // En caso de error, emitimos null
                trySend(null)
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    override suspend fun getPlantMembership(plantId: String, userId: String): Result<PlantMembership?> {
        if (plantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }
        if (userId.isBlank()) {
            return Result.failure(PlantError.UserNotAuthenticated)
        }

        return suspendCancellableCoroutine { continuation ->
            database.reference
                .child("plants")
                .child(plantId)
                .child("userPlants")
                .child(userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        continuation.resume(Result.success(null))
                        return@addOnSuccessListener
                    }

                    val membership = if (snapshot.hasChildren()) {
                        PlantMembership(
                            plantId = snapshot.child("plantId").getValue(String::class.java) ?: plantId,
                            userId = userId,
                            staffId = snapshot.child("staffId").getValue(String::class.java),
                            staffName = snapshot.child("staffName").getValue(String::class.java),
                            staffRole = snapshot.child("staffRole").getValue(String::class.java)
                        )
                    } else {
                        PlantMembership(
                            plantId = plantId,
                            userId = userId,
                            staffId = null,
                            staffName = null,
                            staffRole = null
                        )
                    }
                    continuation.resume(Result.success(membership))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    // --- Operaciones de Union a Planta ---

    override suspend fun joinPlant(plantId: String, invitationCode: String, userId: String): Result<Unit> {
        val cleanPlantId = plantId.trim()
        val cleanCode = invitationCode.trim()

        if (cleanPlantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }
        if (userId.isBlank()) {
            return Result.failure(PlantError.UserNotAuthenticated)
        }

        return suspendCancellableCoroutine { continuation ->
            database.getReference("plants")
                .child(cleanPlantId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        continuation.resume(Result.failure(PlantError.PlantNotFound))
                        return@addOnSuccessListener
                    }

                    val storedCode = snapshot.child("accessPassword").getValue(String::class.java)
                    if (storedCode.isNullOrEmpty() || storedCode != cleanCode) {
                        continuation.resume(Result.failure(PlantError.InvalidInvitationCode))
                        return@addOnSuccessListener
                    }

                    // Codigo valido, proceder a unir
                    val updates = mapOf(
                        "plants/$cleanPlantId/userPlants/$userId/plantId" to cleanPlantId,
                        "users/$userId/plantId" to cleanPlantId
                    )

                    database.reference
                        .updateChildren(updates)
                        .addOnSuccessListener {
                            continuation.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                        }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    override suspend fun linkUserToStaff(plantId: String, userId: String, staff: RegisteredUser): Result<Unit> {
        if (plantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }
        if (userId.isBlank()) {
            return Result.failure(PlantError.UserNotAuthenticated)
        }
        if (staff.id.isBlank()) {
            return Result.failure(PlantError.InvalidStaffId)
        }

        return suspendCancellableCoroutine { continuation ->
            val updates = mapOf(
                "plants/$plantId/userPlants/$userId/plantId" to plantId,
                "plants/$plantId/userPlants/$userId/staffId" to staff.id,
                "plants/$plantId/userPlants/$userId/staffName" to staff.name,
                "plants/$plantId/userPlants/$userId/staffRole" to staff.role,
                "users/$userId/plantId" to plantId,
                "users/$userId/plantStaffId" to staff.id,
                "users/$userId/role" to staff.role,
                "users/$userId/linkedStaffName" to staff.name
            )

            database.reference
                .updateChildren(updates)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    // --- Operaciones de Personal ---

    override suspend fun registerStaff(plantId: String, staff: RegisteredUser): Result<Unit> {
        val cleanPlantId = plantId.trim()

        if (cleanPlantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }
        if (staff.id.isBlank()) {
            return Result.failure(PlantError.InvalidStaffId)
        }

        return suspendCancellableCoroutine { continuation ->
            database.reference
                .child("plants")
                .child(cleanPlantId)
                .child("personal_de_planta")
                .child(staff.id)
                .setValue(staff)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    override suspend fun updateStaff(plantId: String, staff: RegisteredUser): Result<Unit> {
        // La implementacion es identica a registerStaff (setValue sobrescribe)
        return registerStaff(plantId, staff)
    }

    override suspend fun deleteStaff(plantId: String, staffId: String): Result<Unit> {
        if (plantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }
        if (staffId.isBlank()) {
            return Result.failure(PlantError.InvalidStaffId)
        }

        return suspendCancellableCoroutine { continuation ->
            database.reference
                .child("plants")
                .child(plantId)
                .child("personal_de_planta")
                .child(staffId)
                .removeValue()
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    // --- Operaciones de Gestion de Planta ---

    override suspend fun deletePlant(plantId: String): Result<Unit> {
        if (plantId.isBlank()) {
            return Result.failure(PlantError.InvalidPlantId)
        }

        return suspendCancellableCoroutine { continuation ->
            database.getReference("plants")
                .child(plantId)
                .removeValue()
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(PlantError.DatabaseError(exception.message)))
                }
        }
    }

    // --- Extension para convertir DataSnapshot a Plant ---

    private fun DataSnapshot.toPlant(): Plant? {
        if (!exists()) return null

        val shiftTimes = child("shiftTimes").children.mapNotNull { shiftSnapshot ->
            val label = shiftSnapshot.key ?: return@mapNotNull null
            val start = shiftSnapshot.child("start").getValue(String::class.java) ?: ""
            val end = shiftSnapshot.child("end").getValue(String::class.java) ?: ""
            label to ShiftTime(start, end)
        }.toMap()

        val staffRequirements = child("staffRequirements").children.mapNotNull { requirementSnapshot ->
            val label = requirementSnapshot.key ?: return@mapNotNull null
            val value = requirementSnapshot.getValue(Int::class.java)
                ?: requirementSnapshot.getValue(Long::class.java)?.toInt()
                ?: 0
            label to value
        }.toMap()

        val personalDePlanta = child("personal_de_planta").children.mapNotNull { userSnapshot ->
            val oderId = userSnapshot.key ?: return@mapNotNull null
            val registeredUser = userSnapshot.getValue(RegisteredUser::class.java)
                ?: RegisteredUser(
                    id = oderId,
                    name = userSnapshot.child("name").getValue(String::class.java).orEmpty(),
                    role = userSnapshot.child("role").getValue(String::class.java).orEmpty(),
                    email = userSnapshot.child("email").getValue(String::class.java).orEmpty(),
                    profileType = userSnapshot.child("profileType").getValue(String::class.java).orEmpty()
                )
            oderId to registeredUser
        }.toMap()

        return Plant(
            id = child("id").getValue(String::class.java) ?: key.orEmpty(),
            name = child("name").getValue(String::class.java) ?: "",
            unitType = child("unitType").getValue(String::class.java) ?: "",
            hospitalName = child("hospitalName").getValue(String::class.java) ?: "",
            shiftDuration = child("shiftDuration").getValue(String::class.java) ?: "",
            allowHalfDay = child("allowHalfDay").getValue(Boolean::class.java) ?: false,
            staffScope = child("staffScope").getValue(String::class.java) ?: "",
            shiftTimes = shiftTimes,
            staffRequirements = staffRequirements,
            createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
            accessPassword = child("accessPassword").getValue(String::class.java) ?: "",
            personal_de_planta = personalDePlanta
        )
    }
}
