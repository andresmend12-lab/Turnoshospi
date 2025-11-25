package com.example.turnoshospi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.domain.model.FeedbackEntry
import com.example.turnoshospi.ui.viewmodel.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(viewModel: FeedbackViewModel, onBack: () -> Unit) {
    val selectedType = remember { mutableStateOf("bug") }
    val expanded = remember { mutableStateOf(false) }
    val message = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Envíanos tu feedback")
        ExposedDropdownMenuBox(expanded = expanded.value, onExpandedChange = { expanded.value = it }) {
            OutlinedTextField(
                value = selectedType.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                listOf("bug", "feature_request", "other").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        selectedType.value = option
                        expanded.value = false
                    })
                }
            }
        }
        OutlinedTextField(
            value = message.value,
            onValueChange = { message.value = it },
            label = { Text("Mensaje") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            viewModel.sendFeedback(
                FeedbackEntry(
                    userId = "me",
                    plantId = null,
                    type = selectedType.value,
                    message = message.value,
                )
            )
            onBack()
        }) { Text("Enviar") }
    }
}
