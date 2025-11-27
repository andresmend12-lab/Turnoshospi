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

enum class RequestStatus { SEARCHING, MATCH_FOUND, PENDING_APPROVAL, APPROVED, REJECTED }
data class ShiftChangeRequest(val id: String = "", val plantId: String = "", val requesterId: String = "", val requesterName: String = "", val originalDate: String = "", val originalShift: String = "", val offeredDates: List<String> = emptyList(), val status: RequestStatus = RequestStatus.SEARCHING)
data class MyShiftDisplay(val date: String, val shiftName: String, val fullDate: LocalDate)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftChangeScreen(plantId: String, currentUser: UserProfile?, currentUserId: String, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val myShifts = remember { mutableStateListOf<MyShiftDisplay>() }
    val activeRequests = remember { mutableStateListOf<ShiftChangeRequest>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedShift by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(plantId) {
        database.getReference("plants/$plantId/shift_requests").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { activeRequests.clear(); s.children.mapNotNull { it.getValue(ShiftChangeRequest::class.java) }.forEach { activeRequests.add(it) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        // Simulación de carga de turnos propios (aquí deberías consultar Firebase real)
        myShifts.add(MyShiftDisplay("2023-12-25", "Mañana", LocalDate.parse("2023-12-25")))
    }

    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("Cambios", color = Color.White) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTab, containerColor = Color.Transparent) { listOf("Mis Turnos", "Solicitudes").forEachIndexed { i, t -> Tab(selectedTab == i, { selectedTab = i }, text = { Text(t, color = Color.White) }) } }
            Box(Modifier.padding(16.dp)) {
                if (selectedTab == 0) LazyColumn { items(myShifts) { s -> Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${s.date} - ${s.shiftName}", color = Color.White); Button({ selectedShift = s.date to s.shiftName; showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC))) { Text("Cambiar") } } } } }
                else LazyColumn { items(activeRequests) { r -> Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) { Column(Modifier.padding(16.dp)) { Text(r.requesterName, color = Color.White, fontWeight = FontWeight.Bold); Text("Quiere librar: ${r.originalDate}", color = Color.Gray) } } } }
            }
        }
    }

    if (showDialog && selectedShift != null) {
        var dates by remember { mutableStateOf(listOf<String>()) }
        AlertDialog({ showDialog = false }, { Button({
            val req = ShiftChangeRequest(UUID.randomUUID().toString(), plantId, currentUserId, currentUser?.firstName ?: "User", selectedShift!!.first, selectedShift!!.second, dates)
            database.getReference("plants/$plantId/shift_requests/${req.id}").setValue(req); showDialog = false
        }) { Text("Enviar") } }, title = { Text("Solicitar cambio") }, text = { Column { Text("Turno: ${selectedShift!!.first}"); Button({ dates = dates + "2023-12-28" }) { Text("Añadir fecha ejemplo") }; Text("Fechas: $dates") } })
    }
}