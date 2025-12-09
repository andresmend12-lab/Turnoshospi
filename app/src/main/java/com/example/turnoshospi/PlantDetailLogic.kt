package com.example.turnoshospi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.UUID

// --- MODELOS DE DATOS ---

class SlotAssignment(
    primaryName: String = "",
    secondaryName: String = "",
    hasHalfDay: Boolean = false
) {
    var primaryName by mutableStateOf(primaryName)
    var secondaryName by mutableStateOf(secondaryName)
    var hasHalfDay by mutableStateOf(hasHalfDay)
}

data class ShiftAssignmentState(
    val nurseSlots: MutableList<SlotAssignment>,
    val auxSlots: MutableList<SlotAssignment>
)

// --- HELPERS DE ORDEN Y LOCALIZACIÓN ---

fun getShiftPriority(shiftName: String): Int {
    val lower = shiftName.lowercase().trim()
    return when {
        lower.contains("mañana") || lower.contains("morning") || lower.contains("día") || lower.contains("day") -> 1
        lower.contains("tarde") || lower.contains("afternoon") -> 2
        lower.contains("noche") || lower.contains("night") -> 3
        else -> 99
    }
}

@Composable
fun getLocalizedShiftName(rawName: String): String {
    val lower = rawName.lowercase().trim()
    return when {
        lower == "mañana" || lower == "morning" -> stringResource(R.string.shift_morning)
        lower == "tarde" || lower == "afternoon" -> stringResource(R.string.shift_afternoon)
        lower == "noche" || lower == "night" -> stringResource(R.string.shift_night)
        lower == "día" || lower == "dia" || lower == "day" -> stringResource(R.string.shift_day)
        else -> rawName
    }
}

fun sharePlantInvitation(context: Context, plantName: String, plantId: String, accessCode: String) {
    val message = context.getString(R.string.share_plant_message_template, plantName, plantId, accessCode)
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val chooserTitle = context.getString(R.string.share_chooser_title)
    val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
    context.startActivity(shareIntent)
}

// --- EXTENSIONES ---

fun SlotAssignment.toFirebaseMap(unassigned: String, base: String) = mapOf(
    "halfDay" to hasHalfDay,
    "primary" to primaryName.ifBlank { unassigned },
    "secondary" to if (hasHalfDay) secondaryName.ifBlank { unassigned } else "",
    "primaryLabel" to base,
    "secondaryLabel" to if (hasHalfDay) "$base media jornada" else ""
)

fun DataSnapshot.toSlotAssignment(unassignedLabel: String): SlotAssignment {
    val h = child("halfDay").value as? Boolean ?: false
    val p = (child("primary").value as? String).orEmpty()
    val s = (child("secondary").value as? String).orEmpty()

    fun isUnassigned(value: String): Boolean {
        return value.equals(unassignedLabel, ignoreCase = true) ||
                value.equals("Sin asignar", ignoreCase = true) ||
                value.equals("Unassigned", ignoreCase = true) ||
                value.equals("null", ignoreCase = true)
    }

    return SlotAssignment(
        primaryName = if (isUnassigned(p)) "" else p,
        secondaryName = if (isUnassigned(s)) "" else s,
        hasHalfDay = h
    )
}

// --- LÓGICA DE PERSISTENCIA (Firebase) ---

fun saveShiftAssignments(
    context: Context,
    database: FirebaseDatabase,
    plantId: String,
    dateKey: String,
    assignments: Map<String, ShiftAssignmentState>,
    unassignedLabel: String,
    plantStaff: Collection<RegisteredUser>,
    staffIdToUserIdMap: Map<String, String>,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    val turnosRef = database.reference.child("plants/$plantId/turnos/turnos-$dateKey")

    turnosRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val oldNames = mutableSetOf<String>()
            if (snapshot.exists()) {
                snapshot.children.forEach { shiftSnapshot ->
                    shiftSnapshot.child("nurses").children.forEach { slot ->
                        val p = slot.child("primary").value as? String
                        val s = slot.child("secondary").value as? String
                        if (!p.isNullOrBlank() && p != unassignedLabel) oldNames.add(p)
                        if (!s.isNullOrBlank() && s != unassignedLabel) oldNames.add(s)
                    }
                    shiftSnapshot.child("auxiliaries").children.forEach { slot ->
                        val p = slot.child("primary").value as? String
                        val s = slot.child("secondary").value as? String
                        if (!p.isNullOrBlank() && p != unassignedLabel) oldNames.add(p)
                        if (!s.isNullOrBlank() && s != unassignedLabel) oldNames.add(s)
                    }
                }
            }

            val newNames = mutableSetOf<String>()
            val payload = assignments.mapValues { (_, state) ->
                val nurseList = state.nurseSlots.mapIndexed { i, s ->
                    val map = s.toFirebaseMap(unassignedLabel, "enfermero${i + 1}")
                    if (s.primaryName.isNotBlank() && s.primaryName != unassignedLabel) newNames.add(s.primaryName)
                    if (s.hasHalfDay && s.secondaryName.isNotBlank() && s.secondaryName != unassignedLabel) newNames.add(s.secondaryName)
                    map
                }
                val auxList = state.auxSlots.mapIndexed { i, s ->
                    val map = s.toFirebaseMap(unassignedLabel, "auxiliar${i + 1}")
                    if (s.primaryName.isNotBlank() && s.primaryName != unassignedLabel) newNames.add(s.primaryName)
                    if (s.hasHalfDay && s.secondaryName.isNotBlank() && s.secondaryName != unassignedLabel) newNames.add(s.secondaryName)
                    map
                }
                mapOf("nurses" to nurseList, "auxiliaries" to auxList)
            }

            turnosRef.setValue(payload).addOnSuccessListener {
                onComplete(true)
                val staffIdMap = plantStaff.associateBy { it.name }

                val unassignedStaff = oldNames - newNames
                unassignedStaff.forEach { name ->
                    val staffId = staffIdMap[name]?.id
                    val userId = staffId?.let { staffIdToUserIdMap[it] }
                    if (userId != null) {
                        val msg = context.getString(R.string.notif_unassigned_message, dateKey)
                        onSaveNotification(userId, "SHIFT_UNASSIGNED", msg, AppScreen.MainMenu.name, dateKey, {})
                    }
                }

                newNames.forEach { name ->
                    val staffId = staffIdMap[name]?.id
                    val userId = staffId?.let { staffIdToUserIdMap[it] }
                    if (userId != null) {
                        val msg = context.getString(R.string.notif_assigned_message, dateKey)
                        onSaveNotification(userId, "SHIFT_ASSIGNED_STAFF", msg, AppScreen.MainMenu.name, dateKey, {})
                    }
                }

            }.addOnFailureListener { onComplete(false) }
        }
        override fun onCancelled(error: DatabaseError) { onComplete(false) }
    })
}

fun saveVacationDaysToFirebase(
    context: Context,
    plantId: String,
    staffId: String,
    staffName: String,
    dates: List<String>,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    if (dates.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.error_no_date_selected), Toast.LENGTH_SHORT).show()
        return
    }

    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val updates = mutableMapOf<String, Any?>()

    val vacationAssignment = mapOf(
        "nurses" to listOf(mapOf(
            "halfDay" to false,
            "primary" to staffName,
            "secondary" to "",
            "primaryLabel" to "Vacaciones",
            "secondaryLabel" to ""
        )),
        "auxiliaries" to emptyList<Any>()
    )

    dates.forEach { dateKey ->
        updates["plants/$plantId/turnos/turnos-$dateKey/Vacaciones"] = vacationAssignment
    }

    database.reference.updateChildren(updates)
        .addOnSuccessListener {
            Toast.makeText(context, context.getString(R.string.msg_vacation_saved), Toast.LENGTH_LONG).show()
            dates.forEach { dateKey ->
                val msg = context.getString(R.string.notif_vacation_saved_message, dateKey)
                onSaveNotification(
                    FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    "VACATION_SAVED",
                    msg,
                    AppScreen.MainMenu.name,
                    dateKey,
                    {}
                )
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_generic) + ": ${it.message}", Toast.LENGTH_LONG).show()
        }
}

// --- LÓGICA DE CSV ---

fun processCsvImport(
    context: Context,
    uri: Uri,
    plant: Plant,
    onResult: (Boolean, String) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        reader.close()

        if (lines.isEmpty()) {
            onResult(false, context.getString(R.string.error_csv_empty))
            return
        }

        val header = lines[0].split(",").map { it.trim() }
        val dateMap = mutableMapOf<Int, String>()

        for (i in 1 until header.size) {
            val dateStr = header[i]
            if (dateStr.isNotBlank()) {
                try {
                    LocalDate.parse(dateStr)
                    dateMap[i] = dateStr
                } catch (e: Exception) { }
            }
        }

        if (dateMap.isEmpty()) {
            onResult(false, "No se encontraron fechas válidas en la cabecera (formato YYYY-MM-DD).")
            return
        }

        val assignmentsBuffer = mutableMapOf<String, MutableMap<String, Pair<MutableList<String>, MutableList<String>>>>()
        val plantStaff = plant.personal_de_planta.values

        for ((index, line) in lines.drop(1).withIndex()) {
            if (line.isBlank()) continue
            val cols = line.split(",").map { it.trim() }
            if (cols.isEmpty()) continue

            val staffName = cols[0]
            if (staffName.isBlank()) continue

            val staffMember = plantStaff.find { it.name.equals(staffName, ignoreCase = true) }
            if (staffMember == null) continue

            val nurseRoles = listOf("enfermero", "enfermera", "nurse", "supervisor", "supervisora")
            val isNurse = staffMember.role.trim().lowercase().let { r -> nurseRoles.any { r.contains(it) } }

            for ((colIndex, dateKey) in dateMap) {
                if (colIndex >= cols.size) break
                val shiftValue = cols[colIndex]
                if (shiftValue.isNotBlank()) {
                    val matchedShiftKey = plant.shiftTimes.keys.find { it.equals(shiftValue, ignoreCase = true) }

                    if (matchedShiftKey != null) {
                        val dateMapAssignments = assignmentsBuffer.getOrPut(dateKey) { mutableMapOf() }
                        val shiftLists = dateMapAssignments.getOrPut(matchedShiftKey) {
                            Pair(mutableListOf(), mutableListOf())
                        }
                        if (isNurse) shiftLists.first.add(staffMember.name)
                        else shiftLists.second.add(staffMember.name)
                    }
                }
            }
        }

        val updates = mutableMapOf<String, Any>()
        assignmentsBuffer.forEach { (dateKey, shiftsMap) ->
            val datePayload = shiftsMap.mapValues { (_, lists) ->
                val (nursesNames, auxNames) = lists
                mapOf(
                    "nurses" to nursesNames.mapIndexed { i, name ->
                        mapOf(
                            "halfDay" to false, "primary" to name, "secondary" to "",
                            "primaryLabel" to "enfermero${i + 1}", "secondaryLabel" to ""
                        )
                    },
                    "auxiliaries" to auxNames.mapIndexed { i, name ->
                        mapOf(
                            "halfDay" to false, "primary" to name, "secondary" to "",
                            "primaryLabel" to "auxiliar${i + 1}", "secondaryLabel" to ""
                        )
                    }
                )
            }
            updates["plants/${plant.id}/turnos/turnos-$dateKey"] = datePayload
        }

        if (updates.isEmpty()) {
            onResult(true, "El archivo se leyó pero no contenía asignaciones válidas.")
            return
        }

        val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
        database.reference.updateChildren(updates)
            .addOnSuccessListener { onResult(true, context.getString(R.string.msg_import_success)) }
            .addOnFailureListener { onResult(false, context.getString(R.string.error_db_save, it.message)) }

    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false, context.getString(R.string.error_file_read, e.message))
    }
}

fun createAndDownloadMatrixTemplate(context: Context) {
    val fileName = "plantilla_turnos_matriz.csv"
    val sb = StringBuilder()
    sb.append("Nombre Personal")
    val today = LocalDate.now()
    for (i in 0 until 31) sb.append(",").append(today.plusDays(i.toLong()).toString())
    sb.append("\nEjemplo Usuario")
    val examplePattern = listOf("Mañana", "Mañana", "Tarde", "Tarde", "Noche", "Libre")
    for (i in 0 until 31) sb.append(",").append(examplePattern[i % examplePattern.size])
    sb.append("\n")

    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val outFile = File(downloadsDir, fileName)
        FileOutputStream(outFile).use { it.write(sb.toString().toByteArray()) }
        Toast.makeText(context, "Plantilla guardada: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}