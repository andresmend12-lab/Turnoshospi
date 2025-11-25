package com.example.turnoshospi.ui.plants.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.domain.model.plants.ShiftType

@Composable
fun CreatePlantScreen(
    viewModel: CreatePlantViewModel,
    onFinished: (plantId: String, inviteCode: String, inviteLink: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val supervisorName = remember { mutableStateOf("") }

    state.success?.let { success ->
        onFinished(success.plantId, success.inviteCode, success.inviteLink)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Crear Planta")
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre de la planta") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción breve") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = supervisorName.value,
                onValueChange = { supervisorName.value = it },
                label = { Text("Nombre del supervisor") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tipos de turno")
                Button(onClick = { viewModel.addShiftType() }) {
                    Text("Añadir turno")
                }
            }
        }

        items(state.shiftTypes, key = { it.id }) { shift ->
            ShiftTypeEditor(
                shiftType = shift,
                onChange = { updated -> viewModel.updateShiftType(shift.id) { updated } },
                onRemove = { viewModel.removeShiftType(shift.id) }
            )
        }

        item {
            Text(text = "Requerimientos por día")
            RequiredStaffEditor(
                days = CreatePlantViewModel.defaultDays,
                shiftTypes = state.shiftTypes,
                values = state.requiredStaff,
                onChange = viewModel::updateRequiredStaff
            )
        }

        item {
            state.error?.let { errorMessage ->
                Text(text = errorMessage, modifier = Modifier.padding(vertical = 4.dp))
            }
            Button(
                onClick = { viewModel.createPlant(supervisorName.value) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Crear planta")
            }
        }
    }

    if (state.loading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("Guardando planta...")
                }
            }
        )
    }
}

@Composable
private fun ShiftTypeEditor(
    shiftType: ShiftType,
    onChange: (ShiftType) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = shiftType.label.ifBlank { "Turno sin nombre" })
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar turno")
                }
            }
            OutlinedTextField(
                value = shiftType.label,
                onValueChange = { onChange(shiftType.copy(label = it)) },
                label = { Text("Etiqueta") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = shiftType.startTime,
                    onValueChange = { onChange(shiftType.copy(startTime = it)) },
                    label = { Text("Hora inicio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shiftType.endTime,
                    onValueChange = { onChange(shiftType.copy(endTime = it)) },
                    label = { Text("Hora fin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = shiftType.durationMinutes.takeIf { it > 0 }?.toString() ?: "",
                onValueChange = { minutes ->
                    val parsed = minutes.toIntOrNull() ?: 0
                    onChange(shiftType.copy(durationMinutes = parsed))
                },
                label = { Text("Duración (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Turno nocturno")
                    Switch(
                        checked = shiftType.isNightShift,
                        onCheckedChange = { onChange(shiftType.copy(isNightShift = it)) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Media jornada")
                    Switch(
                        checked = shiftType.isHalfDay,
                        onCheckedChange = { onChange(shiftType.copy(isHalfDay = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequiredStaffEditor(
    days: List<String>,
    shiftTypes: List<ShiftType>,
    values: Map<String, Map<String, Int>>,
    onChange: (dayKey: String, shiftId: String, count: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEach { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = day.replaceFirstChar { it.titlecase() })
                    if (shiftTypes.isEmpty()) {
                        Text(text = "Añade un turno para configurar requerimientos")
                    }
                    shiftTypes.forEach { shift ->
                        val count = values[day]?.get(shift.id) ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = shift.label.ifBlank { "Turno" })
                            OutlinedTextField(
                                value = count.toString(),
                                onValueChange = { value ->
                                    onChange(day, shift.id, value.toIntOrNull() ?: 0)
                                },
                                label = { Text("Requeridos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCreationSuccessDialog(
    plantId: String,
    inviteCode: String,
    inviteLink: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Aceptar") }
        },
        title = { Text("Planta creada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ID: $plantId")
                Text("Código de invitación: $inviteCode")
                Text("Enlace: $inviteLink")
            }
        }
    )
}

const val CreatePlantRoute = "create_plant"

fun exampleNavigationUsage() {
    // Example only; integrate within existing NavHost without altering current graph
    // navGraphBuilder.composable(CreatePlantRoute) { CreatePlantScreen(viewModel = hiltViewModel(), onFinished = { _, _, _ -> }) }
}
