package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.example.turnoshospi.core.FirebaseInitializer
import com.example.turnoshospi.data.firebase.FirebaseAuthRepository
import com.example.turnoshospi.data.firebase.FirestoreFeedbackRepository
import com.example.turnoshospi.data.firebase.FirestorePlantRepository
import com.example.turnoshospi.data.firebase.FirestoreShiftRepository
import com.example.turnoshospi.data.firebase.FirestoreSwapRequestRepository
import com.example.turnoshospi.ui.TurnoshospiApp
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.example.turnoshospi.ui.viewmodel.AuthViewModel
import com.example.turnoshospi.ui.viewmodel.DashboardViewModel
import com.example.turnoshospi.ui.viewmodel.FeedbackViewModel
import com.example.turnoshospi.ui.viewmodel.SwapViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val firestore: FirebaseFirestore = FirebaseInitializer.init(this)
        val auth = FirebaseAuth.getInstance()

        val authRepository = FirebaseAuthRepository(auth, firestore)
        val plantRepository = FirestorePlantRepository(firestore)
        val shiftRepository = FirestoreShiftRepository(firestore)
        val swapRepository = FirestoreSwapRequestRepository(firestore)
        val feedbackRepository = FirestoreFeedbackRepository(firestore)

        val authViewModel = AuthViewModel(authRepository)
        val dashboardViewModel = DashboardViewModel(plantRepository, shiftRepository, swapRepository)
        val swapViewModel = SwapViewModel(swapRepository)
        val feedbackViewModel = FeedbackViewModel(feedbackRepository)

        setContent {
            TurnoshospiTheme {
                TurnoshospiApp(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    swapViewModel = swapViewModel,
                    feedbackViewModel = feedbackViewModel,
                )
            }
        }
    }
}
