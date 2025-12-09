package com.example.turnoshospi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlantShiftsList(
    request: ShiftChangeRequest,
    allShifts: List<PlantShift>,
    currentUserId: String,
    userSchedules: Map<String, Map<LocalDate, String>>,
    currentUserSchedule: Map<LocalDate, String>,
    shiftColors: ShiftColors,
    onProposeSwap: (PlantShift) -> Unit
) {
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterPerson by remember { mutableStateOf<String?>(null) }
    var filterShiftType by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }
    var showShiftMenu by remember { mutableStateOf(false) }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewCandidate by remember { mutableStateOf<PlantShift?>(null) }

    val availablePeople = remember(allShifts, request.requesterRole) {
        allShifts
            .filter { ShiftRulesEngine.areRolesCompatible(request.requesterRole, it.userRole) && it.userId != currentUserId }
            .map { it.userName }
            .distinct()
            .sorted()
    }

    val filteredShifts = remember(allShifts, request, currentUserId, filterDate, filterPerson, filterShiftType) {
        val today = LocalDate.now()
        val myDate = LocalDate.parse(request.requesterShiftDate)
        val mySimulatedSchedule = currentUserSchedule.filterKeys { it != myDate }

        allShifts.filter { shift ->
            val isFuture = !shift.date.isBefore(today)
            val isCompatibleRole = ShiftRulesEngine.areRolesCompatible(request.requesterRole, shift.userRole)
            val isNotMe = shift.userId != currentUserId
            val isSameExactShift = (shift.date == myDate && shift.shiftName.trim().equals(request.requesterShiftName.trim(), ignoreCase = true))

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
                    Text(stringResource(R.string.card_compatible_candidates), color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${stringResource(R.string.label_offering_shift)} ${request.requesterShiftDate}", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = filterDate != null,
                    onClick = { showDatePicker = true },
                    label = { Text(if(filterDate != null) filterDate.toString() else stringResource(R.string.filter_date)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp)) },
                    trailingIcon = if (filterDate != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterDate = null }) } } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                )
            }
            item {
                Box {
                    FilterChip(
                        selected = filterPerson != null,
                        onClick = { showPersonMenu = true },
                        label = { Text(filterPerson ?: stringResource(R.string.filter_person)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterPerson != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterPerson = null }) } } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showPersonMenu, onDismissRequest = { showPersonMenu = false }) {
                        availablePeople.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { filterPerson = name; showPersonMenu = false })
                        }
                    }
                }
            }
            item {
                val shiftOptions = listOf(stringResource(R.string.shift_morning) to "Mañana", stringResource(R.string.shift_afternoon) to "Tarde", stringResource(R.string.shift_night) to "Noche")
                val currentVisualLabel = shiftOptions.find { it.second == filterShiftType }?.first
                Box {
                    FilterChip(
                        selected = filterShiftType != null,
                        onClick = { showShiftMenu = true },
                        label = { Text(currentVisualLabel ?: stringResource(R.string.filter_shift)) },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) },
                        trailingIcon = if (filterShiftType != null) { { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { filterShiftType = null }) } } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF54C7EC), selectedLabelColor = Color.Black)
                    )
                    DropdownMenu(expanded = showShiftMenu, onDismissRequest = { showShiftMenu = false }) {
                        shiftOptions.forEach { (label, dbValue) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { filterShiftType = dbValue; showShiftMenu = false })
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
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
            ) { DatePicker(state = datePickerState) }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (filteredShifts.isEmpty()) {
                item { Text(text = stringResource(R.string.msg_no_candidates_found), color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            } else {
                items(filteredShifts) { shift ->
                    PlantShiftCard(
                        shift = shift,
                        shiftColors = shiftColors,
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
        val dateReq = LocalDate.parse(request.requesterShiftDate)
        val labelMe = stringResource(R.string.label_me)

        SchedulePreviewDialog(
            onDismiss = { showPreviewDialog = false },
            row1Schedule = currentUserSchedule,
            row1Name = labelMe,
            row1DateToRemove = dateReq,
            row1DateToAdd = candidate.date,
            row1ShiftToAdd = candidate.shiftName,
            row2Schedule = userSchedules[candidate.userId] ?: emptyMap(),
            row2Name = candidate.userName,
            row2DateToRemove = candidate.date,
            row2DateToAdd = dateReq,
            row2ShiftToAdd = request.requesterShiftName,
            shiftColors = shiftColors
        )
    }
}