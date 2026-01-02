package com.example.turnoshospi.domain.model

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = 0,
    val type: String = "info", // info, shift_change, chat
    val read: Boolean = false,
    val deepLinkRoute: String? = null
)