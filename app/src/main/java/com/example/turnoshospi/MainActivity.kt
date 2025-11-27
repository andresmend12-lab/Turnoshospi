package com.example.turnoshospi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.turnoshospi.ui.theme.TurnoshospiTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
        try {
            storage = FirebaseStorage.getInstance("gs://turnoshospi-f4870.firebasestorage.app")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            TurnoshospiTheme {
                val currentUser = auth.currentUser
                var errorMessage by remember { mutableStateOf<String?>(null) }

                TurnoshospiApp(
                    user = currentUser,
                    errorMessage = errorMessage,
                    onErrorDismiss = { errorMessage = null },
                    onLogin = { e, p, cb ->
                        auth.signInWithEmailAndPassword(e, p).addOnCompleteListener {
                            cb(it.isSuccessful)
                        }
                    },
                    onCreateAccount = { p, pw, cb ->
                        auth.createUserWithEmailAndPassword(p.email, pw).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = task.result!!.user!!.uid
                                val newProfile = p.copy(id = uid) // Aseguramos que el perfil tenga el ID
                                database.reference.child("users").child(uid).setValue(newProfile)
                                    .addOnCompleteListener { dbTask -> cb(dbTask.isSuccessful) }
                            } else {
                                cb(false)
                            }
                        }
                    },
                    onForgotPassword = { e, cb ->
                        auth.sendPasswordResetEmail(e).addOnCompleteListener { cb(it.isSuccessful) }
                    },
                    onLoadProfile = { cb ->
                        if (auth.uid != null) {
                            database.reference.child("users").child(auth.uid!!).get()
                                .addOnSuccessListener { cb(it.getValue(UserProfile::class.java)) }
                                .addOnFailureListener { cb(null) }
                        } else {
                            cb(null)
                        }
                    },
                    onSaveProfile = { p, cb ->
                        if (auth.uid != null) {
                            val updatedProfile = p.copy(id = auth.uid!!)
                            database.reference.child("users").child(auth.uid!!).setValue(updatedProfile)
                                .addOnCompleteListener { cb(it.isSuccessful) }
                        } else {
                            cb(false)
                        }
                    },
                    onLoadPlant = { cb ->
                        database.reference.child("plants").limitToFirst(1).get()
                            .addOnSuccessListener {
                                cb(it.children.firstOrNull()?.getValue(Plant::class.java), null)
                            }
                            .addOnFailureListener { cb(null, it.message) }
                    },
                    onJoinPlant = { _, _, _, cb -> cb(true, null) },
                    onLoadPlantMembership = { _, _, cb -> cb(null) },
                    onLinkUserToStaff = { _, _, cb -> cb(true) },
                    onRegisterPlantStaff = { pid, s, cb ->
                        database.reference.child("plants/$pid/personal_de_planta/${s.id}").setValue(s)
                            .addOnSuccessListener { cb(true) }
                            .addOnFailureListener { cb(false) }
                    },
                    onEditPlantStaff = { pid, s, cb ->
                        database.reference.child("plants/$pid/personal_de_planta/${s.id}").setValue(s)
                            .addOnSuccessListener { cb(true) }
                            .addOnFailureListener { cb(false) }
                    },
                    onListenToShifts = { _, _, _ -> },
                    onFetchColleagues = { _, _, _, cb -> cb(emptyList()) },
                    onSignOut = { auth.signOut() },
                    onDeleteAccount = { auth.currentUser?.delete() },
                    onDeletePlant = { pid ->
                        database.reference.child("plants").child(pid).removeValue()
                    }
                )
            }
        }
    }
}