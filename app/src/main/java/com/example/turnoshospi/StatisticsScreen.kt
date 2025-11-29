package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    plant: Plant?,
    currentUserEmail: String?,
    currentUserName: String?, // Nombre usado en los turnos
    onBack: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var stats by remember { mutableStateOf<MonthlyStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Necesitamos el nombre exacto con el que el usuario está guardado en los turnos.
    // A veces es el nombre, a veces el email, depende de cómo se registró.
    // Asumiremos que se pasa el nombre correcto (staffName) desde la navegación o membership.
    val targetName = currentUserName ?: ""

    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")

    LaunchedEffect(plant, currentMonth, targetName) {
        if (plant == null || targetName.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val startStr = "turnos-${currentMonth.atDay(1)}"
        val endStr = "turnos-${currentMonth.atEndOfMonth()}"

        database.getReference("plants/${plant.id}/turnos")
            .orderByKey()
            .startAt(startStr)
            .endAt(endStr)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val calculatedStats = calculateStatsForMonth(snapshot, plant.shiftTimes, targetName)
                    stats = calculatedStats
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                }
            })
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selector de Mes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).uppercase()} ${currentMonth.year}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF54C7EC))
            } else if (stats == null || stats!!.totalHours == 0.0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                    Icon(Icons.Default.BarChart, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay horas registradas este mes.", color = Color.Gray)
                }
            } else {
                // Tarjeta Principal (Total)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF54C7EC))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Horas Trabajadas", color = Color(0xFF54C7EC), fontSize = 14.sp)
                        Text(
                            text = String.format("%.1f h", stats!!.totalHours),
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${stats!!.totalShifts} turnos realizados", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Desglose por Turno",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stats!!.breakdown.entries.toList().sortedByDescending { it.value.hours }) { (shiftName, data) ->
                        StatRow(shiftName, data)
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(shiftName: String, data: ShiftStatData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(shiftName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${data.count} turnos", color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = String.format("%.1f h", data.hours),
                color = Color(0xFF54C7EC),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

// --- Lógica de Cálculo ---

data class MonthlyStats(
    val totalHours: Double,
    val totalShifts: Int,
    val breakdown: Map<String, ShiftStatData>
)

data class ShiftStatData(
    var hours: Double = 0.0,
    var count: Int = 0
)

fun calculateStatsForMonth(
    snapshot: DataSnapshot,
    shiftTimes: Map<String, ShiftTime>,
    userName: String
): MonthlyStats {
    var totalH = 0.0
    var totalS = 0
    val breakdown = mutableMapOf<String, ShiftStatData>()

    snapshot.children.forEach { daySnapshot ->
        daySnapshot.children.forEach { shiftSnapshot ->
            val shiftName = shiftSnapshot.key ?: return@forEach
            val shiftTime = shiftTimes[shiftName]

            if (shiftTime != null) {
                val duration = calculateDuration(shiftTime.start, shiftTime.end)
                var worked = false
                var halfDay = false

                // Revisar Enfermeros
                shiftSnapshot.child("nurses").children.forEach { slot ->
                    val p = slot.child("primary").value as? String
                    val s = slot.child("secondary").value as? String
                    val h = slot.child("halfDay").value as? Boolean == true

                    if (p == userName) { worked = true; halfDay = h }
                    else if (h && s == userName) { worked = true; halfDay = true }
                }

                // Revisar Auxiliares (si no encontró ya)
                if (!worked) {
                    shiftSnapshot.child("auxiliaries").children.forEach { slot ->
                        val p = slot.child("primary").value as? String
                        val s = slot.child("secondary").value as? String
                        val h = slot.child("halfDay").value as? Boolean == true

                        if (p == userName) { worked = true; halfDay = h }
                        else if (h && s == userName) { worked = true; halfDay = true }
                    }
                }

                if (worked) {
                    val hoursToAdd = if (halfDay) duration / 2.0 else duration
                    totalH += hoursToAdd
                    totalS += 1

                    val stat = breakdown.getOrPut(shiftName) { ShiftStatData() }
                    stat.hours += hoursToAdd
                    stat.count += 1
                }
            }
        }
    }

    return MonthlyStats(totalH, totalS, breakdown)
}

fun calculateDuration(start: String, end: String): Double {
    if (start.isBlank() || end.isBlank()) return 0.0
    try {
        val startTime = LocalTime.parse(start)
        val endTime = LocalTime.parse(end)
        var hours = Duration.between(startTime, endTime).toMinutes() / 60.0
        if (hours < 0) hours += 24.0 // Turno nocturno que cruza medianoche
        return hours
    } catch (e: Exception) {
        return 0.0
    }
}