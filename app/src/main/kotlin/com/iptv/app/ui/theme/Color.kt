package com.iptv.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── Design System — Dynamic Dark & Light Theme ───────────────────────────────
// Source: antigravity/guidelines.md §2

object uTVColors {
    var isLightMode by mutableStateOf(false)

    // Backgrounds
    val BackgroundPrimary: Color
        get() = if (isLightMode) Color(0xFFF4F6FA) else Color(0xFF0B0C10)

    val BackgroundSecondary: Color
        get() = if (isLightMode) Color(0xFFE6EAF2) else Color(0xFF121521)

    val SurfaceCard: Color
        get() = if (isLightMode) Color(0xFFFFFFFF) else Color(0xFF1C2030)

    val SurfaceGlassOSD: Color
        get() = if (isLightMode) Color(0xE6FFFFFF) else Color(0xD90F121C)

    // Accents
    val AccentAnimeOrange = Color(0xFFF47521)
    val AccentElectricBlue = Color(0xFF0063E5)

    // Semantic
    val LiveBadgeRed = Color(0xFFE50914)
    val FavoriteGold = Color(0xFFFFD700)

    // Text
    val TextPrimary: Color
        get() = if (isLightMode) Color(0xFF11141C) else Color(0xFFF9F9F9)

    val TextSecondary: Color
        get() = if (isLightMode) Color(0xFF525B70) else Color(0xFFA0A6B8)

    // Borders
    val BorderSubtle: Color
        get() = if (isLightMode) Color(0x26000000) else Color(0x26FFFFFF)
}
