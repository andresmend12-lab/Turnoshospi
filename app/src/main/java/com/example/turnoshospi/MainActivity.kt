package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.util.Date

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: FirebaseDatabase
    private val currentUserState = mutableStateOf<FirebaseUser?>(null)
    private val authErrorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
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
                    onLoadPlant = { loadUserPlant(it) },
                    onJoinPlant = { plantId, code, profile, onResult ->
                        joinPlantWithCode(plantId, code, profile, onResult)
                    },
                    onLoadPlantMembership = { plantId, userId, onResult ->
                        loadPlantMembership(plantId, userId, onResult)
                    },
                    onLinkUserToStaff = { plantId, staff, onResult ->
                        linkUserToPlantStaff(plantId, staff, onResult)
                    },
                    onRegisterPlantStaff = { plantId, staffMember, onResult ->
                        registerPlantStaff(plantId, staffMember, onResult)
                    },
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

        realtimeDatabase.getReference("users")
            .child(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.toUserProfile(user.email.orEmpty())
                onResult(profile)
            }
            .addOnFailureListener { databaseError ->
                authErrorMessage.value = databaseError.message
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

        saveRealtimeUser(user.uid, profile.copy(email = resolvedEmail)) { success ->
            if (!success && authErrorMessage.value == null) {
                authErrorMessage.value = "No se pudo guardar el perfil en tiempo real"
            }
            onResult(success)
        }
    }

    private fun saveRealtimeUser(
        userId: String,
        profile: UserProfile,
        onResult: (Boolean) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        val userRef = realtimeDatabase.getReference("users").child(userId)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                val persistedCreatedAt = snapshot.child("createdAt").getValue(Long::class.java)
                val persistedPlantId = snapshot.child("plantId").getValue(String::class.java)
                val resolvedPlantId = profile.plantId ?: persistedPlantId
                val realtimePayload = mutableMapOf<String, Any?>().apply {
                    put("firstName", profile.firstName)
                    put("lastName", profile.lastName)
                    put("role", profile.role)
                    put("gender", profile.gender)
                    put("email", profile.email)
                    put("createdAt", persistedCreatedAt ?: profile.createdAt?.toDate()?.time ?: currentTime)
                    put("updatedAt", currentTime)
                    resolvedPlantId?.let { put("plantId", it) }
                }

                userRef.setValue(realtimePayload)
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

    private fun loadUserPlant(onResult: (Plant?, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(null, getString(R.string.plant_auth_error))
            return
        }

        realtimeDatabase.getReference("users")
            .child(user.uid)
            .child("plantId")
            .get()
            .addOnSuccessListener { mappingSnapshot ->
                val plantId = mappingSnapshot.getValue(String::class.java)
                if (plantId.isNullOrBlank()) {
                    onResult(null, null)
                    return@addOnSuccessListener
                }

                realtimeDatabase.getReference("plants")
                    .child(plantId)
                    .get()
                    .addOnSuccessListener { plantSnapshot ->
                        val plant = plantSnapshot.toPlant()
                        if (plant != null) {
                            onResult(plant, null)
                        } else {
                            onResult(null, getString(R.string.plant_load_error))
                        }
                    }
                    .addOnFailureListener {
                        onResult(null, getString(R.string.plant_load_error))
                    }
            }
            .addOnFailureListener {
                onResult(null, getString(R.string.plant_load_error))
            }
    }

    private fun loadPlantMembership(
        plantId: String,
        userId: String,
        onResult: (PlantMembership?) -> Unit
    ) {
        realtimeDatabase.reference
            .child("plants")
            .child(plantId)
            .child("userPlants")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val legacyValue = snapshot.getValue(String::class.java)
                if (legacyValue != null) {
                    onResult(
                        PlantMembership(
                            plantId = plantId,
                            userId = userId,
                            staffId = null,
                            staffName = null,
                            staffRole = null
                        )
                    )
                    return@addOnSuccessListener
                }

                if (!snapshot.exists()) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                val membership = PlantMembership(
                    plantId = snapshot.child("plantId").getValue(String::class.java) ?: plantId,
                    userId = userId,
                    staffId = snapshot.child("staffId").getValue(String::class.java),
                    staffName = snapshot.child("staffName").getValue(String::class.java),
                    staffRole = snapshot.child("staffRole").getValue(String::class.java)
                )
                onResult(membership)
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun joinPlantWithCode(
        plantId: String,
        invitationCode: String,
        profile: UserProfile?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onResult(false, getString(R.string.plant_auth_error))
            return
        }

        val cleanPlantId = plantId.trim()
        val cleanInvitationCode = invitationCode.trim()

        realtimeDatabase.getReference("plants")
            .child(cleanPlantId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(false, getString(R.string.join_plant_not_found))
                    return@addOnSuccessListener
                }

                val storedCode = snapshot.child("accessPassword").getValue(String::class.java)
                if (storedCode.isNullOrEmpty() || storedCode != cleanInvitationCode) {
                    onResult(false, getString(R.string.invalid_invitation_code))
                    return@addOnSuccessListener
                }

                val updates = mapOf(
                    "plants/$cleanPlantId/userPlants/${user.uid}/plantId" to cleanPlantId,
                    "users/${user.uid}/plantId" to cleanPlantId
                )

                realtimeDatabase.reference
                    .updateChildren(updates)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener {
                        onResult(false, getString(R.string.join_plant_error))
                    }
            }
            .addOnFailureListener {
                onResult(false, getString(R.string.join_plant_error))
            }
    }

    private fun registerPlantStaff(
        plantId: String,
        staffMember: RegisteredUser,
        onResult: (Boolean) -> Unit
    ) {
        val cleanPlantId = plantId.trim()

        if (cleanPlantId.isEmpty()) {
            onResult(false)
            return
        }

        realtimeDatabase.reference
            .child("plants")
            .child(cleanPlantId)
            .child("registeredUsers")
            .child(staffMember.id)
            .setValue(staffMember)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    private fun linkUserToPlantStaff(
        plantId: String,
        staffMember: RegisteredUser,
        onResult: (Boolean) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onResult(false)
            return
        }

        val updates = mapOf(
            "plants/$plantId/userPlants/${user.uid}/plantId" to plantId,
            "plants/$plantId/userPlants/${user.uid}/staffId" to staffMember.id,
            "plants/$plantId/userPlants/${user.uid}/staffName" to staffMember.name,
            "plants/$plantId/userPlants/${user.uid}/staffRole" to staffMember.role,
            "users/${user.uid}/plantId" to plantId,
            "users/${user.uid}/plantStaffId" to staffMember.id,
            "users/${user.uid}/role" to staffMember.role,
            "users/${user.uid}/linkedStaffName" to staffMember.name
        )

        realtimeDatabase.reference
            .updateChildren(updates)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
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
    val gender: String = "",
    val email: String = "",
    val plantId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun DataSnapshot.toUserProfile(fallbackEmail: String): UserProfile? {
    if (!exists()) return null
    val createdAtMillis = child("createdAt").getValue(Long::class.java)
    val updatedAtMillis = child("updatedAt").getValue(Long::class.java)

    return UserProfile(
        firstName = child("firstName").getValue(String::class.java) ?: "",
        lastName = child("lastName").getValue(String::class.java) ?: "",
        role = child("role").getValue(String::class.java) ?: "",
        gender = child("gender").getValue(String::class.java) ?: "",
        email = child("email").getValue(String::class.java) ?: fallbackEmail,
        plantId = child("plantId").getValue(String::class.java),
        createdAt = createdAtMillis?.let { Timestamp(Date(it)) },
        updatedAt = updatedAtMillis?.let { Timestamp(Date(it)) }
    )
}

fun DataSnapshot.toPlant(): Plant? {
    if (!exists()) return null

    val shiftTimes = child("shiftTimes").children.mapNotNull { shiftSnapshot ->
        val label = shiftSnapshot.key ?: return@mapNotNull null
        val start = shiftSnapshot.child("start").getValue(String::class.java) ?: ""
        val end = shiftSnapshot.child("end").getValue(String::class.java) ?: ""
        label to ShiftTime(start, end)
    }.toMap()

    val staffRequirements = child("staffRequirements").children.mapNotNull { requirementSnapshot ->
        val label = requirementSnapshot.key ?: return@mapNotNull null
        val value = requirementSnapshot.getValue(Int::class.java)
            ?: requirementSnapshot.getValue(Long::class.java)?.toInt()
            ?: 0
        label to value
    }.toMap()

        val registeredUsers = child("registeredUsers").children.mapNotNull { userSnapshot ->
            val userId = userSnapshot.key ?: return@mapNotNull null
            val registeredUser = userSnapshot.getValue(RegisteredUser::class.java)
                ?: RegisteredUser(
                    id = userId,
                    name = userSnapshot.child("name").getValue(String::class.java).orEmpty(),
                    role = userSnapshot.child("role").getValue(String::class.java).orEmpty(),
                    email = userSnapshot.child("email").getValue(String::class.java).orEmpty(),
                    profileType = userSnapshot.child("profileType").getValue(String::class.java).orEmpty()
                )
            userId to registeredUser
        }.toMap()

    return Plant(
        id = child("id").getValue(String::class.java) ?: key.orEmpty(),
        name = child("name").getValue(String::class.java) ?: "",
        unitType = child("unitType").getValue(String::class.java) ?: "",
        hospitalName = child("hospitalName").getValue(String::class.java) ?: "",
        shiftDuration = child("shiftDuration").getValue(String::class.java) ?: "",
        allowHalfDay = child("allowHalfDay").getValue(Boolean::class.java) ?: false,
        staffScope = child("staffScope").getValue(String::class.java) ?: "",
        shiftTimes = shiftTimes,
        staffRequirements = staffRequirements,
        createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
        accessPassword = child("accessPassword").getValue(String::class.java) ?: "",
        registeredUsers = registeredUsers
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainActivityPreview() {
    TurnoshospiTheme {
        TurnoshospiApp(
            user = null,
            errorMessage = null,
            onErrorDismiss = {},
            onLogin = { _, _, _ -> },
            onCreateAccount = { _, _, _ -> },
            onForgotPassword = { _, _ -> },
            onLoadProfile = { onResult -> onResult(null) },
            onSaveProfile = { _, _ -> },
            onLoadPlant = { onResult -> onResult(null, null) },
            onJoinPlant = { _, _, _, _ -> },
            onLoadPlantMembership = { _, _, _ -> },
            onLinkUserToStaff = { _, _, _ -> },
            onRegisterPlantStaff = { _, _, _ -> },
            onSignOut = {}
        )
    }
}
