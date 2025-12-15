package com.example.turnoshospi

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.turnoshospi.ui.theme.ShiftColors
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
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

    // Datos para el buscador ("Sugerencias")
    val plantStaffMap = remember { mutableStateMapOf<String, RegisteredUser>() }
    val staffNameMap = remember { mutableStateMapOf<String, String>() }

    // Mapa para traducir de StaffId (Planilla) a AuthId (Firebase User) para notificaciones
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
    // Nombre completo para comparaciones de "Fallback" (si el ID es UNREGISTERED)
    val currentUserName = remember(currentUser) {
        if (currentUser != null) "${currentUser.firstName} ${currentUser.lastName}".trim() else ""
    }

    // ========================================================================================
    // CARGA DE DATOS (Secuencial para evitar Race Conditions con IDs)
    // ========================================================================================
    LaunchedEffect(plantId, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val myName = "${currentUser.firstName} ${currentUser.lastName}".trim()

        // 1. CARGAR PERSONAL (Mapeo Nombre -> ID de Staff)
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

            // 2. CARGAR MAPEO DE USUARIOS REGISTRADOS (UserPlants)
            // Esto permite saber qué usuario real (Auth ID) corresponde a cada miembro de la planilla (Staff ID)
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

                    // 3. CARGAR TURNOS (Dentro del callback para asegurar que tenemos los IDs)
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

                                                // -> Procesar Primary
                                                if (name.isNotBlank() && name != "null" && !name.equals("sin asignar", ignoreCase = true)) {
                                                    // Resolución de IDs
                                                    val staffId = staffNameMap[name.trim().lowercase()]
                                                    val authId = if (staffId != null) staffIdToUserId[staffId] else null

                                                    // Preferimos AuthID, luego StaffID, sino Unregistered
                                                    val finalId = authId ?: staffId ?: "UNREGISTERED_$name"

                                                    // Buscamos rol usando el StaffID si existe
                                                    val role = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.role else rolePrefix

                                                    // --- CORRECCIÓN: Usar nombre canónico del staff si existe ---
                                                    val canonicalName = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.name else name

                                                    if (!userSchedules.containsKey(finalId)) userSchedules[finalId] = mutableMapOf()
                                                    userSchedules[finalId]!![date] = displayShiftName
                                                    allPlantShifts.add(PlantShift(finalId, canonicalName, role, date, displayShiftName))

                                                    if (finalId == currentUserId || canonicalName.equals(myName, true)) {
                                                        myShiftsMap[date] = displayShiftName
                                                        myShiftsList.add(MyShiftDisplay(dateKey, displayShiftName, date))
                                                    }
                                                }

                                                // -> Procesar Secondary
                                                if (secondaryName.isNotBlank() && secondaryName != "null" && !secondaryName.equals("sin asignar", ignoreCase = true)) {
                                                    val staffId = staffNameMap[secondaryName.trim().lowercase()]
                                                    val authId = if (staffId != null) staffIdToUserId[staffId] else null
                                                    val finalId = authId ?: staffId ?: "UNREGISTERED_$secondaryName"

                                                    val role = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.role else rolePrefix

                                                    // --- CORRECCIÓN: Usar nombre canónico del staff si existe ---
                                                    val canonicalName = if (staffId != null && plantStaffMap[staffId] != null) plantStaffMap[staffId]!!.name else secondaryName

                                                    if (!userSchedules.containsKey(finalId)) userSchedules[finalId] = mutableMapOf()
                                                    userSchedules[finalId]!![date] = displayShiftName
                                                    allPlantShifts.add(PlantShift(finalId, canonicalName, role, date, displayShiftName))

                                                    if (finalId == currentUserId || canonicalName.equals(myName, true)) {
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

    // Manejo del botón atrás nativo
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
    val msgPartnerAccepted = stringResource(R.string.msg_partner_accepted)
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
                            // CORRECCIÓN: Pasar la lista oficial de personal para el filtro
                            plantStaff = plantStaffMap.values.toList(),
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
                                    // Eliminado bloque duplicado de notificación
                                    Toast.makeText(context, msgSwapApproved, Toast.LENGTH_SHORT).show()
                                }, { err ->
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                })
                            },
                            onRejectBySupervisor = { req ->
                                updateRequestStatus(database, plantId, req.id, RequestStatus.REJECTED)
                                // Eliminado bloque duplicado de notificación
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
                                        // 1. Identificar a los Supervisores (Auth IDs)
                                        val supervisorIds = mutableListOf<String>()
                                        plantStaffMap.forEach { (staffId, user) ->
                                            if (user.role.contains("Supervisor", ignoreCase = true)) {
                                                val authId = staffIdToUserId[staffId]
                                                if (authId != null) {
                                                    supervisorIds.add(authId)
                                                }
                                            }
                                        }

                                        // 2. Preparar actualización en Firebase
                                        val updates = mapOf(
                                            "plants/$plantId/shift_requests/${req.id}/status" to RequestStatus.AWAITING_SUPERVISOR.name,
                                            "plants/$plantId/shift_requests/${req.id}/supervisorIds" to supervisorIds
                                        )

                                        database.reference.updateChildren(updates).addOnSuccessListener {
                                            // 3. Notificación al Solicitante
                                            onSaveNotification(
                                                req.requesterId,
                                                "SHIFT_UPDATE",
                                                "$currentUserName ha aceptado el cambio. Pendiente de supervisor.",
                                                "ShiftChangeScreen",
                                                req.id, {}
                                            )

                                            // 4. Notificación manual a supervisores (opcional si usas Cloud Functions)
                                            supervisorIds.forEach { supId ->
                                                onSaveNotification(
                                                    supId,
                                                    "SHIFT_PENDING_SUPERVISOR",
                                                    "Solicitud aceptada entre ${req.requesterName} y ${currentUserName}. Requiere aprobación.",
                                                    "ShiftChangeScreen",
                                                    req.id, {}
                                                )
                                            }
                                        }
                                    },
                                    onRejectByPartner = { req ->
                                        deleteShiftRequest(database, plantId, req.id)
                                        if (req.targetUserId == currentUserId || req.targetUserName.equals(currentUserName, ignoreCase = true)) {
                                            onSaveNotification(
                                                req.requesterId,
                                                "SHIFT_REJECTED",
                                                msgPartnerRejected,
                                                "ShiftChangeScreen",
                                                req.id, {}
                                            )
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

// ============================================================================================
// COMPONENTE: LISTA DE GESTIÓN (Las 3 Listas)
// ============================================================================================

@Composable
fun ShiftManagementTab(
    currentUserId: String,
    currentUserName: String,
    isSupervisor: Boolean,
    userSchedules: Map<String, Map<LocalDate, String>>,
    mySchedule: Map<LocalDate, String>,
    openRequests: List<ShiftChangeRequest>,
    peerPendingRequests: List<ShiftChangeRequest>,
    historyRequests: List<ShiftChangeRequest>,
    supervisorRequests: List<ShiftChangeRequest>,
    shiftColors: ShiftColors,
    onDeleteRequest: (ShiftChangeRequest) -> Unit,
    onAcceptByPartner: (ShiftChangeRequest) -> Unit,
    onRejectByPartner: (ShiftChangeRequest) -> Unit,
    onApproveBySupervisor: (ShiftChangeRequest) -> Unit,
    onRejectBySupervisor: (ShiftChangeRequest) -> Unit,
    staffIdToUserId: Map<String, String>,
    plantStaffMap: Map<String, RegisteredUser>
) {
    var previewReq by remember { mutableStateOf<ShiftChangeRequest?>(null) }
    var isPreviewSupervisorMode by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(16.dp)) {

        // 1. SUPERVISOR
        if (isSupervisor && supervisorRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_supervisor_pending), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(supervisorRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x22E91E63))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.dialog_simulation_title), color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${req.requesterName} (${req.requesterShiftDate})", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        // Si viene de Marketplace, targetShiftDate puede ser null, ponemos "CUBRIR"
                        Text("${req.targetUserName} (${req.targetShiftDate ?: "CUBRIR"})", color = Color(0xCCFFFFFF), fontSize = 13.sp)

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                previewReq = req
                                isPreviewSupervisorMode = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().height(35.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = Color(0xFF54C7EC))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_view_simulation), color = Color(0xFF54C7EC), fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onRejectBySupervisor(req) }) { Text(stringResource(R.string.btn_reject), color = Color(0xFFFFB4AB)) }
                            Button(onClick = { onApproveBySupervisor(req) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.btn_approve)) }
                        }
                    }
                }
            }
        }

        // 2. EN PROCESO
        if (peerPendingRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_in_process), color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(peerPendingRequests) { req ->
                val amITarget = req.targetUserId == currentUserId || req.targetUserName.equals(currentUserName, ignoreCase = true)
                val isAwaitingSupervisor = req.status == RequestStatus.AWAITING_SUPERVISOR

                val containerColor = when {
                    isAwaitingSupervisor -> Color(0x22E91E63)
                    amITarget -> Color(0x224CAF50)
                    else -> Color(0x22FFA000)
                }

                Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAwaitingSupervisor) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.status_pending_supervisor), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else if (amITarget) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.status_proposal_received), color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Refresh, null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                val targetName = req.targetUserName?.uppercase() ?: "?"
                                Text(stringResource(R.string.status_waiting_partner, targetName), color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_origin), color = Color.Gray, fontSize = 10.sp)
                                Text(req.requesterName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(req.requesterShiftDate, color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_destination), color = Color.Gray, fontSize = 10.sp)
                                Text(req.targetUserName ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(req.targetShiftDate ?: "?", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            }
                        }

                        if (amITarget && !isAwaitingSupervisor) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    previewReq = req
                                    isPreviewSupervisorMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().height(35.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = Color(0xFF54C7EC))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_view_simulation), color = Color(0xFF54C7EC), fontSize = 12.sp)
                            }
                        }

                        if (isAwaitingSupervisor) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.align(Alignment.End)) {
                                Text(stringResource(R.string.btn_cancel), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                            }
                        } else if (amITarget) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onRejectByPartner(req) },
                                    border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB))
                                ) {
                                    Text(stringResource(R.string.btn_reject))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onAcceptByPartner(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                                ) {
                                    Text(stringResource(R.string.btn_accept))
                                }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onRejectByPartner(req) }, modifier = Modifier.align(Alignment.End)) {
                                Text(stringResource(R.string.btn_cancel), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. MIS BÚSQUEDAS
        if (openRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_my_searches), color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(openRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x1154C7EC)), border = BorderStroke(1.dp, Color(0x3354C7EC))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_searching_candidate), color = Color(0xFF54C7EC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.label_offering, "${req.requesterShiftDate} (${req.requesterShiftName})"), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.label_suggestion_hint), color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // 4. HISTORIAL
        if (historyRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_history), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(historyRequests) { req ->
                val statusText = stringResource(R.string.status_approved)
                val statusColor = Color(0xFF4CAF50)

                Card(colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        Text("${req.requesterName} ↔ ${req.targetUserName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${req.requesterShiftDate} / ${req.targetShiftDate}", color = Color.Gray, fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.btn_delete_history), color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (openRequests.isEmpty() && peerPendingRequests.isEmpty()) {
            item {
                Text(stringResource(R.string.msg_no_recent_activity), color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }
        }
    }

    if (previewReq != null) {
        val req = previewReq!!
        val dateReq = LocalDate.parse(req.requesterShiftDate)
        val dateTarget = if(req.targetShiftDate != null) LocalDate.parse(req.targetShiftDate) else null
        val labelMe = stringResource(R.string.label_me)

        val (row1Schedule, row1Name, row1DateOut, row1DateIn, row1ShiftIn) = if (isPreviewSupervisorMode) {
            Quintuple(
                userSchedules[req.requesterId] ?: emptyMap(),
                req.requesterName,
                dateReq, dateTarget, req.targetShiftName
            )
        } else {
            Quintuple(
                mySchedule,
                labelMe,
                dateTarget, dateReq, req.requesterShiftName
            )
        }

        val (row2Schedule, row2Name, row2DateOut, row2DateIn, row2ShiftIn) = if (isPreviewSupervisorMode) {
            Quintuple(
                userSchedules[req.targetUserId] ?: emptyMap(),
                req.targetUserName ?: "?",
                dateTarget, dateReq, req.requesterShiftName
            )
        } else {
            Quintuple(
                userSchedules[req.requesterId] ?: emptyMap(),
                req.requesterName,
                dateReq, dateTarget, req.targetShiftName
            )
        }

        SchedulePreviewDialog(
            onDismiss = { previewReq = null },
            row1Schedule = row1Schedule,
            row1Name = row1Name,
            row1DateToRemove = row1DateOut,
            row1DateToAdd = row1DateIn,
            row1ShiftToAdd = row1ShiftIn,
            row2Schedule = row2Schedule,
            row2Name = row2Name,
            row2DateToRemove = row2DateOut,
            row2DateToAdd = row2DateIn,
            row2ShiftToAdd = row2ShiftIn,
            shiftColors = shiftColors
        )
    }
}

// ============================================================================================
// COMPONENTE: LISTA DE SUGERENCIAS
// ============================================================================================

@Composable
fun MyRequestsForSuggestionsTab(
    myRequests: List<ShiftChangeRequest>,
    onSeeCandidates: (ShiftChangeRequest) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.header_search_candidates), color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.desc_search_candidates), color = Color.Gray, fontSize = 12.sp)
        }

        if (myRequests.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.msg_no_active_offers), color = Color.Gray)
                    Text(stringResource(R.string.desc_create_offer), color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        items(myRequests) { req ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x2254C7EC)),
                border = BorderStroke(1.dp, Color(0x4454C7EC))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_offering_shift), color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            Text("${req.requesterShiftDate} (${req.requesterShiftName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Badge(containerColor = Color(0xFF54C7EC), contentColor = Color.Black) { Text(stringResource(R.string.badge_active)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSeeCandidates(req) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_see_candidates))
                    }
                }
            }
        }
    }
}

// ============================================================================================
// COMPONENTE: LISTADO COMPLETO CON FILTROS
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlantShiftsList(
    request: ShiftChangeRequest,
    allShifts: List<PlantShift>,
    currentUserId: String,
    userSchedules: Map<String, Map<LocalDate, String>>,
    currentUserSchedule: Map<LocalDate, String>,
    plantStaff: List<RegisteredUser>, // NUEVO PARÁMETRO: Lista oficial de personal
    shiftColors: ShiftColors,
    onProposeSwap: (PlantShift) -> Unit
) {
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterPerson by remember { mutableStateOf<String?>(null) }
    var filterShiftType by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }
    var showShiftMenu by remember { mutableStateOf(false) }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewCandidate by remember { mutableStateOf<PlantShift?>(null) }

    // CORRECCIÓN: Calcular 'availablePeople' usando la lista oficial 'plantStaff'
    val availablePeople = remember(plantStaff, request.requesterRole) {
        plantStaff
            .filter {
                ShiftRulesEngine.areRolesCompatible(request.requesterRole, it.role)
                // Nota: Podríamos filtrar al propio usuario por nombre si es necesario,
                // pero allShifts ya filtra por currentUserId luego.
            }
            .map { it.name }
            .distinct()
            .sorted()
    }

    val filteredShifts = remember(allShifts, request, currentUserId, filterDate, filterPerson, filterShiftType) {
        val today = LocalDate.now()
        val myDate = LocalDate.parse(request.requesterShiftDate)
        val mySimulatedSchedule = currentUserSchedule.filterKeys { it != myDate }

        allShifts.filter { shift ->
            val isFuture = !shift.date.isBefore(today)
            val isCompatibleRole = ShiftRulesEngine.areRolesCompatible(request.requesterRole, shift.userRole)
            val isNotMe = shift.userId != currentUserId
            val isSameExactShift = (shift.date == myDate && shift.shiftName.trim().equals(request.requesterShiftName.trim(), ignoreCase = true))

            if (!isFuture || !isCompatibleRole || !isNotMe || isSameExactShift) return@filter false
            if (filterDate != null && shift.date != filterDate) return@filter false

            // CORRECCIÓN: Comparación exacta de nombres (ahora que allShifts tiene nombres canónicos)
            if (filterPerson != null && !shift.userName.equals(filterPerson, ignoreCase = true)) return@filter false

            if (filterShiftType != null && !shift.shiftName.contains(filterShiftType!!, ignoreCase = true)) return@filter false

            val errorForMe = ShiftRulesEngine.validateWorkRules(shift.date, shift.shiftName, mySimulatedSchedule)
            if (errorForMe != null) return@filter false

            val candidateSchedule = userSchedules[shift.userId] ?: emptyMap()
            val candidateSimulatedSchedule = candidateSchedule.filterKeys { it != shift.date }
            val errorForHim = ShiftRulesEngine.validateWorkRules(myDate, request.requesterShiftName, candidateSimulatedSchedule)
            if (errorForHim != null) return@filter false

            true
        }.sortedWith(compareBy({ it.date }, { it.shiftName }))
    }

    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x3354C7EC))
        ) {
            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF54C7EC))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.card_compatible_candidates), color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${stringResource(R.string.label_offering_shift)} ${request.requesterShiftDate}", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = filterDate != null,
                    onClick = { showDatePicker = true },
                    label = { Text(if(filterDate != null) filterDate.toString() else stringResource(R.string.filter_date)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp)) },
                    trailingIcon = if (filterDate != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterDate = null }) } } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                )
            }
            item {
                Box {
                    FilterChip(
                        selected = filterPerson != null,
                        onClick = { showPersonMenu = true },
                        label = { Text(filterPerson ?: stringResource(R.string.filter_person)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterPerson != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterPerson = null }) } } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showPersonMenu, onDismissRequest = { showPersonMenu = false }) {
                        availablePeople.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { filterPerson = name; showPersonMenu = false })
                        }
                    }
                }
            }
            item {
                val shiftOptions = listOf(stringResource(R.string.shift_morning) to "Mañana", stringResource(R.string.shift_afternoon) to "Tarde", stringResource(R.string.shift_night) to "Noche")
                val currentVisualLabel = shiftOptions.find { it.second == filterShiftType }?.first
                Box {
                    FilterChip(
                        selected = filterShiftType != null,
                        onClick = { showShiftMenu = true },
                        label = { Text(currentVisualLabel ?: stringResource(R.string.filter_shift)) },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterShiftType != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterShiftType = null }) } } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showShiftMenu, onDismissRequest = { showShiftMenu = false }) {
                        shiftOptions.forEach { (label, dbValue) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { filterShiftType = dbValue; showShiftMenu = false })
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            filterDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
            ) { DatePicker(state = datePickerState) }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (filteredShifts.isEmpty()) {
                item { Text(text = stringResource(R.string.msg_no_candidates_found), color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            } else {
                items(filteredShifts) { shift ->
                    PlantShiftCard(
                        shift = shift,
                        shiftColors = shiftColors,
                        onAction = { onProposeSwap(shift) },
                        onPreview = {
                            previewCandidate = shift
                            showPreviewDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showPreviewDialog && previewCandidate != null) {
        val candidate = previewCandidate!!
        val dateReq = LocalDate.parse(request.requesterShiftDate)
        val labelMe = stringResource(R.string.label_me)

        SchedulePreviewDialog(
            onDismiss = { showPreviewDialog = false },
            row1Schedule = currentUserSchedule,
            row1Name = labelMe,
            row1DateToRemove = dateReq,
            row1DateToAdd = candidate.date,
            row1ShiftToAdd = candidate.shiftName,
            row2Schedule = userSchedules[candidate.userId] ?: emptyMap(),
            row2Name = candidate.userName,
            row2DateToRemove = candidate.date,
            row2DateToAdd = dateReq,
            row2ShiftToAdd = request.requesterShiftName,
            shiftColors = shiftColors
        )
    }
}

// ============================================================================================
// COMPONENTE: TARJETA DE TURNO (Individual)
// ============================================================================================

@Composable
fun PlantShiftCard(
    shift: PlantShift,
    shiftColors: ShiftColors,
    onAction: () -> Unit,
    onPreview: () -> Unit
) {
    val shiftColor = getShiftColorDynamic(shift.shiftName, false, shiftColors)
    val initials = shift.userName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(shiftColor))
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(36.dp).background(Color(0xFF334155), CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shift.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = shift.userRole, color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${shift.date} • ${shift.shiftName}", color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPreview, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF54C7EC)), border = BorderStroke(1.dp, Color(0xFF54C7EC)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                            Text(stringResource(R.string.btn_preview), fontSize = 11.sp)
                        }
                        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                            Text(stringResource(R.string.btn_choose), color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// COMPONENTE: DIÁLOGO DE PREVIEW
// ============================================================================================

@Composable
fun SchedulePreviewDialog(
    onDismiss: () -> Unit,
    row1Schedule: Map<LocalDate, String>,
    row1Name: String,
    row1DateToRemove: LocalDate?,
    row1DateToAdd: LocalDate?,
    row1ShiftToAdd: String?,
    row2Schedule: Map<LocalDate, String>,
    row2Name: String,
    row2DateToRemove: LocalDate?,
    row2DateToAdd: LocalDate?,
    row2ShiftToAdd: String?,
    shiftColors: ShiftColors
) {
    val date1 = row1DateToRemove ?: LocalDate.now()
    val date2 = row1DateToAdd ?: date1
    val days1 = remember(date1) { (-5..5).map { date1.plusDays(it.toLong()) } }
    val days2 = remember(date2) { (-5..5).map { date2.plusDays(it.toLong()) } }
    val deviceLocale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", deviceLocale)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = Color(0xFF0F172A),
        title = { Text(stringResource(R.string.dialog_simulation_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = stringResource(R.string.label_around_date, date1.format(dateFormatter)), color = Color(0xFF54C7EC), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                ScheduleWeekView(days1, row1Schedule, row1Name, row1DateToRemove, row1DateToAdd, row1ShiftToAdd, row2Schedule, row2Name, row2DateToRemove, row2DateToAdd, row2ShiftToAdd, shiftColors)
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.label_around_date, date2.format(dateFormatter)), color = Color(0xFF54C7EC), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                ScheduleWeekView(days2, row1Schedule, row1Name, row1DateToRemove, row1DateToAdd, row1ShiftToAdd, row2Schedule, row2Name, row2DateToRemove, row2DateToAdd, row2ShiftToAdd, shiftColors)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close), fontSize = 14.sp) } }
    )
}

@Composable
fun ScheduleWeekView(
    days: List<LocalDate>,
    row1Schedule: Map<LocalDate, String>,
    row1Name: String,
    row1DateToRemove: LocalDate?,
    row1DateToAdd: LocalDate?,
    row1ShiftToAdd: String?,
    row2Schedule: Map<LocalDate, String>,
    row2Name: String,
    row2DateToRemove: LocalDate?,
    row2DateToAdd: LocalDate?,
    row2ShiftToAdd: String?,
    shiftColors: ShiftColors
) {
    val nameColumnWidth = 115.dp
    val dayColumnWidth = 38.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
            Column {
                Row {
                    Spacer(modifier = Modifier.width(nameColumnWidth))
                    days.forEach { date ->
                        val isRelevant = date == row1DateToRemove || date == row1DateToAdd
                        val textColor = if (isRelevant) Color(0xFF54C7EC) else Color.Gray
                        Column(modifier = Modifier.width(dayColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${date.dayOfMonth}", color = textColor, fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.1f))
                Spacer(modifier = Modifier.height(6.dp))
                ScheduleRow(row1Name, days, row1Schedule, row1DateToRemove, row1DateToAdd, row1ShiftToAdd, nameColumnWidth, dayColumnWidth, shiftColors)
                Spacer(modifier = Modifier.height(8.dp))
                ScheduleRow(row2Name, days, row2Schedule, row2DateToRemove, row2DateToAdd, row2ShiftToAdd, nameColumnWidth, dayColumnWidth, shiftColors)
            }
        }
    }
}

@Composable
fun ScheduleRow(
    label: String,
    days: List<LocalDate>,
    schedule: Map<LocalDate, String>,
    dateToRemove: LocalDate?,
    dateToAdd: LocalDate?,
    shiftToAdd: String?,
    nameWidth: androidx.compose.ui.unit.Dp,
    cellWidth: androidx.compose.ui.unit.Dp,
    shiftColors: ShiftColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, modifier = Modifier.width(nameWidth).padding(end = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        days.forEach { date ->
            var finalShift = schedule[date] ?: ""
            var isChanged = false
            if (date == dateToRemove) { finalShift = ""; isChanged = true }
            if (date == dateToAdd) { finalShift = shiftToAdd ?: ""; isChanged = true }

            val prevDate = date.minusDays(1)
            var prevShift = schedule[prevDate] ?: ""
            if (prevDate == dateToRemove) prevShift = ""
            if (prevDate == dateToAdd) prevShift = shiftToAdd ?: ""

            val isSaliente = prevShift.contains("Noche", true) && finalShift.isBlank()
            val originalCurrentShift = schedule[date] ?: ""
            val originalPrevShift = schedule[prevDate] ?: ""
            val originalIsSaliente = originalPrevShift.contains("Noche", true) && originalCurrentShift.isBlank()
            val isSalienteChanged = isSaliente != originalIsSaliente
            val shouldHighlight = isChanged || isSalienteChanged

            val baseColor = getShiftColorDynamic(finalShift, isSaliente, shiftColors)
            val cellColor = if (shouldHighlight) baseColor else Color.Transparent
            val lowerShift = finalShift.lowercase()
            val displayText = when {
                lowerShift.contains("media") && lowerShift.contains("tarde") -> "MT"
                lowerShift.contains("media") && lowerShift.contains("mañana") -> "MM"
                finalShift.isNotBlank() -> finalShift.take(1).uppercase()
                isSaliente -> "S"
                else -> "L"
            }

            Box(modifier = Modifier.width(cellWidth).height(cellWidth).padding(2.dp).background(cellColor, RoundedCornerShape(6.dp)).border(if (shouldHighlight) 1.5.dp else 0.dp, if (shouldHighlight) Color.White else Color.Transparent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Text(text = displayText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ============================================================================================
// COMPONENTE: CALENDARIO DE MIS TURNOS
// ============================================================================================

@Composable
fun MyShiftsCalendarTab(
    shifts: List<MyShiftDisplay>,
    shiftColors: ShiftColors,
    onSelectShiftForChange: (MyShiftDisplay) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val shiftsMap = remember(shifts) { shifts.associateBy { it.fullDate } }
    val deviceLocale = Locale.getDefault()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }

                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, deviceLocale).uppercase()} ${currentMonth.year}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.clickable {
                            currentMonth = YearMonth.now() // Vuelve al mes actual
                        }
                    )

                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
                }

                val daysOfWeekShort = androidx.compose.ui.res.stringArrayResource(R.array.days_of_week_short)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    daysOfWeekShort.forEach { Text(it, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                }

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
                                    val shift = shiftsMap[date]
                                    val isSelected = date == selectedDate

                                    // [FIX] Lógica Salientes
                                    val prevDate = date.minusDays(1)
                                    val prevShift = shiftsMap[prevDate]
                                    val isSaliente = (prevShift?.shiftName?.contains("Noche", true) == true) && (shift == null)

                                    val color = getShiftColorDynamic(shift?.shiftName ?: "", isSaliente, shiftColors)

                                    Box(modifier = Modifier.weight(1f).height(48.dp).padding(2.dp).background(color, CircleShape).border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f), CircleShape).clickable { selectedDate = date }, contentAlignment = Alignment.Center) {
                                        Text("$dayIndex", color = if (shift != null) Color.White else Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                } else { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(visible = selectedDate != null) {
            val date = selectedDate!!
            val shift = shiftsMap[date]
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = date.format(DateTimeFormatter.ofPattern("EEEE dd-MM-yyyy", deviceLocale)).replaceFirstChar { it.uppercase() }, color = Color(0xFF54C7EC), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (shift != null) {
                    Text(text = stringResource(R.string.label_current_turn, shift.shiftName), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onSelectShiftForChange(shift) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Default.SwapHoriz, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_search_swap), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else { Text(stringResource(R.string.msg_no_shift_today), color = Color.Gray) }
            }
        }
    }
}

// ============================================================================================
// COMPONENTE: DIALOGO PARA CREAR SOLICITUD
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShiftRequestDialog(
    shift: MyShiftDisplay,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, RequestMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(RequestMode.FLEXIBLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text(stringResource(R.string.dialog_open_request_title), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x2254C7EC)), border = BorderStroke(1.dp, Color(0x4454C7EC))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.label_offering_turn_date), color = Color(0xCCFFFFFF), fontSize = 12.sp)
                        Text("${shift.date} (${shift.shiftName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.1f))
                Column {
                    Text(stringResource(R.string.label_swap_preference), color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == RequestMode.FLEXIBLE, onClick = { selectedMode = RequestMode.FLEXIBLE }, label = { Text(stringResource(R.string.chip_flexible)) },
                            leadingIcon = if (selectedMode == RequestMode.FLEXIBLE) {{ Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }} else null, modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == RequestMode.STRICT, onClick = { selectedMode = RequestMode.STRICT }, label = { Text(stringResource(R.string.chip_strict)) },
                            leadingIcon = if (selectedMode == RequestMode.STRICT) {{ Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }} else null, modifier = Modifier.weight(1f)
                        )
                    }
                    Text(text = if(selectedMode == RequestMode.FLEXIBLE) stringResource(R.string.desc_flexible) else stringResource(R.string.desc_strict), color = Color.Gray, fontSize = 12.sp, lineHeight = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(emptyList(), selectedMode) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_publish_request))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) } }
    )
}

// ============================================================================================
// HELPERS
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

fun performDirectProposal(
    database: FirebaseDatabase,
    plantId: String,
    myReq: ShiftChangeRequest,
    targetShift: PlantShift,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    // Al usar .name nos aseguramos que el Enum se guarda como String ("PENDING_PARTNER")
    val updates = mapOf(
        "plants/$plantId/shift_requests/${myReq.id}/status" to RequestStatus.PENDING_PARTNER.name,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserId" to targetShift.userId,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserName" to targetShift.userName,
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftDate" to targetShift.date.toString(),
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftName" to targetShift.shiftName
    )
    database.reference.updateChildren(updates).addOnSuccessListener {
        // Notificación manual al Target
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

fun rejectSwapProposal(database: FirebaseDatabase, plantId: String, requestId: String) {
    deleteShiftRequest(database, plantId, requestId)
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
                            updates["plants/$plantId/turnos/turnos-${req.targetShiftDate}/${infoB.path}"] = req.requesterName
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

fun deleteShiftRequest(database: FirebaseDatabase, plantId: String, requestId: String) {
    database.getReference("plants/$plantId/shift_requests/$requestId").removeValue()
}

data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
