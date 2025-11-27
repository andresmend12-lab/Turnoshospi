package com.example.turnoshospi

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// --- 1. MODELOS DE DATOS ---

enum class RequestStatus {
    SEARCHING,       // Buscando cambio
    MATCH_FOUND,     // Match encontrado
    PENDING_APPROVAL,// Esperando supervisor
    APPROVED,        // Aprobado
    REJECTED         // Rechazado
}

data class ShiftChangeRequest(
    val id: String = "",
    val plantId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterRole: String = "",
    val originalDate: String = "", // YYYY-MM-DD
    val originalShift: String = "",
    val offeredDates: List<String> = emptyList(),
    val status: RequestStatus = RequestStatus.SEARCHING,
    val timestamp: Long = System.currentTimeMillis()
)

// Modelo auxiliar para mostrar turnos en la lista
data class MyShiftDisplay(
    val date: String,
    val shiftName: String,
    val fullDate: LocalDate
)

// --- 2. VALIDADOR DE UI (Puente con ShiftRulesEngine) ---

object UILogicValidator {
    fun validatePreRequest(
        userRole: String,
        requestDate: LocalDate,
        shiftName: String,
        currentShifts: List<MyShiftDisplay>
    ): String? {
        // Regla 1: Rol
        if (!ShiftRulesEngine.canUserParticipate(userRole)) {
            return "Tu rol ($userRole) no permite solicitar cambios de turno."
        }

        // Regla 2: Turno válido
        val hasShift = currentShifts.any { it.fullDate == requestDate && it.shiftName == shiftName }
        // Simulamos isShiftBlocked como false por ahora
        return ShiftRulesEngine.validateCreateRequest(requestDate, hasShift, false, listOf("temp"))
            ?.replace("Debes ofrecer al menos un día alternativo para trabajar.", "")
    }
}

// --- 3. PANTALLA PRINCIPAL ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftChangeScreen(
    plantId: String,
    currentUser: UserProfile?,
    currentUserId: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis Turnos", "Solicitudes")
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")

    // Estado para los turnos del usuario
    val myShifts = remember { mutableStateListOf<MyShiftDisplay>() }
    var isLoadingShifts by remember { mutableStateOf(true) }

    // Estado para las solicitudes activas
    val activeRequests = remember { mutableStateListOf<ShiftChangeRequest>() }

    // Estado para el diálogo
    var showCreateRequestDialog by remember { mutableStateOf(false) }
    var selectedShiftForRequest by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Cargar turnos del usuario desde Firebase
    LaunchedEffect(plantId, currentUser) {
        if (currentUser == null) return@LaunchedEffect

        // 1. Cargar Turnos
        val turnosRef = database.getReference("plants/$plantId/turnos")

        turnosRef.limitToLast(60).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                myShifts.clear()
                val today = LocalDate.now()
                val userName = "${currentUser.firstName} ${currentUser.lastName}".trim()

                snapshot.children.forEach { dateSnapshot ->
                    val dateKey = dateSnapshot.key?.removePrefix("turnos-") ?: return@forEach
                    try {
                        val date = LocalDate.parse(dateKey)
                        // Cargamos historial reciente para validar reglas laborales, aunque solo mostramos futuros
                        dateSnapshot.children.forEach { shiftSnapshot ->
                            val shiftName = shiftSnapshot.key ?: ""
                            val nurses = shiftSnapshot.child("nurses").children
                            val auxs = shiftSnapshot.child("auxiliaries").children

                            val isNurse = nurses.any { it.child("primary").value.toString().equals(userName, ignoreCase = true) || it.child("secondary").value.toString().equals(userName, ignoreCase = true) }
                            val isAux = auxs.any { it.child("primary").value.toString().equals(userName, ignoreCase = true) || it.child("secondary").value.toString().equals(userName, ignoreCase = true) }

                            if (isNurse || isAux) {
                                myShifts.add(MyShiftDisplay(dateKey, shiftName, date))
                            }
                        }
                    } catch (e: Exception) { /* Ignorar fechas mal formadas */ }
                }
                myShifts.sortBy { it.fullDate }
                isLoadingShifts = false
            }
            override fun onCancelled(error: DatabaseError) { isLoadingShifts = false }
        })

        // 2. Cargar Solicitudes Activas
        val requestsRef = database.getReference("plants/$plantId/shift_requests")
        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activeRequests.clear()
                snapshot.children.mapNotNull { it.getValue(ShiftChangeRequest::class.java) }
                    .filter { it.status != RequestStatus.REJECTED && it.status != RequestStatus.APPROVED }
                    .forEach { activeRequests.add(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Cambio de Turnos", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.close_label), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                        text = {
                            Text(title, color = if (selectedTab == index) Color.White else Color(0xAAFFFFFF), fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (selectedTab == 0) {
                    val futureShifts = remember(myShifts) {
                        val today = LocalDate.now()
                        myShifts.filter { !it.fullDate.isBefore(today) }
                    }

                    MyShiftsTab(
                        shifts = futureShifts,
                        isLoading = isLoadingShifts,
                        onRequestChange = { date, shift ->
                            selectedShiftForRequest = date to shift
                            showCreateRequestDialog = true
                        }
                    )
                } else {
                    RequestsTab(activeRequests, currentUserId, currentUser?.role ?: "")
                }
            }
        }
    }

    // Diálogo de Creación
    if (showCreateRequestDialog && selectedShiftForRequest != null) {
        val (dateStr, shiftName) = selectedShiftForRequest!!
        val requestDate = LocalDate.parse(dateStr)

        // Validación inicial usando el nuevo Engine
        val error = UILogicValidator.validatePreRequest(
            currentUser?.role ?: "",
            requestDate,
            shiftName,
            myShifts
        )

        if (error != null) {
            AlertDialog(
                onDismissRequest = { showCreateRequestDialog = false },
                title = { Text("No permitido") },
                text = { Text(error) },
                confirmButton = { TextButton(onClick = { showCreateRequestDialog = false }) { Text("Entendido") } },
                containerColor = Color(0xFF0F172A),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        } else {
            CreateChangeRequestDialog(
                shiftDate = dateStr,
                shiftName = shiftName,
                onDismiss = { showCreateRequestDialog = false },
                onConfirm = { offeredDates ->
                    if (offeredDates.isEmpty()) {
                        Toast.makeText(context, "Debes seleccionar al menos una fecha alternativa", Toast.LENGTH_LONG).show()
                        return@CreateChangeRequestDialog
                    }

                    val reqId = database.getReference("plants/$plantId/shift_requests").push().key ?: UUID.randomUUID().toString()
                    val newRequest = ShiftChangeRequest(
                        id = reqId,
                        plantId = plantId,
                        requesterId = currentUserId,
                        requesterName = "${currentUser?.firstName} ${currentUser?.lastName}",
                        requesterRole = currentUser?.role ?: "",
                        originalDate = dateStr,
                        originalShift = shiftName,
                        offeredDates = offeredDates,
                        status = RequestStatus.SEARCHING
                    )

                    database.getReference("plants/$plantId/shift_requests/$reqId").setValue(newRequest)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Solicitud enviada correctamente", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al enviar: ${it.message}", Toast.LENGTH_SHORT).show()
                        }

                    showCreateRequestDialog = false
                }
            )
        }
    }
}

@Composable
fun MyShiftsTab(
    shifts: List<MyShiftDisplay>,
    isLoading: Boolean,
    onRequestChange: (String, String) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF54C7EC))
        }
    } else if (shifts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tienes turnos asignados próximamente.", color = Color(0xCCFFFFFF))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Selecciona un turno para librar:", color = Color(0xCCFFFFFF), fontSize = 14.sp)
            }
            items(shifts) { shift ->
                ShiftCard(
                    date = shift.date,
                    shiftName = shift.shiftName,
                    actionLabel = "Buscar Cambio",
                    isDestructive = false,
                    onClick = { onRequestChange(shift.date, shift.shiftName) }
                )
            }
        }
    }
}

@Composable
fun RequestsTab(requests: List<ShiftChangeRequest>, currentUserId: String, currentUserRole: String) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay solicitudes activas.", color = Color(0xCCFFFFFF))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requests) { req ->
                // Regla 1: Visualización - Solo mostrar compatibles o propias
                val isCompatibleRole = ShiftRulesEngine.areRolesCompatible(currentUserRole, req.requesterRole)
                val isMine = req.requesterId == currentUserId

                if (isCompatibleRole || isMine) {
                    RequestCard(
                        request = req,
                        isMine = isMine,
                        canAccept = isCompatibleRole && !isMine
                    )
                }
            }
        }
    }
}

@Composable
fun ShiftCard(date: String, shiftName: String, actionLabel: String, isDestructive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(date, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(shiftName, color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color(0x33FFB4AB) else Color(0xFF54C7EC),
                    contentColor = if (isDestructive) Color(0xFFFFB4AB) else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RequestCard(request: ShiftChangeRequest, isMine: Boolean, canAccept: Boolean) {
    val statusColor = when (request.status) {
        RequestStatus.SEARCHING -> Color(0xFFFFA000)
        RequestStatus.MATCH_FOUND -> Color(0xFF54C7EC)
        else -> Color.Gray
    }

    val statusText = when (request.status) {
        RequestStatus.SEARCHING -> if (isMine) "Buscando..." else "Busca cambio"
        RequestStatus.MATCH_FOUND -> "Match encontrado"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        border = BorderStroke(1.dp, if (isMine) Color(0x6654C7EC) else Color(0x33FFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, null, tint = statusColor)
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isMine) "Tu Solicitud" else request.requesterName, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(statusText, color = statusColor, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quiere librar: ${request.originalDate} (${request.originalShift})", color = Color(0xCCFFFFFF))

            if (request.offeredDates.isNotEmpty()) {
                Text("Ofrece trabajar: ${request.offeredDates.joinToString(", ")}", color = Color(0xAAFFFFFF), style = MaterialTheme.typography.bodySmall)
            }

            if (!isMine && request.status == RequestStatus.SEARCHING) {
                Spacer(modifier = Modifier.height(12.dp))
                if (canAccept) {
                    Button(
                        onClick = { /* Lógica futura de match con ShiftRulesEngine */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Text("Ver compatibilidad")
                    }
                } else {
                    Text(
                        "No disponible para tu rol",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChangeRequestDialog(
    shiftDate: String,
    shiftName: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val offeredDates = remember { mutableStateListOf<String>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val dateStr = date.toString()
                        if (dateStr !in offeredDates && dateStr != shiftDate) {
                            offeredDates.add(dateStr)
                        }
                    }
                    showDatePicker = false
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Cambio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vas a solicitar librar:", color = Color.White)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x33000000))) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("$shiftDate ($shiftName)", fontWeight = FontWeight.Bold, color = Color(0xFF54C7EC))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(0.1f))

                Text("Días que ofreces trabajar (Obligatorio):", color = Color.White)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    offeredDates.forEach { date ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(date, color = Color.White, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Close, "Eliminar", tint = Color(0xFFFFB4AB), modifier = Modifier.clickable { offeredDates.remove(date) }.size(16.dp))
                        }
                    }
                }

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir día disponible")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(offeredDates.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
            ) {
                Text("Enviar Solicitud")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}