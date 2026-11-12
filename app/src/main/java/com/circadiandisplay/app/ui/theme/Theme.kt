package com.circadiandisplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.circadiandisplay.app.data.settings.ThemeMode

/**
 * Material's baseline light scheme. Intentionally unchanged: 2.0.0 introduces a theme system, and
 * repainting light mode would change the appearance of every existing user (DECISIONS.md #010).
 */
private val LightColors = lightColorScheme()

/** Warm dark scheme derived from the carbon/amber palette in `Color.kt`. */
private val DarkColors =
    darkColorScheme(
        primary = AmberLight,
        onPrimary = OnAmberLight,
        primaryContainer = AmberContainerDark,
        onPrimaryContainer = OnAmberContainerDark,
        inversePrimary = AmberContainerDark,
        secondary = SandLight,
        onSecondary = OnSandLight,
        secondaryContainer = SandContainerDark,
        onSecondaryContainer = OnSandContainerDark,
        tertiary = RoseLight,
        onTertiary = OnRoseLight,
        tertiaryContainer = RoseContainerDark,
        onTertiaryContainer = OnRoseContainerDark,
        background = WarmBlack,
        onBackground = WarmOnSurface,
        surface = WarmSurface,
        onSurface = WarmOnSurface,
        surfaceVariant = WarmSurfaceVariant,
        onSurfaceVariant = WarmOnSurfaceVariant,
        surfaceTint = AmberLight,
        surfaceBright = WarmSurfaceBright,
        surfaceDim = WarmSurface,
        surfaceContainerLowest = WarmSurfaceContainerLowest,
        surfaceContainerLow = WarmSurfaceContainerLow,
        surfaceContainer = WarmSurfaceContainer,
        surfaceContainerHigh = WarmSurfaceContainerHigh,
        surfaceContainerHighest = WarmSurfaceContainerHighest,
        inverseSurface = WarmOnSurface,
        inverseOnSurface = WarmSurface,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = WarmOutline,
        outlineVariant = WarmOutlineVariant,
        scrim = Color.Black,
    )

/**
 * Applies the app's colour scheme for [themeMode].
 *
 * [ThemeMode.SYSTEM] is resolved through [isSystemInDarkTheme] on every recomposition, so a
 * device-level dark-mode switch is followed live.
 */
@Composable
fun CircadianDisplayTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (themeMode.isDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

/** Resolves this mode against the device's current dark-mode setting. */
@Composable
fun ThemeMode.isDarkTheme(): Boolean = isDark(isSystemInDarkTheme())

/**
 * Whether the active colour scheme is dark, derived from the scheme rather than from the system
 * setting. Use this inside screens so they also behave correctly under a theme override.
 */
@Composable
fun currentSchemeIsDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
