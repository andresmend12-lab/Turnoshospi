package com.example.turnoshospi.util

/**
 * Constantes centralizadas de la aplicacion.
 * Evita duplicacion y facilita el mantenimiento.
 */
object Constants {

    // =========================================================================
    // NOTIFICACIONES
    // =========================================================================

    /**
     * ID del canal de notificaciones.
     * IMPORTANTE: Debe coincidir con:
     * - AndroidManifest.xml (meta-data com.google.firebase.messaging.default_notification_channel_id)
     * - functions/index.js (channelId en android.notification)
     */
    const val NOTIFICATION_CHANNEL_ID = "turnoshospi_sound_v2"

    /**
     * Nombre del canal de notificaciones (para mostrar en ajustes del sistema).
     */
    const val NOTIFICATION_CHANNEL_NAME = "Notificaciones TurnosHospi"

    /**
     * Descripcion del canal de notificaciones.
     */
    const val NOTIFICATION_CHANNEL_DESC = "Avisos de cambios de turno y chats"

    // =========================================================================
    // NAVEGACION - Claves de Intent Extras
    // =========================================================================

    const val NAV_EXTRA_SCREEN = "nav_screen"
    const val NAV_EXTRA_PLANT_ID = "nav_plant_id"
    const val NAV_EXTRA_ARGUMENT = "nav_argument"

    // =========================================================================
    // FIREBASE DATABASE PATHS
    // =========================================================================

    const val DB_PATH_USERS = "users"
    const val DB_PATH_PLANTS = "plants"
    const val DB_PATH_USER_NOTIFICATIONS = "user_notifications"
    const val DB_PATH_USER_PLANTS = "userPlants"
    const val DB_PATH_SECURITY_LOGS = "security_logs"
}
