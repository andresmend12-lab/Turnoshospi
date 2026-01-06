package com.example.turnoshospi.util

import com.example.turnoshospi.BuildConfig
import com.google.firebase.database.FirebaseDatabase

/**
 * Configuracion centralizada de Firebase.
 *
 * La URL de la base de datos se define en build.gradle.kts como buildConfigField,
 * permitiendo usar diferentes URLs para debug/release si es necesario.
 */
object FirebaseConfig {

    /**
     * URL de Firebase Realtime Database.
     * Definida en build.gradle.kts -> defaultConfig -> buildConfigField
     */
    val DATABASE_URL: String = BuildConfig.FIREBASE_DATABASE_URL

    /**
     * Obtiene una instancia de FirebaseDatabase configurada con la URL correcta.
     * Usar este metodo en lugar de FirebaseDatabase.getInstance("...") directamente.
     */
    fun getDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance(DATABASE_URL)
    }
}
