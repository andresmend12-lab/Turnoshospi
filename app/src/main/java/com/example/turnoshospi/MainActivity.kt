package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.menuAnchor
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val currentUserState = mutableStateOf<FirebaseUser?>(null)
    private val authErrorMessage = mutableStateOf<String?>(null)

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (!idToken.isNullOrEmpty()) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    authErrorMessage.value = "No se pudo obtener el token de Google"
                }
            } catch (e: ApiException) {
                authErrorMessage.value = when (e.statusCode) {
                    CommonStatusCodes.NETWORK_ERROR -> "Comprueba tu conexión e inténtalo de nuevo"
                    CommonStatusCodes.CANCELED -> "Inicio de sesión cancelado"
                    else -> "Error al iniciar sesión: ${e.statusCode}"
                }
            }
        }

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
                    onGoogleSignIn = { launchGoogleSignIn() },
                    onLoadProfile = { loadUserProfile(it) },
                    onSaveProfile = { profile, callback -> saveUserProfile(profile, callback) }
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        authErrorMessage.value = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                currentUserState.value = auth.currentUser
            } else {
                authErrorMessage.value = "No se pudo completar el inicio de sesión"
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
            authErrorMessage.value = "Debes iniciar sesión con Google para continuar"
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
    onGoogleSignIn: () -> Unit,
    onLoadProfile: (onResult: (UserProfile?) -> Unit) -> Unit,
    onSaveProfile: (UserProfile, (Boolean) -> Unit) -> Unit
) {
    var showLogin by remember { mutableStateOf(true) }
    var compactLogo by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var existingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var saveCompleted by remember { mutableStateOf(false) }
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
                if (user == null) {
                    LoginCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                            .padding(horizontal = 8.dp)
                            .graphicsLayer(alpha = loginAlpha),
                        onGoogleSignIn = onGoogleSignIn
                    )
                } else {
                    RegistrationScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                            .padding(horizontal = 8.dp)
                            .graphicsLayer(alpha = loginAlpha),
                        user = user,
                        existingProfile = existingProfile,
                        isLoading = isLoadingProfile,
                        onSave = { profile, onComplete ->
                            saveCompleted = false
                            coroutineScope.launch {
                                onSaveProfile(profile) { success ->
                                    saveCompleted = success
                                    onComplete(success)
                                }
                            }
                        }
                    )
                }
            }
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
}

@Composable
private fun LoginCard(
    modifier: Modifier = Modifier,
    onGoogleSignIn: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
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
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(id = R.string.google_sign_in))
            }

            TextButton(
                onClick = onGoogleSignIn,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = stringResource(id = R.string.create_account_title))
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
                label = { Text("Correo (Gmail)") },
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
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownItem(title = stringResource(id = R.string.role_supervisor)) {
                        role = stringResource(id = R.string.role_supervisor)
                        expanded = false
                    }
                    DropdownItem(title = stringResource(id = R.string.role_nurse)) {
                        role = stringResource(id = R.string.role_nurse)
                        expanded = false
                    }
                    DropdownItem(title = stringResource(id = R.string.role_aux)) {
                        role = stringResource(id = R.string.role_aux)
                        expanded = false
                    }
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

@Composable
private fun DropdownItem(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = Color.White
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashLoginPreview() {
    TurnoshospiTheme {
        TurnoshospiApp(
            user = null,
            errorMessage = null,
            onErrorDismiss = {},
            onGoogleSignIn = {},
            onLoadProfile = {},
            onSaveProfile = { _, _ -> }
        )
    }
}
