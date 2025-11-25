package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.turnoshospi.R
import com.example.turnoshospi.ui.theme.TurnoshospiTheme

@Composable
fun MainMenuScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    profile: UserProfile?,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val displayName = remember(profile, userEmail) {
        val fullName = listOfNotNull(
            profile?.firstName?.takeIf { it.isNotBlank() },
            profile?.lastName?.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        if (fullName.isNotBlank()) fullName else userEmail
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            text = stringResource(id = R.string.main_menu_welcome, displayName),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.side_menu_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        MenuOption(
                            title = stringResource(id = R.string.menu_create_plant),
                            subtitle = stringResource(id = R.string.menu_create_plant_desc)
                        )
                        MenuOption(
                            title = stringResource(id = R.string.menu_my_plants),
                            subtitle = stringResource(id = R.string.menu_my_plants_desc)
                        )
                        MenuOption(
                            title = stringResource(id = R.string.edit_profile),
                            subtitle = stringResource(id = R.string.menu_settings_desc),
                            onClick = onEditProfile
                        )
                        MenuOption(
                            title = stringResource(id = R.string.menu_settings),
                            subtitle = stringResource(id = R.string.menu_settings_desc)
                        )
                        TextButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB4AB))
                        ) {
                            Text(text = stringResource(id = R.string.sign_out))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.7f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .background(Color(0x22000000), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calendario", // Placeholder for future calendar view
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun MenuOption(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onClick?.invoke() },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun MainMenuScreenPreview() {
    TurnoshospiTheme {
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
            onEditProfile = {},
            onSignOut = {}
        )
    }
}
