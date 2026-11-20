package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkOrbitColorScheme = darkColorScheme(
    primary = CyanLaser,
    onPrimary = SpaceVoid,
    primaryContainer = SpaceDeepSurface,
    onPrimaryContainer = CyanLaser,
    secondary = AmberCredits,
    onSecondary = SpaceVoid,
    tertiary = EmeraldHull,
    onTertiary = SpaceVoid,
    error = CrimsonDanger,
    onError = TacticalWhite,
    background = SpaceVoid,
    onBackground = TacticalWhite,
    surface = SpaceDeepSurface,
    onSurface = TacticalWhite,
    surfaceVariant = SpaceHudPanel,
    onSurfaceVariant = TacticalMuted,
    outline = SpaceHudBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkOrbitColorScheme,
        typography = Typography,
        content = content
    )
}
