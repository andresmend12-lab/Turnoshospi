package com.example.turnoshospi.data.common

import com.example.turnoshospi.domain.common.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.io.IOException

object FirebaseErrorMapper {
    fun map(throwable: Throwable): AppError {
        return when (throwable) {
            is IOException, is FirebaseNetworkException -> AppError.Network
            is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> AppError.AccessDenied
            else -> AppError.Unknown(throwable.message)
        }
    }
}
