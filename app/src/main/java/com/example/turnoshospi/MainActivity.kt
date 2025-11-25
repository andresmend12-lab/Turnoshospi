package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val currentUserState = mutableStateOf<FirebaseUser?>(null)
    private val authErrorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
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
                    authErrorMessage.value = "No se pudo iniciar sesión con ese correo"
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
                    authErrorMessage.value = "No se pudo crear la cuenta"
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
                    authErrorMessage.value = "No se pudo enviar el correo de recuperación"
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

        val payload = mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "role" to profile.role,
            "email" to (profile.email.ifEmpty { user.email.orEmpty() }),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener {
                authErrorMessage.value = "No se pudo guardar el perfil"
                onResult(false)
            }
    }

    private fun signOut() {
        auth.signOut()
        currentUserState.value = null
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

private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(
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

@Composable
fun TurnoshospiApp(
    user: FirebaseUser?,
    errorMessage: String?,
    onErrorDismiss: () -> Unit,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onCreateAccount: (UserProfile, String, (Boolean) -> Unit) -> Unit,
    onForgotPassword: (String, (Boolean) -> Unit) -> Unit,
    onLoadProfile: (onResult: (UserProfile?) -> Unit) -> Unit,
    onSaveProfile: (UserProfile, (Boolean) -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    var showLogin by remember { mutableStateOf(true) }
    var showRegistration by remember { mutableStateOf(false) }
    var compactLogo by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var existingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var saveCompleted by remember { mutableStateOf(false) }
    var emailForReset by remember { mutableStateOf("") }
    var showProfileEditor by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(2000)
        compactLogo = true
        delay(300)
        showLogin = true
    }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            isLoadingProfile = true
            onLoadProfile { profile ->
                existingProfile = profile
                isLoadingProfile = false
            }
        } else {
            existingProfile = null
            showRegistration = false
        }
    }

    val logoSize by animateDpAsState(
        targetValue = if (compactLogo) 120.dp else 240.dp,
        animationSpec = tween(durationMillis = 500),
        label = "logoSize"
    )

    val loginAlpha by animateFloatAsState(
        targetValue = if (showLogin) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 100),
        label = "loginAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1021),
                        Color(0xFF0F172A),
                        Color(0xFF0E1A2F)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(180.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x6654C7EC), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(90.dp)
                )
                .blur(50.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(220.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66A855F7), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(110.dp)
                )
                .blur(65.dp)
        )

        if (user == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showLogin) Arrangement.Top else Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(if (showLogin) 32.dp else 0.dp))

                Image(
                    painter = painterResource(id = R.mipmap.ic_logo_hospi_foreground),
                    contentDescription = "Logo Turnoshospi",
                    modifier = Modifier.size(logoSize)
                )

                AnimatedVisibility(visible = showLogin) {
                    if (showRegistration) {
                        CreateAccountScreen(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                                .padding(horizontal = 8.dp)
                                .graphicsLayer(alpha = loginAlpha),
                            onBack = { showRegistration = false },
                            onCreate = { profile, password, onComplete ->
                                coroutineScope.launch {
                                    onCreateAccount(profile, password) { success ->
                                        saveCompleted = success
                                        onComplete(success)
                                    }
                                }
                            }
                        )
                    } else {
                        LoginCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                                .padding(horizontal = 8.dp)
                                .graphicsLayer(alpha = loginAlpha),
                            email = emailForReset,
                            onEmailChange = { emailForReset = it },
                            onLogin = { email, password, onComplete ->
                                coroutineScope.launch {
                                    onLogin(email, password) { onComplete(it) }
                                }
                            },
                            onCreateAccount = { showRegistration = true },
                            onForgotPassword = { email, onComplete ->
                                coroutineScope.launch {
                                    onForgotPassword(email) { onComplete(it) }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            MainMenuScreen(
                modifier = Modifier.fillMaxSize(),
                user = user,
                profile = existingProfile,
                isLoadingProfile = isLoadingProfile,
                onEditProfile = { showProfileEditor = true },
                onSignOut = onSignOut
            )
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            confirmButton = {
                TextButton(onClick = onErrorDismiss) {
                    Text(text = "Entendido")
                }
            },
            title = { Text(text = "Aviso") },
            text = { Text(text = errorMessage) }
        )
    }

    if (saveCompleted) {
        AlertDialog(
            onDismissRequest = { saveCompleted = false },
            confirmButton = {
                TextButton(onClick = { saveCompleted = false }) {
                    Text(text = "Cerrar")
                }
            },
            title = { Text(text = "Perfil guardado") },
            text = { Text(text = "Los datos de tu cuenta se han actualizado correctamente.") }
        )
    }

    if (showProfileEditor && user != null) {
        ProfileEditorOverlay(
            user = user,
            existingProfile = existingProfile,
            isLoading = isLoadingProfile,
            onDismiss = { showProfileEditor = false },
            onSave = { profile, onComplete ->
                saveCompleted = false
                coroutineScope.launch {
                    onSaveProfile(profile) { success ->
                        saveCompleted = success
                        showProfileEditor = !success
                        onComplete(success)
                    }
                }
            }
        )
    }
}

@Composable
private fun MainMenuScreen(
    modifier: Modifier = Modifier,
    user: FirebaseUser,
    profile: UserProfile?,
    isLoadingProfile: Boolean,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val displayName = remember(profile, user.email) {
        val fullName = listOfNotNull(profile?.firstName?.takeIf { it.isNotBlank() }, profile?.lastName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
        if (fullName.isNotBlank()) fullName else user.email.orEmpty()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.main_menu_welcome, displayName),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isLoadingProfile) stringResource(id = R.string.loading_profile) else stringResource(id = R.string.main_menu_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xCCFFFFFF)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(id = R.string.profile_summary_title), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(text = stringResource(id = R.string.profile_email_label, user.email.orEmpty()), color = Color(0xCCFFFFFF))
                    Text(text = stringResource(id = R.string.profile_name_label, displayName), color = Color(0xCCFFFFFF))
                    val roleLabel = profile?.role?.ifBlank { stringResource(id = R.string.role_aux) } ?: stringResource(id = R.string.role_aux)
                    Text(text = stringResource(id = R.string.profile_role_label, roleLabel), color = Color(0xCCFFFFFF))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEditProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) { Text(text = stringResource(id = R.string.edit_profile)) }
                        TextButton(onClick = onSignOut, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB4AB))) {
                            Text(text = stringResource(id = R.string.sign_out))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(260.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(id = R.string.side_menu_title), color = Color.White, fontWeight = FontWeight.Bold)

                MenuOption(title = stringResource(id = R.string.menu_create_plant), subtitle = stringResource(id = R.string.menu_create_plant_desc))
                MenuOption(title = stringResource(id = R.string.menu_my_plants), subtitle = stringResource(id = R.string.menu_my_plants_desc))
                MenuOption(title = stringResource(id = R.string.menu_settings), subtitle = stringResource(id = R.string.menu_settings_desc))
                MenuOption(title = stringResource(id = R.string.menu_info), subtitle = stringResource(id = R.string.menu_info_desc))
            }
        }
    }
}

@Composable
private fun MenuOption(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProfileEditorOverlay(
    user: FirebaseUser,
    existingProfile: UserProfile?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserProfile, (Boolean) -> Unit) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.edit_profile), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))) {
                        Text(text = stringResource(id = R.string.close_label))
                    }
                }

                RegistrationScreen(
                    modifier = Modifier.fillMaxWidth(),
                    user = user,
                    existingProfile = existingProfile,
                    isLoading = isLoading,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
private fun LoginCard(
    modifier: Modifier = Modifier,
    email: String,
    onEmailChange: (String) -> Unit,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: (String, (Boolean) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0x66FFFFFF),
                    Color(0x33FFFFFF)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Iniciar sesión",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    onEmailChange(it)
                    resetSent = false
                },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isLoading = true
                    onLogin(email, password) { success ->
                        isLoading = false
                        if (!success) {
                            password = ""
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text(text = stringResource(id = R.string.login_button))
            }

            TextButton(
                onClick = onCreateAccount,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = stringResource(id = R.string.create_account_title))
            }

            TextButton(
                onClick = {
                    resetSent = false
                    onForgotPassword(email) { success -> resetSent = success }
                },
                enabled = email.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = stringResource(id = R.string.forgot_password))
            }

            if (resetSent) {
                Text(
                    text = stringResource(id = R.string.reset_email_sent),
                    color = Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCreate: (UserProfile, String, (Boolean) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0x66FFFFFF),
                    Color(0x33FFFFFF)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.create_account_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordMismatch = false
                },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordMismatch = false
                },
                label = { Text(stringResource(id = R.string.confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Apellidos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val roleSupervisor = stringResource(id = R.string.role_supervisor)
            val roleNurse = stringResource(id = R.string.role_nurse)
            val roleAux = stringResource(id = R.string.role_aux)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Puesto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0x99FFFFFF),
                        unfocusedIndicatorColor = Color(0x66FFFFFF),
                        focusedContainerColor = Color(0x22FFFFFF),
                        unfocusedContainerColor = Color(0x18FFFFFF),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xCCFFFFFF),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = roleSupervisor) },
                        onClick = {
                            role = roleSupervisor
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleNurse) },
                        onClick = {
                            role = roleNurse
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleAux) },
                        onClick = {
                            role = roleAux
                            expanded = false
                        }
                    )
                }
            }

            if (passwordMismatch) {
                Text(
                    text = "Las contraseñas no coinciden",
                    color = Color(0xFFFFD166),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        passwordMismatch = true
                        return@Button
                    }
                    isSaving = true
                    onCreate(
                        UserProfile(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            role = role,
                            email = email.trim()
                        ),
                        password
                    ) { success ->
                        isSaving = false
                        if (!success) {
                            password = ""
                            confirmPassword = ""
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() &&
                    confirmPassword.isNotBlank() && firstName.isNotBlank() &&
                    lastName.isNotBlank() && role.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text(text = stringResource(id = R.string.create_account_action))
            }

            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = "Volver al inicio de sesión")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationScreen(
    modifier: Modifier = Modifier,
    user: FirebaseUser,
    existingProfile: UserProfile?,
    isLoading: Boolean,
    onSave: (UserProfile, (Boolean) -> Unit) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(existingProfile?.firstName, existingProfile?.lastName, existingProfile?.role) {
        firstName = existingProfile?.firstName.orEmpty()
        lastName = existingProfile?.lastName.orEmpty()
        role = existingProfile?.role.orEmpty()
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0x66FFFFFF),
                    Color(0x33FFFFFF)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.create_account_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(text = "Cargando datos...", color = Color.White)
                }
            }

            OutlinedTextField(
                value = user.email.orEmpty(),
                onValueChange = {},
                enabled = false,
                label = { Text("Correo electrónico") },
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = Color(0x11FFFFFF),
                    disabledIndicatorColor = Color(0x66FFFFFF),
                    disabledLabelColor = Color(0xCCFFFFFF),
                    disabledTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Apellidos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0x99FFFFFF),
                    unfocusedIndicatorColor = Color(0x66FFFFFF),
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xCCFFFFFF),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val roleSupervisor = stringResource(id = R.string.role_supervisor)
            val roleNurse = stringResource(id = R.string.role_nurse)
            val roleAux = stringResource(id = R.string.role_aux)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Puesto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0x99FFFFFF),
                        unfocusedIndicatorColor = Color(0x66FFFFFF),
                        focusedContainerColor = Color(0x22FFFFFF),
                        unfocusedContainerColor = Color(0x18FFFFFF),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xCCFFFFFF),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = roleSupervisor) },
                        onClick = {
                            role = roleSupervisor
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleNurse) },
                        onClick = {
                            role = roleNurse
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleAux) },
                        onClick = {
                            role = roleAux
                            expanded = false
                        }
                    )
                }
            }

            Button(
                onClick = {
                    isSaving = true
                    onSave(
                        UserProfile(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            role = role,
                            email = user.email.orEmpty()
                        )
                    ) { isSaving = false }
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && role.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text(text = stringResource(id = R.string.register_button))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashLoginPreview() {
    TurnoshospiTheme {
        TurnoshospiApp(
            user = null,
            errorMessage = null,
            onErrorDismiss = {},
            onLogin = { _, _, _ -> },
            onCreateAccount = { _, _, _ -> },
            onForgotPassword = { _, _ -> },
            onLoadProfile = {},
            onSaveProfile = { _, _ -> },
            onSignOut = {}
        )
    }
}
