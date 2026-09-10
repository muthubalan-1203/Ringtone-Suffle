package com.ringtoneshuffler.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = ElectricVioletLight,
    onPrimary            = OnElectricViolet,
    primaryContainer     = ElectricVioletDim,
    onPrimaryContainer   = Color(0xFF2A0088),

    secondary            = SignalEmeraldBright,
    onSecondary          = OnSignalEmerald,
    secondaryContainer   = SignalEmeraldContainer,
    onSecondaryContainer = Color(0xFF00613C),

    tertiary             = CyanFrequencyLight,
    onTertiary           = OnCyanFrequency,
    tertiaryContainer    = CyanFrequency,
    onTertiaryContainer  = Color(0xFF002D40),

    error                = ErrorRed,
    onError              = Color(0xFF690005),
    errorContainer       = ErrorRedDark,
    onErrorContainer     = Color(0xFFFFDAD6),

    background           = SurfaceObsidian,
    onBackground         = OnSurface,
    surface              = SurfaceObsidian,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceContainerHighest,
    onSurfaceVariant     = OnSurfaceVariant,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
    surfaceTint          = ElectricVioletLight,
    inverseSurface       = OnSurface,
    inverseOnSurface     = Color(0xFF2E3036),
    inversePrimary       = ElectricVioletDark,
)

@Composable
fun RingtoneShufflerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = ShufflerTypography,
        content     = content
    )
}
