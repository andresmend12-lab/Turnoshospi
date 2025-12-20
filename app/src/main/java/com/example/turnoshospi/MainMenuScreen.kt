package com.example.turnoshospi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.ShiftColors
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    profile: UserProfile?,
    isLoadingProfile: Boolean,
    userPlant: Plant?,
    plantMembership: PlantMembership?,
    datePickerState: DatePickerState,
    shiftColors: ShiftColors,
    onCreatePlant: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPlant: () -> Unit,
    onOpenSettings: () -> Unit,
    onListenToShifts: (String, String, (Map<String, UserShift>) -> Unit) -> Unit,
    onFetchColleagues: (String, String, String, (List<Colleague>) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onOpenDirectChats: () -> Unit,
    unreadChatCount: Int = 0,
    unreadNotificationsCount: Int,
    onOpenNotifications: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var userShifts by remember { mutableStateOf<Map<String, UserShift>>(emptyMap()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedShift by remember { mutableStateOf<UserShift?>(null) }
    var colleaguesList by remember { mutableStateOf<List<Colleague>>(emptyList()) }
    var isLoadingColleagues by remember { mutableStateOf(false) }

    // Estados Supervisor
    val database = remember { FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/") }
    var selectedDateRoster by remember { mutableStateOf<Map<String, ShiftRoster>>(emptyMap()) }
    var isLoadingRoster by remember { mutableStateOf(false) }
    val unassignedLabel = stringResource(id = R.string.staff_unassigned_option)

    LaunchedEffect(userPlant, plantMembership) {
        if (userPlant != null && plantMembership?.staffId != null) {
            onListenToShifts(userPlant.id, plantMembership.staffId) { shifts -> userShifts = shifts }
        }
    }

    val loadingName = stringResource(id = R.string.loading_profile)
    val displayName = when {
        !profile?.firstName.isNullOrBlank() -> profile?.firstName.orEmpty()
        isLoadingProfile -> loadingName
        !profile?.email.isNullOrBlank() -> profile?.email.orEmpty()
        else -> userEmail
    }

    val welcomeStringId = remember(profile?.gender) {
        if (profile?.gender == "female") R.string.main_menu_welcome_female else R.string.main_menu_welcome_male
    }

    // NOTA: "Supervis" es un valor interno de la DB, no un texto de UI, se puede dejar así o mover a constantes
    val showCreatePlant = profile?.role?.contains("Supervis") == true
    val isSupervisor = showCreatePlant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Cabecera Superior (Menú, Nombre, Notificaciones)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = { isMenuOpen = true }
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(R.string.cd_menu), // "Menú"
                        tint = Color.White
                    )
                }

                Crossfade(targetState = displayName, animationSpec = tween(durationMillis = 600)) { name ->
                    Text(
                        text = stringResource(id = welcomeStringId, name),
                        modifier = Modifier.padding(horizontal = 56.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = onOpenNotifications
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(containerColor = Color(0xFFE91E63)) {
                                    Text(if (unreadNotificationsCount > 99) "99+" else "$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications), // "Notificaciones"
                            tint = Color.White
                        )
                    }
                }
            }

            // TARJETA PRINCIPAL DEL CALENDARIO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                if (userPlant == null) {
                    CustomCalendarOffline(shiftColors = shiftColors)
                } else {
                    CustomCalendar(
                        shifts = userShifts,
                        plantId = userPlant.id,
                        selectedDate = selectedDate,
                        selectedShift = selectedShift,
                        colleagues = colleaguesList,
                        isLoadingColleagues = isLoadingColleagues,
                        isSupervisor = isSupervisor,
                        roster = selectedDateRoster,
                        isLoadingRoster = isLoadingRoster,
                        shiftColors = shiftColors,
                        onDayClick = { date, shift ->
                            selectedDate = date
                            selectedShift = shift
                            colleaguesList = emptyList()
                            selectedDateRoster = emptyMap()

                            if (isSupervisor) {
                                isLoadingRoster = true
                                database.reference.child("plants/${userPlant.id}/turnos/turnos-$date")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val newRoster = mutableMapOf<String, ShiftRoster>()
                                            if (snapshot.exists()) {
                                                snapshot.children.forEach { shiftSnap ->
                                                    val shiftName = shiftSnap.key ?: return@forEach
                                                    fun parseSlots(node: String) = shiftSnap.child(node).children.mapNotNull {
                                                        val p = it.child("primary").value as? String
                                                        if (!p.isNullOrBlank() && p != unassignedLabel) p else null
                                                    }
                                                    val nurses = parseSlots("nurses")
                                                    val auxs = parseSlots("auxiliaries")
                                                    if (nurses.isNotEmpty() || auxs.isNotEmpty()) {
                                                        newRoster[shiftName] = ShiftRoster(nurses, auxs)
                                                    }
                                                }
                                            }
                                            selectedDateRoster = newRoster
                                            isLoadingRoster = false
                                        }
                                        override fun onCancelled(error: DatabaseError) { isLoadingRoster = false }
                                    })
                            } else if (shift != null) {
                                isLoadingColleagues = true
                                onFetchColleagues(userPlant.id, date.toString(), shift.shiftName) {
                                    colleaguesList = it
                                    isLoadingColleagues = false
                                }
                            }
                        }
                    )
                }
            }
        }

        // Drawer (Menú Lateral)
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .padding(vertical = 16.dp)
                ) {
                    DrawerHeader(displayName, welcomeStringId)
                    if (showCreatePlant) {
                        DrawerMenuItem(
                            stringResource(R.string.menu_create_plant),
                            stringResource(R.string.menu_create_plant_desc)
                        ) { isMenuOpen = false; onCreatePlant() }
                    }

                    DrawerMenuItem(
                        stringResource(R.string.menu_my_plants),
                        stringResource(R.string.menu_my_plants_desc)
                    ) { isMenuOpen = false; onOpenPlant() }

                    DrawerMenuItem(
                        stringResource(R.string.edit_profile),
                        stringResource(R.string.edit_profile) // Puede que quieras usar menu_edit_profile_desc si existe
                    ) { isMenuOpen = false; onEditProfile() }

                    DrawerMenuItem(
                        stringResource(R.string.menu_settings),
                        stringResource(R.string.menu_settings_desc)
                    ) { isMenuOpen = false; onOpenSettings() }

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = { Text(stringResource(R.string.sign_out), color = Color(0xFFFFB4AB)) },
                        selected = false,
                        onClick = { isMenuOpen = false; onSignOut() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { isMenuOpen = false })
            }
        }

        // FAB (Chat)
        if (userPlant != null && !isMenuOpen) {
            FloatingActionButton(
                onClick = onOpenDirectChats,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp, 40.dp),
                containerColor = Color(0xFF54C7EC),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                BadgedBox(badge = { if (unreadChatCount > 0) Badge(containerColor = Color.Red) { Text("$unreadChatCount") } }) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = stringResource(R.string.cd_chats) // "Chats"
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerHeader(displayName: String, welcomeStringId: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painterResource(R.drawable.ic_logo_hospi_round),
            stringResource(R.string.app_name),
            modifier = Modifier.size(48.dp)
        )
        Crossfade(targetState = displayName) {
            Text(stringResource(welcomeStringId, it), style = MaterialTheme.typography.bodySmall, color = Color(0xCCFFFFFF))
        }
    }
    HorizontalDivider(color = Color(0x22FFFFFF))
}

@Composable
fun DrawerMenuItem(label: String, description: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp),
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(description, color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodySmall)
            }
        },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    TurnoshospiTheme {
        MainMenuScreen(
            userEmail = "test@test.com", profile = null, isLoadingProfile = false,
            userPlant = null, plantMembership = null, datePickerState = rememberDatePickerState(),
            shiftColors = ShiftColors(), onCreatePlant = {}, onEditProfile = {}, onOpenPlant = {},
            onOpenSettings = {}, onListenToShifts = { _, _, _ -> }, onFetchColleagues = { _, _, _, _ -> },
            onSignOut = {}, onOpenDirectChats = {}, unreadNotificationsCount = 0, onOpenNotifications = {}
        )
    }
}
