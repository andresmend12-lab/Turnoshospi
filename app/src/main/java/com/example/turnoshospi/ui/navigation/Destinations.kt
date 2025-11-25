package com.example.turnoshospi.ui.navigation

sealed class Destinations(val route: String) {
    data object Login : Destinations("login")
    data object Onboarding : Destinations("onboarding")
    data object NurseDashboard : Destinations("nurse_dashboard")
    data object SupervisorDashboard : Destinations("supervisor_dashboard")
    data object SwapDetail : Destinations("swap_detail/{swapId}") {
        fun create(swapId: String) = "swap_detail/${'$'}swapId"
    }
    data object Feedback : Destinations("feedback")
}
