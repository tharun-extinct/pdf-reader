package com.pdfreader.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Spacing Tokens ────────────────────────────────────────────────────

@Immutable
data class NoxReaderSpacing(
    val base: Dp = 8.dp,
    val gutter: Dp = 24.dp,
    val marginMobile: Dp = 20.dp,
    val marginDesktop: Dp = 64.dp,
    val readingMaxWidth: Dp = 720.dp
)

val LocalNoxReaderSpacing = staticCompositionLocalOf { NoxReaderSpacing() }

// ── Light Color Scheme ────────────────────────────────────────────────

private val NoxReaderLightColorScheme = lightColorScheme(
    primary = NoxReaderPrimary,
    onPrimary = NoxReaderOnPrimary,
    primaryContainer = NoxReaderPrimaryContainer,
    onPrimaryContainer = NoxReaderOnPrimaryContainer,
    inversePrimary = NoxReaderInversePrimary,
    secondary = NoxReaderSecondary,
    onSecondary = NoxReaderOnSecondary,
    secondaryContainer = NoxReaderSecondaryContainer,
    onSecondaryContainer = NoxReaderOnSecondaryContainer,
    tertiary = NoxReaderTertiary,
    onTertiary = NoxReaderOnTertiary,
    tertiaryContainer = NoxReaderTertiaryContainer,
    onTertiaryContainer = NoxReaderOnTertiaryContainer,
    error = NoxReaderError,
    onError = NoxReaderOnError,
    errorContainer = NoxReaderErrorContainer,
    onErrorContainer = NoxReaderOnErrorContainer,
    background = NoxReaderBackground,
    onBackground = NoxReaderOnBackground,
    surface = NoxReaderSurface,
    onSurface = NoxReaderOnSurface,
    surfaceVariant = NoxReaderSurfaceVariant,
    onSurfaceVariant = NoxReaderOnSurfaceVariant,
    surfaceTint = NoxReaderSurfaceTint,
    inverseSurface = NoxReaderInverseSurface,
    inverseOnSurface = NoxReaderInverseOnSurface,
    outline = NoxReaderOutline,
    outlineVariant = NoxReaderOutlineVariant,
    surfaceBright = NoxReaderSurfaceBright,
    surfaceDim = NoxReaderSurfaceDim,
    surfaceContainerLowest = NoxReaderSurfaceContainerLowest,
    surfaceContainerLow = NoxReaderSurfaceContainerLow,
    surfaceContainer = NoxReaderSurfaceContainer,
    surfaceContainerHigh = NoxReaderSurfaceContainerHigh,
    surfaceContainerHighest = NoxReaderSurfaceContainerHighest,
)

private val NoxReaderDarkColorScheme = darkColorScheme(
    primary = NoxReaderDarkPrimary,
    onPrimary = NoxReaderDarkOnPrimary,
    primaryContainer = NoxReaderDarkPrimaryContainer,
    onPrimaryContainer = NoxReaderDarkOnPrimaryContainer,
    inversePrimary = NoxReaderPrimary,
    secondary = NoxReaderSecondaryFixedDim,
    onSecondary = NoxReaderOnSecondaryFixed,
    secondaryContainer = NoxReaderOnSecondaryFixedVariant,
    onSecondaryContainer = NoxReaderSecondaryFixed,
    tertiary = NoxReaderTertiaryFixedDim,
    onTertiary = NoxReaderOnTertiaryFixed,
    tertiaryContainer = NoxReaderOnTertiaryFixedVariant,
    onTertiaryContainer = NoxReaderTertiaryFixed,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = NoxReaderDarkBackground,
    onBackground = NoxReaderDarkOnBackground,
    surface = NoxReaderDarkSurface,
    onSurface = NoxReaderDarkOnSurface,
    surfaceVariant = NoxReaderDarkSurfaceVariant,
    onSurfaceVariant = NoxReaderDarkOnSurfaceVariant,
    surfaceTint = NoxReaderDarkPrimary,
    inverseSurface = NoxReaderInverseOnSurface,
    inverseOnSurface = NoxReaderInverseSurface,
    outline = NoxReaderDarkOutline,
    outlineVariant = NoxReaderDarkOutlineVariant,
    surfaceBright = NoxReaderDarkSurfaceContainerHighest,
    surfaceDim = NoxReaderDarkSurfaceContainerLowest,
    surfaceContainerLowest = NoxReaderDarkSurfaceContainerLowest,
    surfaceContainerLow = NoxReaderDarkSurfaceContainerLow,
    surfaceContainer = NoxReaderDarkSurfaceContainer,
    surfaceContainerHigh = NoxReaderDarkSurfaceContainerHigh,
    surfaceContainerHighest = NoxReaderDarkSurfaceContainerHighest,
)

// ── Theme Composable ──────────────────────────────────────────────────

@Composable
fun NoxReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNoxReaderSpacing provides NoxReaderSpacing()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) NoxReaderDarkColorScheme else NoxReaderLightColorScheme,
            typography = NoxReaderTypography,
            content = content
        )
    }
}

/**
 * Convenience accessor for NoxReader spacing tokens.
 * Usage: `NoxReaderTheme.spacing.gutter`
 */
object NoxReaderTheme {
    val spacing: NoxReaderSpacing
        @Composable
        get() = LocalNoxReaderSpacing.current
}
