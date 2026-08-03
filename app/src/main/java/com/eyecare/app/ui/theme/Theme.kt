package com.eyecare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
    // Bound to the same brand hues as StatusCancelled/StatusConfirmed so error and
    // "confirmed-adjacent" states read as this app's colors, not Material's defaults.
    error = StatusCancelled,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    tertiary = StatusConfirmed,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = TertiaryContainerDeepGreen,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainerDark,
    onPrimary = OnPrimary,
    onPrimaryContainer = OnPrimaryContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = CardSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = CardBorderDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDeepGreen,
    onTertiaryContainer = OnTertiaryContainerDark,
)

/**
 * Brand-specific roles Material3's ColorScheme has no slot for (the visit-ticket navy
 * anchor, status colors, and the card hairline border), composed per theme rather than
 * mechanically inverted.
 */
data class EyecareExtendedColors(
    val visitNavy: Color,
    val onVisitNavy: Color,
    val statusPending: Color,
    val statusConfirmed: Color,
    val statusCancelled: Color,
    val statusInfo: Color,
    val cardBorder: Color,
    // Cyan used as text/icon color on a light surface — Primary itself fails WCAG AA in
    // that role (~2.0-2.3:1 against white, its own container, or warm-canvas). Resolves
    // to Primary unchanged in dark mode, where it already clears AA.
    val accentText: Color,
)

private val LightExtendedColors = EyecareExtendedColors(
    visitNavy = NavyBlue,
    onVisitNavy = Color.White,
    statusPending = StatusPending,
    statusConfirmed = StatusConfirmed,
    statusCancelled = StatusCancelled,
    // Every current usage of statusInfo is pill/status text on a light tint, so it shares
    // accentText's accessible value rather than the unreadable raw brand cyan.
    statusInfo = AccentTextLight,
    cardBorder = CardBorder,
    accentText = AccentTextLight,
)

private val DarkExtendedColors = EyecareExtendedColors(
    visitNavy = NavyBlueDark,
    onVisitNavy = Color.White,
    // Pending amber already clears WCAG AA against a near-black surface unchanged.
    statusPending = StatusPending,
    statusConfirmed = TertiaryDark,
    statusCancelled = ErrorDark,
    statusInfo = Primary,
    cardBorder = CardBorderDark,
    accentText = Primary,
)

private val LocalEyecareExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Mirrors `MaterialTheme.colorScheme` ergonomics for the app's brand-specific roles. */
object EyecareColors {
    val current: EyecareExtendedColors
        @Composable
        get() = LocalEyecareExtendedColors.current
}

@Composable
fun EyecareTheme(content: @Composable () -> Unit) {
    // Pinned to light mode for now — dark theme is fully implemented below (see
    // DarkColorScheme / DarkExtendedColors) but intentionally not wired to the system
    // setting yet. Restore `isSystemInDarkTheme()` here to re-enable it.
    val darkTheme = false
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalEyecareExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EyecareTypography,
            shapes = EyecareShapes,
            content = content,
        )
    }
}
