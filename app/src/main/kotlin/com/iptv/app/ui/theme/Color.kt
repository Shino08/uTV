package com.iptv.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Design System — Dark Mode Exclusive ─────────────────────────────────────
// Source: antigravity/guidelines.md §2

object uTVColors {
    // Backgrounds
    val BackgroundPrimary    = Color(0xFF0B0C10)   // Deep Space Dark
    val BackgroundSecondary  = Color(0xFF121521)   // Rich Navy Dark
    val SurfaceCard          = Color(0xFF1C2030)   // Card surface at rest
    val SurfaceGlassOSD      = Color(0xD90F121C)   // 85% opacity — OSD / Mini-Guide glass

    // Accents
    val AccentAnimeOrange    = Color(0xFFF47521)   // Crunchyroll orange — Anime hub & active border
    val AccentElectricBlue   = Color(0xFF0063E5)   // Disney+/DirecTV blue — OSD indicators

    // Semantic
    val LiveBadgeRed         = Color(0xFFE50914)   // EN VIVO badge
    val FavoriteGold         = Color(0xFFFFD700)   // Favorite star

    // Text
    val TextPrimary          = Color(0xFFF9F9F9)   // Channel names, dial numbers
    val TextSecondary        = Color(0xFFA0A6B8)   // Metadata, country, category

    // Borders
    val BorderSubtle         = Color(0x26FFFFFF)   // 15% white — Glassmorphism 1dp border
}
