package com.example.turnoshospi.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * NavHost principal de la aplicacion.
 * Define todas las rutas y su navegacion.
 *
 * @param navController Controlador de navegacion
 * @param startDestination Ruta inicial (depende del estado de autenticacion)
 * @param modifier Modificador opcional
 * @param onNavigateToLogin Callback cuando se necesita ir a login
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    // Contenido de cada pantalla - se pasan como lambdas para flexibilidad
    loginContent: @Composable () -> Unit = { /* TODO: LoginScreen */ },
    mainMenuContent: @Composable () -> Unit = { /* TODO: MainMenuScreen */ },
    createPlantContent: @Composable () -> Unit = { /* TODO: CreatePlantScreen */ },
    plantCreatedContent: @Composable () -> Unit = { /* TODO: PlantCreatedScreen */ },
    myPlantContent: @Composable () -> Unit = { /* TODO: MyPlantScreen */ },
    plantDetailContent: @Composable (plantId: String) -> Unit = { /* TODO */ },
    staffManagementContent: @Composable () -> Unit = { /* TODO */ },
    settingsContent: @Composable () -> Unit = { /* TODO */ },
    plantSettingsContent: @Composable () -> Unit = { /* TODO */ },
    importShiftsContent: @Composable () -> Unit = { /* TODO */ },
    groupChatContent: @Composable () -> Unit = { /* TODO */ },
    shiftChangeContent: @Composable (plantId: String, userId: String) -> Unit = { _, _ -> /* TODO */ },
    shiftMarketplaceContent: @Composable () -> Unit = { /* TODO */ },
    statisticsContent: @Composable (plantId: String) -> Unit = { /* TODO */ },
    directChatListContent: @Composable () -> Unit = { /* TODO */ },
    directChatContent: @Composable (chatId: String, otherUserId: String, otherUserName: String) -> Unit = { _, _, _ -> /* TODO */ },
    notificationsContent: @Composable () -> Unit = { /* TODO */ },
    legalInfoContent: @Composable () -> Unit = { /* TODO */ }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- Pantallas sin argumentos ---

        composable(Route.Login.route) {
            loginContent()
        }

        composable(Route.MainMenu.route) {
            mainMenuContent()
        }

        composable(Route.CreatePlant.route) {
            createPlantContent()
        }

        composable(Route.PlantCreated.route) {
            plantCreatedContent()
        }

        composable(Route.MyPlant.route) {
            myPlantContent()
        }

        composable(Route.StaffManagement.route) {
            staffManagementContent()
        }

        composable(Route.Settings.route) {
            settingsContent()
        }

        composable(Route.PlantSettings.route) {
            plantSettingsContent()
        }

        composable(Route.ImportShifts.route) {
            importShiftsContent()
        }

        composable(Route.GroupChat.route) {
            groupChatContent()
        }

        composable(Route.ShiftMarketplace.route) {
            shiftMarketplaceContent()
        }

        composable(Route.DirectChatList.route) {
            directChatListContent()
        }

        composable(
            route = Route.Notifications.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = Route.NOTIFICATIONS_DEEP_LINK }
            )
        ) {
            notificationsContent()
        }

        composable(Route.LegalInfo.route) {
            legalInfoContent()
        }

        // --- Pantallas con argumentos ---

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
            plantDetailContent(plantId)
        }

        composable(
            route = Route.ShiftChange.route,
            arguments = listOf(
                navArgument(Route.ShiftChange.ARG_PLANT_ID) { type = NavType.StringType },
                navArgument(Route.ShiftChange.ARG_CURRENT_USER_ID) { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = Route.ShiftChange.deepLinkPattern() }
            )
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString(Route.ShiftChange.ARG_PLANT_ID) ?: ""
            val userId = backStackEntry.arguments?.getString(Route.ShiftChange.ARG_CURRENT_USER_ID) ?: ""
            shiftChangeContent(plantId, userId)
        }

        composable(
            route = Route.Statistics.route,
            arguments = listOf(
                navArgument(Route.Statistics.ARG_PLANT_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString(Route.Statistics.ARG_PLANT_ID) ?: ""
            statisticsContent(plantId)
        }

        composable(
            route = Route.DirectChat.route,
            arguments = listOf(
                navArgument(Route.DirectChat.ARG_CHAT_ID) { type = NavType.StringType },
                navArgument(Route.DirectChat.ARG_OTHER_USER_ID) { type = NavType.StringType },
                navArgument(Route.DirectChat.ARG_OTHER_USER_NAME) { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = Route.DirectChat.deepLinkPattern() }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString(Route.DirectChat.ARG_CHAT_ID) ?: ""
            val otherUserId = backStackEntry.arguments?.getString(Route.DirectChat.ARG_OTHER_USER_ID) ?: ""
            val otherUserName = backStackEntry.arguments?.getString(Route.DirectChat.ARG_OTHER_USER_NAME) ?: ""
            directChatContent(chatId, otherUserId, otherUserName)
        }
    }
}

/**
 * Extension functions para navegacion mas limpia.
 */
fun NavHostController.navigateToMainMenu() {
    navigate(Route.MainMenu.route) {
        popUpTo(Route.Login.route) { inclusive = true }
    }
}

fun NavHostController.navigateToLogin() {
    navigate(Route.Login.route) {
        popUpTo(0) { inclusive = true }
    }
}

fun NavHostController.navigateToPlantDetail(plantId: String) {
    navigate(Route.PlantDetail.createRoute(plantId))
}

fun NavHostController.navigateToShiftChange(plantId: String, currentUserId: String) {
    navigate(Route.ShiftChange.createRoute(plantId, currentUserId))
}

fun NavHostController.navigateToStatistics(plantId: String) {
    navigate(Route.Statistics.createRoute(plantId))
}

fun NavHostController.navigateToDirectChat(chatId: String, otherUserId: String, otherUserName: String) {
    navigate(Route.DirectChat.createRoute(chatId, otherUserId, otherUserName))
}

fun NavHostController.navigateToNotifications() {
    navigate(Route.Notifications.route)
}

fun NavHostController.navigateToGroupChat() {
    navigate(Route.GroupChat.route)
}

fun NavHostController.navigateToDirectChatList() {
    navigate(Route.DirectChatList.route)
}

fun NavHostController.navigateToSettings() {
    navigate(Route.Settings.route)
}

fun NavHostController.navigateToCreatePlant() {
    navigate(Route.CreatePlant.route)
}

fun NavHostController.navigateToMyPlant() {
    navigate(Route.MyPlant.route)
}

fun NavHostController.navigateToStaffManagement() {
    navigate(Route.StaffManagement.route)
}

fun NavHostController.navigateToPlantSettings() {
    navigate(Route.PlantSettings.route)
}

fun NavHostController.navigateToImportShifts() {
    navigate(Route.ImportShifts.route)
}

fun NavHostController.navigateToShiftMarketplace() {
    navigate(Route.ShiftMarketplace.route)
}

fun NavHostController.navigateToLegalInfo() {
    navigate(Route.LegalInfo.route)
}
