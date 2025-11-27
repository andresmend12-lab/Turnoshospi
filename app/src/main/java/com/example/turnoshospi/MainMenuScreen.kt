package com.example.turnoshospi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    profile: UserProfile?,
    isLoadingProfile: Boolean,
    datePickerState: DatePickerState,
    onCreatePlant: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPlant: () -> Unit,
    onSignOut: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

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

    val supervisorMale = stringResource(id = R.string.role_supervisor_male)
    val supervisorFemale = stringResource(id = R.string.role_supervisor_female)
    val showCreatePlant = profile?.role == supervisorMale || profile?.role == supervisorFemale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // CONTENIDO PRINCIPAL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = {
                        isMenuOpen = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.side_menu_title),
                        tint = Color.White
                    )
                }

                Crossfade(
                    targetState = displayName,
                    animationSpec = tween(durationMillis = 600)
                ) { name ->
                    Text(
                        text = stringResource(id = welcomeStringId, name),
                        modifier = Modifier.padding(horizontal = 56.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                border = BorderStroke(5.dp, Color(0x22FFFFFF))
            ) {
                CalendarSection(state = datePickerState)
            }
        }

        // DRAWER PERSONALIZADO
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Panel lateral
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(vertical = 16.dp)
                ) {
                    DrawerHeader(displayName = displayName, welcomeStringId = welcomeStringId)

                    if (showCreatePlant) {
                        DrawerMenuItem(
                            label = stringResource(id = R.string.menu_create_plant),
                            description = stringResource(id = R.string.menu_create_plant_desc),
                            onClick = {
                                isMenuOpen = false
                                onCreatePlant()
                            }
                        )
                    }

                    DrawerMenuItem(
                        label = stringResource(id = R.string.menu_my_plants),
                        description = stringResource(id = R.string.menu_my_plants_desc),
                        onClick = {
                            isMenuOpen = false
                            onOpenPlant()
                        }
                    )

                    DrawerMenuItem(
                        label = stringResource(id = R.string.edit_profile),
                        description = stringResource(id = R.string.menu_settings_desc),
                        onClick = {
                            isMenuOpen = false
                            onEditProfile()
                        }
                    )

                    DrawerMenuItem(
                        label = stringResource(id = R.string.menu_settings),
                        description = stringResource(id = R.string.menu_settings_desc),
                        onClick = {
                            isMenuOpen = false
                        }
                    )

                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        label = {
                            Text(
                                text = stringResource(id = R.string.sign_out),
                                color = Color(0xFFFFB4AB)
                            )
                        },
                        selected = false,
                        onClick = {
                            isMenuOpen = false
                            onSignOut()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color(0xFFFFB4AB)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Scrim clicable para cerrar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0x80000000))
                        .clickable {
                            isMenuOpen = false
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSection(state: DatePickerState) {
    val selectedDate = state.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A), RoundedCornerShape(22.dp))
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mi Calendario",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        DatePicker(
            state = state,
            title = null,
            headline = null,
            showModeToggle = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = DatePickerDefaults.colors(
                containerColor = (MaterialTheme.colorScheme.background),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.White,
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = Color.White,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF1E293B),
                disabledSelectedYearContainerColor = Color(0x661E293B),
                selectedDayContentColor = Color.White,
                disabledSelectedDayContentColor = Color(0x80FFFFFF),
                selectedDayContainerColor = Color(0xFF1E293B),
                disabledSelectedDayContainerColor = Color(0x661E293B),
                dayContentColor = Color.White,
                disabledDayContentColor = Color(0x80FFFFFF),
                dayInSelectionRangeContentColor = Color.White,
                dayInSelectionRangeContainerColor = Color(0x331E293B),
                todayContentColor = Color.White,
                todayDateBorderColor = Color(0x66FFFFFF)
            )
        )

    }
}

fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy")
    return date.format(formatter)
}

@Composable
fun DrawerHeader(displayName: String, welcomeStringId: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(id = R.string.side_menu_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Crossfade(
            targetState = displayName,
            animationSpec = tween(durationMillis = 600)
        ) { name ->
            Text(
                text = stringResource(id = welcomeStringId, name),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xCCFFFFFF)
            )
        }
    }
    Divider(color = Color(0x22FFFFFF))
}

@Composable
fun DrawerMenuItem(label: String, description: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp),
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    color = Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.White
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreenPreview() {
    TurnoshospiTheme {
        val previewDateState =
            rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        MainMenuScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            userEmail = "demo@example.com",
            profile = UserProfile(
                firstName = "Ana",
                lastName = "Martínez",
                role = "Supervisora",
                email = "demo@example.com"
            ),
            isLoadingProfile = false,
            datePickerState = previewDateState,
            onCreatePlant = {},
            onEditProfile = {},
            onOpenPlant = {},
            onSignOut = {}
        )
    }
}
