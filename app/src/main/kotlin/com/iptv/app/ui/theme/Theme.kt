package com.iptv.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.iptv.app.core.model.ThemePreference

val LocalIsTV = staticCompositionLocalOf { false }
val LocalReduceAnimations = staticCompositionLocalOf { false }
val LocalFontSizeScale = staticCompositionLocalOf { 1.0f }

@Composable
fun uTVTheme(
    isTV: Boolean = false,
    themePreference: ThemePreference = ThemePreference.DARK,
    reduceAnimations: Boolean = false,
    fontSizeScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useLightTheme = when (themePreference) {
        ThemePreference.LIGHT -> true
        ThemePreference.DARK -> false
        ThemePreference.SYSTEM -> !systemDark
    }

    // Actualiza la paleta global reactiva antes de componer
    uTVColors.isLightMode = useLightTheme
    SideEffect {
        uTVColors.isLightMode = useLightTheme
    }

    val colorScheme = if (useLightTheme) {
        lightColorScheme(
            primary          = uTVColors.AccentElectricBlue,
            onPrimary        = uTVColors.TextPrimary,
            secondary        = uTVColors.AccentAnimeOrange,
            onSecondary      = uTVColors.TextPrimary,
            error            = uTVColors.LiveBadgeRed,
            background       = uTVColors.BackgroundPrimary,
            onBackground     = uTVColors.TextPrimary,
            surface          = uTVColors.SurfaceCard,
            onSurface        = uTVColors.TextPrimary,
            surfaceVariant   = uTVColors.BackgroundSecondary,
            onSurfaceVariant = uTVColors.TextSecondary,
            outline          = uTVColors.BorderSubtle,
        )
    } else {
        darkColorScheme(
            primary          = uTVColors.AccentElectricBlue,
            onPrimary        = uTVColors.TextPrimary,
            secondary        = uTVColors.AccentAnimeOrange,
            onSecondary      = uTVColors.TextPrimary,
            error            = uTVColors.LiveBadgeRed,
            background       = uTVColors.BackgroundPrimary,
            onBackground     = uTVColors.TextPrimary,
            surface          = uTVColors.SurfaceCard,
            onSurface        = uTVColors.TextPrimary,
            surfaceVariant   = uTVColors.BackgroundSecondary,
            onSurfaceVariant = uTVColors.TextSecondary,
            outline          = uTVColors.BorderSubtle,
        )
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontSizeScale,
    )

    CompositionLocalProvider(
        LocalIsTV provides isTV,
        LocalReduceAnimations provides reduceAnimations,
        LocalFontSizeScale provides fontSizeScale,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = uTVTypography,
            content = content,
        )
    }
}
