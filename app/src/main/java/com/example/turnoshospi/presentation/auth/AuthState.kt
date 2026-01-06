package com.example.turnoshospi.presentation.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Estados posibles para operaciones de autenticacion.
 * Usa sealed class para type-safety y exhaustive when.
 */
sealed class AuthState<out T> {
    /** Estado inicial, sin operacion en curso */
    data object Idle : AuthState<Nothing>()

    /** Operacion en curso */
    data object Loading : AuthState<Nothing>()

    /** Operacion exitosa con resultado */
    data class Success<T>(val data: T) : AuthState<T>()

    /** Operacion fallida con mensaje de error */
    data class Error(val message: String, val exception: Throwable? = null) : AuthState<Nothing>()
}

/**
 * Estado especifico para login.
 */
typealias LoginState = AuthState<FirebaseUser>

/**
 * Estado especifico para registro.
 */
typealias RegistrationState = AuthState<FirebaseUser>

/**
 * Estado especifico para reset de password.
 */
typealias ResetPasswordState = AuthState<Unit>

/**
 * Estado especifico para eliminacion de cuenta.
 */
typealias DeleteAccountState = AuthState<Unit>

/**
 * Estado global de autenticacion del usuario.
 */
sealed class AuthenticationState {
    /** No se ha determinado el estado aun */
    data object Unknown : AuthenticationState()

    /** Usuario autenticado */
    data class Authenticated(val user: FirebaseUser) : AuthenticationState()

    /** Usuario no autenticado */
    data object Unauthenticated : AuthenticationState()
}
