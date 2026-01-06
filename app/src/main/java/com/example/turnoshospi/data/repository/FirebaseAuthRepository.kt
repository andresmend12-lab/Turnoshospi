package com.example.turnoshospi.data.repository

import com.example.turnoshospi.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementacion de AuthRepository usando Firebase Authentication.
 * Convierte los callbacks de Firebase a suspend functions usando coroutines.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    /**
     * Flow que emite cambios en el estado de autenticacion.
     * Usa callbackFlow para convertir el AuthStateListener a un Flow.
     */
    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Acceso sincrono al usuario actual.
     */
    override val currentUserSync: FirebaseUser?
        get() = auth.currentUser

    /**
     * Inicia sesion con email y contrasena.
     * Usa suspendCancellableCoroutine para convertir el callback a suspend.
     */
    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resume(
                            Result.failure(IllegalStateException("Usuario nulo despues de login exitoso"))
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
                .addOnCanceledListener {
                    continuation.resume(
                        Result.failure(IllegalStateException("Operacion de login cancelada"))
                    )
                }
        }
    }

    /**
     * Crea una nueva cuenta con email y contrasena.
     * Nota: El guardado del perfil en Realtime Database debe hacerse por separado.
     */
    override suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return suspendCancellableCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resume(
                            Result.failure(IllegalStateException("Usuario nulo despues de crear cuenta"))
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
                .addOnCanceledListener {
                    continuation.resume(
                        Result.failure(IllegalStateException("Operacion de crear cuenta cancelada"))
                    )
                }
        }
    }

    /**
     * Envia correo de restablecimiento de contrasena.
     * Configura el idioma del dispositivo antes de enviar.
     */
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            // Usar el idioma del dispositivo para el correo
            auth.useAppLanguage()

            auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
                .addOnCanceledListener {
                    continuation.resume(
                        Result.failure(IllegalStateException("Operacion de reset cancelada"))
                    )
                }
        }
    }

    /**
     * Cierra la sesion del usuario actual.
     * Esta operacion es sincrona en Firebase, pero la exponemos como suspend
     * para consistencia con el resto de la API.
     */
    override suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Elimina la cuenta del usuario actual.
     * IMPORTANTE: Solo elimina de Firebase Auth. Los datos en Realtime Database
     * deben eliminarse por separado antes de llamar a este metodo.
     */
    override suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No hay usuario autenticado"))

        return suspendCancellableCoroutine { continuation ->
            user.delete()
                .addOnSuccessListener {
                    // Cerrar sesion despues de eliminar cuenta
                    auth.signOut()
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
                .addOnCanceledListener {
                    continuation.resume(
                        Result.failure(IllegalStateException("Operacion de eliminar cuenta cancelada"))
                    )
                }
        }
    }
}
