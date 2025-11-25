package com.example.turnoshospi.core

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseInitializer {
    fun init(context: Context): FirebaseFirestore {
        val app = FirebaseApp.getApps(context).firstOrNull()
            ?: FirebaseApp.initializeApp(context)
            ?: throw IllegalStateException(
                "FirebaseApp failed to initialize. Ensure google-services.json is present and the Google Services plugin is applied."
            )
        val firestore = FirebaseFirestore.getInstance(app)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
        return firestore
    }
}
