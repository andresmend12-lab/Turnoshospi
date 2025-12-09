package com.example.turnoshospi

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
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
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftMarketplaceScreen(
    plantId: String,
    currentUserId: String,
    currentUserName: String,
    currentUserRole: String,
    shiftColors: ShiftColors,
    onBack: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val context = LocalContext.current

    // --- ESTADOS DE DATOS ---
    var rawMarketplaceRequests by remember { mutableStateOf<List<ShiftChangeRequest>>(emptyList()) }
    val balancesMap = remember { mutableStateMapOf<String, Int>() }
    val transactionsList = remember { mutableStateListOf<FavorTransaction>() }
    val staffNamesMap = remember { mutableStateMapOf<String, String>() }
    val userIdToStaffIdMap = remember { mutableStateMapOf<String, String>() }
    val myShiftsMap = remember { mutableStateMapOf<LocalDate, String>() }

    var isLoadingRequests by remember { mutableStateOf(true) }
    var isLoadingSchedule by remember { mutableStateOf(true) }

    // --- DIALOGO PREVIEW ---
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewRequest by remember { mutableStateOf<ShiftChangeRequest?>(null) }
    val requesterScheduleForPreview = remember { mutableStateMapOf<LocalDate, String>() }

    val labelUser = stringResource(R.string.default_user)

    fun resolveName(id: String): String {
        if (staffNamesMap.containsKey(id)) return staffNamesMap[id]!!
        val staffId = userIdToStaffIdMap[id]
        if (staffId != null && staffNamesMap.containsKey(staffId)) return staffNamesMap[staffId]!!
        return labelUser
    }

    val errorLoadingSchedule = stringResource(R.string.error_loading_schedule)

    fun fetchWeeklySchedule(userId: String, date: LocalDate) {
        val pivotStart = date.minusDays(7)
        val pivotEnd = date.plusDays(7)
        val startKey = "turnos-$pivotStart"
        val endKey = "turnos-$pivotEnd"

        database.getReference("plants/$plantId/turnos")
            .orderByKey()
            .startAt(startKey)
            .endAt(endKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requesterScheduleForPreview.clear()
                    snapshot.children.forEach { dateSnapshot ->
                        val dateKey = dateSnapshot.key?.removePrefix("turnos-") ?: return@forEach
                        try {
                            val d = LocalDate.parse(dateKey)
                            dateSnapshot.children.forEach { shiftSnap ->
                                val shiftName = shiftSnap.key ?: ""
                                val userName = resolveName(userId)
                                fun checkSlot(node: String) {
                                    shiftSnap.child(node).children.forEach { slot ->
                                        val p = slot.child("primary").value.toString()
                                        val s = slot.child("secondary").value.toString()
                                        if (p.equals(userName, true) || s.equals(userName, true)) {
                                            requesterScheduleForPreview[d] = shiftName
                                        }
                                    }
                                }
                                checkSlot("nurses")
                                checkSlot("auxiliaries")
                            }
                        } catch (_: Exception) {}
                    }
                    showPreviewDialog = true
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, errorLoadingSchedule, Toast.LENGTH_SHORT).show()
                }
            })
    }

    LaunchedEffect(plantId) {
        // 1. Cargar Solicitudes Activas
        database.getReference("plants/$plantId/shift_requests")
            .orderByChild("status")
            .equalTo("SEARCHING")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ShiftChangeRequest>()
                    snapshot.children.forEach { child ->
                        val req = child.getValue(ShiftChangeRequest::class.java)
                        if (req != null && req.requesterId != currentUserId) {
                            list.add(req)
                        }
                    }
                    rawMarketplaceRequests = list
                    isLoadingRequests = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingRequests = false }
            })

        // 2. Cargar TU Horario
        val today = LocalDate.now()
        val startDate = today.minusDays(1)
        val startKey = "turnos-$startDate"

        database.getReference("plants/$plantId/turnos")
            .orderByKey()
            .startAt(startKey)
            .limitToFirst(62)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    myShiftsMap.clear()
                    snapshot.children.forEach { dateSnapshot ->
                        val dateKey = dateSnapshot.key?.removePrefix("turnos-") ?: return@forEach
                        try {
                            val date = LocalDate.parse(dateKey)
                            dateSnapshot.children.forEach { shiftSnap ->
                                val shiftName = shiftSnap.key ?: ""
                                fun checkSlot(node: String) {
                                    shiftSnap.child(node).children.forEach { slot ->
                                        val name = slot.child("primary").value.toString()
                                        val secName = slot.child("secondary").value.toString()
                                        if (name.equals(currentUserName, ignoreCase = true) ||
                                            secName.equals(currentUserName, ignoreCase = true)) {
                                            myShiftsMap[date] = shiftName
                                        }
                                    }
                                }
                                checkSlot("nurses")
                                checkSlot("auxiliaries")
                            }
                        } catch (_: Exception) { }
                    }
                    isLoadingSchedule = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingSchedule = false }
            })

        // 3. Historial y Cálculo de Balances
        database.getReference("plants/$plantId/transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    transactionsList.clear()
                    val tempBalances = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val t = child.getValue(FavorTransaction::class.java)
                        if (t != null) {
                            if (t.covererId == currentUserId) {
                                transactionsList.add(t)
                                val current = tempBalances[t.requesterId] ?: 0
                                tempBalances[t.requesterId] = current + 1
                            } else if (t.requesterId == currentUserId) {
                                transactionsList.add(t)
                                val current = tempBalances[t.covererId] ?: 0
                                tempBalances[t.covererId] = current - 1
                            }
                        }
                    }
                    transactionsList.sortByDescending { it.timestamp }
                    balancesMap.clear()
                    tempBalances.forEach { (partnerId, score) -> balancesMap[partnerId] = score }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // 4. Nombres y IDs
        database.getReference("plants/$plantId/personal_de_planta")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    staffNamesMap.clear()
                    snapshot.children.forEach { child ->
                        val name = child.child("name").value as? String
                        val key = child.key
                        val internalId = child.child("id").value as? String
                        if (name != null) {
                            if (key != null) staffNamesMap[key] = name
                            if (internalId != null) staffNamesMap[internalId] = name
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        database.getReference("plants/$plantId/userPlants")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userIdToStaffIdMap.clear()
                    snapshot.children.forEach { child ->
                        val userId = child.key
                        val staffId = child.child("staffId").value as? String
                        if (userId != null && staffId != null) userIdToStaffIdMap[userId] = staffId
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    val filteredRequests = remember(rawMarketplaceRequests, myShiftsMap, currentUserRole) {
        if (isLoadingSchedule) emptyList()
        else rawMarketplaceRequests.filter { req ->
            try {
                val reqDate = LocalDate.parse(req.requesterShiftDate)
                if (!ShiftRulesEngine.areRolesCompatible(currentUserRole, req.requesterRole)) return@filter false
                val validationError = ShiftRulesEngine.validateWorkRules(
                    targetDate = reqDate,
                    targetShiftName = req.requesterShiftName,
                    userSchedule = myShiftsMap
                )
                validationError == null
            } catch (e: Exception) { false }
        }
    }

    val msgRequestSent = stringResource(R.string.msg_request_sent) // "Solicitud enviada al supervisor"
    val notifTemplate = stringResource(R.string.notif_shift_covered)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_shift_marketplace), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // --- BALANCES CON FILTRO DE CEROS ---
            if (balancesMap.isNotEmpty()) {
                // Filtramos para no mostrar los que sean 0
                val visibleBalances = balancesMap.toList().filter { it.second != 0 }

                if (visibleBalances.isNotEmpty()) {
                    Text(stringResource(R.string.header_balances), color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visibleBalances) { (partnerId, score) ->
                            val partnerName = resolveName(partnerId)
                            val historyWithPartner = transactionsList.filter {
                                (it.covererId == currentUserId && it.requesterId == partnerId) ||
                                        (it.requesterId == currentUserId && it.covererId == partnerId)
                            }
                            BalanceCard(partnerName, score, historyWithPartner, currentUserId)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))
                }
            }

            Text(stringResource(R.string.header_available_shifts), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(stringResource(R.string.desc_available_shifts), color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingRequests || isLoadingSchedule) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF54C7EC)) }
            } else if (filteredRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.msg_no_shifts_available), color = Color.Gray)
                        if (rawMarketplaceRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.msg_hidden_shifts, (rawMarketplaceRequests.size - filteredRequests.size)), color = Color.DarkGray, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(filteredRequests) { req ->
                        val balanceWithRequester = balancesMap[req.requesterId] ?: 0
                        MarketplaceItem(
                            req = req,
                            balanceWithRequester = balanceWithRequester,
                            onAccept = {
                                // Llamamos a la versión nueva que solo envía al supervisor
                                acceptCoverage(
                                    database, plantId, req, currentUserId, currentUserName,
                                    onSuccess = {
                                        onSaveNotification(
                                            req.requesterId,
                                            "SHIFT_PROPOSAL", // Cambiado a propuesta
                                            "${currentUserName} quiere cubrir tu turno del ${req.requesterShiftDate}. Pendiente de supervisor.",
                                            "ShiftChangeScreen",
                                            req.id,
                                            {}
                                        )
                                        Toast.makeText(context, "Solicitud enviada al supervisor", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                )
                            },
                            onPreview = {
                                previewRequest = req
                                val date = LocalDate.parse(req.requesterShiftDate)
                                fetchWeeklySchedule(req.requesterId, date)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPreviewDialog && previewRequest != null) {
        val req = previewRequest!!
        val pivotDate = LocalDate.parse(req.requesterShiftDate)
        SchedulePreviewDialog(
            onDismiss = { showPreviewDialog = false },
            row1Schedule = myShiftsMap,
            row1Name = stringResource(R.string.label_me),
            row1DateToRemove = null,
            row1DateToAdd = pivotDate,
            row1ShiftToAdd = req.requesterShiftName,
            row2Schedule = requesterScheduleForPreview,
            row2Name = req.requesterName,
            row2DateToRemove = pivotDate,
            row2DateToAdd = null,
            row2ShiftToAdd = null,
            shiftColors = shiftColors
        )
    }
}

// ... (BalanceCard y MarketplaceItem se mantienen igual que tu versión anterior) ...
@Composable
fun BalanceCard(partnerName: String, score: Int, history: List<FavorTransaction>, currentUserId: String) {
    var expanded by remember { mutableStateOf(false) }
    val isPositive = score > 0
    val scoreColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE91E63)
    val containerColor = if (isPositive) Color(0xFF1E293B) else Color(0xFF2B1218)
    val borderColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE91E63)

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = containerColor), border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(partnerName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = if (isPositive) stringResource(R.string.balance_owe_you, score) else stringResource(R.string.balance_you_owe, abs(score)), color = scoreColor, fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (score > 0) "+$score" else "$score", color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = stringResource(R.string.desc_expand), tint = Color.Gray)
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.header_favor_history), color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (history.isEmpty()) {
                        Text(stringResource(R.string.msg_no_history), color = Color.Gray, fontSize = 12.sp)
                    } else {
                        history.forEach { t ->
                            val isMeCovering = t.covererId == currentUserId
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Icon(imageVector = if (isMeCovering) Icons.Default.CheckCircle else Icons.Default.History, contentDescription = null, tint = if (isMeCovering) Color(0xFF4CAF50) else Color(0xFFFF9800), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = if (isMeCovering) stringResource(R.string.msg_you_covered) else stringResource(R.string.msg_covered_you), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "${t.shiftName} del ${t.date}", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceItem(req: ShiftChangeRequest, balanceWithRequester: Int, onAccept: () -> Unit, onPreview: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_requests, req.requesterName), color = Color.White, fontWeight = FontWeight.Bold)
                    if (balanceWithRequester != 0) {
                        Text(text = if (balanceWithRequester > 0) stringResource(R.string.balance_owe_you, balanceWithRequester) else stringResource(R.string.balance_you_owe, abs(balanceWithRequester)), color = if (balanceWithRequester > 0) Color(0xFF4CAF50) else Color(0xFFE91E63), fontSize = 11.sp)
                    } else {
                        Text(stringResource(R.string.balance_neutral), color = Color.Gray, fontSize = 11.sp)
                    }
                }
                Badge(containerColor = Color(0xFF54C7EC)) { Text(req.requesterShiftName, color = Color.Black) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.label_date_simple, req.requesterShiftDate), color = Color(0xFF54C7EC), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_cover_shift))
                }
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(0.5f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF54C7EC)), border = BorderStroke(1.dp, Color(0xFF54C7EC))) {
                    Text(stringResource(R.string.btn_preview_eye), fontSize = 12.sp)
                }
            }
        }
    }
}

// ============================================================================================
// FUNCION ACTUALIZADA: Solo envía al supervisor
// ============================================================================================

fun acceptCoverage(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    covererId: String,
    covererName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // 1. Intentar resolver el nombre REAL usado en la planta a partir del User ID (opcional, pero buena práctica)
    // Para simplificar y dado que solo vamos a pedir aprobación, podemos enviar los datos directamente.
    // El supervisor hará la búsqueda robusta cuando apruebe.

    val updates = mapOf(
        "status" to RequestStatus.AWAITING_SUPERVISOR,
        "targetUserId" to covererId,
        "targetUserName" to covererName,
        "type" to RequestType.COVERAGE // Forzamos el tipo COVERAGE para que el supervisor sepa qué hacer
    )

    database.reference.child("plants/$plantId/shift_requests/${req.id}")
        .updateChildren(updates)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it.message ?: "Error al enviar solicitud") }
}