package com.example.turnoshospi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Si el token cambia (ej: reinstalar app), lo actualizamos en la BD
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("users/${user.uid}/fcmToken")
                .setValue(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extraer datos del mensaje
        val data = remoteMessage.data
        // Si el mensaje viene con "notification" (payload automático), úsalo como fallback
        val title = data["title"] ?: remoteMessage.notification?.title ?: "Turnoshospi"
        val body = data["body"] ?: remoteMessage.notification?.body ?: "Tienes una nueva notificación"
        val targetScreen = data["screen"] ?: "MainMenu"

        showNotification(title, body, targetScreen)
    }

    private fun showNotification(title: String, message: String, targetScreen: String) {
        val channelId = "turnoshospi_alerts"
        val notificationId = Random.nextInt()

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigation_target", targetScreen)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Crear la notificación visual
        val builder = NotificationCompat.Builder(this, channelId)
            // Asegúrate de que este icono exista (por defecto suele ser ic_launcher_foreground o crea uno propio)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal de notificaciones (Obligatorio en Android 8+)
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            val channel = NotificationChannel(
                channelId,
                "Avisos de Turnos",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(notificationId, builder.build())
    }
}