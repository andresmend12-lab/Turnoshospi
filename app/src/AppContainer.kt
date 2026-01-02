package com.example.turnoshospi.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.turnoshospi.data.local.DataStoreOfflineStore
import com.example.turnoshospi.data.local.OfflineStore
import com.example.turnoshospi.data.repository.FirebaseAuthRepository
import com.example.turnoshospi.data.repository.FirebaseChatRepository
import com.example.turnoshospi.data.repository.FirebaseNotificationRepository
import com.example.turnoshospi.data.repository.FirebasePlantRepository
import com.example.turnoshospi.data.repository.FirebaseShiftRepository
import com.example.turnoshospi.domain.repository.AuthRepository
import com.example.turnoshospi.domain.repository.ChatRepository
import com.example.turnoshospi.domain.repository.NotificationRepository
import com.example.turnoshospi.domain.repository.PlantRepository
import com.example.turnoshospi.domain.repository.ShiftRepository

/**
 * Contenedor de dependencias de la aplicación.
 * Facilita la inyección manual y la testabilidad.
 */
interface AppContainer {
    val chatRepository: ChatRepository
    val shiftRepository: ShiftRepository
    val authRepository: AuthRepository
    val plantRepository: PlantRepository
    val offlineStore: OfflineStore
    val notificationRepository: NotificationRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val chatRepository: ChatRepository by lazy {
        FirebaseChatRepository()
    }
    override val shiftRepository: ShiftRepository by lazy {
        FirebaseShiftRepository()
    }
    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository()
    }
    override val plantRepository: PlantRepository by lazy {
        FirebasePlantRepository()
    }
    override val offlineStore: OfflineStore by lazy {
        DataStoreOfflineStore(context)
    }
    override val notificationRepository: NotificationRepository by lazy {
        FirebaseNotificationRepository()
    }
}

/**
 * CompositionLocal para acceder al contenedor desde cualquier Composable.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}