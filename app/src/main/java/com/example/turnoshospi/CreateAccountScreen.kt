package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCreate: (UserProfile, String, (Boolean) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
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
                onValueChange = {
                    password = it
                    passwordMismatch = false
                },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
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
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordMismatch = false
                },
                label = { Text(stringResource(id = R.string.confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
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

            val roleSupervisor = stringResource(id = R.string.role_supervisor)
            val roleNurse = stringResource(id = R.string.role_nurse)
            val roleAux = stringResource(id = R.string.role_aux)

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
                        .fillMaxWidth()
                        .menuAnchor()
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = roleSupervisor) },
                        onClick = {
                            role = roleSupervisor
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleNurse) },
                        onClick = {
                            role = roleNurse
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = roleAux) },
                        onClick = {
                            role = roleAux
                            expanded = false
                        }
                    )
                }
            }

            if (passwordMismatch) {
                Text(
                    text = "Las contraseñas no coinciden",
                    color = Color(0xFFFFD166),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        passwordMismatch = true
                        return@Button
                    }
                    isSaving = true
                    onCreate(
                        UserProfile(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            role = role,
                            email = email.trim()
                        ),
                        password
                    ) { success ->
                        isSaving = false
                        if (!success) {
                            password = ""
                            confirmPassword = ""
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() &&
                    confirmPassword.isNotBlank() && firstName.isNotBlank() &&
                    lastName.isNotBlank() && role.isNotBlank() && !isSaving,
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
                Text(text = stringResource(id = R.string.create_account_action))
            }

            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = "Volver al inicio de sesión")
            }
        }
    }
}
