package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.R
import com.google.firebase.auth.FirebaseUser

@Composable
fun MainMenuScreen(
    modifier: Modifier = Modifier,
    user: FirebaseUser,
    profile: UserProfile?,
    isLoadingProfile: Boolean,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val displayName = remember(profile, user.email) {
        val fullName = listOfNotNull(
            profile?.firstName?.takeIf { it.isNotBlank() },
            profile?.lastName?.takeIf { it.isNotBlank() }
        ).joinToString(" ")
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
                text = if (isLoadingProfile) {
                    stringResource(id = R.string.loading_profile)
                } else {
                    stringResource(id = R.string.main_menu_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xCCFFFFFF)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.profile_summary_title),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(id = R.string.profile_email_label, user.email.orEmpty()),
                        color = Color(0xCCFFFFFF)
                    )
                    Text(
                        text = stringResource(id = R.string.profile_name_label, displayName),
                        color = Color(0xCCFFFFFF)
                    )
                    val roleLabel = profile?.role?.ifBlank { stringResource(id = R.string.role_aux) }
                        ?: stringResource(id = R.string.role_aux)
                    Text(
                        text = stringResource(id = R.string.profile_role_label, roleLabel),
                        color = Color(0xCCFFFFFF)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEditProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) { Text(text = stringResource(id = R.string.edit_profile)) }
                        TextButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB4AB))
                        ) {
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
                    title = stringResource(id = R.string.menu_settings),
                    subtitle = stringResource(id = R.string.menu_settings_desc)
                )
                MenuOption(
                    title = stringResource(id = R.string.menu_info),
                    subtitle = stringResource(id = R.string.menu_info_desc)
                )
            }
        }
    }
}

@Composable
fun MenuOption(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
