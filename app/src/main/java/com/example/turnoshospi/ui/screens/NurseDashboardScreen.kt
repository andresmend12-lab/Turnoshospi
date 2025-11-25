package com.example.turnoshospi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.viewmodel.DashboardViewModel

@Composable
fun NurseDashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSwap: (String) -> Unit,
    onFeedback: () -> Unit,
) {
    val shifts = viewModel.shifts.collectAsState()

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Calendario mensual", style = MaterialTheme.typography.titleLarge)
        Card { Column(Modifier.padding(12.dp)) { Text("Vista calendario (placeholder)") } }
        Divider()
        Text("Mis turnos", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shifts.value) { shift ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${'$'}{shift.date}: ${'$'}{shift.shiftTypeId}")
                    AssistChip(onClick = { onOpenSwap(shift.id) }, label = { Text("Buscar cambio") })
                }
            }
        }
        AssistChip(onClick = onFeedback, label = { Text("Enviar feedback") })
    }
}
