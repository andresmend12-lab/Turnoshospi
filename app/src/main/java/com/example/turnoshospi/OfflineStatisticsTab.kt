package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun OfflineStatisticsTab(
    shifts: Map<String, UserShift>,
    customShiftTypes: List<CustomShiftType>,
    shiftDurations: Map<String, Double>
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val stats = remember(shifts, customShiftTypes, shiftDurations, currentMonth) {
        calculateOfflineStatsForMonth(currentMonth, shifts, customShiftTypes, shiftDurations)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${currentMonth.year}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (stats.totalShifts == 0 || stats.totalHours == 0.0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                Icon(Icons.Default.BarChart, null, tint = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Text("Sin estadisticas para este mes", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Horas trabajadas", color = Color(0xFF54C7EC), fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.1f h", stats.totalHours),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%d turnos", stats.totalShifts),
                        color = Color.Gray
                    )
                }
            }

            if (stats.breakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Detalle por turno",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                stats.breakdown.entries
                    .sortedByDescending { it.value.hours }
                    .forEach { (shiftName, data) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
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
                                    Text(String.format(Locale.getDefault(), "%d turnos", data.count), color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f h", data.hours),
                                    color = Color(0xFF54C7EC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
            }
        }
    }
}

data class OfflineMonthlyStats(
    val totalHours: Double,
    val totalShifts: Int,
    val breakdown: Map<String, ShiftStatData>
)

fun calculateOfflineStatsForMonth(
    month: YearMonth,
    shifts: Map<String, UserShift>,
    customShiftTypes: List<CustomShiftType>,
    shiftDurations: Map<String, Double>
): OfflineMonthlyStats {
    var totalHours = 0.0
    var totalShifts = 0
    val breakdown = mutableMapOf<String, ShiftStatData>()

    shifts.forEach { (dateKey, shift) ->
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return@forEach
        if (YearMonth.from(date) != month) return@forEach

        val hours = getShiftDurationHours(shift, shiftDurations, customShiftTypes)
        if (hours <= 0.0) return@forEach

        totalHours += hours
        totalShifts += 1

        val key = normalizeShiftType(shift.shiftName)
        val stat = breakdown.getOrPut(key) { ShiftStatData() }
        stat.hours += hours
        stat.count += 1
    }

    return OfflineMonthlyStats(totalHours, totalShifts, breakdown)
}

fun getShiftDurationHours(
    shift: UserShift,
    shiftDurations: Map<String, Double>,
    customShiftTypes: List<CustomShiftType>
): Double {
    val custom = customShiftTypes.firstOrNull { it.name.equals(shift.shiftName, ignoreCase = true) }
    val baseName = baseShiftNameForDuration(shift.shiftName)
    val baseDuration = custom?.durationHours ?: shiftDurations[baseName] ?: 0.0
    return if (shift.isHalfDay) baseDuration / 2.0 else baseDuration
}

fun baseShiftNameForDuration(shiftName: String): String {
    return when (normalizeShiftType(shiftName)) {
        "Media Manana" -> "Manana"
        "Media Tarde" -> "Tarde"
        "Medio Dia" -> "Dia"
        else -> normalizeShiftType(shiftName)
    }
}
