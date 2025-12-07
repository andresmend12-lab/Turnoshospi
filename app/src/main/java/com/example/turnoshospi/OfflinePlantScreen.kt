package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.ShiftColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePlantScreen(
    onBack: () -> Unit,
    shiftColors: ShiftColors
) {
    // Estado local para los turnos (Nota: al cerrar la app se perderán si no usas Room/DataStore)
    var localShifts by remember { mutableStateOf<Map<String, UserShift>>(emptyMap()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showAddShiftDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Turnario Offline", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
        ) {
            CustomCalendar(
                shifts = localShifts,
                plantId = "OFFLINE", // ID ficticio para habilitar la leyenda
                selectedDate = selectedDate,
                selectedShift = selectedDate?.let { localShifts[it.toString()] },
                colleagues = emptyList(),
                isLoadingColleagues = false,
                isSupervisor = false,
                shiftColors = shiftColors,
                onDayClick = { date, _ ->
                    selectedDate = date
                    showAddShiftDialog = true
                }
            )

            if (showAddShiftDialog && selectedDate != null) {
                OfflineShiftDialog(
                    date = selectedDate!!,
                    currentShift = localShifts[selectedDate.toString()],
                    onDismiss = { showAddShiftDialog = false },
                    onSave = { shiftName ->
                        val dateKey = selectedDate.toString()
                        if (shiftName == "Libre") {
                            localShifts = localShifts - dateKey
                        } else {
                            localShifts = localShifts + (dateKey to UserShift(shiftName, false))
                        }
                        showAddShiftDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineShiftDialog(
    date: LocalDate,
    currentShift: UserShift?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val options = listOf("Mañana", "Tarde", "Noche", "Libre")
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(currentShift?.shiftName ?: "Libre") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Turno - $date") },
        text = {
            Column {
                options.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(text) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF54C7EC))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = text, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedOption) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC))
            ) {
                Text("Guardar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}