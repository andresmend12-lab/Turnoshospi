package com.example.turnoshospi.util

import android.util.Log
import com.example.turnoshospi.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Wrapper para Firebase Crashlytics que facilita el logging de errores
 * en toda la aplicacion.
 *
 * Uso:
 * ```
 * try {
 *     // codigo que puede fallar
 * } catch (e: Exception) {
 *     CrashReporter.logError("MiClase", "Error al procesar datos", e)
 * }
 * ```
 */
object CrashReporter {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    /**
     * Inicializa Crashlytics con la configuracion apropiada.
     * Llamar desde MainActivity.onCreate() o Application.onCreate()
     *
     * @param enableInDebug Si es true, Crashlytics estara activo en debug builds.
     *                      Por defecto es false para no contaminar reportes con errores de desarrollo.
     */
    fun initialize(enableInDebug: Boolean = false) {
        val shouldEnable = !BuildConfig.DEBUG || enableInDebug
        crashlytics.setCrashlyticsCollectionEnabled(shouldEnable)

        if (BuildConfig.DEBUG) {
            Log.d("CrashReporter", "Crashlytics inicializado. Collection enabled: $shouldEnable")
        }
    }

    /**
     * Registra un error no fatal en Crashlytics.
     * El error aparecera en el dashboard de Firebase pero no como crash.
     *
     * @param tag Identificador del componente/clase donde ocurrio el error
     * @param message Descripcion del error
     * @param throwable Excepcion opcional para incluir stack trace
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        // Log local para debug
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }

        // Enviar a Crashlytics
        crashlytics.log("[$tag] $message")

        if (throwable != null) {
            crashlytics.recordException(throwable)
        }
    }

    /**
     * Registra un mensaje informativo en Crashlytics.
     * Util para agregar contexto antes de un crash.
     *
     * @param tag Identificador del componente/clase
     * @param message Mensaje informativo
     */
    fun log(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
        crashlytics.log("[$tag] $message")
    }

    /**
     * Establece el ID del usuario actual para correlacionar crashes.
     * Llamar despues del login exitoso.
     *
     * @param userId ID unico del usuario (Firebase Auth UID recomendado)
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
        if (BuildConfig.DEBUG) {
            Log.d("CrashReporter", "User ID set: $userId")
        }
    }

    /**
     * Limpia el ID del usuario (llamar en logout).
     */
    fun clearUserId() {
        crashlytics.setUserId("")
        if (BuildConfig.DEBUG) {
            Log.d("CrashReporter", "User ID cleared")
        }
    }

    /**
     * Establece un atributo personalizado para el reporte de crash.
     * Util para agregar contexto como nombre de planta, rol, etc.
     *
     * @param key Nombre del atributo
     * @param value Valor del atributo
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Establece un atributo personalizado numerico.
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Establece un atributo personalizado booleano.
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Fuerza un crash para testing.
     * SOLO usar en desarrollo para verificar que Crashlytics funciona.
     */
    fun testCrash() {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("Test Crash from CrashReporter")
        }
    }
}
