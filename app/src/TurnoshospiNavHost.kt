package com.example.turnoshospi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Host de navegación principal.
 * Gestiona el grafo de navegación de la app usando Navigation Compose.
 */
@Composable
fun TurnoshospiNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            // TODO: Insertar LoginScreen aquí
            // LoginScreen(onLoginSuccess = { navController.navigate(Routes.HOME) })
        }

        composable(Routes.HOME) {
            // TODO: Insertar HomeScreen aquí
        }

        composable(Routes.GROUP_CHAT) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: return@composable
            // TODO: Insertar GroupChatScreen aquí, inyectando el ViewModel con plantId
        }

        composable(Routes.SHIFT_CHANGE) {
            // TODO: Insertar ShiftChangeScreen aquí
        }
        
        // Añadir más rutas progresivamente
    }
}
