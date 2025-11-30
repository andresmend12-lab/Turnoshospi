package com.example.turnoshospi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class AppScreen {
    MainMenu,
    CreatePlant,
    PlantCreated,
    MyPlant,
    PlantDetail,
    Settings,
    PlantSettings,
    ImportShifts,
    GroupChat,
    ShiftChange,
    ShiftMarketplace,
    Statistics,
    DirectChatList,
    DirectChat,
    Notifications
}

data class Colleague(
    val name: String,
    val role: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoshospiApp(
    user: FirebaseUser?,
    errorMessage: String?,
    pendingNavigation: Map<String, String>?,
    onNavigationHandled: () -> Unit,
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
    onEditPlantStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onListenToShifts: (String, String, (Map<String, UserShift>) -> Unit) -> Unit,
    onFetchColleagues: (String, String, String, (List<Colleague>) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDeletePlant: (String) -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit,
    onListenToNotifications: (String, (List<UserNotification>) -> Unit) -> Unit,
    onMarkNotificationAsRead: (String, String) -> Unit,
    onDeleteNotification: (String, String) -> Unit,
    onClearAllNotifications: (String) -> Unit,
    onListenToChatUnreadCounts: (String, (Map<String, Int>) -> Unit) -> Unit
) {
    var showLogin by remember { mutableStateOf(true) }
    var showRegistration by remember { mutableStateOf(false) }
    var compactLogo by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var existingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var saveCompleted by remember { mutableStateOf(false) }
    var emailForReset by remember { mutableStateOf("") }
    var showProfileEditor by remember { mutableStateOf(false) }

    // NAVEGACIÓN: Estado y Pila
    var currentScreen by remember { mutableStateOf(AppScreen.MainMenu) }
    val backStack = remember { mutableStateListOf<AppScreen>() }

    var lastCreatedPlantCredentials by remember { mutableStateOf<PlantCredentials?>(null) }
    var userPlant by remember { mutableStateOf<Plant?>(null) }
    var plantMembership by remember { mutableStateOf<PlantMembership?>(null) }
    var isLoadingPlant by remember { mutableStateOf(false) }
    var isLoadingMembership by remember { mutableStateOf(false) }
    var isLinkingStaff by remember { mutableStateOf(false) }
    var plantError by remember { mutableStateOf<String?>(null) }
    var selectedPlantForDetail by remember { mutableStateOf<Plant?>(null) }

    // Estados para el chat directo
    var selectedDirectChatUserId by remember { mutableStateOf("") }
    var selectedDirectChatUserName by remember { mutableStateOf("") }
    var chatUnreadCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Calculamos el total de mensajes no leídos para el botón del menú
    val totalUnreadChats = remember(chatUnreadCounts) {
        chatUnreadCounts.values.sum()
    }

    // Estado para notificaciones
    var userNotifications by remember { mutableStateOf<List<UserNotification>>(emptyList()) }
    val unreadNotificationsCount = remember(userNotifications) {
        userNotifications.count { !it.isRead }
    }

    val coroutineScope = rememberCoroutineScope()

    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val sharedDatePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    // --- Helpers de Navegación ---
    fun navigateTo(screen: AppScreen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeLast()
        }
    }

    // --- Manejo del botón atrás físico ---
    BackHandler(enabled = (user == null && showRegistration) || (user != null && backStack.isNotEmpty())) {
        if (user == null && showRegistration) {
            showRegistration = false
        } else if (user != null) {
            navigateBack()
        }
    }

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

            // Resetear navegación al loguearse
            currentScreen = AppScreen.MainMenu
            backStack.clear()

            onLoadProfile { profile ->
                existingProfile = profile
                isLoadingProfile = false
            }

            // Escuchar notificaciones del usuario
            onListenToNotifications(user.uid) { notifications ->
                userNotifications = notifications
            }

            // Escuchar contadores de chat
            onListenToChatUnreadCounts(user.uid) { counts ->
                chatUnreadCounts = counts
            }

            refreshUserPlant()
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
            backStack.clear()
            userNotifications = emptyList()
            chatUnreadCounts = emptyMap()
        }
    }

    // MANEJO DE DEEP LINKING (NAVEGACIÓN DESDE NOTIFICACIÓN)
    LaunchedEffect(pendingNavigation, user, userPlant) {
        if (user != null && pendingNavigation != null && userPlant != null) {
            val screen = pendingNavigation["screen"]
            if (screen == "DirectChat") {
                val otherId = pendingNavigation["otherUserId"]
                val otherName = pendingNavigation["otherUserName"]

                if (otherId != null && otherName != null) {
                    selectedDirectChatUserId = otherId
                    selectedDirectChatUserName = otherName

                    // Si ya estamos en el chat, no hacemos nada, si no, navegamos
                    if (currentScreen != AppScreen.DirectChat) {
                        navigateTo(AppScreen.DirectChat)
                    }
                }
            }
            // Marcamos como manejada
            onNavigationHandled()
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
                    userPlant = userPlant,
                    plantMembership = plantMembership,
                    datePickerState = sharedDatePickerState,
                    onListenToShifts = onListenToShifts,
                    onFetchColleagues = onFetchColleagues,
                    onCreatePlant = { navigateTo(AppScreen.CreatePlant) },
                    onEditProfile = { showProfileEditor = true },
                    onOpenPlant = {
                        // Verificamos si ya hay una planta cargada
                        if (userPlant != null) {
                            selectedPlantForDetail = userPlant // Preparamos la planta para mostrar sus detalles
                            navigateTo(AppScreen.PlantDetail)  // Vamos directo al detalle
                        } else {
                            navigateTo(AppScreen.MyPlant)      // Si no tiene planta, vamos a la pantalla de selección/unión
                        }
                        refreshUserPlant() // Mantenemos la actualización de datos en segundo plano
                    },
                    onOpenSettings = { navigateTo(AppScreen.Settings) },
                    onSignOut = onSignOut,
                    unreadChatCount = totalUnreadChats,
                    onOpenDirectChats = {
                        if (userPlant != null) {
                            selectedPlantForDetail = userPlant
                            navigateTo(AppScreen.DirectChatList)
                        }
                    },
                    unreadNotificationsCount = unreadNotificationsCount,
                    onOpenNotifications = { navigateTo(AppScreen.Notifications) }
                )

                AppScreen.CreatePlant -> PlantCreationScreen(
                    onBack = { navigateBack() },
                    onPlantCreated = { credentials ->
                        lastCreatedPlantCredentials = credentials
                        navigateTo(AppScreen.PlantCreated)
                    },
                    currentUserId = user?.uid,
                    currentUserProfile = existingProfile
                )

                AppScreen.PlantCreated -> PlantCreatedScreen(
                    credentials = lastCreatedPlantCredentials,
                    onBackToMenu = {
                        backStack.clear()
                        currentScreen = AppScreen.MainMenu
                    }
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
                    onBack = { navigateBack() },
                    onRefresh = { refreshUserPlant() },
                    onOpenPlantDetail = { plant ->
                        selectedPlantForDetail = plant
                        navigateTo(AppScreen.PlantDetail)
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
                    onBack = { navigateBack() },
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
                    },
                    onEditStaff = { plantId, staffMember, onResult ->
                        onEditPlantStaff(plantId, staffMember) { success ->
                            if (success) {
                                selectedPlantForDetail = selectedPlantForDetail?.copy(
                                    personal_de_planta = selectedPlantForDetail?.personal_de_planta.orEmpty() +
                                            (staffMember.id to staffMember)
                                )
                            }
                            onResult(success)
                        }
                    },
                    onOpenPlantSettings = { navigateTo(AppScreen.PlantSettings) },
                    onOpenImportShifts = { navigateTo(AppScreen.ImportShifts) },
                    onOpenChat = { navigateTo(AppScreen.GroupChat) },
                    onOpenShiftChange = { navigateTo(AppScreen.ShiftChange) },
                    onOpenShiftMarketplace = { navigateTo(AppScreen.ShiftMarketplace) },
                    onOpenStatistics = { navigateTo(AppScreen.Statistics) },
                    onSaveNotification = onSaveNotification
                )
                AppScreen.Settings -> SettingsScreen(
                    onBack = { navigateBack() },
                    onDeleteAccount = onDeleteAccount
                )

                AppScreen.PlantSettings -> PlantSettingsScreen(
                    plant = userPlant,
                    onBack = { navigateBack() },
                    onDeletePlant = { plantId ->
                        onDeletePlant(plantId)
                        refreshUserPlant()
                        while(backStack.isNotEmpty() && backStack.last() != AppScreen.MyPlant && backStack.last() != AppScreen.MainMenu) {
                            backStack.removeLast()
                        }
                        if (backStack.isNotEmpty()) currentScreen = backStack.removeLast()
                        else currentScreen = AppScreen.MainMenu
                    }
                )

                AppScreen.ImportShifts -> ImportShiftsScreen(
                    plant = selectedPlantForDetail,
                    onBack = { navigateBack() }
                )

                AppScreen.GroupChat -> GroupChatScreen(
                    plantId = selectedPlantForDetail?.id ?: "",
                    currentUser = existingProfile,
                    currentUserId = user?.uid ?: "",
                    onBack = { navigateBack() },
                    onSaveNotification = onSaveNotification
                )

                AppScreen.ShiftChange -> ShiftChangeScreen(
                    plantId = selectedPlantForDetail?.id ?: "",
                    currentUser = existingProfile,
                    currentUserId = user?.uid ?: "",
                    onBack = { navigateBack() },
                    onSaveNotification = onSaveNotification
                )

                AppScreen.ShiftMarketplace -> ShiftMarketplaceScreen(
                    plantId = selectedPlantForDetail?.id ?: userPlant?.id ?: "",
                    currentUserId = user?.uid ?: "",
                    currentUserName = "${existingProfile?.firstName} ${existingProfile?.lastName}".trim(),
                    currentUserRole = existingProfile?.role ?: "", // <--- CORRECCIÓN AQUÍ
                    onBack = { navigateBack() },
                    onSaveNotification = onSaveNotification
                )

                AppScreen.Statistics -> StatisticsScreen(
                    plant = selectedPlantForDetail ?: userPlant,
                    currentUserEmail = existingProfile?.email,
                    currentUserName = plantMembership?.staffName ?: "${existingProfile?.firstName} ${existingProfile?.lastName}".trim(),
                    onBack = { navigateBack() }
                )

                AppScreen.DirectChatList -> DirectChatListScreen(
                    plantId = selectedPlantForDetail?.id ?: userPlant?.id ?: "",
                    currentUserId = user?.uid ?: "",
                    unreadCounts = chatUnreadCounts,
                    onBack = { navigateBack() },
                    onNavigateToChat = { otherId, otherName ->
                        selectedDirectChatUserId = otherId
                        selectedDirectChatUserName = otherName
                        navigateTo(AppScreen.DirectChat)
                    }
                )

                AppScreen.DirectChat -> DirectChatScreen(
                    plantId = selectedPlantForDetail?.id ?: userPlant?.id ?: "",
                    currentUserId = user?.uid ?: "",
                    otherUserId = selectedDirectChatUserId,
                    otherUserName = selectedDirectChatUserName,
                    onBack = { navigateBack() }
                )

                AppScreen.Notifications -> NotificationsScreen(
                    notifications = userNotifications,
                    onBack = { navigateBack() },
                    onMarkAsRead = { notifId ->
                        user?.uid?.let { uid -> onMarkNotificationAsRead(uid, notifId) }
                    },
                    onDelete = { notifId ->
                        user?.uid?.let { uid -> onDeleteNotification(uid, notifId) }
                    },
                    onDeleteAll = {
                        user?.uid?.let { uid -> onClearAllNotifications(uid) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Gestionar cuenta",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Borrar mi cuenta")
                    }

                    Text(
                        "Esta acción es permanente y no se puede deshacer. Se borrarán todos tus datos asociados a la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esta acción es irreversible. Se borrará tu cuenta y todos tus datos. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantSettingsScreen(
    plant: Plant?,
    onBack: () -> Unit,
    onDeletePlant: (String) -> Unit
) {
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de planta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            if (plant != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            plant.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )

                        Button(
                            onClick = { showConfirmDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Borrar planta")
                        }

                        Text(
                            "Esta acción es permanente y no se puede deshacer. Se borrarán todos los datos de la planta y se desvincularán todos los usuarios asociados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Estás a punto de borrar la planta \"${plant?.name}\". Esta acción es irreversible y afectará a todos los usuarios asociados a ella. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        plant?.id?.let { onDeletePlant(it) }
                    }
                ) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancelar")
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
            pendingNavigation = null,
            onNavigationHandled = {},
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
            onEditPlantStaff = { _, _, _ -> },
            onListenToShifts = { _, _, _ -> },
            onFetchColleagues = { _, _, _, _ -> },
            onSignOut = {},
            onDeleteAccount = {},
            onDeletePlant = {},
            onSaveNotification = { _, _, _, _, _, _ -> },
            onListenToNotifications = { _, _ -> },
            onMarkNotificationAsRead = { _, _ -> },
            onDeleteNotification = { _, _ -> },
            onClearAllNotifications = { _ -> },
            onListenToChatUnreadCounts = { _, _ -> }
        )
    }
}