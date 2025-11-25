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
fun SupervisorDashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSwap: (String) -> Unit,
    onFeedback: () -> Unit,
) {
    val requests = viewModel.pendingRequests.collectAsState()

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Panel supervisor", style = MaterialTheme.typography.titleLarge)
        Card { Column(Modifier.padding(12.dp)) { Text("Indicadores rápidos (placeholder)") } }
        Divider()
        Text("Peticiones de cambio", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(requests.value) { request ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${'$'}{request.type} - ${'$'}{request.status}")
                    AssistChip(onClick = { onOpenSwap(request.id) }, label = { Text("Revisar") })
                }
            }
        }
        AssistChip(onClick = onFeedback, label = { Text("Feedback/Sugerencias") })
    }
}
