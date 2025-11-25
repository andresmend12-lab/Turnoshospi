package com.example.turnoshospi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.R
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                userEmail = user.email.orEmpty(),
                profile = existingProfile,
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
            userEmail = user.email.orEmpty(),
            existingProfile = existingProfile,
            isLoading = isLoadingProfile,
            onDismiss = { showProfileEditor = false },
            onSave = { profile, onComplete ->
                saveCompleted = false
                coroutineScope.launch {
                    onSaveProfile(profile) { success ->
                        if (success) {
                            existingProfile = profile
                            showProfileEditor = false
                            saveCompleted = true
                            isLoadingProfile = true
                            onLoadProfile { refreshedProfile ->
                                existingProfile = refreshedProfile ?: profile
                                isLoadingProfile = false
                                saveCompleted = true
                            }
                        } else {
                            saveCompleted = false
                            showProfileEditor = true
                        }
                        onComplete(success)
                    }
                }
            }
        )
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
