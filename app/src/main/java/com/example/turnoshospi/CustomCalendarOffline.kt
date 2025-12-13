package com.example.turnoshospi

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.turnoshospi.ui.theme.ShiftColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Enum para el patrón de turnos
enum class ShiftPatternMode {
    THREE_SHIFTS, // Mañana, Tarde, Noche
    TWO_SHIFTS    // Día (12h), Noche (12h)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCalendarOffline(
    shiftColors: ShiftColors
) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val sharedPreferences = remember {
        context.getSharedPreferences("TurnosOfflinePrefs", Context.MODE_PRIVATE)
    }

    // --- ESTADO DEL SCROLL PRINCIPAL ---
    val scrollState = rememberScrollState()

    // --- ESTADOS DE DATOS ---
    var localShifts by remember { mutableStateOf<Map<String, UserShift>>(emptyMap()) }
    var localNotes by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    // --- ESTADOS DE CONFIGURACIÓN ---
    var shiftPattern by remember { mutableStateOf(ShiftPatternMode.THREE_SHIFTS) }
    var allowHalfDays by remember { mutableStateOf(false) }

    var isDataLoaded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    // --- CARGAR DATOS Y PREFERENCIAS ---
    LaunchedEffect(Unit) {
        val shiftsJson = sharedPreferences.getString("shifts_map", null)
        if (shiftsJson != null) {
            val type = object : TypeToken<Map<String, UserShift>>() {}.type
            localShifts = gson.fromJson(shiftsJson, type)
        }
        val notesJson = sharedPreferences.getString("notes_map", null)
        if (notesJson != null) {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            localNotes = gson.fromJson(notesJson, type)
        }

        val patternOrdinal = sharedPreferences.getInt("shift_pattern", ShiftPatternMode.THREE_SHIFTS.ordinal)
        shiftPattern = ShiftPatternMode.values().getOrElse(patternOrdinal) { ShiftPatternMode.THREE_SHIFTS }
        allowHalfDays = sharedPreferences.getBoolean("allow_half_days", false)

        isDataLoaded = true
    }

    // --- GUARDAR AUTOMÁTICAMENTE ---
    LaunchedEffect(localShifts, localNotes, shiftPattern, allowHalfDays) {
        if (isDataLoaded) {
            val editor = sharedPreferences.edit()
            editor.putString("shifts_map", gson.toJson(localShifts))
            editor.putString("notes_map", gson.toJson(localNotes))
            editor.putInt("shift_pattern", shiftPattern.ordinal)
            editor.putBoolean("allow_half_days", allowHalfDays)
            editor.apply()
        }
    }

    val activeShiftOptions = remember(shiftPattern, allowHalfDays) {
        val options = mutableListOf<String>()
        if (shiftPattern == ShiftPatternMode.THREE_SHIFTS) {
            options.add("Mañana")
            options.add("Tarde")
            options.add("Noche")
            options.add("Saliente")
            if (allowHalfDays) {
                options.add("Media Mañana")
                options.add("Media Tarde")
            }
        } else {
            options.add("Día")
            options.add("Noche")
            options.add("Saliente")
            if (allowHalfDays) {
                options.add("Medio Día")
            }
        }
        options.add("Vacaciones")
        options.add("Libre")
        options
    }

    fun getDisplayName(raw: String): String {
        return when(raw) {
            "Mañana" -> context.getString(R.string.shift_morning)
            "Tarde" -> context.getString(R.string.shift_afternoon)
            "Noche" -> context.getString(R.string.shift_night)
            "Saliente" -> context.getString(R.string.shift_saliente)
            "Media Mañana" -> context.getString(R.string.shift_morning_half)
            "Media Tarde" -> context.getString(R.string.shift_afternoon_half)
            "Día" -> context.getString(R.string.shift_day)
            "Medio Día" -> "Medio Día"
            "Vacaciones" -> context.getString(R.string.shift_holiday)
            "Libre" -> context.getString(R.string.shift_free)
            else -> raw
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var isAssignmentMode by remember { mutableStateOf(false) }
    var selectedShiftToApply by remember { mutableStateOf("Mañana") }

    LaunchedEffect(activeShiftOptions) {
        if (selectedShiftToApply !in activeShiftOptions && activeShiftOptions.isNotEmpty()) {
            selectedShiftToApply = activeShiftOptions.first()
        }
    }

    var isAddingNote by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var editingNoteIndex by remember { mutableStateOf<Int?>(null) }
    var editingNoteText by remember { mutableStateOf("") }

    LaunchedEffect(selectedDate) {
        isAddingNote = false
        newNoteText = ""
        editingNoteIndex = null
        editingNoteText = ""
    }

    // --- CONTENEDOR PRINCIPAL CON SCROLL ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // <-- Scroll aplicado aquí
    ) {
        // Caja del calendario (sin weight, ocupa su espacio natural)
        Box(modifier = Modifier.fillMaxWidth()) {
            InternalOfflineCalendar(
                shifts = localShifts,
                selectedDate = selectedDate,
                shiftColors = shiftColors,
                notesMap = localNotes,
                onDayClick = { date ->
                    if (isAssignmentMode) {
                        val dateKey = date.toString()
                        if (selectedShiftToApply == "Libre") {
                            localShifts = localShifts - dateKey
                        } else {
                            val isHalf = selectedShiftToApply.contains("Media", ignoreCase = true) ||
                                    selectedShiftToApply.contains("Medio", ignoreCase = true)
                            localShifts = localShifts + (dateKey to UserShift(selectedShiftToApply, isHalf))
                        }
                    } else {
                        selectedDate = date
                    }
                }
            )

            // --- BOTÓN DE CONFIGURACIÓN (ENGRANAJE) ---
            if (!isAssignmentMode) {
                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configurar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isAssignmentMode) {
                FloatingActionButton(
                    onClick = { isAssignmentMode = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color(0xFF54C7EC),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_shifts))
                }
            }
        }

        if (!isAssignmentMode) {
            ShiftLegendDynamic(shiftColors = shiftColors, pattern = shiftPattern, allowHalfDays = allowHalfDays)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // --- PANEL INFERIOR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                if (isAssignmentMode) {
                    Text(
                        text = stringResource(R.string.mode_assignment),
                        color = Color(0xFF54C7EC),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(activeShiftOptions) { rawName ->
                            val isSelected = selectedShiftToApply == rawName
                            val displayName = getDisplayName(rawName)
                            val chipColor = getShiftColorForType(rawName, shiftColors)

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedShiftToApply = rawName },
                                label = { Text(displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = if(rawName == "Noche" || rawName == "Tarde") Color.White else Color.Black,
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = Color.White
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { isAssignmentMode = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.save_and_exit))
                    }

                } else {
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
                    val dateText = selectedDate?.format(formatter) ?: stringResource(R.string.select_day)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = dateText.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val currentShift = selectedDate?.let { localShifts[it.toString()] }
                        Text(
                            text = if (currentShift != null) getDisplayName(currentShift.shiftName) else getDisplayName("Libre"),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.3f))

                    NoteSection(
                        selectedDate = selectedDate,
                        localNotes = localNotes,
                        onNotesChanged = { newMap -> localNotes = newMap },
                        isAddingNote = isAddingNote,
                        onIsAddingNoteChange = { isAddingNote = it },
                        newNoteText = newNoteText,
                        onNewNoteTextChange = { newNoteText = it },
                        editingNoteIndex = editingNoteIndex,
                        onEditingNoteIndexChange = { editingNoteIndex = it },
                        editingNoteText = editingNoteText,
                        onEditingNoteTextChange = { editingNoteText = it }
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configuración de Turnos") },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            text = {
                Column {
                    Text("Tipo de Turnos:", fontWeight = FontWeight.Bold, color = Color(0xFF54C7EC))
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { shiftPattern = ShiftPatternMode.THREE_SHIFTS }) {
                        RadioButton(selected = shiftPattern == ShiftPatternMode.THREE_SHIFTS, onClick = { shiftPattern = ShiftPatternMode.THREE_SHIFTS }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF54C7EC), unselectedColor = Color.White))
                        Text("3 Turnos (M/T/N)", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { shiftPattern = ShiftPatternMode.TWO_SHIFTS }) {
                        RadioButton(selected = shiftPattern == ShiftPatternMode.TWO_SHIFTS, onClick = { shiftPattern = ShiftPatternMode.TWO_SHIFTS }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF54C7EC), unselectedColor = Color.White))
                        Text("2 Turnos (Día 12h / Noche 12h)", color = Color.White)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(0.3f))

                    Text("Opciones Extra:", fontWeight = FontWeight.Bold, color = Color(0xFF54C7EC))
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { allowHalfDays = !allowHalfDays }) {
                        Switch(checked = allowHalfDays, onCheckedChange = { allowHalfDays = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF54C7EC), checkedTrackColor = Color(0xFF0F172A)))
                        Spacer(Modifier.width(8.dp))
                        Text("Permitir Media Jornada", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cerrar", color = Color(0xFF54C7EC))
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// COMPONENTES AUXILIARES
// -------------------------------------------------------------------------

@Composable
fun NoteSection(
    selectedDate: LocalDate?,
    localNotes: Map<String, List<String>>,
    onNotesChanged: (Map<String, List<String>>) -> Unit,
    isAddingNote: Boolean,
    onIsAddingNoteChange: (Boolean) -> Unit,
    newNoteText: String,
    onNewNoteTextChange: (String) -> Unit,
    editingNoteIndex: Int?,
    onEditingNoteIndexChange: (Int?) -> Unit,
    editingNoteText: String,
    onEditingNoteTextChange: (String) -> Unit
) {
    if (selectedDate != null) {
        val dateKey = selectedDate.toString()
        val currentNotes = localNotes[dateKey] ?: emptyList()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.notes_title), color = Color(0xFF54C7EC), style = MaterialTheme.typography.labelLarge)

            if (!isAddingNote && editingNoteIndex == null) {
                IconButton(onClick = {
                    onIsAddingNoteChange(true)
                    onNewNoteTextChange("")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note), tint = Color.White)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            currentNotes.forEachIndexed { index, note ->
                if (editingNoteIndex == index) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editingNoteText,
                            onValueChange = onEditingNoteTextChange,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF54C7EC),
                                focusedBorderColor = Color(0xFF54C7EC),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        IconButton(onClick = {
                            if (editingNoteText.isNotBlank()) {
                                val updatedList = currentNotes.toMutableList()
                                updatedList[index] = editingNoteText
                                onNotesChanged(localNotes + (dateKey to updatedList))
                                onEditingNoteIndexChange(null)
                            }
                        }) { Icon(Icons.Default.Save, null, tint = Color(0xFF4CAF50)) }
                        IconButton(onClick = { onEditingNoteIndexChange(null) }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = note, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onEditingNoteIndexChange(index)
                            onEditingNoteTextChange(note)
                            onIsAddingNoteChange(false)
                        }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            val updatedList = currentNotes.toMutableList()
                            updatedList.removeAt(index)
                            onNotesChanged(localNotes + (dateKey to updatedList))
                        }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }

        if (isAddingNote) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = onNewNoteTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.write_here), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF54C7EC), unfocusedBorderColor = Color.Gray)
                )
                IconButton(onClick = {
                    if (newNoteText.isNotBlank()) {
                        val updatedList = currentNotes.toMutableList()
                        updatedList.add(newNoteText)
                        onNotesChanged(localNotes + (dateKey to updatedList))
                        onIsAddingNoteChange(false)
                        onNewNoteTextChange("")
                    }
                }) { Icon(Icons.Default.Save, null, tint = Color(0xFF4CAF50)) }
                IconButton(onClick = {
                    onIsAddingNoteChange(false)
                    onNewNoteTextChange("")
                }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
            }
        }
    }
}

// -------------------------------------------------------------------------
// LÓGICA DE CALENDARIO Y COLORES
// -------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InternalOfflineCalendar(
    shifts: Map<String, UserShift>,
    selectedDate: LocalDate?,
    shiftColors: ShiftColors,
    notesMap: Map<String, List<String>>,
    onDayClick: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier
            .fillMaxWidth() // Ocupa todo el ancho, altura dinámica
            .background(Color(0xFF0F172A))
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 40.dp),
        // .verticalScroll Eliminado para evitar scroll anidado
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera Mes
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${currentMonth.year}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Días Semana
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
            daysOfWeek.forEach { day -> Text(text = day, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Grid
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
                            val color = if (shift != null) getShiftColorForType(shift.shiftName, shiftColors) else {
                                val yesterday = date.minusDays(1).toString()
                                val yShift = shifts[yesterday]
                                if (yShift?.shiftName == "Noche") shiftColors.saliente else shiftColors.free
                            }

                            val isSelected = date == selectedDate
                            val hasNotes = !notesMap[dateKey].isNullOrEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f).height(48.dp).padding(2.dp)
                                    .background(color, CircleShape)
                                    .border(width = if(isSelected) 2.dp else 0.dp, color = if(isSelected) Color.White else Color.Transparent, shape = CircleShape)
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dayIndex.toString(), color = if (color == shiftColors.free) Color.White else Color.Black, fontWeight = FontWeight.Medium)
                                if (hasNotes) Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(6.dp).background(Color(0xFFE91E63), CircleShape))
                            }
                        } else { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

fun getShiftColorForType(type: String, colors: ShiftColors): Color {
    return when (type) {
        "Mañana" -> colors.morning
        "Tarde" -> colors.afternoon
        "Noche" -> colors.night
        "Saliente" -> colors.saliente
        "Día" -> colors.morning
        "Media Mañana" -> colors.morningHalf
        "Media Tarde" -> colors.afternoonHalf
        "Medio Día" -> colors.morningHalf
        "Vacaciones" -> colors.holiday
        else -> colors.free
    }
}

@Composable
fun ShiftLegendDynamic(shiftColors: ShiftColors, pattern: ShiftPatternMode, allowHalfDays: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (pattern == ShiftPatternMode.THREE_SHIFTS) {
                LegendItem(shiftColors.morning, stringResource(R.string.shift_morning))
                LegendItem(shiftColors.afternoon, stringResource(R.string.shift_afternoon))
                LegendItem(shiftColors.night, stringResource(R.string.shift_night))
            } else {
                LegendItem(shiftColors.morning, stringResource(R.string.shift_day))
                LegendItem(shiftColors.night, stringResource(R.string.shift_night))
            }
            LegendItem(shiftColors.saliente, stringResource(R.string.shift_saliente))
        }

        if (allowHalfDays || pattern == ShiftPatternMode.THREE_SHIFTS) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (allowHalfDays) {
                    if (pattern == ShiftPatternMode.THREE_SHIFTS) {
                        LegendItem(shiftColors.morningHalf, "Med. Mañana")
                        LegendItem(shiftColors.afternoonHalf, "Med. Tarde")
                    } else {
                        LegendItem(shiftColors.morningHalf, "Medio Día")
                    }
                }
                LegendItem(shiftColors.holiday, stringResource(R.string.shift_holiday))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
