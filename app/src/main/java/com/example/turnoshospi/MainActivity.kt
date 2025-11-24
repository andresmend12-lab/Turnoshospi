package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Logout
import androidx.compose.material3.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val positions = listOf("Supervisor", "Enfermero", "Auxiliar")

data class ProfileFormState(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val position: String = positions.first(),
    val phone: String = "",
    val department: String = "",
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            TurnoshospiTheme {
                AccountCreationScreen()
            }
        }
    }
}

@Composable
fun AccountCreationScreen(
    auth: FirebaseAuth = Firebase.auth,
    firestore: FirebaseFirestore = Firebase.firestore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var formState by remember { mutableStateOf(ProfileFormState(email = currentUser?.email.orEmpty())) }
    var isSigningIn by remember { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            signInWithGoogleAccount(
                account = account,
                auth = auth,
                onLoading = { isSigningIn = it },
                onError = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
                onSuccess = { user ->
                    currentUser = user
                    formState = formState.copy(email = user?.email.orEmpty())
                }
            )
        } catch (apiException: ApiException) {
            isSigningIn = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar(apiException.localizedMessage ?: "Error iniciando sesión")
            }
        }
    }

    LaunchedEffect(Unit) {
        currentUser = auth.currentUser
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cuenta y perfil",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (currentUser != null) {
                        IconButton(onClick = {
                            signOut(auth)
                            currentUser = null
                            formState = ProfileFormState()
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            GoogleSignIn.getClient(context, gso).signOut()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cerrar sesión"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderCard(isSignedIn = currentUser != null)
            }

            if (currentUser == null) {
                item {
                    GoogleSignInCard(
                        isSigningIn = isSigningIn,
                        onClick = {
                            val token = context.getString(R.string.default_web_client_id)
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(token)
                                .requestEmail()
                                .build()
                            val googleClient = GoogleSignIn.getClient(context, gso)
                            isSigningIn = true
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                    )
                }
            } else {
                item {
                    ProfileForm(
                        user = currentUser,
                        state = formState,
                        onStateChange = { formState = it },
                        onSave = {
                            if (formState.firstName.isBlank() || formState.lastName.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nombre y apellidos son obligatorios")
                                }
                                return@ProfileForm
                            }
                            coroutineScope.launch {
                                saveProfile(
                                    firestore = firestore,
                                    user = currentUser,
                                    state = formState,
                                    onStateChange = { updated -> formState = updated },
                                    onResult = { message ->
                                        snackbarHostState.showSnackbar(message)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun signOut(auth: FirebaseAuth) {
    auth.signOut()
}

private fun signInWithGoogleAccount(
    account: GoogleSignInAccount?,
    auth: FirebaseAuth,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (FirebaseUser?) -> Unit
) {
    val idToken = account?.idToken
    if (idToken.isNullOrBlank()) {
        onError("No se pudo obtener el token de Google")
        return
    }
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    onLoading(true)
    auth.signInWithCredential(credential)
        .addOnSuccessListener { result ->
            onSuccess(result.user)
            onLoading(false)
        }
        .addOnFailureListener { exception ->
            onError(exception.localizedMessage ?: "No se pudo iniciar sesión")
            onLoading(false)
        }
}

private suspend fun saveProfile(
    firestore: FirebaseFirestore,
    user: FirebaseUser?,
    state: ProfileFormState,
    onStateChange: (ProfileFormState) -> Unit,
    onResult: (String) -> Unit
) {
    val safeUser = user ?: run {
        onResult("Debes iniciar sesión para guardar el perfil")
        return
    }

    onStateChange(state.copy(isSaving = true, error = null, success = false))

    val payload = mapOf(
        "uid" to safeUser.uid,
        "email" to state.email.ifBlank { safeUser.email.orEmpty() },
        "firstName" to state.firstName.trim(),
        "lastName" to state.lastName.trim(),
        "position" to state.position,
        "phone" to state.phone.trim(),
        "department" to state.department.trim(),
        "createdAt" to FieldValue.serverTimestamp()
    )

    try {
        firestore.collection("users")
            .document(safeUser.uid)
            .set(payload, SetOptions.merge())
            .await()
        onStateChange(state.copy(isSaving = false, success = true))
        onResult("Perfil guardado y vinculado a tu cuenta")
    } catch (exception: Exception) {
        onStateChange(state.copy(isSaving = false, success = false, error = exception.localizedMessage))
        onResult(exception.localizedMessage ?: "Error guardando el perfil")
    }
}

@Composable
private fun HeaderCard(isSignedIn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isSignedIn) "Completa tu perfil" else "Crear cuenta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSignedIn) {
                    "Añade tus datos personales y puesto para que Firestore los asocie a tu usuario de Firebase."
                } else {
                    "Accede con tu cuenta de Google para crear el perfil en Firestore y vincularlo a la autenticación de Firebase."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GoogleSignInCard(
    isSigningIn: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inicia sesión con Gmail",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Firebase Auth está configurado para usar Google. Pulsa el botón para abrir el selector de cuenta.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = if (isSigningIn) "Abriendo Google..." else "Usar cuenta de Google")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileForm(
    user: FirebaseUser?,
    state: ProfileFormState,
    onStateChange: (ProfileFormState) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Datos del usuario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Autenticado como: ${user?.email ?: "Sin correo"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.firstName,
                onValueChange = { onStateChange(state.copy(firstName = it)) },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.lastName,
                onValueChange = { onStateChange(state.copy(lastName = it)) },
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = { onStateChange(state.copy(email = it)) },
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )
            Spacer(modifier = Modifier.height(8.dp))
            PositionDropdown(selected = state.position) { selected ->
                onStateChange(state.copy(position = selected))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.phone,
                onValueChange = { onStateChange(state.copy(phone = it)) },
                label = { Text("Teléfono (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.department,
                onValueChange = { onStateChange(state.copy(department = it)) },
                label = { Text("Unidad/Servicio (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (state.error != null) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (state.success) {
                        Text(
                            text = "Guardado en Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Button(onClick = onSave, enabled = !state.isSaving) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = if (state.isSaving) "Guardando..." else "Guardar perfil")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionDropdown(
    selected: String,
    onPositionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Puesto") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            positions.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onPositionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountCreationPreview() {
    TurnoshospiTheme {
        AccountCreationScreen(
            auth = Firebase.auth,
            firestore = Firebase.firestore
        )
    }
}
