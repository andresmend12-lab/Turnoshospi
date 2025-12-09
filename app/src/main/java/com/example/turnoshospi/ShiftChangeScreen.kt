package com.example.turnoshospi

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.turnoshospi.ui.theme.ShiftColors
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.UUID

// ============================================================================================
// PANTALLA PRINCIPAL: ShiftChangeScreen
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftChangeScreen(
    plantId: String,
    currentUser: UserProfile?,
    currentUserId: String,
    shiftColors: ShiftColors,
    onBack: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.tab_my_shifts),
        stringResource(R.string.tab_management),
        stringResource(R.string.tab_suggestions)
    )

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")

    // --- ESTADOS DE DATOS ---
    val myShiftsMap = remember { mutableStateMapOf<LocalDate, String>() }
    val myShiftsList = remember { mutableStateListOf<MyShiftDisplay>() }
    val allRequests = remember { mutableStateListOf<ShiftChangeRequest>() }

    val plantStaffMap = remember { mutableStateMapOf<String, RegisteredUser>() }
    val staffNameMap = remember { mutableStateMapOf<String, String>() }
    val staffIdToUserId = remember { mutableStateMapOf<String, String>() }

    val allPlantShifts = remember { mutableStateListOf<PlantShift>() }
    val userSchedules = remember { mutableStateMapOf<String, MutableMap<LocalDate, String>>() }

    var isLoading by remember { mutableStateOf(true) }

    // --- ESTADOS DE NAVEGACIÓN INTERNA ---
    var selectedRequestForSuggestions by remember { mutableStateOf<ShiftChangeRequest?>(null) }

    // --- DIÁLOGOS ---
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedShiftForRequest by remember { mutableStateOf<MyShiftDisplay?>(null) }

    // --- IDENTIDAD ---
    val isSupervisor = remember(currentUser) {
        currentUser?.role?.contains("Supervisor", ignoreCase = true) == true
    }
    val currentUserName = remember(currentUser) {
        if (currentUser != null) "${currentUser.firstName} ${currentUser.lastName}".trim() else ""
    }

    // ========================================================================================
    // CARGA DE DATOS
    // ========================================================================================
    LaunchedEffect(plantId, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val myName = "${currentUser.firstName} ${currentUser.lastName}".trim()

        // 1. CARGAR PERSONAL
        database.getReference("plants/$plantId/personal_de_planta").get().addOnSuccessListener { snap ->
            plantStaffMap.clear()
            staffNameMap.clear()
            snap.children.forEach { child ->
                val u = child.getValue(RegisteredUser::class.java)
                if (u != null) {
                    plantStaffMap[child.key!!] = u
                    if (u.name.isNotBlank()) {
                        staffNameMap[u.name.trim().lowercase()] = child.key!!
                    }
                }
            }

            // 2. CARGAR MAPEO DE USUARIOS
            database.getReference("plants/$plantId/userPlants").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(userSnap: DataSnapshot) {
                    staffIdToUserId.clear()
                    userSnap.children.forEach { u ->
                        val authId = u.key
                        val sId = u.child("staffId").value as? String
                        if (authId != null && sId != null) {
                            staffIdToUserId[sId] = authId
                        }
                    }

                    // 3. CARGAR TURNOS
                    val startDate = LocalDate.now().minusDays(15)
                    val startKey = "turnos-$startDate"

                    database.getReference("plants/$plantId/turnos")
                        .orderByKey()
                        .startAt(startKey)
                        .limitToFirst(90)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                myShiftsMap.clear()
                                myShiftsList.clear()
                                allPlantShifts.clear()
                                userSchedules.clear()

                                snapshot.children.forEach { dateSnapshot ->
                                    val dateKey = dateSnapshot.key?.removePrefix("turnos-") ?: return@forEach
                                    try {
                                        val date = LocalDate.parse(dateKey)
                                        dateSnapshot.children.forEach { shiftSnap ->
                                            val shiftNameOriginal = shiftSnap.key ?: ""

                                            fun processSlot(slot: DataSnapshot, rolePrefix: String) {
                                                val name = slot.child("primary").value.toString()
                                                val secondaryName = slot.child("secondary").value.toString()
                                                val isHalfDay = slot.child("halfDay").value as? Boolean == true

                                                val displayShiftName = if (isHalfDay) "Media $shiftNameOriginal" else shiftNameOriginal

                                                if (name.isNotBlank() && name != "null" && !name.equals("sin asignar", ignoreCase = true)) {
                                                    val staffId = staffNameMap[name.trim().lowercase()]
                                                    val authId = if (staffId != null) staffIdToUserId[staffId] else null
                                                    val finalId = authId ?: staffId ?: "UNREGISTERED_$name"
                                                    val role = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.role else rolePrefix

                                                    if (!userSchedules.containsKey(finalId)) userSchedules[finalId] = mutableMapOf()
                                                    userSchedules[finalId]!![date] = displayShiftName
                                                    allPlantShifts.add(PlantShift(finalId, name, role, date, displayShiftName))

                                                    if (finalId == currentUserId || name.equals(myName, true)) {
                                                        myShiftsMap[date] = displayShiftName
                                                        myShiftsList.add(MyShiftDisplay(dateKey, displayShiftName, date))
                                                    }
                                                }

                                                if (secondaryName.isNotBlank() && secondaryName != "null" && !secondaryName.equals("sin asignar", ignoreCase = true)) {
                                                    val staffId = staffNameMap[secondaryName.trim().lowercase()]
                                                    val authId = if (staffId != null) staffIdToUserId[staffId] else null
                                                    val finalId = authId ?: staffId ?: "UNREGISTERED_$secondaryName"
                                                    val role = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.role else rolePrefix

                                                    if (!userSchedules.containsKey(finalId)) userSchedules[finalId] = mutableMapOf()
                                                    userSchedules[finalId]!![date] = displayShiftName
                                                    allPlantShifts.add(PlantShift(finalId, secondaryName, role, date, displayShiftName))

                                                    if (finalId == currentUserId || secondaryName.equals(myName, true)) {
                                                        myShiftsMap[date] = displayShiftName
                                                        myShiftsList.add(MyShiftDisplay(dateKey, displayShiftName, date))
                                                    }
                                                }
                                            }
                                            shiftSnap.child("nurses").children.forEach { processSlot(it, "Enfermero/a") }
                                            shiftSnap.child("auxiliaries").children.forEach { processSlot(it, "Auxiliar") }
                                        }
                                    } catch (_: Exception) {}
                                }
                                myShiftsList.sortBy { it.fullDate }
                                allPlantShifts.sortBy { it.date }
                                isLoading = false
                            }
                            override fun onCancelled(error: DatabaseError) { isLoading = false }
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 4. CARGAR SOLICITUDES
        database.getReference("plants/$plantId/shift_requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRequests.clear()
                    snapshot.children.mapNotNull { childSnapshot ->
                        try {
                            val req = childSnapshot.getValue(ShiftChangeRequest::class.java)
                            req?.let { if (it.id.isBlank()) it.copy(id = childSnapshot.key ?: "") else it }
                        } catch (e: Exception) { null }
                    }
                        .filter { it.id.isNotBlank() && it.status != RequestStatus.DRAFT }
                        .forEach { allRequests.add(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    BackHandler(enabled = selectedRequestForSuggestions != null) {
        selectedRequestForSuggestions = null
    }

    // ========================================================================================
    // UI PRINCIPAL
    // ========================================================================================
    val titleCandidateSearch = stringResource(R.string.title_candidate_search)
    val titleManagement = stringResource(R.string.title_change_management)
    val msgRequestSent = stringResource(R.string.msg_request_sent)
    val msgRequestDeleted = stringResource(R.string.msg_request_deleted)
    val msgSwapApproved = stringResource(R.string.msg_swap_approved)
    val msgSupervisorRejected = stringResource(R.string.msg_supervisor_rejected)
    val msgPartnerRejected = stringResource(R.string.msg_partner_rejected)
    val msgRequestCreated = stringResource(R.string.msg_request_created)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (selectedRequestForSuggestions != null)
                        Text(titleCandidateSearch, color = Color.White, fontWeight = FontWeight.Bold)
                    else
                        Text(titleManagement, color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedRequestForSuggestions != null) selectedRequestForSuggestions = null
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            if (selectedRequestForSuggestions == null && !isSupervisor) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFF54C7EC))
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF54C7EC))
                } else {
                    if (selectedRequestForSuggestions != null) {
                        FullPlantShiftsList(
                            request = selectedRequestForSuggestions!!,
                            allShifts = allPlantShifts,
                            currentUserId = currentUserId,
                            userSchedules = userSchedules,
                            currentUserSchedule = myShiftsMap,
                            shiftColors = shiftColors,
                            onProposeSwap = { candidateShift ->
                                performDirectProposal(database, plantId, selectedRequestForSuggestions!!, candidateShift, onSaveNotification)
                                selectedRequestForSuggestions = null
                                Toast.makeText(context, msgRequestSent, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else if (isSupervisor) {
                        val supervisorRequests = allRequests.filter { it.status == RequestStatus.AWAITING_SUPERVISOR }
                        val myPersonalRequests = allRequests.filter {
                            (it.requesterId == currentUserId || it.requesterName.equals(currentUserName, ignoreCase = true)) &&
                                    it.status == RequestStatus.SEARCHING
                        }

                        ShiftManagementTab(
                            currentUserId = currentUserId,
                            currentUserName = currentUserName,
                            isSupervisor = isSupervisor,
                            userSchedules = userSchedules,
                            mySchedule = myShiftsMap,
                            openRequests = myPersonalRequests,
                            peerPendingRequests = emptyList(),
                            historyRequests = allRequests.filter { it.status == RequestStatus.APPROVED },
                            supervisorRequests = supervisorRequests,
                            shiftColors = shiftColors,
                            onDeleteRequest = { req ->
                                deleteShiftRequest(database, plantId, req.id)
                                Toast.makeText(context, msgRequestDeleted, Toast.LENGTH_SHORT).show()
                            },
                            onAcceptByPartner = { },
                            onRejectByPartner = { },
                            onApproveBySupervisor = { req ->
                                approveSwapBySupervisor(database, plantId, req, {
                                    onSaveNotification(req.requesterId, "SHIFT_APPROVED", "Cambio aprobado por supervisor.", "ShiftChangeScreen", req.id, {})
                                    if (req.targetUserId != null && !req.targetUserId.startsWith("UNREGISTERED")) {
                                        onSaveNotification(req.targetUserId, "SHIFT_APPROVED", "Cambio aprobado por supervisor.", "ShiftChangeScreen", req.id, {})
                                    }
                                    Toast.makeText(context, msgSwapApproved, Toast.LENGTH_SHORT).show()
                                }, { err ->
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                })
                            },
                            onRejectBySupervisor = { req ->
                                updateRequestStatus(database, plantId, req.id, RequestStatus.REJECTED)
                                onSaveNotification(req.requesterId, "SHIFT_REJECTED", msgSupervisorRejected, "ShiftChangeScreen", req.id, {})
                                if (!req.targetUserId.isNullOrBlank() && !req.targetUserId.startsWith("UNREGISTERED")) {
                                    onSaveNotification(req.targetUserId, "SHIFT_REJECTED", msgSupervisorRejected, "ShiftChangeScreen", req.id, {})
                                }
                            },
                            staffIdToUserId = staffIdToUserId,
                            plantStaffMap = plantStaffMap
                        )
                    } else {
                        when (selectedTab) {
                            0 -> MyShiftsCalendarTab(
                                shifts = myShiftsList,
                                shiftColors = shiftColors,
                                onSelectShiftForChange = { shift ->
                                    selectedShiftForRequest = shift
                                    showCreateDialog = true
                                }
                            )
                            1 -> {
                                val openRequests = allRequests.filter {
                                    val isMe = it.requesterId == currentUserId || it.requesterName.equals(currentUserName, ignoreCase = true)
                                    isMe && it.status == RequestStatus.SEARCHING
                                }
                                val peerPendingRequests = allRequests.filter {
                                    val isRequester = it.requesterId == currentUserId || it.requesterName.equals(currentUserName, ignoreCase = true)
                                    val isTarget = it.targetUserId == currentUserId || it.targetUserName.equals(currentUserName, ignoreCase = true)
                                    val relevantStatus = it.status == RequestStatus.PENDING_PARTNER || it.status == RequestStatus.AWAITING_SUPERVISOR
                                    relevantStatus && (isRequester || isTarget)
                                }
                                val historyRequests = allRequests.filter {
                                    val isRequester = it.requesterId == currentUserId || it.requesterName.equals(currentUserName, ignoreCase = true)
                                    val isTarget = it.targetUserId == currentUserId || it.targetUserName.equals(currentUserName, ignoreCase = true)
                                    (isRequester || isTarget) && (it.status == RequestStatus.APPROVED)
                                }

                                ShiftManagementTab(
                                    currentUserId = currentUserId,
                                    currentUserName = currentUserName,
                                    isSupervisor = isSupervisor,
                                    userSchedules = userSchedules,
                                    mySchedule = myShiftsMap,
                                    openRequests = openRequests,
                                    peerPendingRequests = peerPendingRequests,
                                    historyRequests = historyRequests,
                                    supervisorRequests = emptyList(),
                                    shiftColors = shiftColors,
                                    onDeleteRequest = { req ->
                                        deleteShiftRequest(database, plantId, req.id)
                                        Toast.makeText(context, msgRequestDeleted, Toast.LENGTH_SHORT).show()
                                    },
                                    onAcceptByPartner = { req ->
                                        val supervisorIds = mutableListOf<String>()
                                        plantStaffMap.forEach { (staffId, user) ->
                                            if (user.role.contains("Supervisor", ignoreCase = true)) {
                                                val authId = staffIdToUserId[staffId]
                                                if (authId != null) supervisorIds.add(authId)
                                            }
                                        }

                                        val updates = mapOf(
                                            "plants/$plantId/shift_requests/${req.id}/status" to RequestStatus.AWAITING_SUPERVISOR.name,
                                            "plants/$plantId/shift_requests/${req.id}/supervisorIds" to supervisorIds
                                        )

                                        database.reference.updateChildren(updates).addOnSuccessListener {
                                            onSaveNotification(req.requesterId, "SHIFT_UPDATE", "$currentUserName ha aceptado el cambio. Pendiente de supervisor.", "ShiftChangeScreen", req.id, {})
                                            supervisorIds.forEach { supId ->
                                                onSaveNotification(supId, "SHIFT_PENDING_SUPERVISOR", "Solicitud aceptada entre ${req.requesterName} y ${currentUserName}. Requiere aprobación.", "ShiftChangeScreen", req.id, {})
                                            }
                                        }
                                    },
                                    onRejectByPartner = { req ->
                                        deleteShiftRequest(database, plantId, req.id)
                                        if (req.targetUserId == currentUserId || req.targetUserName.equals(currentUserName, ignoreCase = true)) {
                                            onSaveNotification(req.requesterId, "SHIFT_REJECTED", msgPartnerRejected, "ShiftChangeScreen", req.id, {})
                                        }
                                    },
                                    onApproveBySupervisor = { },
                                    onRejectBySupervisor = { },
                                    staffIdToUserId = staffIdToUserId,
                                    plantStaffMap = plantStaffMap
                                )
                            }
                            2 -> MyRequestsForSuggestionsTab(
                                myRequests = allRequests.filter {
                                    (it.requesterId == currentUserId || it.requesterName.equals(currentUserName, ignoreCase = true)) &&
                                            it.status == RequestStatus.SEARCHING
                                },
                                onSeeCandidates = { req ->
                                    selectedRequestForSuggestions = req
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && selectedShiftForRequest != null) {
        val shift = selectedShiftForRequest!!
        CreateShiftRequestDialog(
            shift = shift,
            onDismiss = { showCreateDialog = false },
            onConfirm = { offeredDates, mode ->
                val reqId = database.getReference("plants/$plantId/shift_requests").push().key ?: UUID.randomUUID().toString()
                val hardness = ShiftRulesEngine.calculateShiftHardness(shift.fullDate, shift.shiftName)

                val newRequest = ShiftChangeRequest(
                    id = reqId,
                    type = RequestType.SWAP,
                    status = RequestStatus.SEARCHING,
                    mode = mode,
                    hardnessLevel = hardness,
                    requesterId = currentUserId,
                    requesterName = "${currentUser?.firstName} ${currentUser?.lastName}",
                    requesterRole = currentUser?.role ?: "",
                    requesterShiftDate = shift.date,
                    requesterShiftName = shift.shiftName,
                    offeredDates = offeredDates
                )

                database.getReference("plants/$plantId/shift_requests/$reqId").setValue(newRequest)
                Toast.makeText(context, msgRequestCreated, Toast.LENGTH_LONG).show()
                showCreateDialog = false
                selectedTab = 2
            }
        )
    }
}