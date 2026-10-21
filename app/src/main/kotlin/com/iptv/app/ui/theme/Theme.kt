package com.iptv.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// ── Composition locals for TV/Mobile context & Accessibility ─────────────────

val LocalIsTV = staticCompositionLocalOf { false }
val LocalReduceAnimations = staticCompositionLocalOf { false }
val LocalFontSizeScale = staticCompositionLocalOf { 1.0f }

// ── Dark color scheme wired to the design system palette ─────────────────────

private val uTVDarkColorScheme = darkColorScheme(
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

/**
 * Wraps the app in MaterialTheme with the uTV design system.
 * Configured with form factor and user accessibility preferences.
 */
@Composable
fun uTVTheme(
    isTV: Boolean = false,
    reduceAnimations: Boolean = false,
    fontSizeScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalIsTV provides isTV,
        LocalReduceAnimations provides reduceAnimations,
        LocalFontSizeScale provides fontSizeScale,
    ) {
        MaterialTheme(
            colorScheme = uTVDarkColorScheme,
            typography = uTVTypography,
            content = content,
        )
    }
}
