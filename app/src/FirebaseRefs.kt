package com.example.turnoshospi.data.firebase

import com.example.turnoshospi.BuildConfig
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRefs {
    
    fun getDatabase(): FirebaseDatabase {
        // Se asume que BuildConfig.RTDB_URL ha sido configurado en el build.gradle del módulo app
        return FirebaseDatabase.getInstance(BuildConfig.RTDB_URL)
    }

    fun getRootRef(): DatabaseReference = getDatabase().reference

    fun getUsersRef(): DatabaseReference = getRootRef().child("users")
    fun getPlantsRef(): DatabaseReference = getRootRef().child("plants")
    fun getChatsRef(): DatabaseReference = getRootRef().child("chats")
}