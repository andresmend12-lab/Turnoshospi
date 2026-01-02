package com.example.turnoshospi.domain.model

data class Shift(
    val id: String = "",
    val plantId: String = "",
    val userId: String = "",
    val date: String = "", // Formato YYYY-MM-DD
    val type: String = "", // Ej: "M", "T", "N"
    val isCovered: Boolean = true
)