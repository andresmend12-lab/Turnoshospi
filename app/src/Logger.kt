package com.example.turnoshospi.core.logging

import android.util.Log
import com.example.turnoshospi.BuildConfig

object Logger {
    private const val TAG_PREFIX = "TurnosApp:"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX$tag", message, throwable)
        // Aquí se podría conectar Crashlytics: FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}
