package com.example.turnoshospi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.turnoshospi.domain.model.GlobalRole
import com.example.turnoshospi.ui.navigation.Destinations
import com.example.turnoshospi.ui.screens.FeedbackScreen
import com.example.turnoshospi.ui.screens.LoginScreen
import com.example.turnoshospi.ui.screens.NurseDashboardScreen
import com.example.turnoshospi.ui.screens.OnboardingScreen
import com.example.turnoshospi.ui.screens.SupervisorDashboardScreen
import com.example.turnoshospi.ui.screens.SwapRequestDetailScreen
import com.example.turnoshospi.ui.viewmodel.AuthViewModel
import com.example.turnoshospi.ui.viewmodel.DashboardViewModel
import com.example.turnoshospi.ui.viewmodel.FeedbackViewModel
import com.example.turnoshospi.ui.viewmodel.SwapViewModel

@Composable
fun TurnoshospiApp(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    swapViewModel: SwapViewModel,
    feedbackViewModel: FeedbackViewModel,
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = if (user == null) Destinations.Login.route else Destinations.NurseDashboard.route
        ) {
            composable(Destinations.Login.route) {
                LoginScreen(
                    onLogin = { email, password -> authViewModel.login(email, password) },
                    onGoogleLogin = { token -> authViewModel.loginWithGoogle(token) },
                    onSuccess = {
                        navController.navigate(Destinations.Onboarding.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                        }
                    },
                    errorMessage = authViewModel.errorMessage,
                )
            }
            composable(Destinations.Onboarding.route) {
                OnboardingScreen(
                    profile = user,
                    onCreatePlant = { name, description -> dashboardViewModel.createPlant(name, description) },
                    onJoinPlant = { code -> dashboardViewModel.joinPlant(code) },
                    onDone = {
                        val isSupervisor = user?.globalRole == GlobalRole.SUPERVISOR
                        navController.navigate(if (isSupervisor) Destinations.SupervisorDashboard.route else Destinations.NurseDashboard.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destinations.NurseDashboard.route) {
                NurseDashboardScreen(
                    viewModel = dashboardViewModel,
                    onOpenSwap = { navController.navigate(Destinations.SwapDetail.create(it)) },
                    onFeedback = { navController.navigate(Destinations.Feedback.route) },
                )
            }
            composable(Destinations.SupervisorDashboard.route) {
                SupervisorDashboardScreen(
                    viewModel = dashboardViewModel,
                    onOpenSwap = { navController.navigate(Destinations.SwapDetail.create(it)) },
                    onFeedback = { navController.navigate(Destinations.Feedback.route) }
                )
            }
            composable(Destinations.SwapDetail.route) { entry ->
                val swapId = entry.arguments?.getString("swapId").orEmpty()
                SwapRequestDetailScreen(
                    swapId = swapId,
                    viewModel = swapViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destinations.Feedback.route) {
                FeedbackScreen(
                    viewModel = feedbackViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
