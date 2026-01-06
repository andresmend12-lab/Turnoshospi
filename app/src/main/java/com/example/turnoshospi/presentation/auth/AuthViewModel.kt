package com.example.turnoshospi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnoshospi.domain.repository.AuthRepository
import com.example.turnoshospi.util.CrashReporter
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar la logica de autenticacion.
 * Desacopla la UI de la implementacion de Firebase.
 *
 * @param authRepository Repositorio de autenticacion (inyectado)
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- Estados de UI ---

    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(AuthState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(AuthState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(AuthState.Idle)
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(AuthState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    /** Usuario actual (acceso sincrono para conveniencia) */
    val currentUser: FirebaseUser?
        get() = authRepository.currentUserSync

    init {
        // Observar cambios en el estado de autenticacion
        observeAuthState()
    }

    /**
     * Observa el Flow de usuario actual y actualiza el estado de autenticacion.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _authenticationState.value = if (user != null) {
                    AuthenticationState.Authenticated(user)
                } else {
                    AuthenticationState.Unauthenticated
                }
            }
        }
    }

    /**
     * Inicia sesion con email y contrasena.
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("Email y contrasena son requeridos")
            return
        }

        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            authRepository.signIn(email, password)
                .onSuccess { user ->
                    CrashReporter.setUserId(user.uid)
                    _loginState.value = AuthState.Success(user)
                }
                .onFailure { exception ->
                    val errorMessage = formatAuthError(exception)
                    CrashReporter.logError("AuthViewModel", "Login failed: $errorMessage", exception)
                    _loginState.value = AuthState.Error(errorMessage, exception)
                }
        }
    }

    /**
     * Crea una nueva cuenta con email y contrasena.
     * Nota: El guardado del perfil debe hacerse por separado despues del registro exitoso.
     */
    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _registrationState.value = AuthState.Error("Email y contrasena son requeridos")
            return
        }

        viewModelScope.launch {
            _registrationState.value = AuthState.Loading

            authRepository.createAccount(email, password)
                .onSuccess { user ->
                    CrashReporter.setUserId(user.uid)
                    _registrationState.value = AuthState.Success(user)
                }
                .onFailure { exception ->
                    val errorMessage = formatAuthError(exception)
                    CrashReporter.logError("AuthViewModel", "Registration failed: $errorMessage", exception)
                    _registrationState.value = AuthState.Error(errorMessage, exception)
                }
        }
    }

    /**
     * Envia un correo de restablecimiento de contrasena.
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetPasswordState.value = AuthState.Error("Email es requerido")
            return
        }

        viewModelScope.launch {
            _resetPasswordState.value = AuthState.Loading

            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _resetPasswordState.value = AuthState.Success(Unit)
                }
                .onFailure { exception ->
                    val errorMessage = formatAuthError(exception)
                    _resetPasswordState.value = AuthState.Error(errorMessage, exception)
                }
        }
    }

    /**
     * Cierra la sesion del usuario actual.
     */
    fun logout() {
        viewModelScope.launch {
            CrashReporter.clearUserId()
            authRepository.signOut()
            // El estado se actualizara automaticamente via observeAuthState()
        }
    }

    /**
     * Elimina la cuenta del usuario actual.
     * IMPORTANTE: Los datos en Realtime Database deben eliminarse antes de llamar a esto.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = AuthState.Loading

            authRepository.deleteAccount()
                .onSuccess {
                    CrashReporter.clearUserId()
                    _deleteAccountState.value = AuthState.Success(Unit)
                }
                .onFailure { exception ->
                    val errorMessage = formatAuthError(exception)
                    _deleteAccountState.value = AuthState.Error(errorMessage, exception)
                }
        }
    }

    /**
     * Resetea el estado de login a Idle.
     * Util para limpiar errores despues de mostrarlos.
     */
    fun clearLoginState() {
        _loginState.value = AuthState.Idle
    }

    /**
     * Resetea el estado de registro a Idle.
     */
    fun clearRegistrationState() {
        _registrationState.value = AuthState.Idle
    }

    /**
     * Resetea el estado de reset password a Idle.
     */
    fun clearResetPasswordState() {
        _resetPasswordState.value = AuthState.Idle
    }

    /**
     * Resetea el estado de eliminacion de cuenta a Idle.
     */
    fun clearDeleteAccountState() {
        _deleteAccountState.value = AuthState.Idle
    }

    /**
     * Formatea excepciones de Firebase Auth a mensajes legibles.
     * TODO: Mover a un util o usar recursos de strings cuando se integre con la UI.
     */
    private fun formatAuthError(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "La contrasena es muy debil. Usa al menos 6 caracteres."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electronico."
            is FirebaseAuthInvalidCredentialsException -> "Credenciales invalidas. Verifica tu email y contrasena."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con este correo electronico."
            is FirebaseNetworkException -> "Error de conexion. Verifica tu internet."
            else -> exception.localizedMessage ?: "Error desconocido de autenticacion."
        }
    }
}
