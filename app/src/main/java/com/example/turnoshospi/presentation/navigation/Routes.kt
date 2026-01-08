package com.example.turnoshospi.presentation.navigation

import android.net.Uri

/**
 * Define todas las rutas de navegacion de la app.
 * Usa sealed class para type-safety.
 */
sealed class Route(val route: String) {

    // --- Pantallas sin argumentos ---

    data object Login : Route("login")
    data object MainMenu : Route("main_menu")
    data object CreatePlant : Route("create_plant")
    data object PlantCreated : Route("plant_created")
    data object MyPlant : Route("my_plant")
    data object StaffManagement : Route("staff_management")
    data object Settings : Route("settings")
    data object PlantSettings : Route("plant_settings")
    data object ImportShifts : Route("import_shifts")
    data object GroupChat : Route("group_chat")
    data object ShiftMarketplace : Route("shift_marketplace")
    data object DirectChatList : Route("direct_chat_list")
    data object Notifications : Route("notifications")
    data object LegalInfo : Route("legal_info")

    // --- Pantallas con argumentos ---

    data object PlantDetail : Route("plant_detail/{plantId}") {
        const val ARG_PLANT_ID = "plantId"

        fun createRoute(plantId: String): String = "plant_detail/$plantId"

        fun deepLinkPattern(): String = "turnoshospi://plant/{$ARG_PLANT_ID}"
    }

    data object ShiftChange : Route("shift_change/{plantId}/{currentUserId}") {
        const val ARG_PLANT_ID = "plantId"
        const val ARG_CURRENT_USER_ID = "currentUserId"

        fun createRoute(plantId: String, currentUserId: String): String =
            "shift_change/$plantId/$currentUserId"

        fun deepLinkPattern(): String = "turnoshospi://shifts/{$ARG_PLANT_ID}"
    }

    data object Statistics : Route("statistics/{plantId}") {
        const val ARG_PLANT_ID = "plantId"

        fun createRoute(plantId: String): String = "statistics/$plantId"
    }

    data object DirectChat : Route("direct_chat/{chatId}/{otherUserId}/{otherUserName}") {
        const val ARG_CHAT_ID = "chatId"
        const val ARG_OTHER_USER_ID = "otherUserId"
        const val ARG_OTHER_USER_NAME = "otherUserName"

        fun createRoute(chatId: String, otherUserId: String, otherUserName: String): String {
            // Encode para manejar caracteres especiales en el nombre
            val encodedName = Uri.encode(otherUserName)
            return "direct_chat/$chatId/$otherUserId/$encodedName"
        }

        fun deepLinkPattern(): String =
            "turnoshospi://chat/{$ARG_CHAT_ID}/{$ARG_OTHER_USER_ID}/{$ARG_OTHER_USER_NAME}"
    }

    companion object {
        /** Deep link base URI para la app */
        const val DEEP_LINK_URI = "turnoshospi://"

        /** Deep link para notificaciones generales */
        const val NOTIFICATIONS_DEEP_LINK = "${DEEP_LINK_URI}notifications"
    }
}

/**
 * Extension para facilitar navegacion desde notificaciones.
 * Parsea el screen name y construye la ruta apropiada.
 */
fun parseNotificationRoute(screen: String?, plantId: String? = null, chatId: String? = null): String {
    return when (screen) {
        "PlantDetail", "MyPlant" -> {
            if (!plantId.isNullOrBlank()) Route.PlantDetail.createRoute(plantId)
            else Route.MyPlant.route
        }
        "ShiftChange" -> {
            if (!plantId.isNullOrBlank()) Route.ShiftChange.createRoute(plantId, "")
            else Route.MainMenu.route
        }
        "DirectChat" -> {
            if (!chatId.isNullOrBlank()) Route.DirectChatList.route // Navegar a lista y luego al chat
            else Route.DirectChatList.route
        }
        "Notifications" -> Route.Notifications.route
        "GroupChat" -> Route.GroupChat.route
        else -> Route.MainMenu.route
    }
}
