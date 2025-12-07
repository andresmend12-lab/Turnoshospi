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

    // USAR EL MISMO ID QUE EN MAIN ACTIVITY Y MANIFEST
    companion object {
        const val CHANNEL_ID = "turnoshospi_sound_v2"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // Note: MainActivity handles saving the token if a user is logged in.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // 1. Extract data (Priority to DATA to handle navigation)
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d("FCM", "Data payload: $data")
        }

        // If Cloud Function sends 'notification' and app is in foreground, firebase fills this.
        // If it sends only 'data', we build the title/body ourselves.
        // We use strings.xml resources for fallbacks
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: getString(R.string.menu_info) // Generic fallback: "Information"

        // Navigation data
        val screenDest = data["screen"] // Must be "DirectChat", "ShiftChangeScreen", etc.
        val plantId = data["plantId"]
        val argument = data["argument"] // other user ID, requestId, etc.

        sendNotification(title, body, screenDest, plantId, argument)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        screen: String?,
        plantId: String?,
        arg: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Pass exact data so MainActivity can capture it in onNewIntent or onCreate
            if (screen != null) putExtra("nav_screen", screen)
            if (plantId != null) putExtra("nav_plant_id", plantId)
            if (arg != null) putExtra("nav_argument", arg)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // Unique RequestCode so extras are not overwritten if multiple notifs exist
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel if it doesn't exist (using translatable texts)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.notif_channel_name)
            val channelDesc = getString(R.string.notif_channel_desc)

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon is white/transparent (Android rule)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}