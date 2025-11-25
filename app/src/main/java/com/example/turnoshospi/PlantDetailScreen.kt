package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private data class ShiftAssignmentState(
    val nurseNames: MutableList<String>,
    val auxNames: MutableList<String>,
    var halfDay: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plant: Plant?,
    datePickerState: DatePickerState,
    onBack: () -> Unit,
    onAddStaff: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val assignments = remember(plant?.id) { mutableStateMapOf<String, ShiftAssignmentState>() }

    LaunchedEffect(plant?.shiftTimes) {
        plant?.shiftTimes?.keys?.forEach { shift ->
            assignments.putIfAbsent(
                shift,
                ShiftAssignmentState(
                    nurseNames = mutableStateListOf(),
                    auxNames = mutableStateListOf(),
                    halfDay = false
                )
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White
            ) {
                DrawerHeader(
                    displayName = plant?.name ?: "",
                    welcomeStringId = R.string.side_menu_title
                )
                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    label = { Text(text = stringResource(id = R.string.plant_add_staff_option), color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAddStaff()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = Color.White
                    )
                )
                NavigationDrawerItem(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    label = { Text(text = stringResource(id = R.string.back_to_menu), color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onBack()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {
                        Text(
                            text = plant?.name ?: stringResource(id = R.string.menu_my_plants),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(id = R.string.side_menu_title),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = stringResource(id = R.string.close_label),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onAddStaff) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.plant_add_staff_option),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1021), Color(0xFF0F172A))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.plant_calendar_title),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DatePicker(
                                state = datePickerState,
                                title = null,
                                headline = null,
                                showModeToggle = false,
                                colors = DatePickerDefaults.colors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = Color.White,
                                    headlineContentColor = Color.White,
                                    weekdayContentColor = Color.White,
                                    subheadContentColor = Color.White,
                                    yearContentColor = Color.White,
                                    currentYearContentColor = Color.White,
                                    selectedYearContentColor = Color.White,
                                    selectedYearContainerColor = Color(0xFF1E293B),
                                    disabledSelectedYearContainerColor = Color(0x661E293B),
                                    selectedDayContentColor = Color.White,
                                    disabledSelectedDayContentColor = Color(0x80FFFFFF),
                                    selectedDayContainerColor = Color(0xFF1E293B),
                                    disabledSelectedDayContainerColor = Color(0x661E293B),
                                    dayContentColor = Color.White,
                                    disabledDayContentColor = Color(0x80FFFFFF),
                                    dayInSelectionRangeContentColor = Color.White,
                                    dayInSelectionRangeContainerColor = Color(0x331E293B),
                                    todayContentColor = Color.White,
                                    todayDateBorderColor = Color(0x66FFFFFF)
                                )
                            )
                            Text(
                                text = selectedDate?.let { formatDate(it) }
                                    ?: stringResource(id = R.string.select_date_prompt),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (plant != null && selectedDate != null) {
                        ShiftAssignmentsSection(
                            plant = plant,
                            assignments = assignments,
                            selectedDateLabel = formatDate(selectedDate)
                        )
                    } else {
                        InfoMessage(message = stringResource(id = R.string.plant_detail_missing_data))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ShiftAssignmentsSection(
    plant: Plant,
    assignments: MutableMap<String, ShiftAssignmentState>,
    selectedDateLabel: String
) {
    val allowAux = plant.staffScope == stringResource(id = R.string.staff_scope_with_aux)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.plant_shifts_for_date, selectedDateLabel),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        plant.shiftTimes.forEach { (shiftName, timing) ->
            val requirement = plant.staffRequirements[shiftName] ?: 0
            val state = assignments.getOrPut(shiftName) {
                ShiftAssignmentState(
                    nurseNames = mutableStateListOf(),
                    auxNames = mutableStateListOf(),
                    halfDay = false
                )
            }

            ensureSize(state.nurseNames, requirement.coerceAtLeast(1))
            if (allowAux) {
                ensureSize(state.auxNames, requirement.coerceAtLeast(1))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.plant_shift_item,
                            shiftName,
                            timing.start.ifEmpty { "--" },
                            timing.end.ifEmpty { "--" }
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(
                            id = R.string.plant_staff_requirement_item,
                            shiftName,
                            requirement
                        ),
                        color = Color(0xCCFFFFFF),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.nurseNames.forEachIndexed { index, name ->
                            OutlinedTextField(
                                value = name,
                                onValueChange = { newName -> state.nurseNames[index] = newName },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = stringResource(id = R.string.nurse_label, index + 1)) },
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF54C7EC),
                                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color(0xCCFFFFFF),
                                    focusedContainerColor = Color(0x22FFFFFF),
                                    unfocusedContainerColor = Color(0x11FFFFFF)
                                )
                            )
                        }

                        if (allowAux) {
                            Divider(color = Color(0x22FFFFFF))
                            state.auxNames.forEachIndexed { index, name ->
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { newName -> state.auxNames[index] = newName },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = stringResource(id = R.string.aux_label, index + 1)) },
                                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color(0xFF54C7EC),
                                        unfocusedIndicatorColor = Color(0x66FFFFFF),
                                        cursorColor = Color.White,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color(0xCCFFFFFF),
                                        focusedContainerColor = Color(0x22FFFFFF),
                                        unfocusedContainerColor = Color(0x11FFFFFF)
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.plant_half_day_prompt),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = state.halfDay,
                            onCheckedChange = { state.halfDay = it },
                            thumbContent = {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(14.dp)
                                        .background(
                                            color = if (state.halfDay) Color(0xFF54C7EC) else Color.White,
                                            shape = CircleShape
                                        )
                                )
                            }
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {},
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF54C7EC),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = stringResource(id = R.string.save_assignments_action))
                    }
                }
            }
        }
    }
}

private fun ensureSize(list: MutableList<String>, expected: Int) {
    while (list.size < expected) {
        list.add("")
    }
    if (list.size > expected) {
        repeat(list.size - expected) { list.removeLastOrNull() }
    }
}
