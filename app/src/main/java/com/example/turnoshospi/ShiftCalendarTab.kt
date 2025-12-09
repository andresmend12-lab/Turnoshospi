package com.example.turnoshospi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.turnoshospi.ui.theme.ShiftColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
                    Text("${currentMonth.month.getDisplayName(TextStyle.FULL, deviceLocale).uppercase()} ${currentMonth.year}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                Text(text = date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", deviceLocale)).replaceFirstChar { it.uppercase() }, color = Color(0xFF54C7EC), style = MaterialTheme.typography.titleMedium)
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