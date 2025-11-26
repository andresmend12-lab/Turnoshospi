package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.rememberDatePickerState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

private data class SlotAssignment(
    var primaryName: String = "",
    var secondaryName: String = "",
    var hasHalfDay: Boolean = false
)

private data class ShiftAssignmentState(
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
    onAddStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val database = remember {
        FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    }

    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val supervisorRoles = listOf(
        stringResource(id = R.string.role_supervisor_male),
        stringResource(id = R.string.role_supervisor_female)
    )
    val nurseRole = stringResource(id = R.string.role_nurse_generic)
    val nurseRoles = listOf(
        nurseRole,
        stringResource(id = R.string.role_nurse_male),
        stringResource(id = R.string.role_nurse_female)
    )
    val normalizedNurseRoles = remember(nurseRoles) { nurseRoles.map { it.normalizedRole() } }
    val auxRole = stringResource(id = R.string.role_aux_generic)
    val auxRoles = listOf(
        auxRole,
        stringResource(id = R.string.role_aux_male),
        stringResource(id = R.string.role_aux_female)
    )
    val normalizedAuxRoles = remember(auxRoles) { auxRoles.map { it.normalizedRole() } }
    val resolvedRole = currentMembership?.staffRole?.ifBlank { currentUserProfile?.role }
        ?: currentUserProfile?.role
    val isSupervisor = resolvedRole in supervisorRoles
    val assignmentsByDate = remember(plant?.id) {
        mutableStateMapOf<String, MutableMap<String, ShiftAssignmentState>>()
    }
    val savedAssignmentsByDate = remember(plant?.id) {
        mutableStateMapOf<String, Boolean>()
    }
    val allowAuxStaffScope = plant?.staffScope == stringResource(id = R.string.staff_scope_with_aux)

    val plantStaff = plant?.registeredUsers?.values.orEmpty()
    val nurseStaff = remember(plantStaff, normalizedNurseRoles) {
        plantStaff.filter { member -> member.isNurseRole(normalizedNurseRoles) }
    }
    val auxStaff = remember(plantStaff, normalizedAuxRoles) {
        plantStaff.filter { member -> member.isAuxRole(normalizedAuxRoles) }
    }

    val nurseOptions = remember(nurseStaff) {
        nurseStaff
            .map { member -> member.displayName(nurseRole) }
            .sorted()
    }
    val auxOptions = remember(auxStaff) {
        auxStaff
            .map { member -> member.displayName(auxRole) }
            .sorted()
    }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var isSavingStaff by remember { mutableStateOf(false) }
    var showStaffListDialog by remember { mutableStateOf(false) }
    var staffName by remember { mutableStateOf("") }
    var staffRole by remember { mutableStateOf(nurseRole) }
    var addStaffError by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White
            ) {
                DrawerHeader(
                    displayName = plant?.name ?: "",
                    welcomeStringId = R.string.side_menu_title
                )
                if (isSupervisor) {
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = { Text(text = stringResource(id = R.string.plant_add_staff_option), color = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAddStaffDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = { Text(text = stringResource(id = R.string.plant_staff_list_option), color = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showStaffListDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )
                }
                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    label = { Text(text = stringResource(id = R.string.back_to_menu), color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onBack()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {
                        Text(
                            text = plant?.name ?: stringResource(id = R.string.menu_my_plants),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(id = R.string.side_menu_title),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.close_label),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1021), Color(0xFF0F172A))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_calendar_title),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DatePicker(
                                state = datePickerState,
                                title = null,
                                headline = null,
                                showModeToggle = false,
                                colors = DatePickerDefaults.colors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = Color.White,
                                    headlineContentColor = Color.White,
                                    weekdayContentColor = Color.White,
                                    subheadContentColor = Color.White,
                                    yearContentColor = Color.White,
                                    currentYearContentColor = Color.White,
                                    selectedYearContentColor = Color.White,
                                    selectedYearContainerColor = Color(0xFF1E293B),
                                    disabledSelectedYearContainerColor = Color(0x661E293B),
                                    selectedDayContentColor = Color.White,
                                    disabledSelectedDayContentColor = Color(0x80FFFFFF),
                                    selectedDayContainerColor = Color(0xFF1E293B),
                                    disabledSelectedDayContainerColor = Color(0x661E293B),
                                    dayContentColor = Color.White,
                                    disabledDayContentColor = Color(0x80FFFFFF),
                                    dayInSelectionRangeContentColor = Color.White,
                                    dayInSelectionRangeContainerColor = Color(0x331E293B),
                                    todayContentColor = Color.White,
                                    todayDateBorderColor = Color(0x66FFFFFF)
                                )
                            )
                            Text(
                                text = selectedDate?.let { formatDate(it) }
                                    ?: stringResource(id = R.string.select_date_prompt),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (plant != null && selectedDate != null) {
                        val dateKey = selectedDate.toString()
                        val assignments = assignmentsByDate.getOrPut(dateKey) {
                            mutableStateMapOf()
                        }
                        val unassignedLabel = stringResource(id = R.string.staff_unassigned_option)

                        LaunchedEffect(plant.id, dateKey) {
                            database.reference
                                .child("plants")
                                .child(plant.id)
                                .child("turnos")
                                .child("turnos-$dateKey")
                                .addListenerForSingleValueEvent(
                                    object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.exists()) {
                                                val loadedAssignments = mutableMapOf<String, ShiftAssignmentState>()

                                                snapshot.children.forEach { shiftSnapshot ->
                                                    val shiftName = shiftSnapshot.key ?: return@forEach
                                                    val nurseSlots =
                                                        shiftSnapshot.child("nurses")
                                                            .children
                                                            .sortedBy { it.key?.toIntOrNull() ?: 0 }
                                                            .map { it.toSlotAssignment(unassignedLabel) }
                                                            .toMutableList()

                                                    val auxSlots =
                                                        shiftSnapshot.child("auxiliaries")
                                                            .children
                                                            .sortedBy { it.key?.toIntOrNull() ?: 0 }
                                                            .map { it.toSlotAssignment(unassignedLabel) }
                                                            .toMutableList()

                                                    loadedAssignments[shiftName] = ShiftAssignmentState(
                                                        nurseSlots = mutableStateListOf(*nurseSlots.toTypedArray()),
                                                        auxSlots = mutableStateListOf(*auxSlots.toTypedArray())
                                                    )
                                                }

                                                assignments.clear()
                                                assignments.putAll(loadedAssignments)
                                                savedAssignmentsByDate[dateKey] = true
                                            } else {
                                                savedAssignmentsByDate[dateKey] = false
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            savedAssignmentsByDate[dateKey] = false
                                        }
                                    }
                                )
                        }
                        ShiftAssignmentsSection(
                            plant = plant,
                            assignments = assignments,
                            selectedDateLabel = formatDate(selectedDate),
                            isSupervisor = isSupervisor,
                            isSavedForDate = savedAssignmentsByDate[dateKey] == true,
                            unassignedLabel = unassignedLabel,
                            nurseOptions = nurseOptions,
                            auxOptions = auxOptions,
                            onSaveAssignments = { states ->
                                saveShiftAssignments(
                                    database = database,
                                    plantId = plant.id,
                                    dateKey = dateKey,
                                    assignments = states,
                                    unassignedLabel = unassignedLabel
                                ) { success ->
                                    if (success) {
                                        savedAssignmentsByDate[dateKey] = true
                                    }
                                }
                            }
                        )
                    } else {
                        InfoMessage(message = stringResource(id = R.string.plant_detail_missing_data))
                    }
                }
            }
        }
    }

    if (showAddStaffDialog && plant != null) {
        AddStaffDialog(
            staffName = staffName,
            onStaffNameChange = { staffName = it },
            staffRole = staffRole,
            onStaffRoleChange = { staffRole = it },
            isSaving = isSavingStaff,
            errorMessage = addStaffError,
            onDismiss = {
                showAddStaffDialog = false
                addStaffError = null
                staffName = ""
                staffRole = nurseRole
            },
            onConfirm = {
                if (staffName.isBlank()) {
                    addStaffError = context.getString(R.string.staff_dialog_error)
                    return@AddStaffDialog
                }

                isSavingStaff = true
                addStaffError = null

                val newStaff = RegisteredUser(
                    id = UUID.randomUUID().toString(),
                    name = staffName,
                    role = staffRole,
                    email = "",
                    profileType = "plant_staff"
                )

                onAddStaff(plant.id, newStaff) { success ->
                    isSavingStaff = false
                    if (success) {
                        showAddStaffDialog = false
                        staffName = ""
                        staffRole = nurseRole
                    } else {
                        addStaffError = context.getString(R.string.staff_dialog_save_error)
                    }
                }
            }
        )
    }

    if (showStaffListDialog && plant != null) {
        StaffListDialog(
            plantName = plant.name,
            staff = plantStaff.toList(),
            onDismiss = { showStaffListDialog = false }
        )
    }
}

@Composable
private fun InfoMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
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
        registeredUsers = mapOf(
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
        onAddStaff = { _, _, callback -> callback(true) }
    )
}

@Composable
private fun StaffListDialog(
    plantName: String,
    staff: List<RegisteredUser>,
    onDismiss: () -> Unit
) {
    val sortedStaff = remember(staff) {
        staff.sortedBy { it.name.lowercase() }
    }

    val dialogShape = RoundedCornerShape(20.dp)
    val glassSurface = Color(0xEE0B1021)
    val glassStroke = Color(0x33FFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF54C7EC))
            ) {
                Text(text = stringResource(id = R.string.close_label))
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.staff_list_dialog_title, plantName),
                color = Color.White
            )
        },
        text = {
            if (sortedStaff.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.staff_list_dialog_empty),
                    color = Color.White
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedStaff.forEach { member ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = member.role,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    }
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

@Composable
private fun ShiftAssignmentsSection(
    plant: Plant,
    assignments: MutableMap<String, ShiftAssignmentState>,
    selectedDateLabel: String,
    isSupervisor: Boolean,
    isSavedForDate: Boolean,
    unassignedLabel: String,
    nurseOptions: List<String>,
    auxOptions: List<String>,
    onSaveAssignments: (Map<String, ShiftAssignmentState>) -> Unit
) {
    val allowAux = plant.staffScope.normalizedRole() ==
        stringResource(id = R.string.staff_scope_with_aux).normalizedRole() ||
        plant.staffScope.contains("aux", ignoreCase = true) ||
        auxOptions.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.plant_shifts_for_date, selectedDateLabel),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val morningShift = stringResource(id = R.string.shift_morning)
        val afternoonShift = stringResource(id = R.string.shift_afternoon)
        val nightShift = stringResource(id = R.string.shift_night)
        val dayShift = stringResource(id = R.string.shift_day)
        val shiftDurationEightHours = stringResource(id = R.string.shift_duration_8h)
        val orderedShiftEntries = remember(plant.shiftTimes, plant.shiftDuration) {
            val preferredOrder = if (plant.shiftDuration == shiftDurationEightHours) {
                listOf(morningShift, afternoonShift, nightShift)
            } else {
                listOf(dayShift, nightShift)
            }

            val ordered = preferredOrder.mapNotNull { key ->
                plant.shiftTimes[key]?.let { key to it }
            }

            val remaining = plant.shiftTimes.filterKeys { it !in preferredOrder }.toList()
            (ordered + remaining).ifEmpty { plant.shiftTimes.toList() }
        }

        orderedShiftEntries.forEach { (shiftName, timing) ->
            val nurseRequirement = plant.staffRequirements[shiftName] ?: 0
            val auxRequirement = if (allowAux) {
                val requestedAuxSlots = plant.staffRequirements[shiftName] ?: 0
                if (requestedAuxSlots > 0) requestedAuxSlots else auxOptions.size.coerceAtLeast(1)
            } else {
                0
            }
            val state = assignments.getOrPut(shiftName) {
                ShiftAssignmentState(
                    nurseSlots = mutableStateListOf(),
                    auxSlots = mutableStateListOf()
                )
            }

            ensureSlotSize(state.nurseSlots, nurseRequirement.coerceAtLeast(1))
            if (auxRequirement > 0) {
                ensureSlotSize(state.auxSlots, auxRequirement)
            } else {
                state.auxSlots.clear()
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.plant_shift_item,
                            shiftName,
                            timing.start.ifEmpty { "--" },
                            timing.end.ifEmpty { "--" }
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(
                            id = R.string.plant_staff_requirement_item,
                            shiftName,
                            nurseRequirement
                        ),
                        color = Color(0xCCFFFFFF),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.nurseSlots.forEachIndexed { index, slot ->
                            val nurseLabel = stringResource(id = R.string.nurse_label, index + 1)
                            val nurseHalfDayLabel = stringResource(id = R.string.nurse_half_day_label)
                            if (isSupervisor) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val halfDayIndent = 52.dp + 8.dp
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            modifier = Modifier.width(52.dp),
                                            checked = slot.hasHalfDay,
                                            onCheckedChange = { checked ->
                                                state.nurseSlots[index] = state.nurseSlots[index].copy(
                                                    hasHalfDay = checked,
                                                    secondaryName = if (checked) slot.secondaryName else ""
                                                )
                                            },
                                            thumbContent = {
                                                Box(
                                                    modifier = Modifier
                                                        .width(14.dp)
                                                        .height(14.dp)
                                                        .background(
                                                            color = if (slot.hasHalfDay) Color(0xFF54C7EC) else Color.White,
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        )
                                        StaffDropdownField(
                                            modifier = Modifier.weight(1f),
                                            label = if (slot.hasHalfDay) nurseHalfDayLabel else nurseLabel,
                                            selectedValue = slot.primaryName,
                                            options = nurseOptions,
                                            enabled = true,
                                            onOptionSelected = { selection ->
                                                state.nurseSlots[index] = state.nurseSlots[index].copy(primaryName = selection)
                                            },
                                            includeUnassigned = true
                                        )
                                    }

                                    if (slot.hasHalfDay) {
                                        StaffDropdownField(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = halfDayIndent),
                                            label = nurseHalfDayLabel,
                                            selectedValue = slot.secondaryName,
                                            options = nurseOptions,
                                            enabled = true,
                                            onOptionSelected = { selection ->
                                                state.nurseSlots[index] = state.nurseSlots[index].copy(secondaryName = selection)
                                            },
                                            includeUnassigned = true
                                        )
                                    }
                                }
                            } else {
                                ReadOnlyAssignmentRow(
                                    label = if (slot.hasHalfDay) nurseHalfDayLabel else nurseLabel,
                                    halfDayLabel = nurseHalfDayLabel,
                                    slot = slot,
                                    unassignedLabel = unassignedLabel
                                )
                            }
                        }

                        if (allowAux) {
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            state.auxSlots.forEachIndexed { index, slot ->
                                val auxLabel = stringResource(id = R.string.aux_label, index + 1)
                                val auxHalfDayLabel = stringResource(id = R.string.aux_half_day_label)
                                if (isSupervisor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val halfDayIndent = 52.dp + 8.dp
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Switch(
                                                modifier = Modifier.width(52.dp),
                                                checked = slot.hasHalfDay,
                                                onCheckedChange = { checked ->
                                                    state.auxSlots[index] = state.auxSlots[index].copy(
                                                        hasHalfDay = checked,
                                                        secondaryName = if (checked) slot.secondaryName else ""
                                                    )
                                                },
                                                thumbContent = {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(14.dp)
                                                            .height(14.dp)
                                                            .background(
                                                                color = if (slot.hasHalfDay) Color(0xFF54C7EC) else Color.White,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                            )
                                            StaffDropdownField(
                                                modifier = Modifier.weight(1f),
                                                label = if (slot.hasHalfDay) auxHalfDayLabel else auxLabel,
                                                selectedValue = slot.primaryName,
                                                options = auxOptions,
                                                enabled = true,
                                                onOptionSelected = { selection ->
                                                    state.auxSlots[index] = state.auxSlots[index].copy(primaryName = selection)
                                                },
                                                includeUnassigned = true
                                            )
                                        }

                                        if (slot.hasHalfDay) {
                                            StaffDropdownField(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = halfDayIndent),
                                                label = auxHalfDayLabel,
                                                selectedValue = slot.secondaryName,
                                                options = auxOptions,
                                                enabled = true,
                                                onOptionSelected = { selection ->
                                                    state.auxSlots[index] = state.auxSlots[index].copy(secondaryName = selection)
                                                },
                                                includeUnassigned = true
                                            )
                                        }
                                    }
                                } else {
                                    ReadOnlyAssignmentRow(
                                        label = if (slot.hasHalfDay) auxHalfDayLabel else auxLabel,
                                        halfDayLabel = auxHalfDayLabel,
                                        slot = slot,
                                        unassignedLabel = unassignedLabel
                                    )
                                }
                            }
                        }
                    }

                    if (isSupervisor) {
                        val saveButtonLabel = if (isSavedForDate) {
                            stringResource(id = R.string.edit_assignments_action)
                        } else {
                            stringResource(id = R.string.save_assignments_action)
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSaveAssignments(assignments) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF54C7EC),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = saveButtonLabel)
                        }
                    }
                }
            }
        }
    }
}

private fun ensureSlotSize(list: MutableList<SlotAssignment>, expected: Int) {
    while (list.size < expected) {
        list.add(SlotAssignment())
    }
    if (list.size > expected) {
        repeat(list.size - expected) { list.removeLastOrNull() }
    }
}

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
        cursorColor = Color.White,
        focusedBorderColor = Color(0xFF54C7EC),
        unfocusedBorderColor = Color(0x66FFFFFF),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color(0xCCFFFFFF),
        focusedContainerColor = Color(0x22FFFFFF),
        unfocusedContainerColor = Color(0x11FFFFFF)
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
                    text = stringResource(id = R.string.staff_dialog_save_action),
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
        title = { Text(text = stringResource(id = R.string.staff_dialog_title)) },
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
    // expanded toggles the dropdown visibility; keep state local for easier manual inspection
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
                        // rotate to indicate the expanded state without needing ArrowDropUp
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
