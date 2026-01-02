package com.example.turnoshospi.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PLANT_DETAIL = "plant/{plantId}"
    const val GROUP_CHAT = "chat/group/{plantId}"
    
    fun groupChat(plantId: String) = "chat/group/$plantId"
}