package com.example.turnoshospi.domain.common

sealed class AppError {
    object Network : AppError()
    object NotFound : AppError()
    object AccessDenied : AppError()
    object ServiceUnavailable : AppError()
    data class Validation(val reason: String) : AppError()
    data class Unknown(val message: String? = null) : AppError()
}
