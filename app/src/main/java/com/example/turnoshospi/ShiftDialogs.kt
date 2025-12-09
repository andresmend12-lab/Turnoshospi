package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.turnoshospi.ui.theme.ShiftColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM", deviceLocale)

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