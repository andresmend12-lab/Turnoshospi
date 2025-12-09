package com.example.turnoshospi

import androidx.compose.ui.graphics.Color
import com.example.turnoshospi.ui.theme.ShiftColors
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

// Helper Class
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

// ============================================================================================
// HELPERS DE COLORES
// ============================================================================================

fun getShiftColorDynamic(shiftName: String, isSaliente: Boolean = false, colors: ShiftColors): Color {
    if (isSaliente) return colors.saliente
    if (shiftName.isBlank()) return colors.free
    val lower = shiftName.lowercase()
    return when {
        lower.contains("vacaciones") -> colors.holiday
        lower.contains("noche") -> colors.night
        lower.contains("media") && lower.contains("mañana") -> colors.morningHalf
        lower.contains("mañana") -> colors.morning
        lower.contains("media") && lower.contains("tarde") -> colors.afternoonHalf
        lower.contains("tarde") -> colors.afternoon
        lower.contains("día") || lower.contains("dia") -> colors.morning
        else -> colors.free
    }
}

fun getShiftColorExact(shiftName: String, isSaliente: Boolean = false): Color {
    return getShiftColorDynamic(shiftName, isSaliente, ShiftColors())
}

// ============================================================================================
// LÓGICA DE FIREBASE
// ============================================================================================

fun performDirectProposal(
    database: FirebaseDatabase,
    plantId: String,
    myReq: ShiftChangeRequest,
    targetShift: PlantShift,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    val updates = mapOf(
        "plants/$plantId/shift_requests/${myReq.id}/status" to RequestStatus.PENDING_PARTNER.name,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserId" to targetShift.userId,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserName" to targetShift.userName,
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftDate" to targetShift.date.toString(),
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftName" to targetShift.shiftName
    )
    database.reference.updateChildren(updates).addOnSuccessListener {
        onSaveNotification(
            targetShift.userId,
            "SHIFT_PROPOSAL",
            "${myReq.requesterName} te ha solicitado un cambio de turno: tu ${targetShift.shiftName} del ${targetShift.date} por su ${myReq.requesterShiftName} del ${myReq.requesterShiftDate}.",
            "ShiftChangeScreen",
            myReq.id,
            {}
        )
    }
}

fun deleteShiftRequest(database: FirebaseDatabase, plantId: String, requestId: String) {
    database.getReference("plants/$plantId/shift_requests/$requestId").removeValue()
}

fun updateRequestStatus(
    database: FirebaseDatabase,
    plantId: String,
    requestId: String,
    newStatus: RequestStatus
) {
    if (requestId.isBlank()) return
    database.reference.child("plants/$plantId/shift_requests/$requestId/status").setValue(newStatus)
}

fun approveSwapBySupervisor(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (req.type == RequestType.COVERAGE) {
        approveCoverage(database, plantId, req, onSuccess, onError)
        return
    }
    if (req.targetUserId == null || req.targetShiftDate == null || req.targetShiftName == null) {
        onError("Datos de intercambio incompletos.")
        return
    }

    val requesterDayRef = database.reference.child("plants/$plantId/turnos/turnos-${req.requesterShiftDate}")
    val targetDayRef = database.reference.child("plants/$plantId/turnos/turnos-${req.targetShiftDate}")

    requesterDayRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapA: DataSnapshot) {
            targetDayRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapB: DataSnapshot) {
                    data class SlotInfo(val path: String, val isHalfDay: Boolean, val fullSlotKey: String, val group: String)

                    fun findSlotInfo(snapshot: DataSnapshot, shiftName: String, userName: String): SlotInfo? {
                        val realShiftName = shiftName.replace("Media ", "").trim()
                        val shiftRef = snapshot.child(realShiftName)
                        val groups = listOf("nurses", "auxiliaries")
                        for (group in groups) {
                            shiftRef.child(group).children.forEach { slot ->
                                val p = slot.child("primary").value.toString()
                                val s = slot.child("secondary").value.toString()
                                val half = slot.child("halfDay").value as? Boolean == true
                                if (p.equals(userName, true)) return SlotInfo("$realShiftName/$group/${slot.key}/primary", half, slot.key!!, group)
                                if (s.equals(userName, true)) return SlotInfo("$realShiftName/$group/${slot.key}/secondary", half, slot.key!!, group)
                            }
                        }
                        return null
                    }

                    val infoA = findSlotInfo(snapA, req.requesterShiftName, req.requesterName)
                    val infoB = findSlotInfo(snapB, req.targetShiftName!!, req.targetUserName!!)

                    if (infoA != null && infoB != null) {
                        val updates = mutableMapOf<String, Any?>()
                        // Lógica de intercambio (Swap logic omitida por brevedad, copiar del original)
                        if (infoA.isHalfDay && !infoB.isHalfDay) {
                            val partsA = infoA.path.split("/")
                            val basePathA = "plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${partsA[0]}/${partsA[1]}/${partsA[2]}"
                            updates["$basePathA/primary"] = req.targetUserName
                            updates["$basePathA/secondary"] = ""
                            updates["$basePathA/halfDay"] = false
                            updates["plants/$plantId/turnos/turnos-${req.targetShiftDate}/${infoB.path}"] = req.requesterName
                        } else if (!infoA.isHalfDay && infoB.isHalfDay) {
                            val partsB = infoB.path.split("/")
                            val basePathB = "plants/$plantId/turnos/turnos-${req.targetShiftDate}/${partsB[0]}/${partsB[1]}/${partsB[2]}"
                            updates["$basePathB/primary"] = req.requesterName
                            updates["$basePathB/secondary"] = ""
                            updates["$basePathB/halfDay"] = false
                            updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${infoA.path}"] = req.targetUserName
                        } else {
                            updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${infoA.path}"] = req.targetUserName
                            updates["plants/$plantId/turnos/turnos-${req.targetShiftDate}/${infoB.path}"] = req.requesterName
                        }

                        updates["plants/$plantId/shift_requests/${req.id}/status"] = RequestStatus.APPROVED
                        database.reference.updateChildren(updates).addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it.message ?: "Error al guardar") }
                    } else {
                        onError("No se encontraron los turnos originales.")
                    }
                }
                override fun onCancelled(e: DatabaseError) { onError(e.message) }
            })
        }
        override fun onCancelled(e: DatabaseError) { onError(e.message) }
    })
}

private fun approveCoverage(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val userPlantRef = database.reference.child("plants/$plantId/userPlants/${req.requesterId}")
    userPlantRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(upSnap: DataSnapshot) {
            val staffId = upSnap.child("staffId").value as? String
            if (staffId != null) {
                database.reference.child("plants/$plantId/personal_de_planta/$staffId/name")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(nameSnap: DataSnapshot) {
                            val rosterName = nameSnap.value as? String ?: req.requesterName
                            performCoverageUpdate(database, plantId, req, rosterName, onSuccess, onError)
                        }
                        override fun onCancelled(e: DatabaseError) { performCoverageUpdate(database, plantId, req, req.requesterName, onSuccess, onError) }
                    })
            } else {
                performCoverageUpdate(database, plantId, req, req.requesterName, onSuccess, onError)
            }
        }
        override fun onCancelled(e: DatabaseError) { performCoverageUpdate(database, plantId, req, req.requesterName, onSuccess, onError) }
    })
}

private fun performCoverageUpdate(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    nameToSearch: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val turnosRef = database.reference.child("plants/$plantId/turnos/turnos-${req.requesterShiftDate}")
    turnosRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) { onError("Día no encontrado."); return }

            val realShiftName = req.requesterShiftName.replace("Media ", "", ignoreCase = true).trim()
            val shiftSnapshot = snapshot.children.find { it.key?.equals(realShiftName, ignoreCase = true) == true }

            if (shiftSnapshot == null) { onError("Turno no encontrado."); return }

            var pathFound: String? = null
            var slotKey: String? = null
            var fieldToUpdate: String? = null
            val targetName = nameToSearch.trim()

            fun findInNode(nodeName: String) {
                shiftSnapshot.child(nodeName).children.forEach { slot ->
                    val pName = slot.child("primary").value?.toString()?.trim() ?: ""
                    val sName = slot.child("secondary").value?.toString()?.trim() ?: ""
                    if (pName.equals(targetName, ignoreCase = true)) {
                        pathFound = nodeName; slotKey = slot.key; fieldToUpdate = "primary"
                    } else if (sName.equals(targetName, ignoreCase = true)) {
                        pathFound = nodeName; slotKey = slot.key; fieldToUpdate = "secondary"
                    }
                }
            }
            findInNode("nurses")
            if (pathFound == null) findInNode("auxiliaries")

            if (pathFound != null && slotKey != null && fieldToUpdate != null) {
                val transactionId = UUID.randomUUID().toString()
                val transaction = FavorTransaction(
                    id = transactionId,
                    covererId = req.targetUserId ?: "",
                    covererName = req.targetUserName ?: "",
                    requesterId = req.requesterId,
                    requesterName = req.requesterName,
                    date = req.requesterShiftDate,
                    shiftName = req.requesterShiftName,
                    timestamp = System.currentTimeMillis()
                )

                val updates = mutableMapOf<String, Any?>()
                val finalShiftKey = shiftSnapshot.key!!
                updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/$finalShiftKey/$pathFound/$slotKey/$fieldToUpdate"] = req.targetUserName
                updates["plants/$plantId/shift_requests/${req.id}/status"] = RequestStatus.APPROVED
                updates["plants/$plantId/transactions/$transactionId"] = transaction

                database.reference.updateChildren(updates).addOnSuccessListener { onSuccess() }
            } else {
                onError("Usuario '$targetName' no encontrado en el turno.")
            }
        }
        override fun onCancelled(e: DatabaseError) { onError(e.message) }
    })
}