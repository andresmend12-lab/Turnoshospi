package com.example.turnoshospi

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

// Clases públicas para evitar errores de visibilidad
data class SlotAssignment(
    var primaryName: String = "",
    var secondaryName: String = "",
    var hasHalfDay: Boolean = false
)

data class ShiftAssignmentState(
    val nurseSlots: MutableList<SlotAssignment>,
    val auxSlots: MutableList<SlotAssignment>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plant: Plant?,
    datePickerState: DatePickerState,
    currentUserProfile: UserProfile?,
    currentMembership: PlantMembership?,
    onBack: () -> Unit,
    onAddStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onEditStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onOpenPlantSettings: () -> Unit,
    onOpenImportShifts: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenShiftChange: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val database = remember { FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/") }
    val selectedDate = datePickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }

    val nurseRole = stringResource(R.string.role_nurse_generic)
    val resolvedRole = currentMembership?.staffRole?.ifBlank { currentUserProfile?.role } ?: currentUserProfile?.role
    val isSupervisor = resolvedRole == stringResource(R.string.role_supervisor_male) || resolvedRole == stringResource(R.string.role_supervisor_female)

    val assignmentsByDate = remember(plant?.id) { mutableStateMapOf<String, MutableMap<String, ShiftAssignmentState>>() }
    val savedAssignmentsByDate = remember(plant?.id) { mutableStateMapOf<String, Boolean>() }
    val plantStaff = plant?.personal_de_planta?.values.orEmpty()
    val nurseOptions = remember(plantStaff) { plantStaff.filter { it.role.contains("enfermer", true) }.map { it.name }.sorted() }
    val auxOptions = remember(plantStaff) { plantStaff.filter { it.role.contains("auxiliar", true) }.map { it.name }.sorted() }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var isSavingStaff by remember { mutableStateOf(false) }
    var showStaffListDialog by remember { mutableStateOf(false) }
    var staffName by remember { mutableStateOf("") }
    var staffRole by remember { mutableStateOf(nurseRole) }
    var addStaffError by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopAppBar(title = { Text(plant?.name ?: "", color = Color.White) }, navigationIcon = { Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ isMenuOpen = true }) { Icon(Icons.Default.Menu, null, tint = Color.White) }; IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).background(Color.Transparent)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) { PlantCalendar(selectedDate) { datePickerState.selectedDateMillis = it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() } }
                    if (plant != null && selectedDate != null) {
                        val dateKey = selectedDate.toString(); val assignments = assignmentsByDate.getOrPut(dateKey) { mutableStateMapOf() }
                        LaunchedEffect(plant.id, dateKey) {
                            database.reference.child("plants/${plant.id}/turnos/turnos-$dateKey").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val loaded = mutableMapOf<String, ShiftAssignmentState>()
                                        snapshot.children.forEach { s ->
                                            val n = s.child("nurses").children.map { it.toSlotAssignment("Sin asignar") }.toMutableList()
                                            val a = s.child("auxiliaries").children.map { it.toSlotAssignment("Sin asignar") }.toMutableList()
                                            loaded[s.key!!] = ShiftAssignmentState(mutableStateListOf(*n.toTypedArray()), mutableStateListOf(*a.toTypedArray()))
                                        }
                                        assignments.clear(); assignments.putAll(loaded); savedAssignmentsByDate[dateKey] = true
                                    } else savedAssignmentsByDate[dateKey] = false
                                }
                                override fun onCancelled(e: DatabaseError) {}
                            })
                        }
                        ShiftAssignmentsSection(plant, assignments, selectedDate.toString(), isSupervisor, savedAssignmentsByDate[dateKey] == true, nurseOptions, auxOptions) { saveShiftAssignments(database, plant.id, dateKey, it) { if (it) savedAssignmentsByDate[dateKey] = true } }
                    }
                }
            }
        }
        AnimatedVisibility(isMenuOpen, enter = slideInHorizontally { -it } + fadeIn(), exit = slideOutHorizontally { -it } + fadeOut()) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(280.dp).fillMaxHeight().background(Color(0xFF0F172A)).padding(vertical = 16.dp)) {
                    if (isSupervisor) {
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Añadir personal", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; showAddStaffDialog = true }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Lista de personal", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; showStaffListDialog = true }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Configuración", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; onOpenPlantSettings() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Importar Turnos", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; onOpenImportShifts() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    }
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Chat de grupo", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; onOpenChat() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Cambio de turnos", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; onOpenShiftChange() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Text("Volver al menú", color = Color.White) }, selected = false, onClick = { isMenuOpen = false; onBack() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                }
                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0x80000000)).clickable { isMenuOpen = false })
            }
        }
    }
    if (showAddStaffDialog && plant != null) AddStaffDialog(staffName, { staffName = it }, staffRole, { staffRole = it }, isSavingStaff, addStaffError, "Añadir Personal", "Guardar", { showAddStaffDialog = false }) { onAddStaff(plant.id, RegisteredUser(UUID.randomUUID().toString(), staffName, staffRole, "", "plant_staff")) { if (it) showAddStaffDialog = false } }
    if (showStaffListDialog && plant != null) StaffListDialog(plant.name, plantStaff.toList(), isSupervisor, { showStaffListDialog = false }) { m, cb -> onEditStaff(plant.id, m, cb) }
}

<<<<<<< HEAD
private fun saveShiftAssignments(db: FirebaseDatabase, pid: String, date: String, assign: Map<String, ShiftAssignmentState>, cb: (Boolean) -> Unit) {
    val data = assign.mapValues { (_, s) -> mapOf("nurses" to s.nurseSlots.map { it.toMap() }, "auxiliaries" to s.auxSlots.map { it.toMap() }) }
    db.reference.child("plants/$pid/turnos/turnos-$date").setValue(data).addOnSuccessListener {
        notifyAssignedUsers(db, pid, date, assign)
        cb(true)
    }.addOnFailureListener { cb(false) }
}

private fun notifyAssignedUsers(db: FirebaseDatabase, pid: String, date: String, assign: Map<String, ShiftAssignmentState>) {
    db.getReference("plants/$pid/personal_de_planta").get().addOnSuccessListener { snap ->
        val staff = snap.children.mapNotNull { it.getValue(RegisteredUser::class.java) }.associateBy { it.name }
        assign.forEach { (shift, state) ->
            (state.nurseSlots + state.auxSlots).forEach { slot ->
                listOf(slot.primaryName, slot.secondaryName).filter { it.isNotBlank() }.forEach { name ->
                    staff[name]?.let { user -> NotificationHelper.sendNotification(user.id, "Turno Asignado", "Tienes turno de $shift el $date", "SHIFT_ADDED") }
=======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportShiftsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Turnos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Opciones de importación",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Button(
                        onClick = { copyTemplateToDownloads(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) {
                        Text("Descargar plantilla")
                    }

                    Button(
                        onClick = { /* No action yet */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) {
                        Text("Importar turnos")
                    }
>>>>>>> 6ee37c7cae193aa582f6f2b545944f9a8dc39d28
                }
            }
        }
    }
}

<<<<<<< HEAD
private fun SlotAssignment.toMap() = mapOf("primary" to primaryName, "secondary" to secondaryName, "halfDay" to hasHalfDay)
private fun DataSnapshot.toSlotAssignment(def: String) = SlotAssignment((child("primary").value as? String).orEmpty(), (child("secondary").value as? String).orEmpty(), child("halfDay").value as? Boolean ?: false)

@Composable fun PlantCalendar(selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    Column(Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(24.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { IconButton({ month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("${month.month.name} ${month.year}", color = Color.White); IconButton({ month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) } }
        val days = month.lengthOfMonth(); val offset = month.atDay(1).dayOfWeek.value - 1
        Column { for (i in 0 until (days + offset + 6) / 7 * 7 step 7) { Row { for (j in 0 until 7) { val d = i + j - offset + 1; if (d in 1..days) { val date = month.atDay(d); Box(Modifier.weight(1f).padding(2.dp).background(if (date == selected) Color(0xFF54C7EC) else Color.Transparent, CircleShape).clickable { onSelect(date) }, contentAlignment = Alignment.Center) { Text("$d", color = Color.White) } } else Spacer(Modifier.weight(1f)) } } } }
=======
@Composable
fun PlantCalendar(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera Mes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior", tint = Color.White)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).uppercase()} ${currentMonth.year}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Días semana
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        val totalCells = (daysInMonth + dayOfWeekOffset + 6) / 7 * 7

        Column {
            for (i in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (j in 0 until 7) {
                        val dayIndex = i + j - dayOfWeekOffset + 1
                        if (dayIndex in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayIndex)
                            val isSelected = date == selectedDate

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(2.dp)
                                    .background(
                                        color = if (isSelected) Color(0xFF54C7EC) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
>>>>>>> 6ee37c7cae193aa582f6f2b545944f9a8dc39d28
    }
}

@Composable private fun InfoMessage(msg: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22000000)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) { Column(Modifier.padding(16.dp, 12.dp)) { Text(msg, color = Color.White) } } }
fun formatPlantDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy"))

@Composable private fun StaffListDialog(title: String, staff: List<RegisteredUser>, supervisor: Boolean, onDismiss: () -> Unit, onEdit: (RegisteredUser, (Boolean) -> Unit) -> Unit) {
    AlertDialog(onDismiss, { TextButton(onDismiss) { Text("Cerrar") } }, title = { Text("Personal - $title") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { staff.forEach { Text("${it.name} - ${it.role}", color = Color.White) } } })
}

@Composable fun ShiftAssignmentsSection(plant: Plant, assign: MutableMap<String, ShiftAssignmentState>, date: String, supervisor: Boolean, saved: Boolean, nurses: List<String>, auxs: List<String>, onSave: (Map<String, ShiftAssignmentState>) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Turnos $date", color = Color.White)
        plant.shiftTimes.forEach { (name, _) ->
            val state = assign.getOrPut(name) { ShiftAssignmentState(mutableStateListOf(), mutableStateListOf()) }
            if (state.nurseSlots.isEmpty()) state.nurseSlots.add(SlotAssignment())
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x22000000))) {
                Column(Modifier.padding(16.dp)) {
                    Text(name, color = Color.White); state.nurseSlots.forEach { AssignmentRow(it, nurses, supervisor) }
                    if (plant.staffScope.contains("aux")) { HorizontalDivider(); state.auxSlots.forEach { AssignmentRow(it, auxs, supervisor) } }
                }
            }
        }
        if (supervisor) Button({ onSave(assign) }, Modifier.fillMaxWidth()) { Text(if (saved) "Editar" else "Guardar") }
    }
}

@Composable fun AssignmentRow(slot: SlotAssignment, options: List<String>, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (enabled) Switch(slot.hasHalfDay, { slot.hasHalfDay = it })
        StaffField(slot.primaryName, options, enabled) { slot.primaryName = it }
    }
    if (slot.hasHalfDay) StaffField(slot.secondaryName, options, enabled) { slot.secondaryName = it }
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun StaffField(value: String, options: List<String>, enabled: Boolean, onSelect: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(value, {}, readOnly = true, enabled = enabled, trailingIcon = { IconButton({ exp = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
        DropdownMenu(exp, { exp = false }) { options.forEach { o -> DropdownMenuItem({ Text(o) }, { onSelect(o); exp = false }) } }
    }
}

<<<<<<< HEAD
@Composable fun AddStaffDialog(name: String, onName: (String) -> Unit, role: String, onRole: (String) -> Unit, loading: Boolean, error: String?, title: String, btn: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismiss, { Button(onConfirm) { Text(btn) } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }, title = { Text(title) }, text = { Column { OutlinedTextField(name, onName); Text("Rol"); RadioButton(role == "Enfermero", { onRole("Enfermero") }); Text("Enfermero"); RadioButton(role == "Auxiliar", { onRole("Auxiliar") }); Text("Auxiliar"); if (error != null) Text(error, color = Color.Red) } })
=======
private fun saveShiftAssignments(
    database: FirebaseDatabase,
    plantId: String,
    dateKey: String,
    assignments: Map<String, ShiftAssignmentState>,
    unassignedLabel: String,
    onComplete: (Boolean) -> Unit
) {
    val payload = assignments.mapValues { (_, state) ->
        mapOf(
            "nurses" to state.nurseSlots.mapIndexed { index, slot ->
                val baseLabel = "enfermero${index + 1}"
                slot.toFirebaseMap(unassignedLabel, baseLabel)
            },
            "auxiliaries" to state.auxSlots.mapIndexed { index, slot ->
                val baseLabel = "auxiliar${index + 1}"
                slot.toFirebaseMap(unassignedLabel, baseLabel)
            }
        )
    }

    database.reference
        .child("plants")
        .child(plantId)
        .child("turnos")
        .child("turnos-$dateKey")
        .setValue(payload)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}

private fun SlotAssignment.toFirebaseMap(
    unassignedLabel: String,
    baseLabel: String
): Map<String, Any> {
    return mapOf(
        "halfDay" to hasHalfDay,
        "primary" to primaryName.ifBlank { unassignedLabel },
        "secondary" to if (hasHalfDay) secondaryName.ifBlank { unassignedLabel } else "",
        "primaryLabel" to baseLabel,
        "secondaryLabel" to if (hasHalfDay) "$baseLabel media jornada" else ""
    )
}

private fun DataSnapshot.toSlotAssignment(unassignedLabel: String): SlotAssignment {
    val halfDay = child("halfDay").value as? Boolean ?: false
    val primary = (child("primary").value as? String).orEmpty()
    val secondary = (child("secondary").value as? String).orEmpty()

    return SlotAssignment(
        primaryName = primary.takeUnless { it == unassignedLabel } ?: "",
        secondaryName = secondary.takeUnless { it == unassignedLabel } ?: "",
        hasHalfDay = halfDay
    )
}

private fun String.normalizedRole(): String = trim().lowercase()

private fun RegisteredUser.displayName(defaultRoleLabel: String): String {
    if (name.isNotBlank()) return name
    if (email.isNotBlank()) return email
    return role.ifBlank { defaultRoleLabel }
}

private fun RegisteredUser.isNurseRole(normalizedRoles: List<String>): Boolean {
    val normalizedRole = role.normalizedRole()
    return normalizedRoles.any { normalizedRole == it } || normalizedRole.contains("enfermer")
}

private fun RegisteredUser.isAuxRole(normalizedRoles: List<String>): Boolean {
    val normalizedRole = role.normalizedRole()
    return normalizedRoles.any { normalizedRole == it } || normalizedRole.contains("auxiliar")
}

@Composable
private fun ReadOnlyAssignmentRow(
    label: String,
    halfDayLabel: String,
    slot: SlotAssignment,
    unassignedLabel: String
) {
    val primaryDisplay = slot.primaryName.ifBlank { unassignedLabel }
    val secondaryDisplay = if (slot.hasHalfDay) slot.secondaryName.ifBlank { unassignedLabel } else null

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xCCFFFFFF),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = primaryDisplay,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        if (secondaryDisplay != null) {
            Text(
                text = halfDayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xCCFFFFFF),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = secondaryDisplay,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AddStaffDialog(
    staffName: String,
    onStaffNameChange: (String) -> Unit,
    staffRole: String,
    onStaffRoleChange: (String) -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    title: String = stringResource(id = R.string.staff_dialog_title),
    confirmButtonText: String = stringResource(id = R.string.staff_dialog_save_action),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val roleOptions = listOf(
        stringResource(id = R.string.role_nurse_generic),
        stringResource(id = R.string.role_aux_generic)
    )

    val dialogShape = RoundedCornerShape(20.dp)
    val glassSurface = Color(0xEE0B1021)
    val glassStroke = Color(0x33FFFFFF)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color(0xCCFFFFFF),
        focusedBorderColor = Color(0xFF54C7EC),
        unfocusedBorderColor = Color(0x66FFFFFF),
        disabledBorderColor = Color(0x33FFFFFF),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color(0xCCFFFFFF),
        disabledLabelColor = Color(0x80FFFFFF),
        focusedContainerColor = Color(0x22FFFFFF),
        unfocusedContainerColor = Color(0x11FFFFFF),
        disabledContainerColor = Color(0x11FFFFFF)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = confirmButtonText,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF54C7EC))
            ) {
                Text(text = stringResource(id = R.string.cancel_label))
            }
        },
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = onStaffNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.staff_dialog_name_label)) },
                    colors = fieldColors,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )

                Text(
                    text = stringResource(id = R.string.staff_dialog_role_label),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                roleOptions.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = staffRole == option,
                            onClick = { onStaffRoleChange(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF54C7EC),
                                unselectedColor = Color.White
                            )
                        )
                        Text(
                            text = option,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        shape = dialogShape,
        modifier = Modifier.border(1.dp, glassStroke, dialogShape),
        containerColor = glassSurface,
        tonalElevation = 0.dp,
        textContentColor = Color.White,
        titleContentColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffDropdownField(
    modifier: Modifier = Modifier,
    label: String,
    selectedValue: String,
    options: List<String>,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit,
    includeUnassigned: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val unassignedLabel = stringResource(id = R.string.staff_unassigned_option)
    val displayValue = selectedValue.takeIf { it.isNotBlank() } ?: if (includeUnassigned) unassignedLabel else ""
    val menuOptions = remember(options, unassignedLabel, includeUnassigned) {
        if (includeUnassigned) listOf(unassignedLabel) + options else options
    }
    val activeColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color(0xCCFFFFFF),
        focusedBorderColor = Color(0xFF54C7EC),
        unfocusedBorderColor = Color(0x66FFFFFF),
        disabledBorderColor = Color(0x33FFFFFF),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color(0xCCFFFFFF),
        disabledLabelColor = Color(0x80FFFFFF),
        focusedContainerColor = Color(0x22FFFFFF),
        unfocusedContainerColor = Color(0x11FFFFFF),
        disabledContainerColor = Color(0x11FFFFFF)
    )
    val inactiveColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xCCFFFFFF),
        unfocusedTextColor = Color(0xCCFFFFFF),
        disabledTextColor = Color(0xCCFFFFFF),
        focusedBorderColor = Color(0x33FFFFFF),
        unfocusedBorderColor = Color(0x33FFFFFF),
        disabledBorderColor = Color(0x33FFFFFF),
        cursorColor = Color(0xCCFFFFFF),
        focusedLabelColor = Color(0x99FFFFFF),
        unfocusedLabelColor = Color(0x99FFFFFF),
        disabledLabelColor = Color(0x99FFFFFF),
        focusedContainerColor = Color(0x11FFFFFF),
        unfocusedContainerColor = Color(0x11FFFFFF),
        disabledContainerColor = Color(0x11FFFFFF)
    )
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded },
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(text = label) },
            trailingIcon = {
                IconButton(
                    onClick = { if (enabled) expanded = !expanded }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f),
                        contentDescription = null
                    )
                }
            },
            colors = if (enabled) activeColors else inactiveColors,
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xF00B1021),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            menuOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        val resolvedSelection = if (option == unassignedLabel) "" else option
                        onOptionSelected(resolvedSelection)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PlantDetailScreenPreview() {
    val nurseRole = stringResource(id = R.string.role_nurse_generic)
    val auxRole = stringResource(id = R.string.role_aux_generic)
    val staffScopeWithAux = stringResource(id = R.string.staff_scope_with_aux)
    val dayShift = stringResource(id = R.string.shift_day)
    val nightShift = stringResource(id = R.string.shift_night)

    val samplePlant = Plant(
        id = "plant-123",
        name = "Planta Norte",
        unitType = "UCI",
        hospitalName = "Hospital Central",
        shiftDuration = stringResource(id = R.string.shift_duration_12h),
        allowHalfDay = false,
        staffScope = staffScopeWithAux,
        shiftTimes = mapOf(
            dayShift to ShiftTime(start = "08:00", end = "20:00"),
            nightShift to ShiftTime(start = "20:00", end = "08:00")
        ),
        staffRequirements = mapOf(dayShift to 2, nightShift to 2),
        personal_de_planta = mapOf(
            "n1" to RegisteredUser(
                id = "n1",
                name = "María Pérez",
                role = nurseRole,
                profileType = "plant_staff"
            ),
            "n2" to RegisteredUser(
                id = "n2",
                name = "Lucía Gómez",
                role = nurseRole,
                profileType = "plant_staff"
            ),
            "a1" to RegisteredUser(
                id = "a1",
                name = "Carlos Ruiz",
                role = auxRole,
                profileType = "plant_staff"
            )
        )
    )

    PlantDetailScreen(
        plant = samplePlant,
        datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis()),
        currentUserProfile = UserProfile(
            firstName = "Ana",
            lastName = "Supervisor",
            role = stringResource(id = R.string.role_supervisor_female)
        ),
        currentMembership = null,
        onBack = {},
        onAddStaff = { _, _, callback -> callback(true) },
        onEditStaff = { _, _, callback -> callback(true) },
        onOpenPlantSettings = {},
        onOpenImportShifts = {}
    )
}

// Función auxiliar para copiar el archivo desde assets a Descargas
private fun copyTemplateToDownloads(context: Context) {
    val fileName = "plantilla_importacion_turnos.csv"
    try {
        // 1. Abrir el archivo desde los assets de la app
        val inputStream = context.assets.open(fileName)

        // 2. Definir la ruta de destino (Carpeta de Descargas pública)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outFile = File(downloadsDir, fileName)

        // 3. Copiar el contenido
        FileOutputStream(outFile).use { output ->
            inputStream.copyTo(output)
        }

        Toast.makeText(context, "Plantilla guardada en Descargas", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
>>>>>>> 6ee37c7cae193aa582f6f2b545944f9a8dc39d28
}