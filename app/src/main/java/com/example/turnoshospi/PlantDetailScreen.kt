package com.example.turnoshospi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
// import androidx.compose.material.icons.automirrored.filled.Assignment <--- ELIMINADO
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment // <--- AGREGADO (Versión estándar)
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete // Aseguramos que esté Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

// Modelos de datos específicos de PlantDetailScreen
private class SlotAssignment(
    primaryName: String = "",
    secondaryName: String = "",
    hasHalfDay: Boolean = false
) {
    var primaryName by mutableStateOf(primaryName)
    var secondaryName by mutableStateOf(secondaryName)
    var hasHalfDay by mutableStateOf(hasHalfDay)
}

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
    unreadChatCount: Int,
    onBack: () -> Unit,
    onOpenStaffManagement: () -> Unit,
    onOpenPlantSettings: () -> Unit,
    onOpenImportShifts: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenDirectChats: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenShiftChange: () -> Unit,
    onOpenShiftMarketplace: () -> Unit,
    onOpenStatistics: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var showVacationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val database = remember {
        FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    }

    var unreadNotifCount by remember { mutableIntStateOf(0) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    DisposableEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            val notifsRef = database.getReference("user_notifications").child(currentUserId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.children.count { child ->
                        child.child("read").getValue(Boolean::class.java) == false
                    }
                    unreadNotifCount = count
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            notifsRef.addValueEventListener(listener)
            onDispose { notifsRef.removeEventListener(listener) }
        } else {
            onDispose { }
        }
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
    val plantStaff = plant?.personal_de_planta?.values.orEmpty()
    val nurseStaff = remember(plantStaff, normalizedNurseRoles) {
        plantStaff.filter { member -> member.isNurseRole(normalizedNurseRoles) }
    }
    val auxStaff = remember(plantStaff, normalizedAuxRoles) {
        plantStaff.filter { member -> member.isAuxRole(normalizedAuxRoles) }
    }

    val nurseOptions = remember(nurseStaff) {
        nurseStaff.map { member -> member.displayName(nurseRole) }.sorted()
    }
    val auxOptions = remember(auxStaff) {
        auxStaff.map { member -> member.displayName(auxRole) }.sorted()
    }

    val staffIdToUserIdMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(plant?.id) {
        if (plant?.id != null) {
            database.reference.child("plants/${plant.id}/userPlants")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        staffIdToUserIdMap.clear()
                        for (userSnapshot in snapshot.children) {
                            val userId = userSnapshot.key ?: continue
                            val staffId = userSnapshot.child("staffId").value as? String
                            if (staffId != null) {
                                staffIdToUserIdMap[staffId] = userId
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = plant?.name ?: stringResource(id = R.string.menu_my_plants),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(id = R.string.side_menu_title),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenNotifications) {
                            if (unreadNotifCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(text = unreadNotifCount.toString()) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = stringResource(id = R.string.title_notifications),
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(id = R.string.title_notifications),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenDirectChats,
                    containerColor = Color(0xFF54C7EC),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    if (unreadChatCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(text = unreadChatCount.toString()) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = stringResource(id = R.string.chat_screen_title),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = stringResource(id = R.string.chat_screen_title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
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
                            onDateSelected = { newDate ->
                                datePickerState.selectedDateMillis = newDate
                                    .atStartOfDay(ZoneOffset.UTC)
                                    .toInstant()
                                    .toEpochMilli()
                            }
                        )
                    }

                    if (plant != null && selectedDate != null) {
                        val dateKey = selectedDate.toString()
                        val assignments = assignmentsByDate.getOrPut(dateKey) { mutableStateMapOf() }
                        val unassignedLabel = stringResource(id = R.string.staff_unassigned_option)

                        LaunchedEffect(plant.id, dateKey) {
                            database.reference
                                .child("plants")
                                .child(plant.id)
                                .child("turnos")
                                .child("turnos-$dateKey")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val loadedAssignments = mutableMapOf<String, ShiftAssignmentState>()
                                            snapshot.children.forEach { shiftSnapshot ->
                                                val shiftName = shiftSnapshot.key ?: return@forEach
                                                val nurseSlots = shiftSnapshot.child("nurses").children
                                                    .sortedBy { it.key?.toIntOrNull() ?: 0 }
                                                    .map { it.toSlotAssignment(unassignedLabel) }
                                                    .toMutableList()
                                                val auxSlots = shiftSnapshot.child("auxiliaries").children
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
                                })
                        }

                        ShiftAssignmentsSection(
                            plant = plant,
                            assignments = assignments,
                            selectedDateLabel = formatPlantDate(selectedDate),
                            isSupervisor = isSupervisor,
                            isSavedForDate = savedAssignmentsByDate[dateKey] == true,
                            unassignedLabel = unassignedLabel,
                            nurseOptions = nurseOptions,
                            auxOptions = auxOptions,
                            onSaveAssignments = { states ->
                                saveShiftAssignments(
                                    context = context,
                                    database = database,
                                    plantId = plant.id,
                                    dateKey = dateKey,
                                    assignments = states,
                                    unassignedLabel = unassignedLabel,
                                    plantStaff = plantStaff,
                                    staffIdToUserIdMap = staffIdToUserIdMap,
                                    onSaveNotification = onSaveNotification
                                ) { success ->
                                    if (success) savedAssignmentsByDate[dateKey] = true
                                }
                            }
                        )
                    } else {
                        InfoMessage(message = stringResource(id = R.string.plant_detail_missing_data))
                    }
                }
            }
        }

        // --- MENU LATERAL (DRAWER) ---
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .padding(vertical = 16.dp)
                ) {
                    DrawerHeader(displayName = plant?.name ?: "", welcomeStringId = R.string.side_menu_title)

                    if (isSupervisor) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(id = R.string.plant_manage_staff_option), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                isMenuOpen = false
                                if (plant != null) {
                                    onOpenStaffManagement()
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.plant_settings_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = { isMenuOpen = false; onOpenPlantSettings() },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.import_shifts_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = { isMenuOpen = false; onOpenImportShifts() },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )

                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.title_change_management), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                isMenuOpen = false
                                onOpenShiftChange()
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.invite_colleagues_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                isMenuOpen = false
                                if (plant != null) {
                                    sharePlantInvitation(context, plant.name, plant.id, plant.accessPassword)
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))

                    if (currentMembership?.staffId != null) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BeachAccess, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.vacation_days_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = { isMenuOpen = false; showVacationDialog = true },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }

                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.group_chat_title), color = Color.White)
                            }
                        },
                        selected = false,
                        onClick = {
                            isMenuOpen = false
                            onOpenChat()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    if (!isSupervisor) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.shift_change_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                isMenuOpen = false
                                onOpenShiftChange()
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )

                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Assignment, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp)) // CORRECCIÓN
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.shift_marketplace_label), color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                isMenuOpen = false
                                onOpenShiftMarketplace()
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }

                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ShowChart, null, modifier = Modifier.size(20.dp), tint = Color(0xFF54C7EC))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.statistics_label), color = Color.White)
                            }
                        },
                        selected = false,
                        onClick = {
                            isMenuOpen = false
                            onOpenStatistics()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(id = R.string.back_to_menu), color = Color.White)
                            }
                        },
                        selected = false,
                        onClick = { isMenuOpen = false; onBack() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0x80000000))
                        .clickable { isMenuOpen = false }
                )
            }
        }
    }

    if (showVacationDialog && plant != null && currentMembership?.staffId != null) {
        VacationDaysDialog(
            onDismiss = { showVacationDialog = false },
            onConfirm = { dates ->
                saveVacationDaysToFirebase(
                    context = context,
                    plantId = plant.id,
                    staffId = currentMembership.staffId,
                    staffName = currentMembership.staffName ?: currentUserProfile?.firstName.orEmpty(),
                    dates = dates,
                    onSaveNotification = onSaveNotification
                )
                showVacationDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// FUNCIONES AUXILIARES DE ORDEN Y LOCALIZACIÓN DE TURNOS
// -----------------------------------------------------------------------------

// Asigna prioridad: 1=Mañana, 2=Tarde, 3=Noche
fun getShiftPriority(shiftName: String): Int {
    val lower = shiftName.lowercase().trim()
    return when {
        lower.contains("mañana") || lower.contains("morning") || lower.contains("día") || lower.contains("day") -> 1
        lower.contains("tarde") || lower.contains("afternoon") -> 2
        lower.contains("noche") || lower.contains("night") -> 3
        else -> 99
    }
}

// Traduce el nombre de la BD al string resource local
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

// -----------------------------------------------------------------------------
// LÓGICA DE COMPARTIR PLANTA
// -----------------------------------------------------------------------------
private fun sharePlantInvitation(context: Context, plantName: String, plantId: String, accessCode: String) {
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

// -----------------------------------------------------------------------------
// LÓGICA DE PERSISTENCIA DE ASIGNACIÓN DE TURNOS CON NOTIFICACIONES
// -----------------------------------------------------------------------------
private fun saveShiftAssignments(
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

    // 1. LEER DATOS ANTIGUOS PARA COMPARAR
    turnosRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val oldNames = mutableSetOf<String>()
            if (snapshot.exists()) {
                snapshot.children.forEach { shiftSnapshot ->
                    // Enfermeros
                    shiftSnapshot.child("nurses").children.forEach { slot ->
                        val p = slot.child("primary").value as? String
                        val s = slot.child("secondary").value as? String
                        if (!p.isNullOrBlank() && p != unassignedLabel) oldNames.add(p)
                        if (!s.isNullOrBlank() && s != unassignedLabel) oldNames.add(s)
                    }
                    // Auxiliares
                    shiftSnapshot.child("auxiliaries").children.forEach { slot ->
                        val p = slot.child("primary").value as? String
                        val s = slot.child("secondary").value as? String
                        if (!p.isNullOrBlank() && p != unassignedLabel) oldNames.add(p)
                        if (!s.isNullOrBlank() && s != unassignedLabel) oldNames.add(s)
                    }
                }
            }

            // 2. PREPARAR DATOS NUEVOS
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

            // 3. GUARDAR
            turnosRef.setValue(payload).addOnSuccessListener {
                onComplete(true)

                // 4. NOTIFICACIONES
                val staffIdMap = plantStaff.associateBy { it.name }

                // A. Desasignados (Estaban antes y ahora no)
                val unassignedStaff = oldNames - newNames
                unassignedStaff.forEach { name ->
                    val staffId = staffIdMap[name]?.id
                    val userId = staffId?.let { staffIdToUserIdMap[it] }
                    if (userId != null) {
                        val msg = context.getString(R.string.notif_unassigned_message, dateKey)
                        onSaveNotification(userId, "SHIFT_UNASSIGNED", msg, AppScreen.MainMenu.name, dateKey, {})
                    }
                }

                // B. Asignados/Actualizados (Están ahora)
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

        override fun onCancelled(error: DatabaseError) {
            onComplete(false)
        }
    })
}

// -----------------------------------------------------------------------------
// LÓGICA DE PERSISTENCIA DE VACACIONES
// -----------------------------------------------------------------------------

private fun saveVacationDaysToFirebase(
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

// -----------------------------------------------------------------------------
// DIÁLOGOS Y COMPONENTES AUXILIARES
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationDaysDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val offeredDates = remember { mutableStateListOf<String>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDate = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
    val context = LocalContext.current

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val dateStr = date.toString()
                        if (date.isBefore(LocalDate.now())) {
                            Toast.makeText(context, context.getString(R.string.error_past_date_selection), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (dateStr !in offeredDates) {
                            offeredDates.add(dateStr)
                            offeredDates.sortBy { it }
                        }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.btn_add)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_label)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_vacation_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dialog_vacation_instruction), color = Color.White)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(0.1f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (offeredDates.isEmpty()) {
                        Text(stringResource(R.string.label_no_days_selected), color = Color.Gray)
                    } else {
                        offeredDates.forEach { date ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(date, color = Color.White, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Close, stringResource(R.string.delete), tint = Color(0xFFFFB4AB), modifier = Modifier.clickable { offeredDates.remove(date) }.size(16.dp))
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_add_day))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(offeredDates.toList()) },
                enabled = offeredDates.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White)
            ) {
                Text(stringResource(R.string.btn_confirm_days, offeredDates.size))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_label)) } },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// -----------------------------------------------------------------------------
// IMPORTACIÓN DE CSV
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportShiftsScreen(
    plant: Plant? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (plant == null) {
                importStatus = context.getString(R.string.error_plant_not_loaded)
                isError = true
                return@let
            }
            isImporting = true
            importStatus = context.getString(R.string.status_processing)
            isError = false

            processCsvImport(context, it, plant) { success, message ->
                isImporting = false
                isError = !success
                importStatus = message
                if (success) {
                    Toast.makeText(context, context.getString(R.string.msg_import_success), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_import_shifts), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    Text(stringResource(R.string.subtitle_import_options), style = MaterialTheme.typography.titleMedium, color = Color.White)

                    // Se ha cambiado para usar la nueva generación de plantilla dinámica
                    Button(
                        onClick = { createAndDownloadMatrixTemplate(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) {
                        Text(stringResource(R.string.btn_download_template))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Button(
                        onClick = {
                            importStatus = null
                            launcher.launch("text/*")
                        },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) {
                        Text(if (isImporting) stringResource(R.string.btn_importing) else stringResource(R.string.btn_import))
                    }

                    if (importStatus != null) {
                        Text(
                            text = importStatus.orEmpty(),
                            color = if (isError) Color(0xFFFFB4AB) else Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// Nueva función que reemplaza la lógica anterior. Ahora procesa matriz: Nombres (filas) x Fechas (columnas)
private fun processCsvImport(
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

        // 1. Analizar la cabecera para obtener las fechas
        // Se asume que la celda [0,0] está vacía o contiene algo como "Nombre", y las fechas empiezan en la col 1
        val header = lines[0].split(",").map { it.trim() }
        val dateMap = mutableMapOf<Int, String>() // Índice de columna -> Fecha string

        for (i in 1 until header.size) {
            val dateStr = header[i]
            if (dateStr.isNotBlank()) {
                try {
                    // Validar formato fecha YYYY-MM-DD
                    LocalDate.parse(dateStr)
                    dateMap[i] = dateStr
                } catch (e: Exception) {
                    // Si una cabecera no es fecha válida, se ignora esa columna
                }
            }
        }

        if (dateMap.isEmpty()) {
            onResult(false, "No se encontraron fechas válidas en la cabecera (formato YYYY-MM-DD).")
            return
        }

        // Estructura temporal para acumular: Fecha -> Turno -> Listas de Staff
        // Map<Fecha, Map<Turno, Pair<MutableList<Enfermeros>, MutableList<Auxiliares>>>>
        val assignmentsBuffer = mutableMapOf<String, MutableMap<String, Pair<MutableList<String>, MutableList<String>>>>()

        val plantStaff = plant.personal_de_planta.values
        // val unassignedLabel = context.getString(R.string.staff_unassigned_option)

        // 2. Recorrer las filas (Personal)
        for ((index, line) in lines.drop(1).withIndex()) {
            if (line.isBlank()) continue
            val cols = line.split(",").map { it.trim() }
            if (cols.isEmpty()) continue

            val staffName = cols[0]
            if (staffName.isBlank()) continue

            // Buscar al empleado en la planta para saber su rol
            val staffMember = plantStaff.find { it.name.equals(staffName, ignoreCase = true) }

            // Si el empleado no existe en la app, lo saltamos silenciosamente o registramos error.
            if (staffMember == null) {
                // onResult(false, "El empleado '$staffName' (fila ${index + 2}) no existe en la planta.")
                // return
                continue
            }

            // Determinar si es Enfermero o Auxiliar
            val nurseRoles = listOf("enfermero", "enfermera", "nurse", "supervisor", "supervisora")
            // val auxRoles = listOf("auxiliar", "tcae", "aux")

            val isNurse = staffMember.role.trim().lowercase().let { r -> nurseRoles.any { r.contains(it) } }

            // 3. Recorrer las columnas de fechas para este empleado
            for ((colIndex, dateKey) in dateMap) {
                if (colIndex >= cols.size) break

                val shiftValue = cols[colIndex]
                if (shiftValue.isNotBlank()) {
                    // Validar que el turno exista (Mañana, Tarde, Noche...)
                    // Match insensible a mayúsculas con las keys de shiftTimes de la planta
                    val matchedShiftKey = plant.shiftTimes.keys.find { it.equals(shiftValue, ignoreCase = true) }

                    if (matchedShiftKey != null) {
                        val dateMapAssignments = assignmentsBuffer.getOrPut(dateKey) { mutableMapOf() }
                        val shiftLists = dateMapAssignments.getOrPut(matchedShiftKey) {
                            Pair(mutableListOf(), mutableListOf())
                        }

                        if (isNurse) {
                            shiftLists.first.add(staffMember.name)
                        } else {
                            shiftLists.second.add(staffMember.name)
                        }
                    }
                }
            }
        }

        // 4. Convertir el buffer a formato de actualización de Firebase
        val updates = mutableMapOf<String, Any>()

        assignmentsBuffer.forEach { (dateKey, shiftsMap) ->
            val datePayload = shiftsMap.mapValues { (_, lists) ->
                val (nursesNames, auxNames) = lists

                mapOf(
                    "nurses" to nursesNames.mapIndexed { i, name ->
                        mapOf(
                            "halfDay" to false,
                            "primary" to name,
                            "secondary" to "",
                            "primaryLabel" to "enfermero${i + 1}",
                            "secondaryLabel" to ""
                        )
                    },
                    "auxiliaries" to auxNames.mapIndexed { i, name ->
                        mapOf(
                            "halfDay" to false,
                            "primary" to name,
                            "secondary" to "",
                            "primaryLabel" to "auxiliar${i + 1}",
                            "secondaryLabel" to ""
                        )
                    }
                )
            }
            updates["plants/${plant.id}/turnos/turnos-$dateKey"] = datePayload
        }

        if (updates.isEmpty()) {
            onResult(true, "El archivo se leyó pero no contenía asignaciones de turnos válidas.")
            return
        }

        // 5. Guardar en Firebase
        val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
        database.reference.updateChildren(updates)
            .addOnSuccessListener { onResult(true, context.getString(R.string.msg_import_success)) }
            .addOnFailureListener { onResult(false, context.getString(R.string.error_db_save, it.message)) }

    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false, context.getString(R.string.error_file_read, e.message))
    }
}

// Nueva función para generar la plantilla dinámica
private fun createAndDownloadMatrixTemplate(context: Context) {
    val fileName = "plantilla_turnos_matriz.csv"
    val sb = StringBuilder()

    // 1. Generar Cabecera: Nombre, Fecha1, Fecha2, ...
    sb.append("Nombre Personal")
    val today = LocalDate.now()
    val daysToGenerate = 31
    for (i in 0 until daysToGenerate) {
        sb.append(",").append(today.plusDays(i.toLong()).toString())
    }
    sb.append("\n")

    // 2. Generar Fila de Ejemplo
    sb.append("Ejemplo Apellido")
    // Rellenamos con un patrón simple de ejemplo (Mañana, Tarde, Noche, Libre...)
    val examplePattern = listOf("Mañana", "Mañana", "Tarde", "Tarde", "Noche", "Libre")
    for (i in 0 until daysToGenerate) {
        sb.append(",").append(examplePattern[i % examplePattern.size])
    }
    sb.append("\n")

    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val outFile = File(downloadsDir, fileName)
        FileOutputStream(outFile).use { output ->
            output.write(sb.toString().toByteArray())
        }

        Toast.makeText(context, "Plantilla guardada en Descargas: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al guardar plantilla: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES EXISTENTES Y HELPERS
// -----------------------------------------------------------------------------

@Composable
fun PlantCalendar(selectedDate: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    // Locale dinámico
    val deviceLocale = Locale.getDefault()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_prev_month), tint = Color.White)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, deviceLocale).uppercase()} ${currentMonth.year}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.desc_next_month), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeekShort = androidx.compose.ui.res.stringArrayResource(R.array.days_of_week_short)
            daysOfWeekShort.forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val offset = firstDay.dayOfWeek.value - 1
        val totalCells = (daysInMonth + offset + 6) / 7 * 7
        Column {
            for (i in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (j in 0 until 7) {
                        val dayIndex = i + j - offset + 1
                        if (dayIndex in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayIndex)
                            val isSelected = date == selectedDate
                            Box(
                                modifier = Modifier
                                    .weight(1f).height(48.dp).padding(2.dp)
                                    .background(if (isSelected) Color(0xFF54C7EC) else Color.Transparent, CircleShape)
                                    .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f), CircleShape)
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dayIndex.toString(), color = if (isSelected) Color.Black else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun InfoMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun formatPlantDate(date: LocalDate): String {
    // Fecha localizada
    return date.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.getDefault()))
}

@Composable
private fun StaffListDialog(
    plantName: String,
    staff: List<RegisteredUser>,
    isSupervisor: Boolean,
    onDismiss: () -> Unit,
    onSaveEdit: (RegisteredUser, (Boolean) -> Unit) -> Unit,
    // NUEVO PARÁMETRO PARA BORRAR
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    val sortedStaff = remember(staff) { staff.sortedBy { it.name.lowercase() } }

    // Estado para edición
    var memberInEdition by remember { mutableStateOf<RegisteredUser?>(null) }

    // NUEVO: Estado para confirmación de borrado
    var memberToDelete by remember { mutableStateOf<RegisteredUser?>(null) }

    if (memberInEdition != null) {
        // --- LÓGICA DE EDICIÓN (EXISTENTE) ---
        val member = memberInEdition!!
        var editName by remember { mutableStateOf(member.name) }
        val roleAux = stringResource(id = R.string.role_aux_generic)
        val roleAuxOld = "Auxiliar"
        var editRole by remember {
            mutableStateOf(
                if (member.role.contains(roleAuxOld, ignoreCase = true)) roleAux
                else member.role
            )
        }
        var isSaving by remember { mutableStateOf(false) }
        var editError by remember { mutableStateOf<String?>(null) }

        AddStaffDialog(
            staffName = editName, onStaffNameChange = { editName = it },
            staffRole = editRole, onStaffRoleChange = { editRole = it },
            isSaving = isSaving, errorMessage = editError,
            title = stringResource(id = R.string.edit_profile),
            confirmButtonText = stringResource(id = R.string.register_button),
            onDismiss = { memberInEdition = null },
            onConfirm = {
                if (editName.isBlank()) { editError = "El nombre es obligatorio" }
                else {
                    isSaving = true
                    onSaveEdit(member.copy(name = editName, role = editRole)) { if (it) memberInEdition = null else editError = "Error" }
                }
            }
        )
    } else if (memberToDelete != null) {
        // --- NUEVO: DIÁLOGO DE CONFIRMACIÓN DE BORRADO ---
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Eliminar Personal") }, // Puedes usar un string resource aquí
            text = { Text("¿Estás seguro de que quieres eliminar a ${memberToDelete?.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = memberToDelete?.id ?: return@Button
                        onDelete(id) { success ->
                            if (success) memberToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) { Text(stringResource(id = R.string.cancel_label)) }
            },
            containerColor = Color(0xEE0B1021),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    } else {
        // --- LISTA DE PERSONAL ---
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close_label)) } },
            title = { Text(stringResource(id = R.string.staff_list_dialog_title, plantName)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(350.dp).verticalScroll(rememberScrollState())) {
                    if (sortedStaff.isEmpty()) Text(stringResource(id = R.string.staff_list_dialog_empty))
                    else sortedStaff.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                val roleAux = stringResource(id = R.string.role_aux_generic)
                                val displayRole = if (member.role.contains("Auxiliar", ignoreCase = true)) roleAux else member.role
                                Text(displayRole, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                            }

                            if (isSupervisor) {
                                Row {
                                    // Botón Editar
                                    IconButton(onClick = { memberInEdition = member }) {
                                        Icon(Icons.Default.Edit, null, tint = Color(0xFF54C7EC))
                                    }
                                    // NUEVO: Botón Borrar
                                    IconButton(onClick = { memberToDelete = member }) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    }
                }
            },
            containerColor = Color(0xEE0B1021), titleContentColor = Color.White, textContentColor = Color.White
        )
    }
}

@Composable
private fun ShiftAssignmentsSection(
    plant: Plant, assignments: MutableMap<String, ShiftAssignmentState>, selectedDateLabel: String,
    isSupervisor: Boolean, isSavedForDate: Boolean, unassignedLabel: String,
    nurseOptions: List<String>, auxOptions: List<String>, onSaveAssignments: (Map<String, ShiftAssignmentState>) -> Unit
) {
    val allowAux = plant.staffScope.normalizedRole() == stringResource(id = R.string.staff_scope_with_aux).normalizedRole() ||
            plant.staffScope.contains("aux", ignoreCase = true) || auxOptions.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = R.string.plant_shifts_for_date, selectedDateLabel), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // CAMBIO: Ordenar los turnos disponibles por prioridad fija (Mañana -> Tarde -> Noche)
        val orderedShifts = remember(plant.shiftTimes) {
            plant.shiftTimes.entries.sortedBy { getShiftPriority(it.key) }
        }

        orderedShifts.forEach { (rawShiftName, timing) ->
            // CAMBIO: Obtener nombre localizado para mostrar en UI
            val displayShiftName = getLocalizedShiftName(rawShiftName)

            val nurseReq = plant.staffRequirements[rawShiftName] ?: 0
            val auxReq = if (allowAux) (plant.staffRequirements[rawShiftName] ?: 0).coerceAtLeast(1) else 0
            val state = assignments.getOrPut(rawShiftName) { ShiftAssignmentState(mutableStateListOf(), mutableStateListOf()) }
            ensureSlotSize(state.nurseSlots, nurseReq.coerceAtLeast(1))
            if (auxReq > 0) ensureSlotSize(state.auxSlots, auxReq) else state.auxSlots.clear()

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22000000)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(id = R.string.plant_shift_item, displayShiftName, timing.start.ifEmpty { "--" }, timing.end.ifEmpty { "--" }), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    state.nurseSlots.forEachIndexed { i, slot ->
                        val label = if (slot.hasHalfDay) stringResource(id = R.string.nurse_half_day_label) else stringResource(id = R.string.nurse_label, i + 1)
                        if (isSupervisor) EditableAssignmentRow(label, slot, nurseOptions, stringResource(id = R.string.nurse_half_day_label))
                        else ReadOnlyAssignmentRow(label, stringResource(id = R.string.nurse_half_day_label), slot, unassignedLabel)
                    }
                    if (allowAux) {
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        state.auxSlots.forEachIndexed { i, slot ->
                            val label = if (slot.hasHalfDay) stringResource(id = R.string.aux_half_day_label) else stringResource(id = R.string.aux_label, i + 1)
                            if (isSupervisor) EditableAssignmentRow(label, slot, auxOptions, stringResource(id = R.string.aux_half_day_label))
                            else ReadOnlyAssignmentRow(label, stringResource(id = R.string.aux_half_day_label), slot, unassignedLabel)
                        }
                    }
                }
            }
        }
        if (isSupervisor) {
            Button(onClick = { onSaveAssignments(assignments) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)) {
                Text(if (isSavedForDate) stringResource(id = R.string.edit_assignments_action) else stringResource(id = R.string.save_assignments_action))
            }
        }
    }
}

@Composable
private fun EditableAssignmentRow(label: String, slot: SlotAssignment, options: List<String>, halfDayLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = slot.hasHalfDay, onCheckedChange = { slot.hasHalfDay = it; if (!it) slot.secondaryName = "" }, modifier = Modifier.width(52.dp))
            StaffDropdownField(modifier = Modifier.weight(1f), label = label, selectedValue = slot.primaryName, options = options, enabled = true, onOptionSelected = { slot.primaryName = it }, includeUnassigned = true)
        }
        if (slot.hasHalfDay) {
            StaffDropdownField(modifier = Modifier.fillMaxWidth().padding(start = 60.dp), label = halfDayLabel, selectedValue = slot.secondaryName, options = options, enabled = true, onOptionSelected = { slot.secondaryName = it }, includeUnassigned = true)
        }
    }
}

private fun ensureSlotSize(list: MutableList<SlotAssignment>, expected: Int) {
    while (list.size < expected) list.add(SlotAssignment())
    if (list.size > expected) repeat(list.size - expected) { list.removeLastOrNull() }
}

private fun SlotAssignment.toFirebaseMap(unassigned: String, base: String) = mapOf(
    "halfDay" to hasHalfDay,
    "primary" to primaryName.ifBlank { unassigned },
    "secondary" to if (hasHalfDay) secondaryName.ifBlank { unassigned } else "",
    "primaryLabel" to base,
    "secondaryLabel" to if (hasHalfDay) "$base media jornada" else ""
)

// CORRECCIÓN: Normalizar el valor de "Sin asignar" al leer desde Firebase
private fun DataSnapshot.toSlotAssignment(unassignedLabel: String): SlotAssignment {
    val h = child("halfDay").value as? Boolean ?: false
    val p = (child("primary").value as? String).orEmpty()
    val s = (child("secondary").value as? String).orEmpty()

    fun isUnassigned(value: String): Boolean {
        // Detecta variantes de "sin asignar" para limpiarlas y que la UI muestre la traducción actual
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

private fun String.normalizedRole() = trim().lowercase()
private fun RegisteredUser.displayName(def: String) = name.ifBlank { email.ifBlank { role.ifBlank { def } } }
private fun RegisteredUser.isNurseRole(roles: List<String>) = role.normalizedRole() in roles || role.normalizedRole().contains("enfermer")
private fun RegisteredUser.isAuxRole(roles: List<String>) = role.normalizedRole() in roles || role.normalizedRole().contains("auxiliar")

@Composable
private fun ReadOnlyAssignmentRow(label: String, halfDayLabel: String, slot: SlotAssignment, unassigned: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF), fontWeight = FontWeight.SemiBold)
        Text(slot.primaryName.ifBlank { unassigned }, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        if (slot.hasHalfDay) {
            Text(halfDayLabel, style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF), fontWeight = FontWeight.SemiBold)
            Text(slot.secondaryName.ifBlank { unassigned }, style = MaterialTheme.typography.bodyLarge, color = Color.White)
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
    val roles = listOf(stringResource(id = R.string.role_nurse_generic), stringResource(id = R.string.role_aux_generic))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm, enabled = !isSaving, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))) { Text(confirmButtonText, color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(id = R.string.cancel_label)) } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = staffName, onValueChange = onStaffNameChange, label = { Text(stringResource(id = R.string.staff_dialog_name_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF54C7EC), unfocusedBorderColor = Color(0x66FFFFFF)))
                Text(stringResource(id = R.string.staff_dialog_role_label), color = Color.White)
                roles.forEach { role ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = staffRole == role, onClick = { onStaffRoleChange(role) }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF54C7EC), unselectedColor = Color.White))
                        Text(role, color = Color.White)
                    }
                }
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        },
        containerColor = Color(0xEE0B1021), titleContentColor = Color.White, textContentColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffDropdownField(modifier: Modifier = Modifier, label: String, selectedValue: String, options: List<String>, enabled: Boolean, onOptionSelected: (String) -> Unit, includeUnassigned: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val unassigned = stringResource(id = R.string.staff_unassigned_option)
    val display = selectedValue.ifBlank { if (includeUnassigned) unassigned else "" }
    val menuOptions = if (includeUnassigned) listOf(unassigned) + options else options
    Box(modifier = modifier) {
        OutlinedTextField(
            value = display, onValueChange = {}, readOnly = true, enabled = enabled, label = { Text(label) },
            trailingIcon = { IconButton(onClick = { if (enabled) expanded = !expanded }) { Icon(Icons.Filled.ArrowDropDown, null, Modifier.rotate(if (expanded) 180f else 0f) ) } },
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) { expanded = !expanded },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF54C7EC), unfocusedBorderColor = Color(0x66FFFFFF))
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(), containerColor = Color(0xF00B1021)) {
            menuOptions.forEach { op -> DropdownMenuItem(text = { Text(op, color = Color.White) }, onClick = { onOptionSelected(if (op == unassigned) "" else op); expanded = false }) }
        }
    }
}
