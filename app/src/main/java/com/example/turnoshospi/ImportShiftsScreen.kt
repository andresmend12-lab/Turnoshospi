package com.example.turnoshospi

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportShiftsScreen(
    plant: Plant? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (plant == null) {
                importStatus = context.getString(R.string.error_plant_not_loaded)
                isError = true
                return@let
            }
            isImporting = true
            importStatus = context.getString(R.string.status_processing)
            isError = false

            processCsvImport(context, it, plant) { success, message ->
                isImporting = false
                isError = !success
                importStatus = message
                if (success) {
                    Toast.makeText(context, context.getString(R.string.msg_import_success), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_import_shifts), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.subtitle_import_options), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Button(
                        onClick = { createAndDownloadMatrixTemplate(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) { Text(stringResource(R.string.btn_download_template)) }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Button(
                        onClick = { importStatus = null; launcher.launch("text/*") },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                    ) { Text(if (isImporting) stringResource(R.string.btn_importing) else stringResource(R.string.btn_import)) }

                    if (importStatus != null) {
                        Text(
                            text = importStatus.orEmpty(),
                            color = if (isError) Color(0xFFFFB4AB) else Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}