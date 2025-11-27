package com.example.turnoshospi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// --- DEFINICIONES DE CLASES COMPARTIDAS (MODELOS) ---
data class UserProfile(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "",
    val gender: String = ""
)

data class UserShift(
    val shiftName: String = "",
    val date: String = "",
    val isHalfDay: Boolean = false
)

data class Colleague(
    val name: String,
    val role: String
)

enum class AppScreen {
    MainMenu, CreatePlant, PlantCreated, MyPlant, PlantDetail, Settings, PlantSettings, ImportShifts, GroupChat, ShiftChange, Notifications
}

// --- APP PRINCIPAL ---
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
    onEditPlantStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onListenToShifts: (String, String, (Map<String, UserShift>) -> Unit) -> Unit,
    onFetchColleagues: (String, String, String, (List<Colleague>) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDeletePlant: (String) -> Unit
) {
    // --- ESTADOS UI ---
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
    val context = LocalContext.current

    // --- ESTADOS NOTIFICACIONES ---
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { NotificationHelper.createNotificationChannel(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(user) {
        if (user != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            val db = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
            val ref = db.getReference("users/${user.uid}/notifications")
            ref.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val notif = snapshot.getValue(AppNotification::class.java)
                    if (notif != null && !notif.read) {
                        unreadNotificationsCount++
                        if (System.currentTimeMillis() - notif.timestamp < 10000) {
                            NotificationHelper.showSystemNotification(context, notif.title, notif.message)
                        }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    ref.orderByChild("read").equalTo(false).get().addOnSuccessListener { unreadNotificationsCount = it.childrenCount.toInt() }
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
            ref.orderByChild("read").equalTo(false).get().addOnSuccessListener { unreadNotificationsCount = it.childrenCount.toInt() }
        }
    }

    val todayMillis = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val sharedDatePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    val refreshUserPlant: () -> Unit = {
        plantError = null; isLoadingPlant = true; isLoadingMembership = true; plantMembership = null
        onLoadPlant { plant, error ->
            userPlant = plant; plantError = error; isLoadingPlant = false
            if (plant != null && user?.uid != null) {
                onLoadPlantMembership(plant.id, user.uid) { membership -> plantMembership = membership; isLoadingMembership = false }
            } else { isLoadingMembership = false }
        }
    }

    LaunchedEffect(Unit) { delay(2000); compactLogo = true; delay(300); showLogin = true }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            isLoadingProfile = true; existingProfile = null; userPlant = null; currentScreen = AppScreen.MainMenu
            onLoadProfile { profile -> existingProfile = profile; isLoadingProfile = false }
            refreshUserPlant()
        } else {
            existingProfile = null; showRegistration = false; isLoadingProfile = false; userPlant = null
            plantMembership = null; plantError = null; isLoadingPlant = false; isLoadingMembership = false; currentScreen = AppScreen.MainMenu
        }
    }

    val logoSize by animateDpAsState(if (compactLogo) 120.dp else 240.dp, tween(500), label = "logoSize")
    val loginAlpha by animateFloatAsState(if (showLogin) 1f else 0f, tween(350, 100), label = "loginAlpha")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1021), Color(0xFF0F172A), Color(0xFF0E1A2F)))).padding(24.dp)) {
        Box(Modifier.align(Alignment.TopEnd).size(180.dp).background(Brush.radialGradient(listOf(Color(0x6654C7EC), Color.Transparent)), RoundedCornerShape(90.dp)).blur(50.dp))
        Box(Modifier.align(Alignment.BottomStart).size(220.dp).background(Brush.radialGradient(listOf(Color(0x66A855F7), Color.Transparent)), RoundedCornerShape(110.dp)).blur(65.dp))

        if (user == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = if (showLogin) Arrangement.Top else Arrangement.Center) {
                Spacer(modifier = Modifier.height(if (showLogin) 32.dp else 0.dp))
                Image(painterResource(id = R.mipmap.ic_logo_hospi_foreground), "Logo", Modifier.size(logoSize))
                AnimatedVisibility(visible = showLogin) {
                    if (showRegistration) {
                        CreateAccountScreen(Modifier.fillMaxWidth().padding(top = 32.dp).graphicsLayer(alpha = loginAlpha), onBack = { showRegistration = false }, onCreate = { p, pw, cb -> coroutineScope.launch { onCreateAccount(p, pw) { s -> saveCompleted = s; cb(s) } } })
                    } else {
                        LoginCard(Modifier.fillMaxWidth().padding(top = 32.dp).graphicsLayer(alpha = loginAlpha), email = emailForReset, onEmailChange = { emailForReset = it }, onLogin = { e, p, cb -> coroutineScope.launch { onLogin(e, p) { cb(it) } } }, onCreateAccount = { showRegistration = true }, onForgotPassword = { e, cb -> coroutineScope.launch { onForgotPassword(e) { cb(it) } } })
                    }
                }
            }
        } else if (isLoadingProfile) {
            ProfileLoadingScreen(stringResource(id = R.string.loading_profile))
        } else {
            Scaffold(containerColor = Color.Transparent, topBar = { }) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.MainMenu -> MainMenuScreen(Modifier.fillMaxSize(), user.email.orEmpty(), existingProfile, isLoadingProfile, userPlant, plantMembership, sharedDatePickerState, onListenToShifts, onFetchColleagues, { currentScreen = AppScreen.CreatePlant }, { showProfileEditor = true }, { currentScreen = AppScreen.MyPlant; refreshUserPlant() }, { currentScreen = AppScreen.Settings }, onSignOut)
                        AppScreen.CreatePlant -> PlantCreationScreen({ currentScreen = AppScreen.MainMenu }, { creds -> lastCreatedPlantCredentials = creds; currentScreen = AppScreen.PlantCreated }, user.uid, existingProfile)
                        AppScreen.PlantCreated -> PlantCreatedScreen(lastCreatedPlantCredentials) { currentScreen = AppScreen.MainMenu }
                        AppScreen.MyPlant -> MyPlantScreen(Modifier.fillMaxSize(), userPlant, isLoadingPlant, isLoadingMembership, existingProfile, plantMembership, plantError, isLinkingStaff, { currentScreen = AppScreen.MainMenu }, { refreshUserPlant() }, { p -> selectedPlantForDetail = p; currentScreen = AppScreen.PlantDetail }, { id, code, res -> onJoinPlant(id, code, existingProfile) { s, m -> if (!s && m != null) plantError = m; res(s, m); if (s) refreshUserPlant() } }) { s -> isLinkingStaff = true; onLinkUserToStaff(userPlant!!.id, s) { if (it) { plantMembership = PlantMembership(userPlant!!.id, user.uid, s.id, s.name, s.role); existingProfile = existingProfile?.copy(role = s.role) }; isLinkingStaff = false } }
                        AppScreen.PlantDetail -> PlantDetailScreen(selectedPlantForDetail, sharedDatePickerState, existingProfile, plantMembership, { currentScreen = AppScreen.MyPlant }, { pid, sm, res -> onRegisterPlantStaff(pid, sm) { if (it) selectedPlantForDetail = selectedPlantForDetail?.copy(personal_de_planta = selectedPlantForDetail?.personal_de_planta.orEmpty() + (sm.id to sm)); res(it) } }, { pid, sm, res -> onEditPlantStaff(pid, sm) { if (it) selectedPlantForDetail = selectedPlantForDetail?.copy(personal_de_planta = selectedPlantForDetail?.personal_de_planta.orEmpty() + (sm.id to sm)); res(it) } }, { currentScreen = AppScreen.PlantSettings }, { currentScreen = AppScreen.ImportShifts }, { currentScreen = AppScreen.GroupChat }, { currentScreen = AppScreen.ShiftChange })
                        AppScreen.Settings -> SettingsScreen({ currentScreen = AppScreen.MainMenu }, onDeleteAccount)
                        AppScreen.PlantSettings -> PlantSettingsScreen(userPlant, { currentScreen = AppScreen.MyPlant }, { pid -> onDeletePlant(pid); refreshUserPlant(); currentScreen = AppScreen.MyPlant })
                        AppScreen.ImportShifts -> ImportShiftsScreen(selectedPlantForDetail) { currentScreen = AppScreen.PlantDetail }
                        AppScreen.GroupChat -> GroupChatScreen(selectedPlantForDetail?.id ?: "", existingProfile, user.uid) { currentScreen = AppScreen.PlantDetail }
                        AppScreen.ShiftChange -> ShiftChangeScreen(selectedPlantForDetail?.id ?: "", existingProfile, user.uid) { currentScreen = AppScreen.PlantDetail }
                        AppScreen.Notifications -> NotificationsScreen(user.uid) { currentScreen = AppScreen.MainMenu }
                    }

                    if (currentScreen != AppScreen.Notifications && currentScreen != AppScreen.GroupChat && currentScreen != AppScreen.ShiftChange) {
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)) {
                            IconButton(onClick = { currentScreen = AppScreen.Notifications }) { Icon(Icons.Default.Notifications, "Notificaciones", tint = Color.White) }
                            if (unreadNotificationsCount > 0) { Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) { Text(text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                        }
                    }
                }
            }
        }
    }

    if (errorMessage != null) AlertDialog(onDismissRequest = onErrorDismiss, confirmButton = { TextButton(onClick = onErrorDismiss) { Text("Entendido") } }, title = { Text("Aviso") }, text = { Text(errorMessage) })
    if (saveCompleted) AlertDialog(onDismissRequest = { saveCompleted = false }, confirmButton = { TextButton(onClick = { saveCompleted = false }) { Text("Cerrar") } }, title = { Text("Perfil guardado") }, text = { Text("Los datos de tu cuenta se han actualizado correctamente.") })
    if (showProfileEditor && user != null) ProfileEditorOverlay(user.email.orEmpty(), existingProfile, isLoadingProfile, { showProfileEditor = false }, { p, cb -> saveCompleted = false; coroutineScope.launch { onSaveProfile(p) { s -> if (s) { existingProfile = p; showProfileEditor = false; saveCompleted = true; isLoadingProfile = true; onLoadProfile { existingProfile = it ?: p; isLoadingProfile = false; saveCompleted = true }; cb(true) } else { saveCompleted = false; showProfileEditor = true; cb(false) } } } })
}

@Composable
fun ProfileLoadingScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0x33000000)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
            Column(Modifier.padding(horizontal = 32.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = Color(0xFF54C7EC)); Text(text = message, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onDeleteAccount: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("Configuración", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)), border = BorderStroke(1.dp, Color(0x33FFFFFF))) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Gestionar cuenta", style = MaterialTheme.typography.titleLarge, color = Color.White); Button({ showConfirmDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Borrar mi cuenta") }; Text("Esta acción es permanente.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
    if (showConfirmDialog) AlertDialog({ showConfirmDialog = false }, { TextButton({ showConfirmDialog = false; onDeleteAccount() }) { Text("Borrar", color = MaterialTheme.colorScheme.error) } }, { TextButton({ showConfirmDialog = false }) { Text("Cancelar") } }, title = { Text("¿Estás seguro?") }, text = { Text("Se borrará tu cuenta.") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantSettingsScreen(plant: Plant?, onBack: () -> Unit, onDeletePlant: (String) -> Unit) {
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("Configuración de planta", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(16.dp))
            if (plant != null) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)), border = BorderStroke(1.dp, Color(0x33FFFFFF))) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(plant.name, style = MaterialTheme.typography.titleLarge, color = Color.White); Button({ showConfirmDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Borrar planta") }; Text("Esta acción es permanente.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
    if (showConfirmDeleteDialog) AlertDialog({ showConfirmDeleteDialog = false }, { TextButton({ showConfirmDeleteDialog = false; plant?.id?.let { onDeletePlant(it) } }) { Text("Borrar", color = MaterialTheme.colorScheme.error) } }, { TextButton({ showConfirmDeleteDialog = false }) { Text("Cancelar") } }, title = { Text("¿Borrar planta?") }, text = { Text("Esta acción es irreversible.") })
}