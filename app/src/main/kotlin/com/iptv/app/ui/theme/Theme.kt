package com.iptv.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// ── Composition local for TV/Mobile context ──────────────────────────────────

val LocalIsTV = staticCompositionLocalOf { false }

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
 * The [isTV] flag adjusts typography scaling and is accessible
 * anywhere in the tree via [LocalIsTV].
 */
@Composable
fun uTVTheme(
    isTV: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIsTV provides isTV) {
        MaterialTheme(
            colorScheme = uTVDarkColorScheme,
            typography = uTVTypography,
            content = content,
        )
    }
}
