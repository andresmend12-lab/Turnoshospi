package com.example.turnoshospi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.R
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate
import java.time.ZoneId
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppScreen {
    MainMenu,
    CreatePlant,
    PlantCreated,
    MyPlant,
    PlantDetail
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onLoadPlant: (onResult: (Plant?, String?) -> Unit) -> Unit,
    onJoinPlant: (String, String, UserProfile?, (Boolean, String?) -> Unit) -> Unit,
    onLoadPlantMembership: (String, String, (PlantMembership?) -> Unit) -> Unit,
    onLinkUserToStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onRegisterPlantStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
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
    var currentScreen by remember { mutableStateOf(AppScreen.MainMenu) }
    var lastCreatedPlantCredentials by remember { mutableStateOf<PlantCredentials?>(null) }
    var userPlant by remember { mutableStateOf<Plant?>(null) }
    var plantMembership by remember { mutableStateOf<PlantMembership?>(null) }
    var isLoadingPlant by remember { mutableStateOf(false) }
    var isLoadingMembership by remember { mutableStateOf(false) }
    var isLinkingStaff by remember { mutableStateOf(false) }
    var plantError by remember { mutableStateOf<String?>(null) }
    var selectedPlantForDetail by remember { mutableStateOf<Plant?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val sharedDatePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    val refreshUserPlant: () -> Unit = {
        plantError = null
        isLoadingPlant = true
        isLoadingMembership = true
        plantMembership = null
        onLoadPlant { plant, error ->
            userPlant = plant
            plantError = error
            isLoadingPlant = false
            val uid = user?.uid
            if (plant != null && uid != null) {
                onLoadPlantMembership(plant.id, uid) { membership ->
                    plantMembership = membership
                    isLoadingMembership = false
                }
            } else {
                isLoadingMembership = false
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        compactLogo = true
        delay(300)
        showLogin = true
    }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            isLoadingProfile = true
            existingProfile = null
            userPlant = null
            currentScreen = AppScreen.MainMenu
            onLoadProfile { profile ->
                existingProfile = profile
                isLoadingProfile = false
            }
        } else {
            existingProfile = null
            showRegistration = false
            isLoadingProfile = false
            userPlant = null
            plantMembership = null
            plantError = null
            isLoadingPlant = false
            isLoadingMembership = false
            currentScreen = AppScreen.MainMenu
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
        } else if (isLoadingProfile) {
            ProfileLoadingScreen(message = stringResource(id = R.string.loading_profile))
        } else {
            when (currentScreen) {
                AppScreen.MainMenu -> MainMenuScreen(
                    modifier = Modifier.fillMaxSize(),
                    userEmail = user.email.orEmpty(),
                    profile = existingProfile,
                    isLoadingProfile = isLoadingProfile,
                    datePickerState = sharedDatePickerState,
                    onCreatePlant = { currentScreen = AppScreen.CreatePlant },
                    onEditProfile = { showProfileEditor = true },
                    onOpenPlant = {
                        currentScreen = AppScreen.MyPlant
                        refreshUserPlant()
                    },
                    onSignOut = onSignOut
                )

                AppScreen.CreatePlant -> PlantCreationScreen(
                    onBack = { currentScreen = AppScreen.MainMenu },
                    onPlantCreated = { credentials ->
                        lastCreatedPlantCredentials = credentials
                        currentScreen = AppScreen.PlantCreated
                    },
                    currentUserId = user?.uid,
                    currentUserProfile = existingProfile
                )

                AppScreen.PlantCreated -> PlantCreatedScreen(
                    credentials = lastCreatedPlantCredentials,
                    onBackToMenu = { currentScreen = AppScreen.MainMenu }
                )

                AppScreen.MyPlant -> MyPlantScreen(
                    modifier = Modifier.fillMaxSize(),
                    plant = userPlant,
                    isLoading = isLoadingPlant,
                    isLoadingMembership = isLoadingMembership,
                    currentUserProfile = existingProfile,
                    plantMembership = plantMembership,
                    errorMessage = plantError,
                    isLinkingStaff = isLinkingStaff,
                    onBack = { currentScreen = AppScreen.MainMenu },
                    onRefresh = { refreshUserPlant() },
                    onOpenPlantDetail = { plant ->
                        selectedPlantForDetail = plant
                        currentScreen = AppScreen.PlantDetail
                    },
                    onJoinPlant = { plantId, invitationCode, onResult ->
                        onJoinPlant(plantId, invitationCode, existingProfile) { success, message ->
                            if (!success && message != null) {
                                plantError = message
                            }
                            onResult(success, message)
                            if (success) {
                                refreshUserPlant()
                            }
                        }
                    },
                    onLinkUserToStaff = { staff ->
                        val plantId = userPlant?.id ?: return@MyPlantScreen
                        isLinkingStaff = true
                        onLinkUserToStaff(plantId, staff) { success ->
                            if (success) {
                                val uid = user?.uid.orEmpty()
                                plantMembership = PlantMembership(
                                    plantId = plantId,
                                    userId = uid,
                                    staffId = staff.id,
                                    staffName = staff.name,
                                    staffRole = staff.role
                                )
                                existingProfile = existingProfile?.copy(role = staff.role)
                            }
                            isLinkingStaff = false
                        }
                    }
                )

                AppScreen.PlantDetail -> PlantDetailScreen(
                    plant = selectedPlantForDetail,
                    datePickerState = sharedDatePickerState,
                    currentUserProfile = existingProfile,
                    currentMembership = plantMembership,
                    onBack = { currentScreen = AppScreen.MyPlant },
                    onAddStaff = { plantId, staffMember, onResult ->
                        onRegisterPlantStaff(plantId, staffMember) { success ->
                            if (success) {
                                selectedPlantForDetail = selectedPlantForDetail?.copy(
                                    personal_de_planta = selectedPlantForDetail?.personal_de_planta.orEmpty() +
                                        (staffMember.id to staffMember)
                                )
                            }
                            onResult(success)
                        }
                    }
                )
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
                            onComplete(true)
                        } else {
                            saveCompleted = false
                            showProfileEditor = true
                            onComplete(false)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ProfileLoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x22FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFF54C7EC))
                Text(text = message, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
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
            onLoadPlant = { onResult -> onResult(null, null) },
            onJoinPlant = { _, _, _, _ -> },
            onLoadPlantMembership = { _, _, _ -> },
            onLinkUserToStaff = { _, _, _ -> },
            onRegisterPlantStaff = { _, _, _ -> },
            onSignOut = {}
        )
    }
}
