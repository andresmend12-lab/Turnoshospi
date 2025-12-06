package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.ShiftColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Modelo de datos para la vista de supervisor
data class ShiftRoster(
    val nurses: List<String>,
    val auxiliaries: List<String>
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomCalendar(
    shifts: Map<String, UserShift>,
    plantId: String?,
    selectedDate: LocalDate?,
    selectedShift: UserShift?,
    colleagues: List<Colleague>,
    isLoadingColleagues: Boolean,
    isSupervisor: Boolean = false,
    roster: Map<String, ShiftRoster> = emptyMap(),
    isLoadingRoster: Boolean = false,
    shiftColors: ShiftColors,
    onDayClick: (LocalDate, UserShift?) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera del calendario (Mes y Navegación)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior", tint = Color.White)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).uppercase()} ${currentMonth.year}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Días de la semana
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cuadrícula de días
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        val totalCells = (daysInMonth + dayOfWeekOffset + 6) / 7 * 7

        Column {
            for (i in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (j in 0 until 7) {
                        val dayIndex = i + j - dayOfWeekOffset + 1
                        if (dayIndex in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayIndex)
                            val dateKey = date.toString()
                            val shift = shifts[dateKey]

                            val color = if (isSupervisor) Color.Transparent else getDayColor(date, shifts, shiftColors)
                            val isSelected = date == selectedDate

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(2.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if(isSelected) 2.dp else 0.dp,
                                        color = if(isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onDayClick(date, shift) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Leyenda y Detalles
        if (plantId != null && !isSupervisor) {
            CalendarLegend(shiftColors)
        }

        if (selectedDate != null) {
            DayDetailsSection(
                date = selectedDate,
                selectedShift = selectedShift,
                isSupervisor = isSupervisor,
                isLoadingRoster = isLoadingRoster,
                roster = roster,
                isLoadingColleagues = isLoadingColleagues,
                colleagues = colleagues,
                plantId = plantId
            )
        }
    }
}

// Función auxiliar para la leyenda
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend(shiftColors: ShiftColors) {
    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        val legendItems = listOf(
            Triple(shiftColors.free, "Libre", null),
            Triple(shiftColors.morning, "Mañana", null),
            Triple(shiftColors.morningHalf, "M. Mañana", null),
            Triple(shiftColors.afternoon, "Tarde", null),
            Triple(shiftColors.afternoonHalf, "M. Tarde", null),
            Triple(shiftColors.night, "Noche", null),
            Triple(shiftColors.saliente, "Saliente", null),
            Triple(shiftColors.holiday, "Vacaciones", null)
        )

        legendItems.forEach { (color, text, _) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = text, color = Color(0xFFCCCCCC), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// Componente para los detalles del día seleccionado
@Composable
private fun DayDetailsSection(
    date: LocalDate,
    selectedShift: UserShift?,
    isSupervisor: Boolean,
    isLoadingRoster: Boolean,
    roster: Map<String, ShiftRoster>,
    isLoadingColleagues: Boolean,
    colleagues: List<Colleague>,
    plantId: String?
) {
    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    Spacer(modifier = Modifier.height(16.dp))

    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    val dateStr = date.format(formatter)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSupervisor) {
            Text(
                text = "Agenda del día: $dateStr",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingRoster) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF54C7EC))
            } else if (roster.isEmpty()) {
                Text(text = "No hay turnos asignados.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                roster.forEach { (shiftName, data) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = shiftName, style = MaterialTheme.typography.titleSmall, color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold)
                        if (data.nurses.isNotEmpty()) {
                            Text("Enfermeros: ${data.nurses.joinToString(", ")}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        if (data.auxiliaries.isNotEmpty()) {
                            Text("Auxiliares: ${data.auxiliaries.joinToString(", ")}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Turno: ${selectedShift?.shiftName ?: "Libre"}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF54C7EC))
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingColleagues) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF54C7EC))
            } else if (colleagues.isNotEmpty()) {
                Text("Compañeros:", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                colleagues.forEach { colleague ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(colleague.name, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(colleague.role, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (plantId != null && selectedShift != null) {
                Text("No se encontraron compañeros.", color = Color.Gray)
            }
        }
    }
}

fun getDayColor(date: LocalDate, shifts: Map<String, UserShift>, colors: ShiftColors): Color {
    val dateKey = date.toString()
    val shift = shifts[dateKey]

    if (shift != null) {
        val type = shift.shiftName.lowercase()
        return when {
            type.contains("vacaciones") -> colors.holiday
            type.contains("noche") -> colors.night
            type.contains("media") && (type.contains("mañana") || type.contains("dia")) -> colors.morningHalf
            type.contains("mañana") || type.contains("día") -> colors.morning
            type.contains("media") && type.contains("tarde") -> colors.afternoonHalf
            type.contains("tarde") -> colors.afternoon
            else -> colors.morning
        }
    }
    val yesterdayKey = date.minusDays(1).toString()
    val yesterdayShift = shifts[yesterdayKey]
    if (yesterdayShift != null && yesterdayShift.shiftName.lowercase().contains("noche")) {
        return colors.saliente
    }
    return colors.free
}