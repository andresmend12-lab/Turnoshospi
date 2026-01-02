package com.example.turnoshospi.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.turnoshospi.data.repository.FirebaseAuthRepository
import com.example.turnoshospi.data.repository.FirebaseChatRepository
import com.example.turnoshospi.data.repository.FirebasePlantRepository
import com.example.turnoshospi.domain.repository.AuthRepository
import com.example.turnoshospi.domain.repository.ChatRepository
import com.example.turnoshospi.domain.repository.PlantRepository

/**
 * Contenedor de dependencias de la aplicación.
 * Facilita la inyección manual y la testabilidad.
 */
interface AppContainer {
    val chatRepository: ChatRepository
    val authRepository: AuthRepository
    val plantRepository: PlantRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val chatRepository: ChatRepository by lazy {
        FirebaseChatRepository()
    }
    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository()
    }
    override val plantRepository: PlantRepository by lazy {
        FirebasePlantRepository()
    }
}

/**
 * CompositionLocal para acceder al contenedor desde cualquier Composable.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}