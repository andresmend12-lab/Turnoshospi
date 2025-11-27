package com.example.turnoshospi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 1. MODELO DE DATOS ---
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // INFO, SHIFT_ADDED, REQUEST, MATCH, APPROVED
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

// --- 2. PANTALLA DE NOTIFICACIONES ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: String,
    onBack: () -> Unit
) {
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val notifsRef = database.getReference("users/$userId/notifications")
    val notifications = remember { mutableStateListOf<AppNotification>() }

    // Cargar notificaciones
    LaunchedEffect(userId) {
        notifsRef.orderByChild("timestamp").limitToLast(50).get().addOnSuccessListener { snapshot ->
            notifications.clear()
            snapshot.children.mapNotNull { it.getValue(AppNotification::class.java) }
                .reversed() // Más nuevas primero
                .forEach { notifications.add(it) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes notificaciones", color = Color(0x88FFFFFF))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    NotificationCard(notif) {
                        // Marcar como leída en Firebase al hacer clic
                        if (!notif.read) {
                            notifsRef.child(notif.id).child("read").setValue(true)
                            val index = notifications.indexOf(notif)
                            if (index != -1) {
                                notifications[index] = notif.copy(read = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val bgColor = if (notification.read) Color(0x11FFFFFF) else Color(0x2254C7EC)
    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (notification.read) Color.Gray else Color(0xFF54C7EC),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(notification.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(notification.message, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(notification.timestamp)),
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- 3. HELPER PARA NOTIFICACIONES DEL SISTEMA ---
object NotificationHelper {
    private const val CHANNEL_ID = "turnoshospi_alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Turnos"
            val descriptionText = "Notificaciones de cambios y asignaciones"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // No tenemos permiso
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Usa un icono nativo por defecto
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    // Función para ENVIAR notificación a Firebase
    fun sendNotification(targetUserId: String, title: String, message: String, type: String) {
        if (targetUserId.isBlank()) return
        val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
        val ref = database.getReference("users/$targetUserId/notifications").push()
        val notif = AppNotification(
            id = ref.key ?: "",
            title = title,
            message = message,
            type = type
        )
        ref.setValue(notif)
    }
}