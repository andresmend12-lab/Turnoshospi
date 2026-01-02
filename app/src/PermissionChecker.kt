package com.example.turnoshospi.domain.logic

import com.example.turnoshospi.domain.model.UserRole

object PermissionChecker {
    
    fun canCreatePlant(role: UserRole?): Boolean {
        // Ejemplo: Solo ADMIN o MANAGER pueden crear plantas (o todos si es política abierta)
        return true 
    }

    fun canManageStaff(role: UserRole?): Boolean {
        return role == UserRole.ADMIN || role == UserRole.MANAGER
    }

    fun canDeletePlant(role: UserRole?): Boolean {
        return role == UserRole.ADMIN
    }
}