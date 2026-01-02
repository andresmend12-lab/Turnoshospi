package com.example.turnoshospi.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.turnoshospi.data.repository.FirebaseChatRepository
import com.example.turnoshospi.domain.repository.ChatRepository

/**
 * Contenedor de dependencias de la aplicación.
 * Facilita la inyección manual y la testabilidad.
 */
interface AppContainer {
    val chatRepository: ChatRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val chatRepository: ChatRepository by lazy {
        FirebaseChatRepository()
    }
}

/**
 * CompositionLocal para acceder al contenedor desde cualquier Composable.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}