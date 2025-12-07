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
import com.example.turnoshospi.R // Asegúrate de importar tu R
import com.example.turnoshospi.ui.theme.ShiftColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

    // --- ESTADOS DE DATOS ---
    var localShifts by remember { mutableStateOf<Map<String, UserShift>>(emptyMap()) }
    var localNotes by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // --- CARGAR DATOS ---
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
        isDataLoaded = true
    }

    // --- GUARDAR AUTOMÁTICAMENTE ---
    LaunchedEffect(localShifts) {
        if (isDataLoaded) {
            val json = gson.toJson(localShifts)
            sharedPreferences.edit().putString("shifts_map", json).apply()
        }
    }
    LaunchedEffect(localNotes) {
        if (isDataLoaded) {
            val json = gson.toJson(localNotes)
            sharedPreferences.edit().putString("notes_map", json).apply()
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var isAssignmentMode by remember { mutableStateOf(false) }

    // Referencias a los strings para usar en lógica
    val strMorning = stringResource(R.string.shift_morning)
    val strFree = stringResource(R.string.shift_free)

    var selectedShiftToApply by remember { mutableStateOf(strMorning) }

    // LISTA DE TIPOS VINCULADA A RECURSOS
    // Usamos pares para tener fácil acceso en el bucle: (NombreDisplay)
    val shiftTypes = listOf(
        stringResource(R.string.shift_morning),
        stringResource(R.string.shift_afternoon),
        stringResource(R.string.shift_night),
        stringResource(R.string.shift_saliente),
        stringResource(R.string.shift_morning_half),
        stringResource(R.string.shift_afternoon_half),
        stringResource(R.string.shift_holiday),
        stringResource(R.string.shift_free)
    )

    // Estados para notas
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ---------------------------------------------------------
        // CALENDARIO
        // ---------------------------------------------------------
        Box(modifier = Modifier.weight(1f)) {
            InternalOfflineCalendar(
                shifts = localShifts,
                selectedDate = selectedDate,
                shiftColors = shiftColors,
                notesMap = localNotes,
                onDayClick = { date ->
                    if (isAssignmentMode) {
                        // MODO PINTAR
                        val dateKey = date.toString()
                        if (selectedShiftToApply == strFree) {
                            localShifts = localShifts - dateKey
                        } else {
                            // Detectar si es media jornada basado en el string
                            // Nota: Esto depende del string localizado. Lo ideal a futuro es usar IDs/Enums.
                            val isHalf = selectedShiftToApply.contains("M.", ignoreCase = true) ||
                                    selectedShiftToApply.contains("Half", ignoreCase = true)

                            localShifts = localShifts + (dateKey to UserShift(selectedShiftToApply, isHalf))
                        }
                    } else {
                        // MODO SELECCIÓN
                        selectedDate = date
                    }
                }
            )

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
            ShiftLegend(shiftColors = shiftColors)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---------------------------------------------------------
        // PANEL INFERIOR
        // ---------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                if (isAssignmentMode) {
                    // --- MODO ASIGNACIÓN ---
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
                        items(shiftTypes) { typeName ->
                            val isSelected = selectedShiftToApply == typeName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedShiftToApply = typeName },
                                label = { Text(typeName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF54C7EC),
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color.White,
                                    selectedLabelColor = Color.Black
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = Color.Transparent
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
                    // --- MODO VISUALIZACIÓN / NOTAS ---

                    // Formato de fecha localizado
                    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
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
                            text = currentShift?.shiftName ?: stringResource(R.string.shift_free),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.3f))

                    // SECCIÓN DE NOTAS
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
                                    isAddingNote = true
                                    newNoteText = ""
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note), tint = Color.White)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentNotes.forEachIndexed { index, note ->
                                if (editingNoteIndex == index) {
                                    // Editando
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = editingNoteText,
                                            onValueChange = { editingNoteText = it },
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
                                                localNotes = localNotes + (dateKey to updatedList)
                                                editingNoteIndex = null
                                            }
                                        }) {
                                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = Color(0xFF4CAF50))
                                        }
                                        IconButton(onClick = { editingNoteIndex = null }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.Red)
                                        }
                                    }
                                } else {
                                    // Viendo
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = note,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingNoteIndex = index
                                                    editingNoteText = note
                                                    isAddingNote = false
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Color(0xFF54C7EC), modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    val updatedList = currentNotes.toMutableList()
                                                    updatedList.removeAt(index)
                                                    localNotes = localNotes + (dateKey to updatedList)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isAddingNote) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newNoteText,
                                    onValueChange = { newNoteText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.write_here), color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color(0xFF54C7EC),
                                        focusedBorderColor = Color(0xFF54C7EC),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                IconButton(onClick = {
                                    if (newNoteText.isNotBlank()) {
                                        val updatedList = currentNotes.toMutableList()
                                        updatedList.add(newNoteText)
                                        localNotes = localNotes + (dateKey to updatedList)
                                        isAddingNote = false
                                        newNoteText = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = Color(0xFF4CAF50))
                                }
                                IconButton(onClick = {
                                    isAddingNote = false
                                    newNoteText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.Red)
                                }
                            }
                        } else if (currentNotes.isEmpty() && editingNoteIndex == null) {
                            Text(
                                stringResource(R.string.no_notes_hint),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// LÓGICA INTERNA DE CALENDARIO (Localizada)
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

    // Obtenemos los strings actuales para pasar a la lógica de color
    val context = LocalContext.current
    // Creamos un mapa simple para facilitar la lógica de colores
    val localizedShiftMap = mapOf(
        "holiday" to stringResource(R.string.shift_holiday),
        "night" to stringResource(R.string.shift_night),
        "morning" to stringResource(R.string.shift_morning),
        "afternoon" to stringResource(R.string.shift_afternoon),
        "morning_half" to stringResource(R.string.shift_morning_half),
        "afternoon_half" to stringResource(R.string.shift_afternoon_half),
        "saliente" to stringResource(R.string.shift_saliente)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_prev_month), tint = Color.White)
            }

            // NOMBRE DEL MES LOCALIZADO
            val monthTitle = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(
                text = "$monthTitle ${currentMonth.year}".uppercase(Locale.getDefault()),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.desc_next_month), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Días de la semana (Localizados o estáticos L, M, X...)
        // Para simplificar, mantenemos L, M, X pero se podrían localizar usando DateSymbols
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

        // Cuadrícula
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

                            // Pasamos el mapa de strings para comparar
                            val color = getOfflineDayColor(date, shifts, shiftColors, localizedShiftMap)
                            val isSelected = date == selectedDate
                            val hasNotes = !notesMap[dateKey].isNullOrEmpty()

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
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                if (hasNotes) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 2.dp)
                                            .size(8.dp)
                                            .background(Color(0xFFE91E63), CircleShape)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    }
}

// Lógica de color actualizada para usar los strings locales
private fun getOfflineDayColor(
    date: LocalDate,
    shifts: Map<String, UserShift>,
    colors: ShiftColors,
    localizedMap: Map<String, String> // Recibimos los strings reales (ej: "Vacaciones" o "Holidays")
): Color {
    val dateKey = date.toString()
    val shift = shifts[dateKey]

    if (shift != null) {
        val type = shift.shiftName.lowercase() // Nombre guardado en minúsculas

        // Comparamos el nombre guardado con los strings localizados en minúsculas
        return when {
            type.contains(localizedMap["holiday"]!!.lowercase()) -> colors.holiday
            type.contains(localizedMap["night"]!!.lowercase()) -> colors.night
            type.contains(localizedMap["morning_half"]!!.lowercase()) -> colors.morningHalf
            type.contains(localizedMap["morning"]!!.lowercase()) -> colors.morning
            type.contains(localizedMap["afternoon_half"]!!.lowercase()) -> colors.afternoonHalf
            type.contains(localizedMap["afternoon"]!!.lowercase()) -> colors.afternoon
            type.contains(localizedMap["saliente"]!!.lowercase()) -> colors.saliente
            else -> colors.morning // Fallback por defecto
        }
    }

    // Lógica automática de saliente
    val yesterdayKey = date.minusDays(1).toString()
    val yesterdayShift = shifts[yesterdayKey]
    if (yesterdayShift != null && yesterdayShift.shiftName.lowercase().contains(localizedMap["night"]!!.lowercase())) {
        return colors.saliente
    }

    return colors.free
}

@Composable
fun ShiftLegend(shiftColors: ShiftColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(shiftColors.morning, stringResource(R.string.shift_morning))
            LegendItem(shiftColors.afternoon, stringResource(R.string.shift_afternoon))
            LegendItem(shiftColors.night, stringResource(R.string.shift_night))
            LegendItem(shiftColors.saliente, stringResource(R.string.shift_saliente))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fila 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(shiftColors.morningHalf, stringResource(R.string.shift_morning_half))
            LegendItem(shiftColors.afternoonHalf, stringResource(R.string.shift_afternoon_half))
            LegendItem(shiftColors.holiday, stringResource(R.string.shift_holiday))
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}