package com.example.turnoshospi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Debe coincidir exactamente con el de index.js y AndroidManifest.xml
    companion object {
        const val CHANNEL_ID = "turnoshospi_sound_v2"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // Nota: Asegúrate de guardar este token en Firebase Database vinculado a tu usuario
        // normalmente en el Login o MainActivity.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // 1. Extraer Datos
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d("FCM", "Data payload: $data")
        }

        // 2. Determinar Título y Cuerpo
        // Prioridad: Notificación -> Data -> Fallback
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: "Tienes una nueva notificación"

        // 3. Extraer Datos de Navegación (Crucial para que coincida con index.js)
        val screenDest = data["screen"] // Ej: "DirectChat", "ShiftChangeScreen"
        val plantId = data["plantId"]

        // El index.js envía IDs con nombres distintos según el caso, aquí los unificamos
        // para pasarlos a la MainActivity como un único "nav_argument"
        var argument = data["argument"] // Si existe genérico
        if (argument == null) argument = data["requestId"] // Para cambios de turno
        if (argument == null) argument = data["chatId"]    // Para chats
        if (argument == null) argument = data["targetId"]  // Para notificaciones generales

        sendNotification(title, body, screenDest, plantId, argument)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        screen: String?,
        plantId: String?,
        arg: String?
    ) {
        // Intent para abrir MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            // CLEAR_TOP: Si la app ya está abierta, no crea otra instancia encima, sino que usa la existente
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            // Pasar datos extras para que MainActivity sepa a dónde navegar
            if (screen != null) putExtra("nav_screen", screen)
            if (plantId != null) putExtra("nav_plant_id", plantId)
            if (arg != null) putExtra("nav_argument", arg)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // RequestCode único para que los extras no se sobrescriban
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal si es Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Usamos nombres hardcodeados por seguridad, idealmente usa getString(R.string...)
            val channelName = "Notificaciones TurnosHospi"
            val channelDesc = "Avisos de cambios de turno y chats"

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
                enableLights(true)
                lightColor = Color.CYAN
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            // IMPORTANTE: Usa un icono drawable (blanco/transparente).
            // Si usas mipmap.ic_launcher aquí, saldrá un cuadrado blanco en Android modernos.
            .setSmallIcon(R.drawable.ic_logo_hospi_round)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody)) // Para textos largos
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            // Color de acento (opcional, para el texto del título o iconos)
            .setColor(Color.parseColor("#54C7EC"))

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}