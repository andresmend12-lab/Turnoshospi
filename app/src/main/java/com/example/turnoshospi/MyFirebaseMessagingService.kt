package com.example.turnoshospi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Aquí podrías guardar el token en tu base de datos si es necesario actualizarlo
        // getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extraer datos del payload (Data Message)
        // Se asume que la Cloud Function envía: "screen", "plantId", "argument" (requestId)
        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Turnos Hospi"
        val body = remoteMessage.notification?.body ?: data["body"] ?: "Nueva notificación"

        val screenDest = data["screen"]
        val plantId = data["plantId"]
        val argument = data["argument"] // Puede ser el requestId

        sendNotification(title, body, screenDest, plantId, argument)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        screen: String?,
        plantId: String?,
        arg: String?
    ) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // PASAR DATOS PARA NAVEGACIÓN
        if (screen != null) intent.putExtra("nav_screen", screen)
        if (plantId != null) intent.putExtra("nav_plant_id", plantId)
        if (arg != null) intent.putExtra("nav_argument", arg)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "turnos_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de tener este icono o usa uno válido
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Fixed: Build.VERSION_CODES.O
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Turnos",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}