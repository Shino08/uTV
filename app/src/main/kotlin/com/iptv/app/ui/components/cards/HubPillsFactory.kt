package com.iptv.app.ui.components.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.iptv.app.core.model.Category
import com.iptv.app.ui.theme.uTVColors

/**
 * Factory that builds the canonical [CategoryHubPillData] list for the Home screen.
 * Centralises label strings, icons, and gradients so they're never duplicated.
 *
 * @param selectedCategory The currently active category filter (null = "All").
 * @param selectedCountry  The currently active country filter (null = "All").
 * @param onAnime          Callback when the Anime & Geek pill is clicked.
 * @param onMovies         Callback when the Cine & Series pill is clicked.
 * @param onSports         Callback when the Deportes pill is clicked.
 * @param onCountry        Callback when the TV por País pill is clicked.
 * @param onFavorites      Callback when the Favoritos pill is clicked.
 */
data class HubPills(
    val anime: CategoryHubPillData,
    val movies: CategoryHubPillData,
    val sports: CategoryHubPillData,
    val country: CategoryHubPillData,
    val favorites: CategoryHubPillData,
) {
    val all get() = listOf(anime, movies, sports, country, favorites)
}

@Composable
fun rememberHubPills(
    selectedCategory: Category? = null,
    isFavoritesSelected: Boolean = false,
    isCountrySelected: Boolean = false,
): HubPills = HubPills(
    anime = CategoryHubPillData(
        label = "Anime & Geek",
        icon = Icons.Default.SportsEsports,
        gradientColors = listOf(Color(0xFF6B2D0A), uTVColors.AccentAnimeOrange),
        isSelected = selectedCategory == Category.ANIMATION,
    ),
    movies = CategoryHubPillData(
        label = "Cine & Series",
        icon = Icons.Default.Movie,
        gradientColors = listOf(Color(0xFF1A0A4A), Color(0xFF4B2BA0)),
        isSelected = selectedCategory == Category.MOVIES,
    ),
    sports = CategoryHubPillData(
        label = "Deportes",
        icon = Icons.Default.TvOff,   // Replaced with sports icon in next pass
        gradientColors = listOf(Color(0xFF0A2A0A), Color(0xFF1A6B1A)),
        isSelected = selectedCategory == Category.SPORTS,
    ),
    country = CategoryHubPillData(
        label = "TV por País",
        icon = Icons.Default.Public,
        gradientColors = listOf(Color(0xFF0A1A3A), uTVColors.AccentElectricBlue),
        isSelected = isCountrySelected,
    ),
    favorites = CategoryHubPillData(
        label = "Favoritos",
        icon = Icons.Default.Star,
        gradientColors = listOf(Color(0xFF3A2A00), Color(0xFFFFD700)),
        isSelected = isFavoritesSelected,
    ),
)
