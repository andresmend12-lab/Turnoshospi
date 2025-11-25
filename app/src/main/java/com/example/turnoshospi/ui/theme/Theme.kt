package com.example.turnoshospi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = HospitalBlue,
    secondary = HospitalGreen,
    tertiary = AccentAmber,
    background = SurfaceLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = OnSurface,
    onBackground = OnSurface,
    onSurface = OnSurface,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = HospitalBlue,
    secondary = HospitalGreen,
    tertiary = AccentAmber,
)

@Composable
fun TurnoshospiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
