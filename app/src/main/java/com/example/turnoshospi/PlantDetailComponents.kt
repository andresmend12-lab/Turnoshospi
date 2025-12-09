package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlantCalendar(selectedDate: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val deviceLocale = Locale.getDefault()

    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(24.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_prev_month), tint = Color.White) }
            Text("${currentMonth.month.getDisplayName(TextStyle.FULL, deviceLocale).uppercase()} ${currentMonth.year}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.desc_next_month), tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeekShort = androidx.compose.ui.res.stringArrayResource(R.array.days_of_week_short)
            daysOfWeekShort.forEach { day -> Text(text = day, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
                            val isSelected = date == selectedDate
                            Box(
                                modifier = Modifier.weight(1f).height(48.dp).padding(2.dp)
                                    .background(if (isSelected) Color(0xFF54C7EC) else Color.Transparent, CircleShape)
                                    .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f), CircleShape)
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dayIndex.toString(), color = if (isSelected) Color.Black else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        } else { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ShiftAssignmentsSection(
    plant: Plant, assignments: MutableMap<String, ShiftAssignmentState>, selectedDateLabel: String,
    isSupervisor: Boolean, isSavedForDate: Boolean, unassignedLabel: String,
    nurseOptions: List<String>, auxOptions: List<String>, onSaveAssignments: (Map<String, ShiftAssignmentState>) -> Unit
) {
    // CORREGIDO: Se asegura que plant.staffScope.normalizedRole() llama a la extensión correcta.
    val allowAux = plant.staffScope.normalizedRole() == stringResource(id = R.string.staff_scope_with_aux).normalizedRole() ||
            plant.staffScope.contains("aux", ignoreCase = true) || auxOptions.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = R.string.plant_shifts_for_date, selectedDateLabel), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val orderedShifts = remember(plant.shiftTimes) { plant.shiftTimes.entries.sortedBy { getShiftPriority(it.key) } }

        orderedShifts.forEach { (rawShiftName, timing) ->
            val displayShiftName = getLocalizedShiftName(rawShiftName)
            val nurseReq = plant.staffRequirements[rawShiftName] ?: 0
            val auxReq = if (allowAux) (plant.staffRequirements[rawShiftName] ?: 0).coerceAtLeast(1) else 0
            val state = assignments.getOrPut(rawShiftName) { ShiftAssignmentState(mutableStateListOf(), mutableStateListOf()) }
            ensureSlotSize(state.nurseSlots, nurseReq.coerceAtLeast(1))
            if (auxReq > 0) ensureSlotSize(state.auxSlots, auxReq) else state.auxSlots.clear()

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22000000)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(id = R.string.plant_shift_item, displayShiftName, timing.start.ifEmpty { "--" }, timing.end.ifEmpty { "--" }), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    state.nurseSlots.forEachIndexed { i, slot ->
                        val label = if (slot.hasHalfDay) stringResource(id = R.string.nurse_half_day_label) else stringResource(id = R.string.nurse_label, i + 1)
                        if (isSupervisor) EditableAssignmentRow(label, slot, nurseOptions, stringResource(id = R.string.nurse_half_day_label))
                        else ReadOnlyAssignmentRow(label, stringResource(id = R.string.nurse_half_day_label), slot, unassignedLabel)
                    }
                    if (allowAux) {
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        state.auxSlots.forEachIndexed { i, slot ->
                            val label = if (slot.hasHalfDay) stringResource(id = R.string.aux_half_day_label) else stringResource(id = R.string.aux_label, i + 1)
                            if (isSupervisor) EditableAssignmentRow(label, slot, auxOptions, stringResource(id = R.string.aux_half_day_label))
                            else ReadOnlyAssignmentRow(label, stringResource(id = R.string.aux_half_day_label), slot, unassignedLabel)
                        }
                    }
                }
            }
        }
        if (isSupervisor) {
            Button(onClick = { onSaveAssignments(assignments) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)) {
                Text(if (isSavedForDate) stringResource(id = R.string.edit_assignments_action) else stringResource(id = R.string.save_assignments_action))
            }
        }
    }
}

@Composable
fun EditableAssignmentRow(label: String, slot: SlotAssignment, options: List<String>, halfDayLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = slot.hasHalfDay, onCheckedChange = { slot.hasHalfDay = it; if (!it) slot.secondaryName = "" }, modifier = Modifier.width(52.dp))
            StaffDropdownField(modifier = Modifier.weight(1f), label = label, selectedValue = slot.primaryName, options = options, enabled = true, onOptionSelected = { slot.primaryName = it }, includeUnassigned = true)
        }
        if (slot.hasHalfDay) {
            StaffDropdownField(modifier = Modifier.fillMaxWidth().padding(start = 60.dp), label = halfDayLabel, selectedValue = slot.secondaryName, options = options, enabled = true, onOptionSelected = { slot.secondaryName = it }, includeUnassigned = true)
        }
    }
}

@Composable
fun ReadOnlyAssignmentRow(label: String, halfDayLabel: String, slot: SlotAssignment, unassigned: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF), fontWeight = FontWeight.SemiBold)
        Text(slot.primaryName.ifBlank { unassigned }, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        if (slot.hasHalfDay) {
            Text(halfDayLabel, style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF), fontWeight = FontWeight.SemiBold)
            Text(slot.secondaryName.ifBlank { unassigned }, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDropdownField(modifier: Modifier = Modifier, label: String, selectedValue: String, options: List<String>, enabled: Boolean, onOptionSelected: (String) -> Unit, includeUnassigned: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val unassigned = stringResource(id = R.string.staff_unassigned_option)
    val display = selectedValue.ifBlank { if (includeUnassigned) unassigned else "" }
    val menuOptions = if (includeUnassigned) listOf(unassigned) + options else options
    Box(modifier = modifier) {
        OutlinedTextField(
            value = display, onValueChange = {}, readOnly = true, enabled = enabled, label = { Text(label) },
            trailingIcon = { IconButton(onClick = { if (enabled) expanded = !expanded }) { Icon(Icons.Filled.ArrowDropDown, null, Modifier.rotate(if (expanded) 180f else 0f) ) } },
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) { expanded = !expanded },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF54C7EC), unfocusedBorderColor = Color(0x66FFFFFF))
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(), containerColor = Color(0xF00B1021)) {
            menuOptions.forEach { op -> DropdownMenuItem(text = { Text(op, color = Color.White) }, onClick = { onOptionSelected(if (op == unassigned) "" else op); expanded = false }) }
        }
    }
}

@Composable
fun InfoMessage(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22000000)), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
    }
}

fun formatPlantDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.getDefault()))
}

private fun ensureSlotSize(list: MutableList<SlotAssignment>, expected: Int) {
    while (list.size < expected) list.add(SlotAssignment())
    if (list.size > expected) repeat(list.size - expected) { list.removeLastOrNull() }
}