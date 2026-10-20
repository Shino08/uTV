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
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/** Sections available in Android TV Home. */
enum class TvSection(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Default.Home),
    GUIDE("Guía TV", Icons.Default.FormatListNumbered),
    SEARCH("Buscar", Icons.Default.Search),
    ANIME("Anime Hub", Icons.Default.SportsEsports),
    FAVORITES("Favoritos", Icons.Default.Star),
}

/**
 * Android TV / Google TV Home Screen — implements guidelines.md §1, §3, §4, §5:
 * - Top TV Navigation Bar: Inicio, Guía TV (Diales 100-799), Buscar canales, Anime Hub y Favoritos.
 * - 100% D-Pad remote navigable: focusable cards, animated scale 1.08f + illuminated glow border.
 * - Dynamic Hero Banner: updates in real time to feature the channel currently focused by the D-Pad cursor.
 * - Category Hub Pills: D-Pad reachable horizontal filter pills (Anime, Cine, Deportes, TV por País, Favoritos).
 * - Multi-category horizontal carousels with 48dp TV overscan safe padding.
 * - Full TV Guide view (100–799) with category filter pills and responsive 3-column TV grid.
 * - Dedicated Search screen with TV search input, quick filter tags, and real-time results.
 * - Dedicated Favorites screen with quick channel access and empty state.
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

    var selectedSection by remember { mutableStateOf(TvSection.HOME) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var showCountrySelector by remember { mutableStateOf(false) }

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
        isFavoritesSelected = false,
        isCountrySelected = uiState.activeCountry != null,
    )

    // Filtered channels if a category or country filter is active in Home
    val filteredChannels = remember(allChannels, uiState.activeCategory, uiState.activeCountry) {
        when {
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

            // Operational TV Interface
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── 1. Top TV Navigation Header Bar ──────────────────────
                    TvTopNavigationBar(
                        currentSection = selectedSection,
                        onSectionSelected = { newSection ->
                            selectedSection = newSection
                            if (newSection != TvSection.HOME) {
                                viewModel.setActiveCategory(null)
                                viewModel.setActiveCountry(null)
                                showCountrySelector = false
                            }
                        },
                    )

                    // ── 2. Active Section Content ────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedSection) {
                            TvSection.HOME -> TvHomeCatalogContent(
                                allChannels = allChannels,
                                recentlyWatched = recentlyWatched,
                                favorites = favorites,
                                animeChannels = animeChannels,
                                moviesChannels = moviesChannels,
                                sportsChannels = sportsChannels,
                                generalChannels = generalChannels,
                                musicChannels = musicChannels,
                                newsChannels = newsChannels,
                                kidsChannels = kidsChannels,
                                filteredChannels = filteredChannels,
                                activeHeroChannel = activeHeroChannel,
                                hubPills = hubPills,
                                activeCategory = uiState.activeCategory,
                                activeCountry = uiState.activeCountry,
                                showCountrySelector = showCountrySelector,
                                onChannelSelect = handleChannelSelect,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelFocus = { focusedChannel = it },
                                onAnimeHubClick = { selectedSection = TvSection.ANIME },
                                onMoviesClick = {
                                    viewModel.setActiveCountry(null)
                                    viewModel.setActiveCategory(
                                        if (uiState.activeCategory == Category.MOVIES) null else Category.MOVIES
                                    )
                                },
                                onSportsClick = {
                                    viewModel.setActiveCountry(null)
                                    viewModel.setActiveCategory(
                                        if (uiState.activeCategory == Category.SPORTS) null else Category.SPORTS
                                    )
                                },
                                onCountryClick = { showCountrySelector = !showCountrySelector },
                                onFavoritesClick = { selectedSection = TvSection.FAVORITES },
                                onCountrySelected = { code ->
                                    viewModel.setActiveCountry(code)
                                    showCountrySelector = false
                                },
                                onClearCountry = {
                                    viewModel.setActiveCountry(null)
                                    showCountrySelector = false
                                },
                                onClearFilters = {
                                    viewModel.setActiveCategory(null)
                                    viewModel.setActiveCountry(null)
                                },
                            )

                            TvSection.GUIDE -> TvGuideContent(
                                allChannels = allChannels,
                                onChannelSelect = handleChannelSelect,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelFocus = { focusedChannel = it },
                            )

                            TvSection.SEARCH -> TvSearchContent(
                                allChannels = allChannels,
                                onChannelSelect = handleChannelSelect,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelFocus = { focusedChannel = it },
                            )

                            TvSection.ANIME -> TvAnimeHubContent(
                                animeChannels = animeChannels,
                                onChannelSelect = handleChannelSelect,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelFocus = { focusedChannel = it },
                            )

                            TvSection.FAVORITES -> TvFavoritesContent(
                                favorites = favorites,
                                onChannelSelect = handleChannelSelect,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelFocus = { focusedChannel = it },
                                onExploreGuide = { selectedSection = TvSection.GUIDE },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Top Navigation Bar for TV ────────────────────────────────────────────────

@Composable
private fun TvTopNavigationBar(
    currentSection: TvSection,
    onSectionSelected: (TvSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Brand logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "u",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Black,
                color = uTVColors.AccentAnimeOrange,
            )
            Text(
                text = "TV",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Black,
                color = uTVColors.TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(uTVColors.AccentElectricBlue, shape = RoundedCornerShape(3.dp)),
            )
        }

        // Navigable section tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvSection.entries.forEach { section ->
                TvNavPill(
                    section = section,
                    isSelected = currentSection == section,
                    onClick = { onSectionSelected(section) },
                )
            }
        }
    }
}

@Composable
private fun TvNavPill(
    section: TvSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val accentColor = if (section == TvSection.ANIME) {
        uTVColors.AccentAnimeOrange
    } else {
        uTVColors.AccentElectricBlue
    }

    val backgroundColor = when {
        isFocused -> accentColor.copy(alpha = 0.35f)
        isSelected -> accentColor.copy(alpha = 0.20f)
        else -> uTVColors.SurfaceCard.copy(alpha = 0.6f)
    }

    val borderColor = when {
        isFocused -> accentColor
        isSelected -> accentColor.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val scale = if (isFocused) 1.08f else 1.0f

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(if (isFocused || isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = section.label,
                tint = if (isSelected || isFocused) uTVColors.TextPrimary else uTVColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = section.label,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isFocused) uTVColors.TextPrimary else uTVColors.TextSecondary,
            )
        }
    }
}

// ── Tab 1: Home Catalog View ──────────────────────────────────────────────────

@Composable
private fun TvHomeCatalogContent(
    allChannels: List<Channel>,
    recentlyWatched: List<Channel>,
    favorites: List<Channel>,
    animeChannels: List<Channel>,
    moviesChannels: List<Channel>,
    sportsChannels: List<Channel>,
    generalChannels: List<Channel>,
    musicChannels: List<Channel>,
    newsChannels: List<Channel>,
    kidsChannels: List<Channel>,
    filteredChannels: List<Channel>?,
    activeHeroChannel: Channel?,
    hubPills: com.iptv.app.ui.components.cards.HubPills,
    activeCategory: Category?,
    activeCountry: String?,
    showCountrySelector: Boolean,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
    onAnimeHubClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSportsClick: () -> Unit,
    onCountryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCountrySelected: (String) -> Unit,
    onClearCountry: () -> Unit,
    onClearFilters: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        // ── 1. Top Cinematic Hero Banner ─────────────────────────
        item(key = "tv_hero_banner") {
            HeroFeaturedBanner(
                channel = activeHeroChannel,
                onPlayClick = { activeHeroChannel?.let(onChannelSelect) },
                onFavoriteClick = { activeHeroChannel?.let(onFavoriteToggle) },
            )
        }

        // ── 2. Category Hub Pills (D-Pad reachable) ───────────────
        item(key = "tv_hub_pills") {
            Spacer(Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { CategoryHubPill(data = hubPills.anime, onClick = onAnimeHubClick) }
                item { CategoryHubPill(data = hubPills.movies, onClick = onMoviesClick) }
                item { CategoryHubPill(data = hubPills.sports, onClick = onSportsClick) }
                item { CategoryHubPill(data = hubPills.country, onClick = onCountryClick) }
                item { CategoryHubPill(data = hubPills.favorites, onClick = onFavoritesClick) }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 3. TV Country Selector Row (if activated) ────────────
        if (showCountrySelector) {
            item(key = "tv_country_selector") {
                TvCountrySelectorRow(
                    channels = allChannels,
                    activeCountry = activeCountry,
                    onCountrySelected = onCountrySelected,
                    onClearCountry = onClearCountry,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── 4. Active Filter Badge ───────────────────────────────
        if (activeCategory != null || activeCountry != null) {
            item(key = "tv_filter_badge") {
                val label = activeCategory?.displayName ?: activeCountry?.let { countryDisplayName(it) } ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = uTVColors.AccentElectricBlue.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, uTVColors.AccentElectricBlue),
                        modifier = Modifier
                            .focusable()
                            .clickable(onClick = onClearFilters),
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
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── 5. Channels Content ──────────────────────────────────
        if (filteredChannels != null) {
            item(key = "tv_filtered_header") {
                TvSectionHeader(
                    title = activeCategory?.displayName ?: "Canales de $activeCountry",
                    count = filteredChannels.size,
                    accentColor = uTVColors.AccentElectricBlue,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 6.dp),
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
                            onClick = { onChannelSelect(channel) },
                            onFavorite = { onFavoriteToggle(channel) },
                            onFocus = { onChannelFocus(channel) },
                            modifier = Modifier.width(260.dp),
                        )
                    }
                }
            }
        } else {
            // Standard Multi-Category Carousels
            if (recentlyWatched.isNotEmpty()) {
                item(key = "tv_section_recent") {
                    TvChannelCarouselRow(
                        title = "Continuar viendo",
                        channels = recentlyWatched,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (favorites.isNotEmpty()) {
                item(key = "tv_section_fav") {
                    TvChannelCarouselRow(
                        title = "Tus Canales Favoritos",
                        channels = favorites,
                        accentColor = uTVColors.FavoriteGold,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (animeChannels.isNotEmpty()) {
                item(key = "tv_section_anime") {
                    TvChannelCarouselRow(
                        title = "Anime & Geek Hub",
                        channels = animeChannels,
                        accentColor = uTVColors.AccentAnimeOrange,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (moviesChannels.isNotEmpty()) {
                item(key = "tv_section_movies") {
                    TvChannelCarouselRow(
                        title = "Cine & Series",
                        channels = moviesChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (sportsChannels.isNotEmpty()) {
                item(key = "tv_section_sports") {
                    TvChannelCarouselRow(
                        title = "Deportes en Vivo",
                        channels = sportsChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (generalChannels.isNotEmpty()) {
                item(key = "tv_section_general") {
                    TvChannelCarouselRow(
                        title = "Variedades & Entretenimiento",
                        channels = generalChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (musicChannels.isNotEmpty()) {
                item(key = "tv_section_music") {
                    TvChannelCarouselRow(
                        title = "Música & Conciertos",
                        channels = musicChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (newsChannels.isNotEmpty()) {
                item(key = "tv_section_news") {
                    TvChannelCarouselRow(
                        title = "Noticias en Español",
                        channels = newsChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }

            if (kidsChannels.isNotEmpty()) {
                item(key = "tv_section_kids") {
                    TvChannelCarouselRow(
                        title = "Infantil & Familia",
                        channels = kidsChannels,
                        onChannelClick = onChannelSelect,
                        onFavoriteToggle = onFavoriteToggle,
                        onChannelFocus = onChannelFocus,
                    )
                }
            }
        }
    }
}

// ── Tab 2: Guía TV en Vivo (Diales 100 – 799) ────────────────────────────────

@Composable
private fun TvGuideContent(
    allChannels: List<Channel>,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredGuide = remember(allChannels, selectedCategory, searchQuery) {
        allChannels
            .filter { ch ->
                val matchesCat = selectedCategory == null || ch.category == selectedCategory
                val matchesQuery = searchQuery.isBlank() ||
                    ch.name.contains(searchQuery, ignoreCase = true) ||
                    ch.dialNumber.toString().contains(searchQuery) ||
                    ch.countryCode.contains(searchQuery, ignoreCase = true)
                matchesCat && matchesQuery
            }
            .sortedBy { it.dialNumber }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        // Header
        item(key = "tv_guide_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Guía TV en Vivo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = uTVColors.TextPrimary,
                )
                Text(
                    text = "Diales 100 – 799 • Catálogo completo por orden numérico",
                    style = MaterialTheme.typography.bodyMedium,
                    color = uTVColors.TextSecondary,
                )
            }
        }

        // Category filter chips
        item(key = "tv_guide_categories") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    TvCategoryFilterChip(
                        label = "Todos (${allChannels.size})",
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                    )
                }
                items(Category.entries.filter { it != Category.UNDEFINED }) { cat ->
                    val count = allChannels.count { it.category == cat }
                    if (count > 0) {
                        TvCategoryFilterChip(
                            label = "${cat.displayName} ($count)",
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Channels in a responsive 3-column TV grid
        items(filteredGuide.chunked(3), key = { it.first().id }) { rowChannels ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowChannels.forEach { channel ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelSelect(channel) },
                            onFavorite = { onFavoriteToggle(channel) },
                            onFocus = { onChannelFocus(channel) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(3 - rowChannels.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Tab 3: TV Search View ────────────────────────────────────────────────────

@Composable
private fun TvSearchContent(
    allChannels: List<Channel>,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val searchResults = remember(allChannels, searchQuery, selectedTag) {
        if (searchQuery.isBlank() && selectedTag == null) {
            emptyList()
        } else {
            allChannels.filter { channel ->
                val matchesQuery = if (searchQuery.isNotBlank()) {
                    channel.name.contains(searchQuery, ignoreCase = true) ||
                        channel.dialNumber.toString().contains(searchQuery) ||
                        channel.category.displayName.contains(searchQuery, ignoreCase = true) ||
                        channel.countryCode.contains(searchQuery, ignoreCase = true)
                } else true

                val matchesTag = if (selectedTag != null) {
                    channel.category.displayName.equals(selectedTag, ignoreCase = true) ||
                        channel.countryCode.equals(selectedTag, ignoreCase = true)
                } else true

                matchesQuery && matchesTag
            }.sortedBy { it.dialNumber }
        }
    }

    val quickTags = remember {
        listOf("Anime", "Cine", "Deportes", "Noticias", "Música", "Infantil", "CL", "MX", "AR", "VE", "ES", "CO", "PE")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        // Search Input Bar
        item(key = "tv_search_input") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Buscar Canales de TV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = uTVColors.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Escribe el nombre del canal o número de dial (ej. 301)...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = uTVColors.TextSecondary,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = uTVColors.TextSecondary,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = uTVColors.TextSecondary,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = uTVColors.SurfaceCard,
                        unfocusedContainerColor = uTVColors.SurfaceCard,
                        focusedBorderColor = uTVColors.AccentElectricBlue,
                        unfocusedBorderColor = uTVColors.BorderSubtle,
                        focusedTextColor = uTVColors.TextPrimary,
                        unfocusedTextColor = uTVColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                // Quick tags
                Text(
                    text = "Filtros rápidos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = uTVColors.TextSecondary,
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(quickTags) { tag ->
                        val isSelected = selectedTag == tag
                        TvCategoryFilterChip(
                            label = tag,
                            isSelected = isSelected,
                            onClick = { selectedTag = if (isSelected) null else tag },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Results or Suggestions
        if (searchResults.isNotEmpty()) {
            item(key = "search_results_header") {
                TvSectionHeader(
                    title = "Resultados encontrados",
                    count = searchResults.size,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                )
            }

            items(searchResults.chunked(3), key = { it.first().id }) { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowChannels.forEach { channel ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelSelect(channel) },
                                onFavorite = { onFavoriteToggle(channel) },
                                onFocus = { onChannelFocus(channel) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    repeat(3 - rowChannels.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else if (searchQuery.isNotBlank() || selectedTag != null) {
            item(key = "search_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No se encontraron canales que coincidan con la búsqueda.",
                        style = MaterialTheme.typography.titleMedium,
                        color = uTVColors.TextSecondary,
                    )
                }
            }
        } else {
            // Initial suggestions
            item(key = "search_suggestions_header") {
                TvSectionHeader(
                    title = "Canales Populares Sugeridos",
                    count = allChannels.take(12).size,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
                )
            }

            items(allChannels.take(12).chunked(3), key = { it.first().id }) { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowChannels.forEach { channel ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelSelect(channel) },
                                onFavorite = { onFavoriteToggle(channel) },
                                onFocus = { onChannelFocus(channel) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    repeat(3 - rowChannels.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Tab 4: Anime Hub ──────────────────────────────────────────────────────────

@Composable
private fun TvAnimeHubContent(
    animeChannels: List<Channel>,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
) {
    val featuredAnime = remember(animeChannels) { animeChannels.firstOrNull() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        item(key = "anime_hero") {
            HeroFeaturedBanner(
                channel = featuredAnime,
                onPlayClick = { featuredAnime?.let(onChannelSelect) },
                onFavoriteClick = { featuredAnime?.let(onFavoriteToggle) },
            )
        }

        item(key = "anime_section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 24.dp)
                        .background(uTVColors.AccentAnimeOrange, shape = RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Anime & Geek Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = uTVColors.TextPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${animeChannels.size} canales 24/7",
                    style = MaterialTheme.typography.labelMedium,
                    color = uTVColors.TextSecondary,
                )
            }
        }

        items(animeChannels.chunked(3), key = { it.first().id }) { rowChannels ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowChannels.forEach { channel ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelSelect(channel) },
                            onFavorite = { onFavoriteToggle(channel) },
                            onFocus = { onChannelFocus(channel) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(3 - rowChannels.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Tab 5: Favorites View ─────────────────────────────────────────────────────

@Composable
private fun TvFavoritesContent(
    favorites: List<Channel>,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onChannelFocus: (Channel) -> Unit,
    onExploreGuide: () -> Unit,
) {
    if (favorites.isEmpty()) {
        TvEmptyFavoritesView(onExploreGuide = onExploreGuide)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item(key = "fav_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = "Tus Canales Favoritos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "${favorites.size} canales guardados para acceso directo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextSecondary,
                    )
                }
            }

            items(favorites.chunked(3), key = { it.first().id }) { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowChannels.forEach { channel ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelSelect(channel) },
                                onFavorite = { onFavoriteToggle(channel) },
                                onFocus = { onChannelFocus(channel) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    repeat(3 - rowChannels.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TvEmptyFavoritesView(onExploreGuide: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = uTVColors.FavoriteGold,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No tienes canales favoritos aún",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = uTVColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Presiona el ícono de estrella ★ en cualquier canal para agregarlo aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = uTVColors.TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            var isBtnFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onExploreGuide,
                colors = ButtonDefaults.buttonColors(containerColor = uTVColors.AccentElectricBlue),
                modifier = Modifier
                    .scale(if (isBtnFocused) 1.08f else 1f)
                    .onFocusChanged { isBtnFocused = it.isFocused },
            ) {
                Text("Explorar Guía de Canales")
            }
        }
    }
}

// ── Shared Subcomponents for TV ──────────────────────────────────────────────

@Composable
private fun TvCategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected || isFocused) uTVColors.AccentElectricBlue else uTVColors.SurfaceCard,
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else if (isSelected) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
        ),
        modifier = modifier
            .scale(if (isFocused) 1.06f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected || isFocused) uTVColors.TextPrimary else uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

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
                TvCategoryFilterChip(
                    label = "Todos los países",
                    isSelected = activeCountry == null,
                    onClick = onClearCountry,
                )
            }
            items(countries, key = { it.first }) { (code, count) ->
                TvCategoryFilterChip(
                    label = "${countryDisplayName(code)} ($count)",
                    isSelected = code.equals(activeCountry, ignoreCase = true),
                    onClick = { onCountrySelected(code) },
                )
            }
        }
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
