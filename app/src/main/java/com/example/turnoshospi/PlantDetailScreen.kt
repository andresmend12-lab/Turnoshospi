package com.example.turnoshospi

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
                }
            }
        }
    }
}

private fun SlotAssignment.toMap() = mapOf("primary" to primaryName, "secondary" to secondaryName, "halfDay" to hasHalfDay)
private fun DataSnapshot.toSlotAssignment(def: String) = SlotAssignment((child("primary").value as? String).orEmpty(), (child("secondary").value as? String).orEmpty(), child("halfDay").value as? Boolean ?: false)

@Composable fun PlantCalendar(selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    Column(Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(24.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { IconButton({ month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("${month.month.name} ${month.year}", color = Color.White); IconButton({ month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) } }
        val days = month.lengthOfMonth(); val offset = month.atDay(1).dayOfWeek.value - 1
        Column { for (i in 0 until (days + offset + 6) / 7 * 7 step 7) { Row { for (j in 0 until 7) { val d = i + j - offset + 1; if (d in 1..days) { val date = month.atDay(d); Box(Modifier.weight(1f).padding(2.dp).background(if (date == selected) Color(0xFF54C7EC) else Color.Transparent, CircleShape).clickable { onSelect(date) }, contentAlignment = Alignment.Center) { Text("$d", color = Color.White) } } else Spacer(Modifier.weight(1f)) } } } }
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

@Composable fun AddStaffDialog(name: String, onName: (String) -> Unit, role: String, onRole: (String) -> Unit, loading: Boolean, error: String?, title: String, btn: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismiss, { Button(onConfirm) { Text(btn) } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }, title = { Text(title) }, text = { Column { OutlinedTextField(name, onName); Text("Rol"); RadioButton(role == "Enfermero", { onRole("Enfermero") }); Text("Enfermero"); RadioButton(role == "Auxiliar", { onRole("Auxiliar") }); Text("Auxiliar"); if (error != null) Text(error, color = Color.Red) } })
}