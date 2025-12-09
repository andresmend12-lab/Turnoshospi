package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data model for internal notification
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false,
    val screen: String? = null,   // Destination screen (e.g., "ShiftChangeScreen")
    val plantId: String? = null,  // Plant ID for context
    val argument: String? = null  // Extra argument (e.g., requestId, otherUserId)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onNavigateToScreen: (String, String?, String?) -> Unit // Route/Screen, PlantId, Argument
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_notifications),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = onDeleteAll) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.desc_delete_all),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.msg_no_notifications),
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notif ->
                        NotificationItem(
                            notification = notif,
                            onClick = {
                                // Mark as read
                                if (!notif.read) {
                                    onMarkAsRead(notif.id)
                                }
                                // Navigate
                                if (notif.screen != null) {
                                    onNavigateToScreen(notif.screen, notif.plantId, notif.argument)
                                }
                            },
                            onDelete = {
                                onDelete(notif.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
 
@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) Color(0xFF1E293B) else Color(0xFF2D3B4E)
        ),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread indicator
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF54C7EC), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = notification.message,
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateString,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.desc_delete),
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}