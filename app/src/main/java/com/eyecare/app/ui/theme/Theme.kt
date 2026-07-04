package com.eyecare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimary = OnPrimary,
    // Warm off-white background so the whole app has subtle depth
    background = Background,
    onBackground = OnSurface,
    // Cards use pure white so they visually float above the warm background
    surface = CardSurface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    // outline is used as chip borders, dividers, and card strokes
    outline = Outline,
    // outlineVariant is available for the subtle card-border token
    outlineVariant = CardBorder,
)

@Composable
fun EyecareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = EyecareTypography,
        shapes = EyecareShapes,
        content = content,
    )
}
