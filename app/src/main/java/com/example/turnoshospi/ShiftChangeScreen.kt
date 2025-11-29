package com.example.turnoshospi

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

// --- 1. MODELOS DE DATOS ---

enum class RequestType { COVERAGE, SWAP }
enum class RequestMode { STRICT, FLEXIBLE }
enum class RequestStatus { DRAFT, SEARCHING, PENDING_PARTNER, AWAITING_SUPERVISOR, APPROVED, REJECTED }

data class ShiftChangeRequest(
    val id: String = "",
    val type: RequestType = RequestType.SWAP,
    val status: RequestStatus = RequestStatus.SEARCHING,
    val mode: RequestMode = RequestMode.FLEXIBLE,
    val hardnessLevel: ShiftRulesEngine.Hardness = ShiftRulesEngine.Hardness.NORMAL,

    // Solicitante
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterRole: String = "",
    val requesterShiftDate: String = "", // YYYY-MM-DD
    val requesterShiftName: String = "",

    // Configuración
    val offeredDates: List<String> = emptyList(), // Fechas que acepto a cambio
    val targetUserId: String? = null, // Para cambios directos
    val timestamp: Long = System.currentTimeMillis()
)

// Helper para UI
data class MyShiftDisplay(
    val date: String,
    val shiftName: String,
    val fullDate: LocalDate,
    val isHalfDay: Boolean = false
)

// --- 2. PANTALLA PRINCIPAL ---

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
    val tabs = listOf("Mis Turnos", "Sugerencias")
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")

    // Estados
    val myShiftsMap = remember { mutableStateMapOf<LocalDate, String>() } // Para el Engine
    val myShiftsList = remember { mutableStateListOf<MyShiftDisplay>() }  // Para UI
    val allActiveRequests = remember { mutableStateListOf<ShiftChangeRequest>() }
    var isLoading by remember { mutableStateOf(true) }

    // Diálogos
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedShiftForRequest by remember { mutableStateOf<MyShiftDisplay?>(null) }

    // Cargar Datos
    LaunchedEffect(plantId, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        val userName = "${currentUser.firstName} ${currentUser.lastName}".trim()

        // 1. Cargar Turnos Propios (y construir mapa para validaciones)
        database.getReference("plants/$plantId/turnos").limitToLast(90)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    myShiftsMap.clear()
                    myShiftsList.clear()

                    snapshot.children.forEach { dateSnapshot ->
                        val dateKey = dateSnapshot.key?.removePrefix("turnos-") ?: return@forEach
                        try {
                            val date = LocalDate.parse(dateKey)
                            dateSnapshot.children.forEach { shiftSnap ->
                                val shiftName = shiftSnap.key ?: ""
                                // Buscar si soy enfermero o auxiliar
                                val isNurse = shiftSnap.child("nurses").children.any {
                                    it.child("primary").value.toString().equals(userName, true)
                                }
                                val isAux = shiftSnap.child("auxiliaries").children.any {
                                    it.child("primary").value.toString().equals(userName, true)
                                }

                                if (isNurse || isAux) {
                                    myShiftsMap[date] = shiftName
                                    myShiftsList.add(MyShiftDisplay(dateKey, shiftName, date))
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    myShiftsList.sortBy { it.fullDate }
                    isLoading = false
                }
                override fun onCancelled(error: DatabaseError) { isLoading = false }
            })

        // 2. Cargar Solicitudes Globales (Mercado)
        database.getReference("plants/$plantId/shift_requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allActiveRequests.clear()
                    snapshot.children.mapNotNull { it.getValue(ShiftChangeRequest::class.java) }
                        .filter { it.status == RequestStatus.SEARCHING } // Solo las abiertas
                        .forEach { allActiveRequests.add(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Cambios", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

            // TABS
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
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CONTENIDO
            Box(modifier = Modifier
                .fillMaxSize()
                ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF54C7EC))
                } else {
                    when (selectedTab) {
                        0 -> MyShiftsCalendarTab(
                            shifts = myShiftsList,
                            onSelectShiftForChange = { shift ->
                                selectedShiftForRequest = shift
                                showCreateDialog = true
                            }
                        )
                        1 -> MatchSuggestionsTab(
                            currentUserId = currentUserId,
                            currentUserRole = currentUser?.role ?: "",
                            myRequests = allActiveRequests.filter { it.requesterId == currentUserId },
                            otherRequests = allActiveRequests.filter { it.requesterId != currentUserId },
                            mySchedule = myShiftsMap,
                            // Nota: En una app real, deberías traer el schedule de los candidatos bajo demanda.
                            // Aquí simularemos que validamos contra reglas locales básicas por falta de esos datos completos.
                            onRequestMatch = { myReq, otherReq ->
                                // Lógica para iniciar el "Swap"
                                performMatchProposal(database, plantId, myReq, otherReq, onSaveNotification)
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGO CREAR SOLICITUD
    if (showCreateDialog && selectedShiftForRequest != null) {
        val shift = selectedShiftForRequest!!
        // Usa el nuevo diálogo sin campo obligatorio de fechas.
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
                Toast.makeText(context, "Buscando cambio...", Toast.LENGTH_SHORT).show()
                showCreateDialog = false
            }
        )
    }
}

// --- 3. TAB 1: CALENDARIO DE MIS TURNOS (Mismo estilo que PlantCalendar) ---

@Composable
fun MyShiftsCalendarTab(
    shifts: List<MyShiftDisplay>,
    onSelectShiftForChange: (MyShiftDisplay) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Convertir lista a mapa para colorear fácil
    val shiftsMap = remember(shifts) { shifts.associateBy { it.fullDate } }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {

        // --- CALENDARIO VISUAL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).uppercase()} ${currentMonth.year}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                    }
                }

                // Días Semana
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                        Text(it, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }

                // Grid Días
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

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp) // Tamaño grande
                                            .padding(2.dp)
                                            .background(if(shift != null) color else Color.Transparent, CircleShape)
                                            .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f), CircleShape)
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayIndex",
                                            color = if (shift != null) Color.White else Color(0xFFAAAAAA),
                                            fontWeight = if(shift!=null) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 18.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- DETALLE DEL DÍA SELECCIONADO ---
        AnimatedVisibility(visible = selectedDate != null) {
            val date = selectedDate!!
            val shift = shiftsMap[date]

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))).replaceFirstChar { it.uppercase() },
                    color = Color(0xFF54C7EC),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (shift != null) {
                    Text(
                        text = "Turno actual: ${shift.shiftName}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectShiftForChange(shift) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUSCAR CAMBIO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("No tienes turno este día.", color = Color.Gray)
                }
            }
        }
    }
}

// --- 4. TAB 2: SUGERENCIAS (MATCHING) ---
// (Contenido de MatchSuggestionsTab igual)

@Composable
fun MatchSuggestionsTab(
    currentUserId: String,
    currentUserRole: String,
    myRequests: List<ShiftChangeRequest>,
    otherRequests: List<ShiftChangeRequest>,
    mySchedule: Map<LocalDate, String>,
    onRequestMatch: (ShiftChangeRequest, ShiftChangeRequest) -> Unit
) {
    // Algoritmo de Match en Cliente
    val suggestions = remember(myRequests, otherRequests) {
        val matches = mutableListOf<Pair<ShiftChangeRequest, ShiftChangeRequest>>() // Mi Petición -> Su Petición

        myRequests.forEach { myReq ->
            otherRequests.forEach { otherReq ->
                // Usamos el Engine con datos simulados del otro usuario (asumimos vacio para validacion estricta inicial)
                // En prod: necesitarías leer el horario del otro usuario.
                val isMatch = ShiftRulesEngine.checkMatch(
                    requesterRequest = myReq,
                    candidateRequest = otherReq,
                    requesterSchedule = mySchedule,
                    candidateSchedule = emptyMap() // TODO: Cargar horario del candidato
                )
                if (isMatch) {
                    matches.add(myReq to otherReq)
                }
            }
        }
        matches
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(
                "Tus Solicitudes Activas (${myRequests.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (myRequests.isEmpty()) {
            item { Text("No estás buscando cambios actualmente.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
        } else {
            items(myRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x22FFA000))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sueltas: ${req.requesterShiftDate} (${req.requesterShiftName})", color = Color.White, fontWeight = FontWeight.Bold)
                            // Texto que indica que acepta cualquier fecha
                            Text(
                                if(req.offeredDates.isEmpty()) "Busca: Cualquier turno compatible" else "Buscas: ${req.offeredDates.joinToString(", ")}",
                                color = Color.White.copy(0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFFFFA000))
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Oportunidades de Cambio (${suggestions.size})",
                color = Color(0xFF54C7EC),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (suggestions.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text("No hay coincidencias compatibles aún.", color = Color.Gray)
                }
            }
        } else {
            items(suggestions) { (myReq, otherReq) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x224CAF50)),
                    border = BorderStroke(1.dp, Color(0x444CAF50))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MATCH ENCONTRADO", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Badge { Text("${otherReq.hardnessLevel}") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Con ${otherReq.requesterName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Tú das:", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                                Text("${myReq.requesterShiftDate}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Tú recibes:", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                                Text("${otherReq.requesterShiftDate}", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("(${otherReq.requesterShiftName})", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onRequestMatch(myReq, otherReq) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SOLICITAR INTERCAMBIO")
                        }
                    }
                }
            }
        }
    }
}

// --- 5. DIÁLOGOS Y UTILS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShiftRequestDialog(
    shift: MyShiftDisplay,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, RequestMode) -> Unit
) {
    // Ya no necesitamos estado para offeredDates ni DatePicker
    var selectedMode by remember { mutableStateOf(RequestMode.FLEXIBLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("Solicitar Cambio Abierto", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tarjeta informativa del turno a soltar
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x2254C7EC)),
                    border = BorderStroke(1.dp, Color(0x4454C7EC))
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vas a ofrecer tu turno del:", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                        Text(
                            "${shift.date} (${shift.shiftName})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.1f))

                // Selección de Modo
                Column {
                    Text("Preferencia de Cambio:", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == RequestMode.FLEXIBLE,
                            onClick = { selectedMode = RequestMode.FLEXIBLE },
                            label = { Text("Flexible") },
                            leadingIcon = if (selectedMode == RequestMode.FLEXIBLE) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == RequestMode.STRICT,
                            onClick = { selectedMode = RequestMode.STRICT },
                            label = { Text("Estricto") }, // Estricto prioriza deudas
                            leadingIcon = if (selectedMode == RequestMode.STRICT) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if(selectedMode == RequestMode.FLEXIBLE)
                            "Se buscará cualquier compañero compatible."
                        else
                            "Se priorizará a quienes te deben turnos.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Enviamos una lista vacía para indicar "Cualquier fecha"
                    onConfirm(emptyList(), selectedMode)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PUBLICAR SOLICITUD ABIERTA")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    )
}

// Utils de Color (Adaptado de MainMenuScreen)
fun getShiftColor(shiftName: String?): Color {
    if (shiftName == null) return Color.Transparent
    val name = shiftName.lowercase()
    return when {
        name.contains("noche") -> Color(0xFF9C27B0) // Violeta
        name.contains("mañana") -> Color(0xFFFFA500) // Naranja
        name.contains("tarde") -> Color(0xFF2196F3)  // Azul
        name.contains("vacaciones") -> Color(0xFFE91E63) // Rosa
        else -> Color(0xFF4CAF50) // Verde default
    }
}

fun performMatchProposal(
    database: FirebaseDatabase,
    plantId: String,
    myReq: ShiftChangeRequest,
    otherReq: ShiftChangeRequest,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    // 1. Actualizar estado de AMBAS peticiones a PENDING_PARTNER
    val updates = mapOf(
        "plants/$plantId/shift_requests/${myReq.id}/status" to RequestStatus.PENDING_PARTNER,
        "plants/$plantId/shift_requests/${myReq.id}/targetUserId" to otherReq.requesterId,
        "plants/$plantId/shift_requests/${otherReq.id}/status" to RequestStatus.PENDING_PARTNER,
        "plants/$plantId/shift_requests/${otherReq.id}/targetUserId" to myReq.requesterId
    )

    database.reference.updateChildren(updates).addOnSuccessListener {
        // Notificar al otro usuario
        onSaveNotification(
            otherReq.requesterId,
            "SHIFT_MATCH_PROPOSAL",
            "${myReq.requesterName} quiere intercambiar su turno del ${myReq.requesterShiftDate} por el tuyo.",
            "ShiftChangeScreen",
            myReq.id,
            {}
        )
    }
}