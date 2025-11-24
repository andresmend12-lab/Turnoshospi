package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TurnoshospiTheme {
                SetupScreen()
            }
        }
    }
}

data class SetupItem(val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val prerequisites = listOf(
        SetupItem(
            title = "Android Studio y SDKs",
            description = "Instala Android Studio Iguana o superior con el SDK de Android 24+ y el emulador actualizado."
        ),
        SetupItem(
            title = "Java y Kotlin",
            description = "Configura JDK 11 para las builds y habilita Kotlin en el asistente de proyecto si creas un módulo nuevo."
        ),
        SetupItem(
            title = "Emulador o dispositivo",
            description = "Prepara un dispositivo físico con opciones de desarrollador o crea un AVD con Google APIs para pruebas."
        )
    )

    val projectSteps = listOf(
        SetupItem(
            title = "Sincronizar Gradle",
            description = "Abre el proyecto en Android Studio y ejecuta 'Sync Project with Gradle Files' para descargar dependencias."
        ),
        SetupItem(
            title = "Configurar ejecución",
            description = "Selecciona el módulo 'app', elige el dispositivo de destino y usa el botón 'Run' para lanzar la app."
        ),
        SetupItem(
            title = "Verificación rápida",
            description = "Si la app inicia, deberías ver este resumen; desde ahí añade pantallas y navegación según tus requerimientos."
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "Guía de arranque", style = MaterialTheme.typography.titleLarge) })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                IntroCard()
            }
            item {
                SectionTitle(text = "Requisitos previos")
            }
            items(prerequisites) { item ->
                SetupCard(item)
            }
            item {
                SectionTitle(text = "Pasos en Android Studio")
            }
            items(projectSteps) { item ->
                SetupCard(item)
            }
            item {
                FooterNote()
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Turnoshospi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Usa esta guía rápida para validar que tu entorno Compose está listo antes de agregar funcionalidades específicas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SetupCard(item: SetupItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FooterNote() {
    Text(
        text = "Con el entorno listo, puedes conectar tu backend de turnos, definir navegación y agregar pruebas instrumentadas.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    TurnoshospiTheme {
        SetupScreen()
    }
}
