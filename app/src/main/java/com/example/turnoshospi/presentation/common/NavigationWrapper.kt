package com.example.turnoshospi.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.turnoshospi.presentation.auth.AuthState
import com.example.turnoshospi.presentation.auth.AuthViewModel
import com.example.turnoshospi.presentation.auth.AuthenticationState
import com.example.turnoshospi.presentation.navigation.Route
import com.example.turnoshospi.presentation.navigation.navigateToLogin
import com.example.turnoshospi.presentation.navigation.navigateToMainMenu
import com.example.turnoshospi.presentation.plant.PlantViewModel

/**
 * Wrapper que maneja la autenticacion y redirige segun el estado.
 * Usa este patron para pantallas que requieren autenticacion.
 */
@Composable
fun AuthenticatedWrapper(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    content: @Composable (userId: String) -> Unit
) {
    val authState by authViewModel.authenticationState.collectAsState()

    when (authState) {
        is AuthenticationState.Authenticated -> {
            val userId = (authState as AuthenticationState.Authenticated).user.uid
            content(userId)
        }
        is AuthenticationState.Unauthenticated -> {
            LaunchedEffect(Unit) {
                navController.navigateToLogin()
            }
        }
        AuthenticationState.Unknown -> {
            // Mostrar loading mientras se determina el estado
            LoadingScreen()
        }
    }
}

/**
 * Pantalla de carga simple.
 */
@Composable
fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

private fun androidx.compose.ui.Modifier.fillMaxSize() = this.then(
    androidx.compose.foundation.layout.fillMaxSize()
)
