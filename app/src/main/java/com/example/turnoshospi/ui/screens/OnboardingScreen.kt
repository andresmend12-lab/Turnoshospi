package com.example.turnoshospi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.domain.model.UserProfile

@Composable
fun OnboardingScreen(
    profile: UserProfile?,
    onCreatePlant: (String, String) -> Unit,
    onJoinPlant: (String) -> Unit,
    onDone: () -> Unit,
) {
    val plantName = remember { mutableStateOf("") }
    val plantDescription = remember { mutableStateOf("") }
    val invitation = remember { mutableStateOf("") }
    val displayName = profile?.displayName.orEmpty()

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Hola ${'$'}displayName", style = MaterialTheme.typography.titleLarge)
        Card { Column(Modifier.padding(12.dp)) {
            Text("Crear planta")
            OutlinedTextField(value = plantName.value, onValueChange = { plantName.value = it }, label = { Text("Nombre") })
            OutlinedTextField(value = plantDescription.value, onValueChange = { plantDescription.value = it }, label = { Text("Descripción") })
            Button(onClick = { onCreatePlant(plantName.value, plantDescription.value); onDone() }) { Text("Crear") }
        } }
        Card { Column(Modifier.padding(12.dp)) {
            Text("Unirme a planta")
            OutlinedTextField(value = invitation.value, onValueChange = { invitation.value = it }, label = { Text("Código") })
            Button(onClick = { onJoinPlant(invitation.value); onDone() }) { Text("Unirme") }
        } }
    }
}
