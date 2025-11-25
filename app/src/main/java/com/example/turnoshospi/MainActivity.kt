package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: FirebaseDatabase
    private val currentUserState = mutableStateOf<FirebaseUser?>(null)
    private val authErrorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
        currentUserState.value = auth.currentUser

        setContent {
            TurnoshospiTheme {
                TurnoshospiApp(
                    user = currentUserState.value,
                    errorMessage = authErrorMessage.value,
                    onErrorDismiss = { authErrorMessage.value = null },
                    onLogin = { email, password, onResult ->
                        signInWithEmail(email, password, onResult)
                    },
                    onCreateAccount = { profile, password, onResult ->
                        createAccountWithEmail(profile, password, onResult)
                    },
                    onForgotPassword = { email, onResult -> sendPasswordReset(email, onResult) },
                    onLoadProfile = { loadUserProfile(it) },
                    onSaveProfile = { profile, callback -> saveUserProfile(profile, callback) },
                    onSignOut = { signOut() }
                )
            }
        }
    }

    private fun signInWithEmail(email: String, password: String, onResult: (Boolean) -> Unit) {
        authErrorMessage.value = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUserState.value = auth.currentUser
                    onResult(true)
                } else {
                    val errorMessage = task.exception?.let { formatAuthError(it) }
                        ?: "No se pudo iniciar sesión con ese correo"
                    authErrorMessage.value = errorMessage
                    onResult(false)
                }
            }
    }

    private fun createAccountWithEmail(
        profile: UserProfile,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        authErrorMessage.value = null
        auth.createUserWithEmailAndPassword(profile.email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUserState.value = auth.currentUser
                    saveUserProfile(profile) { success ->
                        onResult(success)
                    }
                } else {
                    val errorMessage = task.exception?.let { formatAuthError(it) }
                        ?: "No se pudo crear la cuenta"
                    authErrorMessage.value = errorMessage
                    onResult(false)
                }
            }
    }

    private fun sendPasswordReset(email: String, onResult: (Boolean) -> Unit) {
        authErrorMessage.value = null
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true)
                } else {
                    val errorMessage = task.exception?.let { formatAuthError(it) }
                        ?: "No se pudo enviar el correo de recuperación"
                    authErrorMessage.value = errorMessage
                    onResult(false)
                }
            }
    }

    private fun loadUserProfile(onResult: (UserProfile?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(null)
            return
        }
        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val profile = document.toUserProfile(user.email.orEmpty())
                onResult(profile)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun saveUserProfile(profile: UserProfile, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run {
            authErrorMessage.value = "Debes iniciar sesión para continuar"
            onResult(false)
            return
        }

        val resolvedEmail = profile.email.ifEmpty { user.email.orEmpty() }

        val payload = mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "role" to profile.role,
            "email" to resolvedEmail,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                saveRealtimeUser(user.uid, profile.copy(email = resolvedEmail)) { success ->
                    onResult(success)
                }
            }
            .addOnFailureListener {
                authErrorMessage.value = "No se pudo guardar el perfil"
                onResult(false)
            }
    }

    private fun saveRealtimeUser(
        userId: String,
        profile: UserProfile,
        onResult: (Boolean) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        val realtimePayload = mutableMapOf<String, Any?>().apply {
            put("firstName", profile.firstName)
            put("lastName", profile.lastName)
            put("role", profile.role)
            put("email", profile.email)
            put("createdAt", profile.createdAt?.toDate()?.time ?: currentTime)
            put("updatedAt", currentTime)
        }

        realtimeDatabase.getReference("users")
            .child(userId)
            .updateChildren(realtimePayload)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener {
                val message = if (it is DatabaseException) {
                    "Revisa las reglas de Realtime Database y los permisos de escritura"
                } else {
                    "No se pudo guardar el perfil en tiempo real"
                }
                authErrorMessage.value = message
                onResult(false)
            }
    }

    private fun signOut() {
        auth.signOut()
        currentUserState.value = null
    }

    private fun formatAuthError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil; debe tener al menos 6 caracteres"
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta registrada con este correo"
            is FirebaseAuthInvalidCredentialsException -> "El correo o la contraseña no son válidos"
            is FirebaseAuthInvalidUserException -> "Esta cuenta no existe o fue deshabilitada"
            is FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" -> "Habilita el proveedor de Email/Contraseña en la consola de Firebase"
                else -> "Error de autenticación: ${exception.localizedMessage ?: "intenta de nuevo"}"
            }
            is FirebaseNetworkException -> "No hay conexión con el servidor. Revisa tu conexión a internet"
            else -> "No se pudo completar la operación. Inténtalo de nuevo"
        }
    }
}

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(
    fallbackEmail: String
): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        firstName = getString("firstName") ?: "",
        lastName = getString("lastName") ?: "",
        role = getString("role") ?: "",
        email = getString("email") ?: fallbackEmail,
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}
