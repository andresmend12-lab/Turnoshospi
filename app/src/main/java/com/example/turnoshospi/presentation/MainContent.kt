package com.example.turnoshospi.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.turnoshospi.presentation.auth.AuthState
import com.example.turnoshospi.presentation.auth.AuthViewModel
import com.example.turnoshospi.presentation.auth.AuthenticationState
import com.example.turnoshospi.presentation.common.LoadingScreen
import com.example.turnoshospi.presentation.navigation.Route
import com.example.turnoshospi.presentation.navigation.navigateToLogin
import com.example.turnoshospi.presentation.navigation.navigateToMainMenu
import com.example.turnoshospi.presentation.navigation.navigateToMyPlant
import com.example.turnoshospi.presentation.navigation.navigateToPlantDetail
import com.example.turnoshospi.presentation.plant.PlantViewModel

/**
 * Contenido principal de la app con Navigation Compose.
 * Reemplaza a TurnoshospiApp gradualmente.
 *
 * PATRON DE MIGRACION:
 * 1. Cada pantalla recibe NavController para navegar
 * 2. Usa hiltViewModel() para obtener ViewModels
 * 3. Los callbacks de navegacion usan extension functions de NavController
 */
@Composable
fun MainContent(
    pendingNavigation: Map<String, String>? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authenticationState.collectAsState()

    // Manejar navegacion desde notificaciones (deep linking manual)
    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { navData ->
            handlePendingNavigation(navController, navData)
            onNavigationHandled()
        }
    }

    // Determinar pantalla inicial segun autenticacion
    val startDestination = when (authState) {
        is AuthenticationState.Authenticated -> Route.MainMenu.route
        is AuthenticationState.Unauthenticated -> Route.Login.route
        AuthenticationState.Unknown -> Route.Login.route // Se redirige despues
    }

    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- LOGIN ---
            composable(Route.Login.route) {
                LoginScreenWrapper(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            // --- MAIN MENU ---
            composable(Route.MainMenu.route) {
                MainMenuScreenWrapper(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            // --- MY PLANT ---
            composable(Route.MyPlant.route) {
                MyPlantScreenWrapper(
                    navController = navController
                )
            }

            // --- PLANT DETAIL (con argumento) ---
            composable(
                route = Route.PlantDetail.route,
                arguments = listOf(
                    navArgument(Route.PlantDetail.ARG_PLANT_ID) { type = NavType.StringType }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = Route.PlantDetail.deepLinkPattern() }
                )
            ) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getString(Route.PlantDetail.ARG_PLANT_ID) ?: ""
                PlantDetailScreenWrapper(
                    plantId = plantId,
                    navController = navController
                )
            }

            // ... Agregar resto de pantallas siguiendo el mismo patron
        }
    }
}

/**
 * Maneja navegacion pendiente desde notificaciones.
 */
private fun handlePendingNavigation(
    navController: NavHostController,
    navData: Map<String, String>
) {
    val screen = navData["screen"]
    val plantId = navData["plantId"]

    when (screen) {
        "PlantDetail", "MyPlant" -> {
            if (!plantId.isNullOrBlank()) {
                navController.navigateToPlantDetail(plantId)
            } else {
                navController.navigateToMyPlant()
            }
        }
        "Notifications" -> {
            navController.navigate(Route.Notifications.route)
        }
        // Agregar mas casos segun necesidad
    }
}

// =============================================================================
// SCREEN WRAPPERS - Cada uno muestra el patron de migracion
// =============================================================================

/**
 * Wrapper para LoginScreen.
 * PATRON: ViewModel + callbacks de navegacion.
 */
@Composable
private fun LoginScreenWrapper(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val loginState by authViewModel.loginState.collectAsState()
    val authState by authViewModel.authenticationState.collectAsState()

    // Navegar a MainMenu cuando el login es exitoso
    LaunchedEffect(authState) {
        if (authState is AuthenticationState.Authenticated) {
            navController.navigateToMainMenu()
        }
    }

    // TODO: Reemplazar con tu LoginScreen existente
    // Ejemplo de como conectarlo:
    /*
    LoginScreen(
        loginState = loginState,
        onLogin = { email, password ->
            authViewModel.login(email, password)
        },
        onCreateAccount = { /* navegar a registro */ },
        onForgotPassword = { email ->
            authViewModel.resetPassword(email)
        },
        onClearError = { authViewModel.clearLoginState() }
    )
    */

    // Placeholder temporal
    androidx.compose.material3.Text("Login Screen - Implementar")
}

/**
 * Wrapper para MainMenuScreen.
 * PATRON: Multiples ViewModels + navegacion.
 */
@Composable
private fun MainMenuScreenWrapper(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val plantViewModel: PlantViewModel = hiltViewModel()
    val authState by authViewModel.authenticationState.collectAsState()
    val plantState by plantViewModel.screenState.collectAsState()

    // Cargar planta del usuario al entrar
    LaunchedEffect(authState) {
        if (authState is AuthenticationState.Authenticated) {
            val userId = (authState as AuthenticationState.Authenticated).user.uid
            plantViewModel.loadUserPlant(userId)
        }
    }

    // TODO: Conectar con tu MainMenuScreen existente
    // Ejemplo:
    /*
    MainMenuScreen(
        userPlant = plantState.plant,
        isLoading = plantState.isLoading,
        onOpenPlant = {
            plantState.plant?.let { plant ->
                navController.navigateToPlantDetail(plant.id)
            } ?: navController.navigateToMyPlant()
        },
        onCreatePlant = { navController.navigate(Route.CreatePlant.route) },
        onOpenSettings = { navController.navigate(Route.Settings.route) },
        onSignOut = { authViewModel.logout() }
    )
    */

    // Placeholder temporal
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text("Main Menu - Implementar")
        androidx.compose.material3.Button(onClick = { navController.navigateToMyPlant() }) {
            androidx.compose.material3.Text("Ir a Mi Planta")
        }
        androidx.compose.material3.Button(onClick = { authViewModel.logout() }) {
            androidx.compose.material3.Text("Cerrar Sesion")
        }
    }
}

/**
 * Wrapper para MyPlantScreen.
 * PATRON: ViewModel con carga de datos.
 */
@Composable
private fun MyPlantScreenWrapper(
    navController: NavHostController
) {
    val plantViewModel: PlantViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val plantState by plantViewModel.screenState.collectAsState()
    val authState by authViewModel.authenticationState.collectAsState()

    // Cargar datos
    LaunchedEffect(authState) {
        if (authState is AuthenticationState.Authenticated) {
            val userId = (authState as AuthenticationState.Authenticated).user.uid
            plantViewModel.loadUserPlant(userId)
        }
    }

    // TODO: Conectar con tu MyPlantScreen existente
    /*
    MyPlantScreen(
        plant = plantState.plant,
        membership = plantState.membership,
        isLoading = plantState.isLoading,
        onBack = { navController.popBackStack() },
        onOpenPlantDetail = { plant ->
            navController.navigateToPlantDetail(plant.id)
        },
        onJoinPlant = { plantId, code, onResult ->
            // Usar plantViewModel.joinPlant(...)
        }
    )
    */

    // Placeholder temporal
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text("My Plant Screen")
        if (plantState.isLoading) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        plantState.plant?.let { plant ->
            androidx.compose.material3.Text("Planta: ${plant.name}")
            androidx.compose.material3.Button(
                onClick = { navController.navigateToPlantDetail(plant.id) }
            ) {
                androidx.compose.material3.Text("Ver Detalles")
            }
        }
        androidx.compose.material3.Button(onClick = { navController.popBackStack() }) {
            androidx.compose.material3.Text("Volver")
        }
    }
}

/**
 * Wrapper para PlantDetailScreen.
 * PATRON: Recibe argumento de navegacion.
 */
@Composable
private fun PlantDetailScreenWrapper(
    plantId: String,
    navController: NavHostController
) {
    val plantViewModel: PlantViewModel = hiltViewModel()
    val plantState by plantViewModel.screenState.collectAsState()

    // Observar planta en tiempo real
    LaunchedEffect(plantId) {
        if (plantId.isNotBlank()) {
            plantViewModel.observePlant(plantId)
        }
    }

    // TODO: Conectar con tu PlantDetailScreen existente
    /*
    PlantDetailScreen(
        plant = plantState.plant,
        onBack = { navController.popBackStack() },
        onOpenStaffManagement = { navController.navigate(Route.StaffManagement.route) },
        onOpenSettings = { navController.navigate(Route.PlantSettings.route) },
        // ... resto de callbacks
    )
    */

    // Placeholder temporal
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text("Plant Detail: $plantId")
        plantState.plant?.let { plant ->
            androidx.compose.material3.Text("Nombre: ${plant.name}")
            androidx.compose.material3.Text("Hospital: ${plant.hospitalName}")
        }
        androidx.compose.material3.Button(onClick = { navController.popBackStack() }) {
            androidx.compose.material3.Text("Volver")
        }
    }
}
