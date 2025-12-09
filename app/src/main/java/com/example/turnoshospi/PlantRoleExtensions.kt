package com.example.turnoshospi

// Funciones de extensión para manejar roles y nombres
fun String.normalizedRole() = trim().lowercase()

fun RegisteredUser.displayName(def: String) = name.ifBlank { email.ifBlank { role.ifBlank { def } } }

// Nota: Las comprobaciones de rol deben ser insensibles a mayúsculas y minúsculas
fun RegisteredUser.isNurseRole(roles: List<String>) = role.normalizedRole() in roles || role.normalizedRole().contains("enfermer")
fun RegisteredUser.isAuxRole(roles: List<String>) = role.normalizedRole() in roles || role.normalizedRole().contains("auxiliar")