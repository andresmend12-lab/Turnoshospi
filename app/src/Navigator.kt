package com.example.turnoshospi.navigation

interface Navigator {
    fun navigateTo(route: String)
    fun back()
}