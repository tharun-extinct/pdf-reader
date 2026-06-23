package com.pdfreader.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Spacing Tokens ────────────────────────────────────────────────────

@Immutable
data class LibroSpacing(
    val base: Dp = 8.dp,
    val gutter: Dp = 24.dp,
    val marginMobile: Dp = 20.dp,
    val marginDesktop: Dp = 64.dp,
    val readingMaxWidth: Dp = 720.dp
)

val LocalLibroSpacing = staticCompositionLocalOf { LibroSpacing() }

// ── Light Color Scheme ────────────────────────────────────────────────

private val LibroLightColorScheme = lightColorScheme(
    primary = LibroPrimary,
    onPrimary = LibroOnPrimary,
    primaryContainer = LibroPrimaryContainer,
    onPrimaryContainer = LibroOnPrimaryContainer,
    inversePrimary = LibroInversePrimary,
    secondary = LibroSecondary,
    onSecondary = LibroOnSecondary,
    secondaryContainer = LibroSecondaryContainer,
    onSecondaryContainer = LibroOnSecondaryContainer,
    tertiary = LibroTertiary,
    onTertiary = LibroOnTertiary,
    tertiaryContainer = LibroTertiaryContainer,
    onTertiaryContainer = LibroOnTertiaryContainer,
    error = LibroError,
    onError = LibroOnError,
    errorContainer = LibroErrorContainer,
    onErrorContainer = LibroOnErrorContainer,
    background = LibroBackground,
    onBackground = LibroOnBackground,
    surface = LibroSurface,
    onSurface = LibroOnSurface,
    surfaceVariant = LibroSurfaceVariant,
    onSurfaceVariant = LibroOnSurfaceVariant,
    surfaceTint = LibroSurfaceTint,
    inverseSurface = LibroInverseSurface,
    inverseOnSurface = LibroInverseOnSurface,
    outline = LibroOutline,
    outlineVariant = LibroOutlineVariant,
    surfaceBright = LibroSurfaceBright,
    surfaceDim = LibroSurfaceDim,
    surfaceContainerLowest = LibroSurfaceContainerLowest,
    surfaceContainerLow = LibroSurfaceContainerLow,
    surfaceContainer = LibroSurfaceContainer,
    surfaceContainerHigh = LibroSurfaceContainerHigh,
    surfaceContainerHighest = LibroSurfaceContainerHighest,
)

// ── Theme Composable ──────────────────────────────────────────────────

@Composable
fun LibroTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLibroSpacing provides LibroSpacing()
    ) {
        MaterialTheme(
            colorScheme = LibroLightColorScheme,
            typography = LibroTypography,
            content = content
        )
    }
}

/**
 * Convenience accessor for Libro spacing tokens.
 * Usage: `LibroTheme.spacing.gutter`
 */
object LibroTheme {
    val spacing: LibroSpacing
        @Composable
        get() = LocalLibroSpacing.current
}
