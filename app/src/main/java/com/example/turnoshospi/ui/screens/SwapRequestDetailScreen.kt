package com.example.turnoshospi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.domain.model.ChatMessage
import com.example.turnoshospi.ui.viewmodel.SwapViewModel

@Composable
fun SwapRequestDetailScreen(
    swapId: String,
    viewModel: SwapViewModel,
    onBack: () -> Unit,
) {
    val messages = viewModel.messages
    val input = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalle de swap ${'$'}swapId")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(messages.value) { message ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${'$'}{message.senderUserId}: ${'$'}{message.text}")
                }
            }
        }
        OutlinedTextField(value = input.value, onValueChange = { input.value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Mensaje") })
        Button(onClick = {
            viewModel.sendMessage(
                ChatMessage(
                    swapRequestId = swapId,
                    text = input.value,
                    senderUserId = "me",
                    plantId = "plant"
                )
            )
            input.value = ""
        }) { Text("Enviar") }
        Button(onClick = onBack) { Text("Volver") }
    }
}
