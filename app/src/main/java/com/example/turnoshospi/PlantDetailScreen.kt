package com.example.turnoshospi

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

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
    onOpenNotifications: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onOpenDirectChats: () -> Unit,
    unreadChatCount: Int = 0,
    onOpenChat: () -> Unit,
    onOpenShiftChange: () -> Unit,
    onOpenShiftMarketplace: () -> Unit,
    onOpenStatistics: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var showVacationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val database = remember { FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/") }

    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // --- ROLES ---
    val supervisorRoles = listOf(stringResource(id = R.string.role_supervisor_male), stringResource(id = R.string.role_supervisor_female))
    val nurseRole = stringResource(id = R.string.role_nurse_generic)
    val nurseRoles = listOf(nurseRole, stringResource(id = R.string.role_nurse_male), stringResource(id = R.string.role_nurse_female))
    val normalizedNurseRoles = remember(nurseRoles) { nurseRoles.map { it.normalizedRole() } }
    val auxRole = stringResource(id = R.string.role_aux_generic)
    val auxRoles = listOf(auxRole, stringResource(id = R.string.role_aux_male), stringResource(id = R.string.role_aux_female))
    val normalizedAuxRoles = remember(auxRoles) { auxRoles.map { it.normalizedRole() } }

    val resolvedRole = currentMembership?.staffRole?.ifBlank { currentUserProfile?.role } ?: currentUserProfile?.role
    val isSupervisor = remember(resolvedRole, supervisorRoles) {
        resolvedRole != null && resolvedRole.normalizedRole() in supervisorRoles.map { it.normalizedRole() }
    }

    // --- DATOS ---
    val assignmentsByDate = remember(plant?.id) { mutableStateMapOf<String, MutableMap<String, ShiftAssignmentState>>() }
    val savedAssignmentsByDate = remember(plant?.id) { mutableStateMapOf<String, Boolean>() }

    // FIX 1: Explicit toList() para ayudar a la inferencia de tipos y corregir errores de contiene.
    val plantStaff = plant?.personal_de_planta?.values.orEmpty().toList()

    // FIX 2: Especificar el tipo en el lambda para corregir la ambigüedad de sobrecarga.
    val nurseStaff = remember(plantStaff, normalizedNurseRoles) { plantStaff.filter { member: RegisteredUser -> member.isNurseRole(normalizedNurseRoles) } }
    // FIX 3: Usar plantStaff en lugar de autogeneración 'auxStaff' y tipado explícito.
    val auxStaff = remember(plantStaff, normalizedAuxRoles) { plantStaff.filter { member: RegisteredUser -> member.isAuxRole(normalizedAuxRoles) } }

    // FIX 4: Especificar el tipo en el lambda para corregir la ambigüedad de sobrecarga en map.
    val nurseOptions = remember(nurseStaff) { nurseStaff.map { member: RegisteredUser -> member.displayName(nurseRole) }.sorted() }
    val auxOptions = remember(auxStaff) { auxStaff.map { member: RegisteredUser -> member.displayName(auxRole) }.sorted() }

    val staffIdToUserIdMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(plant?.id) {
        if (plant?.id != null) {
            database.reference.child("plants/${plant.id}/userPlants").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    staffIdToUserIdMap.clear()
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        val staffId = userSnapshot.child("staffId").value as? String
                        if (staffId != null) staffIdToUserIdMap[staffId] = userId
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // --- DIALOGOS STAFF ---
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var isSavingStaff by remember { mutableStateOf(false) }
    var showStaffListDialog by remember { mutableStateOf(false) }
    var staffName by remember { mutableStateOf("") }
    var staffRole by remember { mutableStateOf(nurseRole) }
    var addStaffError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = plant?.name ?: stringResource(id = R.string.menu_my_plants), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.side_menu_title), tint = Color.White) }
                    },
                    actions = {
                        IconButton(onClick = onOpenNotifications) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = Color(0xFFE91E63)) {
                                            Text(if (unreadNotificationsCount > 99) "99+" else "$unreadNotificationsCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.cd_notifications),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, navigationIconContentColor = Color.White, titleContentColor = Color.White)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Transparent)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        PlantCalendar(
                            selectedDate = selectedDate,
                            onDateSelected = { newDate -> datePickerState.selectedDateMillis = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                        )
                    }

                    if (plant != null && selectedDate != null) {
                        val dateKey = selectedDate.toString()
                        val assignments = assignmentsByDate.getOrPut(dateKey) { mutableStateMapOf() }
                        val unassignedLabel = stringResource(id = R.string.staff_unassigned_option)

                        LaunchedEffect(plant.id, dateKey) {
                            database.reference.child("plants").child(plant.id).child("turnos").child("turnos-$dateKey")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val loadedAssignments = mutableMapOf<String, ShiftAssignmentState>()
                                            snapshot.children.forEach { shiftSnapshot ->
                                                val shiftName = shiftSnapshot.key ?: return@forEach
                                                val nurseSlots = shiftSnapshot.child("nurses").children.sortedBy { it.key?.toIntOrNull() ?: 0 }.map { it.toSlotAssignment(unassignedLabel) }.toMutableList()
                                                val auxSlots = shiftSnapshot.child("auxiliaries").children.sortedBy { it.key?.toIntOrNull() ?: 0 }.map { it.toSlotAssignment(unassignedLabel) }.toMutableList()
                                                loadedAssignments[shiftName] = ShiftAssignmentState(mutableStateListOf(*nurseSlots.toTypedArray()), mutableStateListOf(*auxSlots.toTypedArray()))
                                            }
                                            assignments.clear()
                                            assignments.putAll(loadedAssignments)
                                            savedAssignmentsByDate[dateKey] = true
                                        } else { savedAssignmentsByDate[dateKey] = false }
                                    }
                                    override fun onCancelled(error: DatabaseError) { savedAssignmentsByDate[dateKey] = false }
                                })
                        }

                        ShiftAssignmentsSection(
                            plant = plant, assignments = assignments, selectedDateLabel = formatPlantDate(selectedDate),
                            isSupervisor = isSupervisor, isSavedForDate = savedAssignmentsByDate[dateKey] == true,
                            unassignedLabel = unassignedLabel, nurseOptions = nurseOptions, auxOptions = auxOptions,
                            onSaveAssignments = { states ->
                                saveShiftAssignments(context, database, plant.id, dateKey, states, unassignedLabel, plantStaff, staffIdToUserIdMap, onSaveNotification) { success -> if (success) savedAssignmentsByDate[dateKey] = true }
                            }
                        )
                    } else {
                        InfoMessage(message = stringResource(id = R.string.plant_detail_missing_data))
                    }
                }
            }
        }

        if (plant != null && !isMenuOpen) {
            FloatingActionButton(
                onClick = onOpenDirectChats,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp, 40.dp),
                containerColor = Color(0xFF54C7EC),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                BadgedBox(badge = { if (unreadChatCount > 0) Badge(containerColor = Color.Red) { Text("$unreadChatCount") } }) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = stringResource(R.string.cd_chats)
                    )
                }
            }
        }

        // --- DRAWER ---
        AnimatedVisibility(visible = isMenuOpen, enter = slideInHorizontally { -it } + fadeIn(), exit = slideOutHorizontally { -it } + fadeOut()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.width(280.dp).fillMaxHeight().background(Color(0xFF0F172A), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)).padding(vertical = 16.dp)
                ) {
                    // LLAMA A LA DEFINICIÓN EN MainMenuScreen.kt
                    DrawerHeader(displayName = plant?.name ?: "", welcomeStringId = R.string.side_menu_title)

                    if (isSupervisor) {
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(id = R.string.plant_add_staff_option), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; showAddStaffDialog = true }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(id = R.string.plant_staff_list_option), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; showStaffListDialog = true }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Settings, null, tint = Color.LightGray, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.plant_settings_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenPlantSettings() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.import_shifts_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenImportShifts() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        // CORRECCIÓN: Se mantiene Icons.Default.FactCheck, aunque genera warning, para evitar rotura por AutoMirrored no existente.
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FactCheck, null, tint = Color(0xFFFFA726), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.title_change_management), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenShiftChange() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        HorizontalDivider(color = Color.White.copy(0.2f), modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Share, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.invite_colleagues_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; if (plant != null) sharePlantInvitation(context, plant.name, plant.id, plant.accessPassword) }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    }
                    HorizontalDivider(color = Color.White.copy(0.2f), modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
                    if (currentMembership?.staffId != null) {
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.BeachAccess, null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.vacation_days_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; showVacationDialog = true }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    }
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.group_chat_title), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenChat() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    if (!isSupervisor) {
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.shift_change_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenShiftChange() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                        // CORRECCIÓN: Se mantiene Icons.Default.Assignment, aunque genera warning, para evitar rotura por AutoMirrored no existente.
                        NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Assignment, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.shift_marketplace_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenShiftMarketplace() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    }
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ShowChart, null, modifier = Modifier.size(20.dp), tint = Color(0xFF54C7EC)); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.statistics_label), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onOpenStatistics() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    NavigationDrawerItem(modifier = Modifier.padding(horizontal = 12.dp), label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(stringResource(id = R.string.back_to_menu), color = Color.White) } }, selected = false, onClick = { isMenuOpen = false; onBack() }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x80000000)).clickable { isMenuOpen = false })
            }
        }
    }

    if (showAddStaffDialog && plant != null) {
        AddStaffDialog(
            staffName = staffName, onStaffNameChange = { staffName = it },
            staffRole = staffRole, onStaffRoleChange = { staffRole = it },
            isSaving = isSavingStaff, errorMessage = addStaffError,
            onDismiss = { showAddStaffDialog = false; addStaffError = null; staffName = ""; staffRole = nurseRole },
            onConfirm = {
                if (staffName.isBlank()) addStaffError = context.getString(R.string.staff_dialog_error)
                else {
                    isSavingStaff = true
                    addStaffError = null
                    val newStaff = RegisteredUser(UUID.randomUUID().toString(), staffName, staffRole, "", "plant_staff")
                    onAddStaff(plant.id, newStaff) { success ->
                        isSavingStaff = false
                        if (success) { showAddStaffDialog = false; staffName = ""; staffRole = nurseRole } else addStaffError = context.getString(R.string.staff_dialog_save_error)
                    }
                }
            }
        )
    }

    if (showStaffListDialog && plant != null) {
        StaffListDialog(
            plantName = plant.name, staff = plantStaff, isSupervisor = isSupervisor,
            onDismiss = { showStaffListDialog = false },
            onSaveEdit = { editedMember, callback -> onEditStaff(plant.id, editedMember, callback) }
        )
    }

    if (showVacationDialog && plant != null && currentMembership?.staffId != null) {
        VacationDaysDialog(
            onDismiss = { showVacationDialog = false },
            onConfirm = { dates ->
                saveVacationDaysToFirebase(
                    context, plant.id, currentMembership.staffId,
                    currentMembership.staffName ?: currentUserProfile?.firstName.orEmpty(),
                    dates, onSaveNotification
                )
                showVacationDialog = false
            }
        )
    }
}