package com.iptv.app.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.components.cards.CategoryHubPill
import com.iptv.app.ui.components.cards.ChannelCard
import com.iptv.app.ui.components.cards.HeroFeaturedBanner
import com.iptv.app.ui.components.cards.rememberHubPills
import com.iptv.app.ui.mobile.countryDisplayName
import com.iptv.app.ui.shared.HomeViewModel
import com.iptv.app.ui.theme.uTVColors

/**
 * Android TV / Google TV Home Screen — implements guidelines.md §1, §3, §4, §5:
 * - 100% D-Pad remote navigable: focusable cards, animated scale 1.08f + illuminated glow border.
 * - Dynamic Hero Banner: updates in real time to feature the channel currently focused by the D-Pad cursor.
 * - Category Hub Pills: D-Pad reachable horizontal filter pills (Anime, Cine, Deportes, TV por País, Favoritos).
 * - Multi-category horizontal carousels with 48dp TV overscan safe padding.
 * - Fast TV Country selector row for Latin America / Spain.
 */
@Composable
fun TvHomeScreen(
    onChannelSelected: (Channel) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()

    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var showCountrySelector by remember { mutableStateOf(false) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    // Fallback featured channel when no card has explicit focus
    val defaultFeatured = remember(allChannels, recentlyWatched) {
        recentlyWatched.firstOrNull()
            ?: allChannels.firstOrNull { it.category == Category.ANIMATION }
            ?: allChannels.firstOrNull { it.category == Category.MOVIES }
            ?: allChannels.firstOrNull()
    }

    val activeHeroChannel = focusedChannel ?: defaultFeatured

    val handleChannelSelect: (Channel) -> Unit = { channel ->
        viewModel.onChannelSelected(channel)
        onChannelSelected(channel)
    }

    val hubPills = rememberHubPills(
        selectedCategory = uiState.activeCategory,
        isFavoritesSelected = showOnlyFavorites,
        isCountrySelected = uiState.activeCountry != null,
    )

    // Filtered channels if a category, country, or favorites filter is active
    val filteredChannels = remember(allChannels, favorites, uiState.activeCategory, uiState.activeCountry, showOnlyFavorites) {
        when {
            showOnlyFavorites              -> favorites
            uiState.activeCategory != null -> allChannels.filter { it.category == uiState.activeCategory }
            uiState.activeCountry != null  -> allChannels.filter { it.countryCode.equals(uiState.activeCountry, ignoreCase = true) }
            else                           -> null
        }
    }

    // Pre-filter category carousels once per channel sync to avoid UI lag during D-Pad navigation
    val animeChannels = remember(allChannels) { allChannels.filter { it.category == Category.ANIMATION } }
    val moviesChannels = remember(allChannels) { allChannels.filter { it.category == Category.MOVIES } }
    val sportsChannels = remember(allChannels) { allChannels.filter { it.category == Category.SPORTS } }
    val generalChannels = remember(allChannels) { allChannels.filter { it.category == Category.GENERAL } }
    val musicChannels = remember(allChannels) { allChannels.filter { it.category == Category.MUSIC } }
    val newsChannels = remember(allChannels) { allChannels.filter { it.category == Category.NEWS } }
    val kidsChannels = remember(allChannels) { allChannels.filter { it.category == Category.KIDS } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(uTVColors.BackgroundPrimary),
    ) {
        when {
            // Loading state
            uiState.isLoading && allChannels.isEmpty() -> {
                TvLoadingView()
            }

            // Sync error state
            uiState.syncError != null && allChannels.isEmpty() -> {
                TvErrorView(
                    message = uiState.syncError ?: "Error al sincronizar canales de TV.",
                    onRetry = { viewModel.retrySync() },
                )
            }

            // Normal catalog exploration
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    // ── 1. Top Cinematic Hero Banner ─────────────────────────
                    item(key = "tv_hero_banner") {
                        HeroFeaturedBanner(
                            channel = activeHeroChannel,
                            onPlayClick = { activeHeroChannel?.let(handleChannelSelect) },
                            onFavoriteClick = { activeHeroChannel?.let { viewModel.toggleFavorite(it) } },
                        )
                    }

                    // ── 2. Category Hub Pills (D-Pad reachable) ───────────────
                    item(key = "tv_hub_pills") {
                        Spacer(Modifier.height(20.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            item {
                                CategoryHubPill(
                                    data = hubPills.anime,
                                    onClick = {
                                        showOnlyFavorites = false
                                        viewModel.setActiveCountry(null)
                                        viewModel.setActiveCategory(
                                            if (uiState.activeCategory == Category.ANIMATION) null else Category.ANIMATION
                                        )
                                    },
                                )
                            }
                            item {
                                CategoryHubPill(
                                    data = hubPills.movies,
                                    onClick = {
                                        showOnlyFavorites = false
                                        viewModel.setActiveCountry(null)
                                        viewModel.setActiveCategory(
                                            if (uiState.activeCategory == Category.MOVIES) null else Category.MOVIES
                                        )
                                    },
                                )
                            }
                            item {
                                CategoryHubPill(
                                    data = hubPills.sports,
                                    onClick = {
                                        showOnlyFavorites = false
                                        viewModel.setActiveCountry(null)
                                        viewModel.setActiveCategory(
                                            if (uiState.activeCategory == Category.SPORTS) null else Category.SPORTS
                                        )
                                    },
                                )
                            }
                            item {
                                CategoryHubPill(
                                    data = hubPills.country,
                                    onClick = {
                                        showOnlyFavorites = false
                                        showCountrySelector = !showCountrySelector
                                    },
                                )
                            }
                            item {
                                CategoryHubPill(
                                    data = hubPills.favorites,
                                    onClick = {
                                        showOnlyFavorites = !showOnlyFavorites
                                        if (showOnlyFavorites) {
                                            viewModel.setActiveCountry(null)
                                            viewModel.setActiveCategory(null)
                                            showCountrySelector = false
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── 3. TV Country Selector Row (if activated) ────────────
                    if (showCountrySelector) {
                        item(key = "tv_country_selector") {
                            TvCountrySelectorRow(
                                channels = allChannels,
                                activeCountry = uiState.activeCountry,
                                onCountrySelected = { code ->
                                    viewModel.setActiveCountry(code)
                                    showCountrySelector = false
                                },
                                onClearCountry = {
                                    viewModel.setActiveCountry(null)
                                    showCountrySelector = false
                                },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── 4. Active Filter Badge ───────────────────────────────
                    if (showOnlyFavorites || uiState.activeCategory != null || uiState.activeCountry != null) {
                        item(key = "tv_filter_badge") {
                            val label = when {
                                showOnlyFavorites -> "Mis Canales Favoritos"
                                uiState.activeCategory != null -> uiState.activeCategory?.displayName ?: ""
                                else -> uiState.activeCountry?.let { countryDisplayName(it) } ?: ""
                            }

                            val badgeColor = if (showOnlyFavorites) uTVColors.FavoriteGold else uTVColors.AccentElectricBlue

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(50.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor),
                                    modifier = Modifier
                                        .focusable()
                                        .clickable {
                                            showOnlyFavorites = false
                                            viewModel.setActiveCategory(null)
                                            viewModel.setActiveCountry(null)
                                        },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "Filtro activo: $label",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = uTVColors.TextPrimary,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar filtro",
                                            tint = uTVColors.TextPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // ── 5. Channels Content ──────────────────────────────────
                    if (filteredChannels != null) {
                        // Filtered view
                        item(key = "tv_filtered_header") {
                            val sectionTitle = when {
                                showOnlyFavorites -> "Mis Canales Favoritos"
                                uiState.activeCategory != null -> uiState.activeCategory?.displayName ?: ""
                                else -> "Canales de ${uiState.activeCountry}"
                            }
                            TvSectionHeader(
                                title = sectionTitle,
                                count = filteredChannels.size,
                                accentColor = if (showOnlyFavorites) uTVColors.FavoriteGold else uTVColors.AccentElectricBlue,
                            )
                        }

                        item(key = "tv_filtered_row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(filteredChannels, key = { it.id }) { channel ->
                                    ChannelCard(
                                        channel = channel,
                                        onClick = { handleChannelSelect(channel) },
                                        onFavorite = { viewModel.toggleFavorite(channel) },
                                        onFocus = { focusedChannel = channel },
                                        modifier = Modifier.width(260.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard Carousels
                        if (recentlyWatched.isNotEmpty()) {
                            item(key = "tv_section_recent") {
                                TvChannelCarouselRow(
                                    title = "Continuar viendo",
                                    channels = recentlyWatched,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (favorites.isNotEmpty()) {
                            item(key = "tv_section_fav") {
                                TvChannelCarouselRow(
                                    title = "Tus Canales Favoritos",
                                    channels = favorites,
                                    accentColor = uTVColors.FavoriteGold,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (animeChannels.isNotEmpty()) {
                            item(key = "tv_section_anime") {
                                TvChannelCarouselRow(
                                    title = "Anime & Geek Hub",
                                    channels = animeChannels,
                                    accentColor = uTVColors.AccentAnimeOrange,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (moviesChannels.isNotEmpty()) {
                            item(key = "tv_section_movies") {
                                TvChannelCarouselRow(
                                    title = "Cine & Series",
                                    channels = moviesChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (sportsChannels.isNotEmpty()) {
                            item(key = "tv_section_sports") {
                                TvChannelCarouselRow(
                                    title = "Deportes en Vivo",
                                    channels = sportsChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (generalChannels.isNotEmpty()) {
                            item(key = "tv_section_general") {
                                TvChannelCarouselRow(
                                    title = "Variedades & Entretenimiento",
                                    channels = generalChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (musicChannels.isNotEmpty()) {
                            item(key = "tv_section_music") {
                                TvChannelCarouselRow(
                                    title = "Música & Conciertos",
                                    channels = musicChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (newsChannels.isNotEmpty()) {
                            item(key = "tv_section_news") {
                                TvChannelCarouselRow(
                                    title = "Noticias en Español",
                                    channels = newsChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }

                        if (kidsChannels.isNotEmpty()) {
                            item(key = "tv_section_kids") {
                                TvChannelCarouselRow(
                                    title = "Infantil & Familia",
                                    channels = kidsChannels,
                                    onChannelClick = handleChannelSelect,
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onChannelFocus = { focusedChannel = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── TV Carousel Row ──────────────────────────────────────────────────────────

@Composable
private fun TvChannelCarouselRow(
    title: String,
    channels: List<Channel>,
    accentColor: Color = uTVColors.AccentElectricBlue,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        TvSectionHeader(
            title = title,
            count = channels.size,
            accentColor = accentColor,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 6.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onFavorite = { onFavoriteToggle(channel) },
                    onFocus = { onChannelFocus(channel) },
                    modifier = Modifier.width(260.dp),
                )
            }
        }
    }
}

@Composable
private fun TvSectionHeader(
    title: String,
    count: Int? = null,
    accentColor: Color = uTVColors.AccentElectricBlue,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 22.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = uTVColors.TextPrimary,
            )
        }

        if (count != null) {
            Text(
                text = "$count canales",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = uTVColors.TextSecondary,
            )
        }
    }
}

// ── TV Country Selector Row ──────────────────────────────────────────────────

@Composable
private fun TvCountrySelectorRow(
    channels: List<Channel>,
    activeCountry: String?,
    onCountrySelected: (String) -> Unit,
    onClearCountry: () -> Unit,
) {
    val countries = remember(channels) {
        channels.map { it.countryCode }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
    ) {
        Text(
            text = "Seleccionar País (Latinoamérica y España)",
            style = MaterialTheme.typography.titleMedium,
            color = uTVColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                TvCountryChip(
                    label = "Todos los países",
                    isSelected = activeCountry == null,
                    onClick = onClearCountry,
                )
            }
            items(countries, key = { it.first }) { (code, count) ->
                TvCountryChip(
                    label = "${countryDisplayName(code)} ($count)",
                    isSelected = code.equals(activeCountry, ignoreCase = true),
                    onClick = { onCountrySelected(code) },
                )
            }
        }
    }
}

@Composable
private fun TvCountryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected || isFocused) uTVColors.AccentElectricBlue else uTVColors.SurfaceCard,
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else if (isSelected) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
        ),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = if (isSelected || isFocused) uTVColors.TextPrimary else uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// ── Loading & Error States for TV ────────────────────────────────────────────

@Composable
private fun TvLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                color = uTVColors.AccentElectricBlue,
                modifier = Modifier.size(64.dp),
                strokeWidth = 5.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Sintonizando catálogo para Smart TV...",
                style = MaterialTheme.typography.titleLarge,
                color = uTVColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun TvErrorView(
    message: String,
    onRetry: () -> Unit,
) {
    var isButtonFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp),
        ) {
            Text(
                text = "No se pudieron sintonizar los canales",
                style = MaterialTheme.typography.headlineLarge,
                color = uTVColors.TextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = uTVColors.TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonFocused) uTVColors.AccentAnimeOrange else uTVColors.AccentElectricBlue,
                    contentColor = uTVColors.TextPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .onFocusChanged { isButtonFocused = it.isFocused }
                    .focusable(),
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Reintentar conexión",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
