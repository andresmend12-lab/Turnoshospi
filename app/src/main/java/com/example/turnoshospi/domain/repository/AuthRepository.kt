package com.example.turnoshospi.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de autenticacion que abstrae la fuente de datos (Firebase Auth).
 * Permite testing con mocks y desacopla la capa de presentacion de Firebase.
 */
interface AuthRepository {
    /**
     * Flow que emite el usuario actual. Emite null si no hay sesion activa.
     */
    val currentUser: Flow<FirebaseUser?>

    /**
     * Usuario actual de forma sincrona (puede ser null).
     */
    val currentUserSync: FirebaseUser?

    /**
     * Inicia sesion con email y contrasena.
     * @return Result.success con FirebaseUser si es exitoso, Result.failure con excepcion si falla
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>

    /**
     * Crea una nueva cuenta con email y contrasena.
     * @return Result.success con FirebaseUser si es exitoso, Result.failure con excepcion si falla
     */
    suspend fun createAccount(email: String, password: String): Result<FirebaseUser>

    /**
     * Envia un correo de restablecimiento de contrasena.
     * @return Result.success(Unit) si se envio correctamente, Result.failure si hubo error
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Cierra la sesion del usuario actual.
     */
    suspend fun signOut()

    /**
     * Elimina la cuenta del usuario actual de Firebase Auth.
     * Nota: Los datos del usuario en Realtime Database deben eliminarse por separado.
     * @return Result.success(Unit) si se elimino, Result.failure si hubo error
     */
    suspend fun deleteAccount(): Result<Unit>
}
