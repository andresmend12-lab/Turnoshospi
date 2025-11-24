package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.turnoshospi.R
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TurnoshospiTheme {
                SplashLoginScreen()
            }
        }
    }
}

@Composable
fun SplashLoginScreen(modifier: Modifier = Modifier) {
    var showLogin by remember { mutableStateOf(false) }
    var compactLogo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        compactLogo = true
        delay(300)
        showLogin = true
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
        modifier = modifier
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
                LoginCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .padding(horizontal = 8.dp)
                        .graphicsLayer(alpha = loginAlpha)
                )
            }
        }
    }
}

@Composable
private fun LoginCard(modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Iniciar sesión",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
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
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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

            Button(
                onClick = { /* TODO: manejar inicio de sesión */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Iniciar sesión")
            }

            TextButton(
                onClick = { /* TODO: ir a recuperación */ },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = "¿Olvidó su contraseña?")
            }

            TextButton(
                onClick = { /* TODO: ir a registro */ },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xCCFFFFFF))
            ) {
                Text(text = "Crear cuenta")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashLoginPreview() {
    TurnoshospiTheme {
        SplashLoginScreen()
    }
}
