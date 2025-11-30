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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- PANTALLA PRINCIPAL ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftChangeScreen(
    plantId: String,
    currentUser: UserProfile?,
    currentUserId: String,
    onBack: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis Turnos", "Gestión de Cambios", "Sugerencias")

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")

    // Estados Globales
    val myShiftsMap = remember { mutableStateMapOf<LocalDate, String>() }
    val myShiftsList = remember { mutableStateListOf<MyShiftDisplay>() }
    val allRequests = remember { mutableStateListOf<ShiftChangeRequest>() }

    // Estados para "Sugerencias"
    val plantStaffMap = remember { mutableStateMapOf<String, RegisteredUser>() }
    val staffNameMap = remember { mutableStateMapOf<String, String>() }
    val allPlantShifts = remember { mutableStateListOf<PlantShift>() }
    val userSchedules = remember { mutableStateMapOf<String, MutableMap<LocalDate, String>>() }

    var isLoading by remember { mutableStateOf(true) }

    // Navegación interna
    var selectedRequestForSuggestions by remember { mutableStateOf<ShiftChangeRequest?>(null) }

    // Diálogos
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedShiftForRequest by remember { mutableStateOf<MyShiftDisplay?>(null) }

    // Check Supervisor
    val isSupervisor = remember(currentUser) {
        currentUser?.role?.contains("Supervisor", ignoreCase = true) == true
    }

    // --- CARGA DE DATOS ---
    LaunchedEffect(plantId, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val myName = "${currentUser.firstName} ${currentUser.lastName}".trim()

        // 1. Cargar Personal (Normalización de nombres)
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
        }

        // 2. Cargar TODOS los turnos
        val startDate = LocalDate.now().minusDays(1)
        val startKey = "turnos-$startDate"

        database.getReference("plants/$plantId/turnos")
            .orderByKey()
            .startAt(startKey)
            .limitToFirst(62)
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

                                    // PROCESAR PRIMARY
                                    if (name.isNotBlank() && name != "null" && !name.equals("sin asignar", ignoreCase = true)) {
                                        val uid = staffNameMap[name.trim().lowercase()]
                                        val finalId = uid ?: "UNREGISTERED_$name"
                                        val role = if (plantStaffMap[finalId] != null) plantStaffMap[finalId]!!.role else rolePrefix

                                        if (!userSchedules.containsKey(finalId)) {
                                            userSchedules[finalId] = mutableMapOf()
                                        }
                                        userSchedules[finalId]!![date] = displayShiftName

                                        allPlantShifts.add(PlantShift(finalId, name, role, date, displayShiftName))

                                        if (name.equals(myName, true)) {
                                            myShiftsMap[date] = displayShiftName
                                            if (!date.isBefore(LocalDate.now())) {
                                                myShiftsList.add(MyShiftDisplay(dateKey, displayShiftName, date))
                                            }
                                        }
                                    }

                                    // PROCESAR SECONDARY
                                    if (secondaryName.isNotBlank() && secondaryName != "null" && !secondaryName.equals("sin asignar", ignoreCase = true)) {
                                        val uid = staffNameMap[secondaryName.trim().lowercase()]
                                        val finalId = uid ?: "UNREGISTERED_$secondaryName"
                                        val role = if (plantStaffMap[finalId] != null) plantStaffMap[finalId]!!.role else rolePrefix

                                        if (!userSchedules.containsKey(finalId)) {
                                            userSchedules[finalId] = mutableMapOf()
                                        }
                                        userSchedules[finalId]!![date] = displayShiftName

                                        allPlantShifts.add(PlantShift(finalId, secondaryName, role, date, displayShiftName))

                                        if (secondaryName.equals(myName, true)) {
                                            myShiftsMap[date] = displayShiftName
                                            if (!date.isBefore(LocalDate.now())) {
                                                myShiftsList.add(MyShiftDisplay(dateKey, displayShiftName, date))
                                            }
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

        // 3. Cargar Solicitudes
        database.getReference("plants/$plantId/shift_requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRequests.clear()
                    snapshot.children.mapNotNull { it.getValue(ShiftChangeRequest::class.java) }
                        .filter {
                            it.status == RequestStatus.SEARCHING ||
                                    it.status == RequestStatus.PENDING_PARTNER ||
                                    it.status == RequestStatus.AWAITING_SUPERVISOR
                        }
                        .forEach { allRequests.add(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    BackHandler(enabled = selectedRequestForSuggestions != null) {
        selectedRequestForSuggestions = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (selectedRequestForSuggestions != null)
                        Text("Buscador de Candidatos", color = Color.White, fontWeight = FontWeight.Bold)
                    else
                        Text("Gestión de Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedRequestForSuggestions != null) selectedRequestForSuggestions = null
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            // SI NO ES SUPERVISOR, MOSTRAMOS LOS TABS
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
                        // VISTA DE LISTA CON FILTROS
                        FullPlantShiftsList(
                            request = selectedRequestForSuggestions!!,
                            allShifts = allPlantShifts,
                            currentUserId = currentUserId,
                            userSchedules = userSchedules,
                            currentUserSchedule = myShiftsMap,
                            onProposeSwap = { candidateShift ->
                                performDirectProposal(database, plantId, selectedRequestForSuggestions!!, candidateShift, onSaveNotification)
                                selectedRequestForSuggestions = null
                                Toast.makeText(context, "Solicitud enviada. Esperando aceptación del compañero.", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else if (isSupervisor) {
                        // VISTA SUPERVISOR: GESTIÓN DIRECTA
                        ShiftManagementTab(
                            currentUserId = currentUserId,
                            isSupervisor = isSupervisor,
                            myRequests = allRequests.filter { it.requesterId == currentUserId },
                            supervisorRequests = allRequests.filter { it.status == RequestStatus.AWAITING_SUPERVISOR },
                            // Supervisor ve TODOS los pendientes de partner
                            inProgressRequests = allRequests.filter { it.status == RequestStatus.PENDING_PARTNER },
                            onDeleteRequest = { req ->
                                deleteShiftRequest(database, plantId, req.id)
                                Toast.makeText(context, "Solicitud borrada", Toast.LENGTH_SHORT).show()
                            },
                            onAcceptByPartner = { }, // Supervisor no acepta por el partner
                            onRejectByPartner = { },
                            onApproveBySupervisor = { req ->
                                approveSwapBySupervisor(database, plantId, req, {
                                    onSaveNotification(req.requesterId, "SHIFT_APPROVED", "Cambio aprobado por supervisor.", "ShiftChangeScreen", req.id, {})
                                    if (req.targetUserId != null) {
                                        onSaveNotification(req.targetUserId, "SHIFT_APPROVED", "Cambio aprobado por supervisor.", "ShiftChangeScreen", req.id, {})
                                    }
                                    Toast.makeText(context, "Cambio ejecutado y aprobado.", Toast.LENGTH_SHORT).show()
                                }, { err ->
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                })
                            },
                            onRejectBySupervisor = { req ->
                                updateRequestStatus(database, plantId, req.id, RequestStatus.REJECTED)
                                onSaveNotification(req.requesterId, "SHIFT_REJECTED", "El supervisor rechazó el cambio.", "ShiftChangeScreen", req.id, {})
                            }
                        )
                    } else {
                        // VISTA USUARIO NORMAL: TABS
                        when (selectedTab) {
                            0 -> MyShiftsCalendarTab(
                                shifts = myShiftsList,
                                onSelectShiftForChange = { shift ->
                                    selectedShiftForRequest = shift
                                    showCreateDialog = true
                                }
                            )
                            1 -> ShiftManagementTab(
                                currentUserId = currentUserId,
                                isSupervisor = isSupervisor,
                                myRequests = allRequests.filter { it.requesterId == currentUserId },
                                supervisorRequests = emptyList(),
                                // Usuario normal ve los que le implican (Requester o Target) y están pendientes
                                inProgressRequests = allRequests.filter {
                                    (it.requesterId == currentUserId || it.targetUserId == currentUserId) &&
                                            it.status == RequestStatus.PENDING_PARTNER
                                },
                                onDeleteRequest = { req ->
                                    deleteShiftRequest(database, plantId, req.id)
                                    Toast.makeText(context, "Solicitud borrada", Toast.LENGTH_SHORT).show()
                                },
                                onAcceptByPartner = { req ->
                                    updateRequestStatus(database, plantId, req.id, RequestStatus.AWAITING_SUPERVISOR)
                                    onSaveNotification(
                                        req.requesterId,
                                        "SHIFT_UPDATE",
                                        "Tu compañero ha aceptado el cambio. Pendiente de Supervisor.",
                                        "ShiftChangeScreen",
                                        req.id, {}
                                    )
                                },
                                onRejectByPartner = { req ->
                                    rejectSwapProposal(database, plantId, req.id)
                                    onSaveNotification(
                                        req.requesterId,
                                        "SHIFT_REJECTED",
                                        "El compañero rechazó el cambio. Tu solicitud vuelve a estar abierta.",
                                        "ShiftChangeScreen",
                                        req.id, {}
                                    )
                                },
                                onApproveBySupervisor = { },
                                onRejectBySupervisor = { }
                            )
                            2 -> MyRequestsForSuggestionsTab(
                                myRequests = allRequests.filter { it.requesterId == currentUserId && it.status == RequestStatus.SEARCHING },
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
                Toast.makeText(context, "Solicitud creada. Ve a 'Sugerencias' para buscar candidatos.", Toast.LENGTH_LONG).show()
                showCreateDialog = false
                selectedTab = 2
            }
        )
    }
}

// --- 3. LISTA DE TURNOS CON BARRA DE FILTROS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlantShiftsList(
    request: ShiftChangeRequest,
    allShifts: List<PlantShift>,
    currentUserId: String,
    userSchedules: Map<String, Map<LocalDate, String>>,
    currentUserSchedule: Map<LocalDate, String>,
    onProposeSwap: (PlantShift) -> Unit
) {
    // ESTADOS UI
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterPerson by remember { mutableStateOf<String?>(null) }
    var filterShiftType by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }
    var showShiftMenu by remember { mutableStateOf(false) }

    // Estado para el Dialog de Preview
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewCandidate by remember { mutableStateOf<PlantShift?>(null) }

    // Personas disponibles (mismo rol)
    val availablePeople = remember(allShifts, request.requesterRole) {
        allShifts
            .filter { ShiftRulesEngine.areRolesCompatible(request.requesterRole, it.userRole) && it.userId != currentUserId }
            .map { it.userName }
            .distinct()
            .sorted()
    }

    // LÓGICA DE FILTRADO UNIFICADA
    val filteredShifts = remember(allShifts, request, currentUserId, filterDate, filterPerson, filterShiftType) {
        val today = LocalDate.now()
        val myDate = LocalDate.parse(request.requesterShiftDate)

        val mySimulatedSchedule = currentUserSchedule.filterKeys { it != myDate }

        allShifts.filter { shift ->
            val isFuture = !shift.date.isBefore(today)
            val isCompatibleRole = ShiftRulesEngine.areRolesCompatible(request.requesterRole, shift.userRole)
            val isNotMe = shift.userId != currentUserId

            val isSameExactShift = (
                    shift.date == myDate &&
                            shift.shiftName.trim().equals(request.requesterShiftName.trim(), ignoreCase = true)
                    )

            if (!isFuture || !isCompatibleRole || !isNotMe || isSameExactShift) return@filter false

            if (filterDate != null && shift.date != filterDate) return@filter false
            if (filterPerson != null && shift.userName != filterPerson) return@filter false
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
                    Text("Candidatos Compatibles", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Turno del: ${request.requesterShiftDate}", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                }
            }
        }

        // --- BARRA DE FILTROS (LazyRow) ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // 1. FECHA
            item {
                FilterChip(
                    selected = filterDate != null,
                    onClick = { showDatePicker = true },
                    label = { Text(if(filterDate != null) filterDate.toString() else "Fecha") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp)) },
                    trailingIcon = if (filterDate != null) {
                        { Icon(Icons.Default.Close, "Borrar", Modifier.size(16.dp).clickable { filterDate = null }) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                )
            }

            // 2. PERSONA
            item {
                Box {
                    FilterChip(
                        selected = filterPerson != null,
                        onClick = { showPersonMenu = true },
                        label = { Text(filterPerson ?: "Persona") },
                        leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterPerson != null) {
                            { Icon(Icons.Default.Close, "Borrar", Modifier.size(16.dp).clickable { filterPerson = null }) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showPersonMenu, onDismissRequest = { showPersonMenu = false }) {
                        availablePeople.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { filterPerson = name; showPersonMenu = false }
                            )
                        }
                    }
                }
            }

            // 3. TURNO
            item {
                Box {
                    FilterChip(
                        selected = filterShiftType != null,
                        onClick = { showShiftMenu = true },
                        label = { Text(filterShiftType ?: "Turno") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterShiftType != null) {
                            { Icon(Icons.Default.Close, "Borrar", Modifier.size(16.dp).clickable { filterShiftType = null }) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showShiftMenu, onDismissRequest = { showShiftMenu = false }) {
                        listOf("Mañana", "Tarde", "Noche").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { filterShiftType = type; showShiftMenu = false }
                            )
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
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (filteredShifts.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron compañeros disponibles con estos filtros.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            } else {
                items(filteredShifts) { shift ->
                    PlantShiftCard(
                        shift = shift,
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
        val myDate = LocalDate.parse(request.requesterShiftDate)

        val mySchedule = currentUserSchedule
        val theirSchedule = userSchedules[candidate.userId] ?: emptyMap()

        SchedulePreviewDialog(
            onDismiss = { showPreviewDialog = false },
            mySchedule = mySchedule,
            theirSchedule = theirSchedule,
            theirName = candidate.userName,
            dateImGiving = myDate,
            shiftImGiving = request.requesterShiftName,
            dateImTaking = candidate.date,
            shiftImTaking = candidate.shiftName
        )
    }
}

// --- 4. TARJETA DE TURNO ---

@Composable
fun PlantShiftCard(
    shift: PlantShift,
    onAction: () -> Unit,
    onPreview: () -> Unit // Callback para preview
) {
    val shiftColor = getShiftColor(shift.shiftName)

    val initials = shift.userName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(shiftColor)
            )

            Column(
                modifier = Modifier
                    .padding(8.dp) // Reducimos padding
                    .fillMaxWidth()
            ) {
                // Fila Superior: Avatar + Nombre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp) // Avatar más pequeño
                            .background(Color(0xFF334155), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shift.userName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shift.userRole,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila Inferior: Info Turno + Botones
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Info Turno
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${shift.date} • ${shift.shiftName}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botones (Compactos)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPreview,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF54C7EC)),
                            border = BorderStroke(1.dp, Color(0xFF54C7EC)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Preview", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Elegir", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOGO DE PREVIEW (MEJORADO - DOBLE SEMANA Y SALIENTES) ---

@Composable
fun SchedulePreviewDialog(
    onDismiss: () -> Unit,
    mySchedule: Map<LocalDate, String>,
    theirSchedule: Map<LocalDate, String>,
    theirName: String,
    dateImGiving: LocalDate?,
    shiftImGiving: String?,
    dateImTaking: LocalDate,
    shiftImTaking: String
) {
    // Calculamos los rangos a mostrar basados en las fechas relevantes
    // Rango 1: Alrededor de la fecha que TOMO
    val rangesToShow = remember(dateImTaking, dateImGiving) {
        val ranges = mutableListOf<List<LocalDate>>()

        // Rango para la fecha que TOMO
        val takeStart = dateImTaking.minusDays(3)
        val takeRange = (0..6).map { takeStart.plusDays(it.toLong()) }
        ranges.add(takeRange)

        // Rango para la fecha que DOY (si existe)
        if (dateImGiving != null) {
            val giveStart = dateImGiving.minusDays(3)
            val giveRange = (0..6).map { giveStart.plusDays(it.toLong()) }

            // Si los rangos se solapan significativamente o son cercanos, los fusionamos?
            // Por simplicidad y claridad, si están muy lejos, mostramos dos bloques.
            // Si están cerca (menos de 7 días de diferencia), fusionamos en una sola vista.
            val daysDiff = ChronoUnit.DAYS.between(dateImTaking, dateImGiving)
            if (kotlin.math.abs(daysDiff) > 7) {
                ranges.add(giveRange)
            } else {
                // Fusionar: Crear un rango que cubra ambos + margen
                val minDate = if (dateImTaking.isBefore(dateImGiving)) dateImTaking else dateImGiving
                val maxDate = if (dateImTaking.isAfter(dateImGiving)) dateImTaking else dateImGiving
                val mergedStart = minDate.minusDays(3)
                val mergedEnd = maxDate.plusDays(3)
                val daysCount = ChronoUnit.DAYS.between(mergedStart, mergedEnd) + 1
                val mergedRange = (0 until daysCount).map { mergedStart.plusDays(it) }

                ranges.clear()
                ranges.add(mergedRange)
            }
        }

        // Ordenar cronológicamente
        ranges.sortBy { it.first() }
        ranges
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("Simulación de Cambio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            // Usamos verticalScroll para soportar múltiples rangos si es necesario
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

                rangesToShow.forEachIndexed { index, days ->
                    // Separador si hay más de un bloque
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color.White.copy(0.2f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Título del rango
                    val start = days.first()
                    val end = days.last()
                    val headerText = "Del ${start.dayOfMonth} al ${end.dayOfMonth} de ${start.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).lowercase()}"

                    Text(
                        text = headerText,
                        color = Color(0xFF54C7EC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Componente reutilizable para pintar el rango
                    ScheduleWeekView(
                        days = days,
                        mySchedule = mySchedule,
                        theirSchedule = theirSchedule,
                        theirName = theirName,
                        dateImGiving = dateImGiving,
                        shiftImGiving = shiftImGiving,
                        dateImTaking = dateImTaking,
                        shiftImTaking = shiftImTaking
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun ScheduleWeekView(
    days: List<LocalDate>,
    mySchedule: Map<LocalDate, String>,
    theirSchedule: Map<LocalDate, String>,
    theirName: String,
    dateImGiving: LocalDate?,
    shiftImGiving: String?,
    dateImTaking: LocalDate,
    shiftImTaking: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera Días (Scroll horizontal si son muchos días)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Columna fija para nombres? No, aquí scrollamos todo junto mejor
            // O mejor, el row de nombres es fijo y el contenido scrollea?
            // Para rangos cortos (+/-3 días = 7 días) cabe en pantalla.
            // Para rangos fusionados puede ser más largo.

            // Layout tabla
            Column {
                // Header Dias
                Row {
                    Spacer(modifier = Modifier.width(50.dp)) // Espacio para nombre
                    days.forEach { date ->
                        val isTarget = date == dateImTaking || date == dateImGiving
                        val textColor = if (isTarget) Color(0xFF54C7EC) else Color.Gray

                        Column(
                            modifier = Modifier.width(40.dp), // Ancho fijo por celda
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).uppercase().take(1),
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${date.dayOfMonth}",
                                color = textColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.1f))

                // Fila Mía
                ScheduleRow(
                    label = "Yo",
                    days = days,
                    originalSchedule = mySchedule,
                    dateRemoved = dateImGiving,
                    dateAdded = dateImTaking,
                    shiftAdded = shiftImTaking
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fila Suya
                ScheduleRow(
                    label = theirName.split(" ").first().take(6),
                    days = days,
                    originalSchedule = theirSchedule,
                    dateRemoved = dateImTaking,
                    dateAdded = dateImGiving,
                    shiftAdded = shiftImGiving ?: ""
                )
            }
        }
    }
}

@Composable
fun ScheduleRow(
    label: String,
    days: List<LocalDate>,
    originalSchedule: Map<LocalDate, String>,
    dateRemoved: LocalDate?,
    dateAdded: LocalDate?,
    shiftAdded: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(50.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        days.forEach { date ->
            // --- 1. Calcular turno actual (Simulación) ---
            var shiftName = originalSchedule[date] ?: ""
            var isModified = false
            var isAdded = false
            var isRemoved = false

            if (date == dateRemoved) {
                shiftName = "" // Se quita
                isModified = true
                isRemoved = true
            }
            if (date == dateAdded) {
                shiftName = shiftAdded // Se añade
                isModified = true
                isAdded = true
            }

            // --- 2. Calcular estado "Saliente" (Mirando día anterior) ---
            val prevDate = date.minusDays(1)
            var prevShiftName = originalSchedule[prevDate] ?: ""
            var prevIsAdded = false
            var prevIsRemoved = false

            // Aplicar simulación también al día anterior para saber si HOY cambia el estado de saliente
            if (prevDate == dateRemoved) {
                prevShiftName = "" // Ayer se quitó el turno
                prevIsRemoved = true
            }
            if (prevDate == dateAdded) {
                prevShiftName = shiftAdded // Ayer se añadió turno
                prevIsAdded = true
            }

            // Estado actual del día (Saliente si ayer hubo noche y hoy no hay turno)
            val isSaliente = prevShiftName.contains("Noche", true) && shiftName.isEmpty()

            // Estado original del día (para detectar si perdimos un saliente)
            val originalPrevShift = originalSchedule[prevDate] ?: ""
            val wasSaliente = originalPrevShift.contains("Noche", true) && (originalSchedule[date] ?: "").isEmpty()

            // Detectar CAMBIOS en el estado de Saliente
            val isNewSaliente = isSaliente && prevIsAdded // Se añadió noche ayer -> Ganamos saliente hoy
            val isLostSaliente = wasSaliente && !isSaliente && prevIsRemoved // Se quitó noche ayer -> Perdimos saliente hoy

            val cellColor = when {
                isAdded -> Color(0xFF4CAF50) // Verde
                isRemoved -> Color(0xFFE91E63) // Rojo
                isNewSaliente -> Color(0xFF4CAF50) // Verde (Ganamos Saliente)
                isLostSaliente -> Color(0xFFE91E63) // Rojo (Perdimos Saliente)
                shiftName.isNotEmpty() -> Color(0xFF1E293B) // Azul oscuro (normal)
                isSaliente -> Color(0xFF1E293B) // Azul oscuro (Igual que M/T/N para "S")
                else -> Color.Transparent // Libre (Guión)
            }

            // Texto corto para turno (M, T, N, S, "-")
            // VISUALIZACIÓN MEDIA JORNADA (MM / MT)
            val text = when {
                shiftName.contains("Media", true) && shiftName.contains("Mañana", true) -> "MM"
                shiftName.contains("Media", true) && shiftName.contains("Tarde", true) -> "MT"
                shiftName.contains("Mañana", true) -> "M"
                shiftName.contains("Tarde", true) -> "T"
                shiftName.contains("Noche", true) -> "N"
                shiftName.isNotEmpty() -> shiftName.take(1)
                isSaliente -> "S"
                else -> "-" // Guión para días libres
            }

            // Color del texto
            val textColor = when {
                // Para celdas con fondo de color (cambios), usamos blanco para contraste
                isAdded || isRemoved || isNewSaliente || isLostSaliente -> Color.White
                // Para días sin turno (S o -), usamos Gris para que se vea "desactivado" o secundario
                // text == "S" || text == "-" -> Color.Gray // ANTES
                text == "-" -> Color.Gray // El guion sigue en gris
                else -> Color.White // "S", "M", "T", "N" en blanco
            }

            Box(
                modifier = Modifier
                    .width(40.dp) // Ancho fijo celda
                    .height(30.dp)
                    .padding(1.dp)
                    .background(cellColor, RoundedCornerShape(4.dp))
                    .border(
                        if(isModified || isNewSaliente || isLostSaliente) 1.dp else 0.dp,
                        if(isModified || isNewSaliente || isLostSaliente) Color.White else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// --- 5. TABS Y CALENDARIO (EXISTENTE) ---

@Composable
fun MyShiftsCalendarTab(
    shifts: List<MyShiftDisplay>,
    onSelectShiftForChange: (MyShiftDisplay) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val shiftsMap = remember(shifts) { shifts.associateBy { it.fullDate } }

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
                    Text("${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).uppercase()} ${currentMonth.year}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
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
                                    val color = getShiftColor(shift?.shiftName)
                                    Box(modifier = Modifier.weight(1f).height(48.dp).padding(2.dp).background(if(shift != null) color else Color.Transparent, CircleShape).border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f), CircleShape).clickable { selectedDate = date }, contentAlignment = Alignment.Center) {
                                        Text("$dayIndex", color = if (shift != null) Color.White else Color(0xFFAAAAAA), fontWeight = if(shift!=null) FontWeight.Bold else FontWeight.Normal, fontSize = 18.sp)
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
                Text(text = date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))).replaceFirstChar { it.uppercase() }, color = Color(0xFF54C7EC), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (shift != null) {
                    Text(text = "Turno actual: ${shift.shiftName}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onSelectShiftForChange(shift) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Default.SwapHoriz, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUSCAR CAMBIO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else { Text("No tienes turno este día.", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun ShiftManagementTab(
    currentUserId: String,
    isSupervisor: Boolean,
    myRequests: List<ShiftChangeRequest>,
    incomingRequests: List<ShiftChangeRequest> = emptyList(), // Deprecated but kept for compatibility if needed
    supervisorRequests: List<ShiftChangeRequest>,
    inProgressRequests: List<ShiftChangeRequest> = emptyList(),
    onDeleteRequest: (ShiftChangeRequest) -> Unit,
    onAcceptByPartner: (ShiftChangeRequest) -> Unit,
    onRejectByPartner: (ShiftChangeRequest) -> Unit,
    onApproveBySupervisor: (ShiftChangeRequest) -> Unit,
    onRejectBySupervisor: (ShiftChangeRequest) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // --- SECCIÓN SUPERVISOR: PENDIENTES DE APROBACIÓN ---
        if (isSupervisor && supervisorRequests.isNotEmpty()) {
            item { Text("Pendientes de Aprobación (Supervisor)", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold) }
            items(supervisorRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x22E91E63))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Confirmación de Cambio", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        // Mostrar detalles del cambio completo (A <-> B)
                        Text("${req.requesterName} (${req.requesterShiftDate})", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("${req.targetUserName} (${req.targetShiftDate})", color = Color(0xCCFFFFFF), fontSize = 13.sp)

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onRejectBySupervisor(req) }) {
                                Text("Rechazar", color = Color(0xFFFFB4AB))
                            }
                            Button(
                                onClick = { onApproveBySupervisor(req) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Aprobar Cambio")
                            }
                        }
                    }
                }
            }
        }

        // --- SECCIÓN CAMBIOS EN PROCESO (UNIFICADA) ---
        // Aquí salen tanto los que yo espero como los que esperan por mí
        if (inProgressRequests.isNotEmpty()) {
            item { Text("Cambios en Proceso", color = Color(0xFFFFA000), fontWeight = FontWeight.Bold) }
            items(inProgressRequests) { req ->
                val amITarget = req.targetUserId == currentUserId
                val containerColor = if (amITarget) Color(0x224CAF50) else Color(0x22FFA000)

                Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (amITarget) {
                                // Si soy el destino, ES UN AVISO DE ACCIÓN
                                Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("¡Te han propuesto un cambio!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                // Si soy el origen, ES UN AVISO DE ESPERA
                                Icon(Icons.Default.Refresh, null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Esperando respuesta...", color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Propuesta de: ${req.requesterName}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Cambio por: ${req.targetUserName ?: "Sin asignar"} (${req.targetShiftDate})", color = Color(0xCCFFFFFF), fontSize = 13.sp)

                        if (amITarget) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onRejectByPartner(req) },
                                    border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB))
                                ) {
                                    Text("Rechazar")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onAcceptByPartner(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                                ) {
                                    Text("Aceptar")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECCIÓN MIS SOLICITUDES (Borradores / Buscando) ---
        item {
            Text("Mis Solicitudes (${myRequests.size})", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }
        if (myRequests.isEmpty()) {
            item { Text("No tienes solicitudes activas.", color = Color.Gray) }
        } else {
            items(myRequests) { req ->
                val statusColor = when(req.status) {
                    RequestStatus.SEARCHING -> Color(0xFFFFA000)
                    RequestStatus.PENDING_PARTNER -> Color(0xFF2196F3)
                    RequestStatus.AWAITING_SUPERVISOR -> Color(0xFFE91E63)
                    RequestStatus.APPROVED -> Color(0xFF4CAF50)
                    RequestStatus.REJECTED -> Color.Red
                    else -> Color.Gray
                }
                val statusText = when(req.status) {
                    RequestStatus.SEARCHING -> "Buscando candidato..."
                    RequestStatus.PENDING_PARTNER -> "Esperando a ${req.targetUserName ?: "compañero"}"
                    RequestStatus.AWAITING_SUPERVISOR -> "Esperando Supervisor"
                    else -> req.status.name
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = statusColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            // Solo permitir borrar si no está aprobada/rechazada final
                            if (req.status != RequestStatus.APPROVED && req.status != RequestStatus.REJECTED) {
                                IconButton(onClick = { onDeleteRequest(req) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFFFB4AB))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Sueltas: ${req.requesterShiftDate} (${req.requesterShiftName})", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- AGREGAR AL FINAL DE ShiftChangeScreen.kt ---

@Composable
fun MyRequestsForSuggestionsTab(
    myRequests: List<ShiftChangeRequest>,
    onSeeCandidates: (ShiftChangeRequest) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(
                "Busca candidatos para tus turnos",
                color = Color(0xFF54C7EC),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Selecciona uno de tus turnos ofertados para ver quién puede cambiártelo.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        if (myRequests.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text("No tienes ofertas activas.", color = Color.Gray)
                    Text("Crea una en 'Mis Turnos' primero.", color = Color.Gray, fontSize = 12.sp)
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
                            Text("Ofreces el turno del:", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            Text("${req.requesterShiftDate} (${req.requesterShiftName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Badge(containerColor = Color(0xFF54C7EC), contentColor = Color.Black) { Text("ACTIVA") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSeeCandidates(req) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VER CANDIDATOS COMPATIBLES")
                    }
                }
            }
        }
    }
}

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
        title = { Text("Solicitar Cambio Abierto", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x2254C7EC)),
                    border = BorderStroke(1.dp, Color(0x4454C7EC))
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vas a ofrecer tu turno del:", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                        Text("${shift.date} (${shift.shiftName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.1f))
                Column {
                    Text("Preferencia de Cambio:", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == RequestMode.FLEXIBLE,
                            onClick = { selectedMode = RequestMode.FLEXIBLE },
                            label = { Text("Flexible") },
                            leadingIcon = if (selectedMode == RequestMode.FLEXIBLE) {{ Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }} else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == RequestMode.STRICT,
                            onClick = { selectedMode = RequestMode.STRICT },
                            label = { Text("Estricto") },
                            leadingIcon = if (selectedMode == RequestMode.STRICT) {{ Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }} else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if(selectedMode == RequestMode.FLEXIBLE) "Se buscará cualquier compañero compatible." else "Se priorizará a quienes te deben turnos.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(emptyList(), selectedMode) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PUBLICAR SOLICITUD ABIERTA")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    )
}

fun getShiftColor(shiftName: String?): Color {
    if (shiftName == null) return Color.Transparent
    val name = shiftName.lowercase()
    return when {
        name.contains("noche") -> Color(0xFF9C27B0)
        name.contains("mañana") -> Color(0xFFFFA500)
        name.contains("tarde") -> Color(0xFF2196F3)
        name.contains("vacaciones") -> Color(0xFFE91E63)
        else -> Color(0xFF4CAF50)
    }
}

// --- LÓGICA DE NEGOCIO ---

fun performDirectProposal(
    database: FirebaseDatabase,
    plantId: String,
    myReq: ShiftChangeRequest,
    targetShift: PlantShift,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    val updates = mapOf(
        "plants/$plantId/shift_requests/${myReq.id}/status" to RequestStatus.PENDING_PARTNER,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserId" to targetShift.userId,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserName" to targetShift.userName,
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftDate" to targetShift.date.toString(),
        "plants/$plantId/shift_requests/${myReq.id}/targetShiftName" to targetShift.shiftName
    )

    database.reference.updateChildren(updates).addOnSuccessListener {
        onSaveNotification(
            targetShift.userId,
            "SHIFT_PROPOSAL",
            "${myReq.requesterName} te propone cambiar tu turno del ${targetShift.date} por el suyo del ${myReq.requesterShiftDate}.",
            "ShiftChangeScreen",
            myReq.id,
            {}
        )
    }
}

fun rejectSwapProposal(
    database: FirebaseDatabase,
    plantId: String,
    requestId: String
) {
    val updates = mapOf(
        "plants/$plantId/shift_requests/$requestId/status" to RequestStatus.SEARCHING,
        "plants/$plantId/shift_requests/$requestId/targetUserId" to null,
        "plants/$plantId/shift_requests/$requestId/targetUserName" to null,
        "plants/$plantId/shift_requests/$requestId/targetShiftDate" to null,
        "plants/$plantId/shift_requests/$requestId/targetShiftName" to null
    )
    database.reference.updateChildren(updates)
}

fun updateRequestStatus(
    database: FirebaseDatabase,
    plantId: String,
    requestId: String,
    newStatus: RequestStatus
) {
    database.reference.child("plants/$plantId/shift_requests/$requestId/status").setValue(newStatus)
}

fun approveSwapBySupervisor(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
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

                    // Helper para obtener datos completos del slot (path y si es media jornada)
                    data class SlotInfo(val path: String, val isHalfDay: Boolean, val fullSlotKey: String, val group: String)

                    fun findSlotInfo(snapshot: DataSnapshot, shiftName: String, userName: String): SlotInfo? {
                        // Limpiamos "Media " si viene de la UI para buscar el nodo real
                        val realShiftName = shiftName.replace("Media ", "").trim()
                        val shiftRef = snapshot.child(realShiftName)
                        val groups = listOf("nurses", "auxiliaries")

                        for (group in groups) {
                            shiftRef.child(group).children.forEach { slot ->
                                val p = slot.child("primary").value.toString()
                                val s = slot.child("secondary").value.toString()
                                val half = slot.child("halfDay").value as? Boolean == true

                                if (p == userName) {
                                    return SlotInfo("$realShiftName/$group/${slot.key}/primary", half, slot.key!!, group)
                                }
                                if (s == userName) {
                                    return SlotInfo("$realShiftName/$group/${slot.key}/secondary", half, slot.key!!, group)
                                }
                            }
                        }
                        return null
                    }

                    val infoA = findSlotInfo(snapA, req.requesterShiftName, req.requesterName)
                    val infoB = findSlotInfo(snapB, req.targetShiftName!!, req.targetUserName!!)

                    if (infoA != null && infoB != null) {
                        val updates = mutableMapOf<String, Any?>()

                        // LOGICA MEDIA JORNADA -> COMPLETA (Y VICEVERSA)
                        if (infoA.isHalfDay && !infoB.isHalfDay) {
                            // A (Media) toma el turno de B (Completo). El turno de A se vuelve completo y B lo ocupa.
                            val partsA = infoA.path.split("/") // Turno/Group/Key/Role
                            val basePathA = "plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${partsA[0]}/${partsA[1]}/${partsA[2]}"

                            updates["$basePathA/primary"] = req.targetUserName
                            updates["$basePathA/secondary"] = "" // LIBERAR COMPAÑERO DE MEDIA JORNADA
                            updates["$basePathA/halfDay"] = false

                            updates["plants/$plantId/turnos/turnos-${req.targetShiftDate}/${infoB.path}"] = req.requesterName

                        } else if (!infoA.isHalfDay && infoB.isHalfDay) {
                            // A (Completo) toma el turno de B (Media). El turno de B se vuelve completo y A lo ocupa.
                            val partsB = infoB.path.split("/")
                            val basePathB = "plants/$plantId/turnos/turnos-${req.targetShiftDate}/${partsB[0]}/${partsB[1]}/${partsB[2]}"

                            updates["$basePathB/primary"] = req.requesterName
                            updates["$basePathB/secondary"] = "" // LIBERAR COMPAÑERO DE MEDIA JORNADA
                            updates["$basePathB/halfDay"] = false

                            updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${infoA.path}"] = req.targetUserName

                        } else {
                            // Cambio Normal (Completo <-> Completo o Media <-> Media)
                            updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${infoA.path}"] = req.targetUserName
                            updates["plants/$plantId/turnos/turnos-${req.targetShiftDate}/${infoB.path}"] = req.requesterName
                        }

                        updates["plants/$plantId/shift_requests/${req.id}/status"] = RequestStatus.APPROVED

                        database.reference.updateChildren(updates)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onError(it.message ?: "Error al guardar") }
                    } else {
                        onError("No se encontraron los turnos originales (quizás cambiaron).")
                    }
                }
                override fun onCancelled(e: DatabaseError) { onError(e.message) }
            })
        }
        override fun onCancelled(e: DatabaseError) { onError(e.message) }
    })
}

fun deleteShiftRequest(database: FirebaseDatabase, plantId: String, requestId: String) {
    database.getReference("plants/$plantId/shift_requests/$requestId").removeValue()
}