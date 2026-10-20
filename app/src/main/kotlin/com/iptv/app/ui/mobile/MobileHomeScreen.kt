package com.iptv.app.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.components.cards.CategoryHubPill
import com.iptv.app.ui.components.cards.ChannelCard
import com.iptv.app.ui.components.cards.HeroFeaturedBanner
import com.iptv.app.ui.components.cards.rememberHubPills
import com.iptv.app.ui.shared.HomeViewModel
import com.iptv.app.ui.theme.uTVColors

/** Bottom navigation tab definitions for Mobile. */
enum class MobileTab(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Inicio", Icons.Default.Home),
    GUIDE("Guía TV", Icons.Default.FormatListNumbered),
    ANIME("Anime Hub", Icons.Default.SportsEsports),
    FAVORITES("Favoritos", Icons.Default.Star),
}

/**
 * Mobile Home Screen — implements guidelines.md §5:
 * - Bottom NavigationBar: Inicio, Guía TV (Diales), Anime Hub, Favoritos
 * - HeroFeaturedBanner with cinematic backdrop + "Ver ahora" CTA
 * - CategoryHubPills row (Anime, Cine, Deportes, TV por País, Favoritos)
 * - Carousels for recent channels, favorites, and categorized rows
 * - Country Picker ModalBottomSheet for Latin America / Spain
 * - Full TV Guide list sorted by dialNumber (100–799)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileHomeScreen(
    onChannelSelected: (Channel) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()

    var selectedTab by remember { mutableStateOf(MobileTab.HOME) }
    var showCountrySheet by remember { mutableStateOf(false) }

    val handleChannelClick: (Channel) -> Unit = { channel ->
        viewModel.onChannelSelected(channel)
        onChannelSelected(channel)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(uTVColors.BackgroundPrimary),
        containerColor = uTVColors.BackgroundPrimary,
        bottomBar = {
            NavigationBar(
                containerColor = uTVColors.BackgroundSecondary,
                contentColor = uTVColors.TextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                MobileTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabAccentColor = if (tab == MobileTab.ANIME) {
                        uTVColors.AccentAnimeOrange
                    } else {
                        uTVColors.AccentElectricBlue
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            if (tab != MobileTab.HOME) {
                                viewModel.setActiveCategory(null)
                                viewModel.setActiveCountry(null)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) tabAccentColor else uTVColors.TextSecondary,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) uTVColors.TextPrimary else uTVColors.TextSecondary,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = tabAccentColor.copy(alpha = 0.2f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                // Loading state (initial sync)
                uiState.isLoading && allChannels.isEmpty() -> {
                    LoadingView()
                }

                // Error state (network failure with empty cache)
                uiState.syncError != null && allChannels.isEmpty() -> {
                    ErrorStateView(
                        message = uiState.syncError ?: "Error al sincronizar canales.",
                        onRetry = { viewModel.retrySync() },
                    )
                }

                // Normal operational screens
                else -> {
                    when (selectedTab) {
                        MobileTab.HOME -> HomeTabContent(
                            allChannels = allChannels,
                            recentlyWatched = recentlyWatched,
                            favorites = favorites,
                            activeCategory = uiState.activeCategory,
                            activeCountry = uiState.activeCountry,
                            onChannelClick = handleChannelClick,
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onAnimeHubClick = {
                                viewModel.setActiveCountry(null)
                                viewModel.setActiveCategory(
                                    if (uiState.activeCategory == Category.ANIMATION) null else Category.ANIMATION
                                )
                            },
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
                            onCountryClick = { showCountrySheet = true },
                            onFavoritesHubClick = { selectedTab = MobileTab.FAVORITES },
                            onClearFilters = {
                                viewModel.setActiveCategory(null)
                                viewModel.setActiveCountry(null)
                            },
                        )

                        MobileTab.GUIDE -> GuideTabContent(
                            channels = allChannels,
                            onChannelClick = handleChannelClick,
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        )

                        MobileTab.ANIME -> AnimeHubTabContent(
                            channels = allChannels.filter { it.category == Category.ANIMATION },
                            onChannelClick = handleChannelClick,
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        )

                        MobileTab.FAVORITES -> FavoritesTabContent(
                            favorites = favorites,
                            onChannelClick = handleChannelClick,
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        )
                    }
                }
            }
        }
    }

    // ── Country Picker Bottom Sheet ──────────────────────────────────────────
    if (showCountrySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val availableCountries = remember(allChannels) {
            allChannels.map { it.countryCode }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
        }

        ModalBottomSheet(
            onDismissRequest = { showCountrySheet = false },
            sheetState = sheetState,
            containerColor = uTVColors.BackgroundSecondary,
            contentColor = uTVColors.TextPrimary,
        ) {
            CountryPickerContent(
                countries = availableCountries,
                activeCountry = uiState.activeCountry,
                onCountrySelected = { code ->
                    viewModel.setActiveCountry(code)
                    showCountrySheet = false
                },
                onClearCountry = {
                    viewModel.setActiveCountry(null)
                    showCountrySheet = false
                },
            )
        }
    }
}

// ── Tab 1: Home Catalog View ──────────────────────────────────────────────────

@Composable
private fun HomeTabContent(
    allChannels: List<Channel>,
    recentlyWatched: List<Channel>,
    favorites: List<Channel>,
    activeCategory: Category?,
    activeCountry: String?,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    onAnimeHubClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSportsClick: () -> Unit,
    onCountryClick: () -> Unit,
    onFavoritesHubClick: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val featuredChannel = remember(allChannels, recentlyWatched) {
        recentlyWatched.firstOrNull()
            ?: allChannels.firstOrNull { it.category == Category.ANIMATION }
            ?: allChannels.firstOrNull { it.category == Category.MOVIES }
            ?: allChannels.firstOrNull()
    }

    val hubPills = rememberHubPills(
        selectedCategory = activeCategory,
        isFavoritesSelected = false,
        isCountrySelected = activeCountry != null,
    )

    // Filter channels when user active a category or country filter
    val filteredChannels = remember(allChannels, activeCategory, activeCountry) {
        when {
            activeCategory != null -> allChannels.filter { it.category == activeCategory }
            activeCountry != null  -> allChannels.filter { it.countryCode.equals(activeCountry, ignoreCase = true) }
            else                   -> null
        }
    }

    // Pre-filter category carousels once per channel sync to eliminate UI thread lag when scrolling
    val animeChannels = remember(allChannels) { allChannels.filter { it.category == Category.ANIMATION } }
    val moviesChannels = remember(allChannels) { allChannels.filter { it.category == Category.MOVIES } }
    val sportsChannels = remember(allChannels) { allChannels.filter { it.category == Category.SPORTS } }
    val generalChannels = remember(allChannels) { allChannels.filter { it.category == Category.GENERAL } }
    val musicChannels = remember(allChannels) { allChannels.filter { it.category == Category.MUSIC } }
    val newsChannels = remember(allChannels) { allChannels.filter { it.category == Category.NEWS } }
    val kidsChannels = remember(allChannels) { allChannels.filter { it.category == Category.KIDS } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Hero Featured Banner
        item(key = "hero_banner") {
            HeroFeaturedBanner(
                channel = featuredChannel,
                onPlayClick = { featuredChannel?.let(onChannelClick) },
                onFavoriteClick = { featuredChannel?.let(onFavoriteToggle) },
            )
        }

        // Category Hub Pills (Horizontal Row)
        item(key = "hub_pills_row") {
            Spacer(Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { CategoryHubPill(data = hubPills.anime, onClick = onAnimeHubClick) }
                item { CategoryHubPill(data = hubPills.movies, onClick = onMoviesClick) }
                item { CategoryHubPill(data = hubPills.sports, onClick = onSportsClick) }
                item { CategoryHubPill(data = hubPills.country, onClick = onCountryClick) }
                item { CategoryHubPill(data = hubPills.favorites, onClick = onFavoritesHubClick) }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Active filter indicator badge
        if (activeCategory != null || activeCountry != null) {
            item(key = "active_filter_badge") {
                val label = activeCategory?.displayName ?: activeCountry?.let { countryDisplayName(it) } ?: ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        color = uTVColors.AccentElectricBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, uTVColors.AccentElectricBlue),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Filtro activo: $label",
                                style = MaterialTheme.typography.labelSmall,
                                color = uTVColors.TextPrimary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar filtro",
                                tint = uTVColors.TextPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onClearFilters),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Filtered Mode ─────────────────────────────────────────────────────
        if (filteredChannels != null) {
            item(key = "filtered_section_title") {
                SectionHeader(
                    title = activeCategory?.displayName ?: "Canales de $activeCountry",
                    count = filteredChannels.size,
                )
            }

            items(filteredChannels.chunked(2), key = { it.first().id }) { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowChannels.forEach { ch ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChannelCard(
                                channel = ch,
                                onClick = { onChannelClick(ch) },
                                onFavorite = { onFavoriteToggle(ch) },
                            )
                        }
                    }
                    if (rowChannels.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // ── Standard Multi-Carousel Mode ──────────────────────────────────
            if (recentlyWatched.isNotEmpty()) {
                item(key = "section_recent") {
                    ChannelCarouselSection(
                        title = "Continuar viendo",
                        channels = recentlyWatched,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (favorites.isNotEmpty()) {
                item(key = "section_favorites") {
                    ChannelCarouselSection(
                        title = "Tus Canales Favoritos",
                        channels = favorites,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            // Category carousels
            if (animeChannels.isNotEmpty()) {
                item(key = "section_anime") {
                    ChannelCarouselSection(
                        title = "Anime & Geek Hub",
                        channels = animeChannels,
                        accentColor = uTVColors.AccentAnimeOrange,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (moviesChannels.isNotEmpty()) {
                item(key = "section_movies") {
                    ChannelCarouselSection(
                        title = "Cine & Series",
                        channels = moviesChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (sportsChannels.isNotEmpty()) {
                item(key = "section_sports") {
                    ChannelCarouselSection(
                        title = "Deportes en Vivo",
                        channels = sportsChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (generalChannels.isNotEmpty()) {
                item(key = "section_general") {
                    ChannelCarouselSection(
                        title = "Variedades & Entretenimiento",
                        channels = generalChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (musicChannels.isNotEmpty()) {
                item(key = "section_music") {
                    ChannelCarouselSection(
                        title = "Música & Conciertos",
                        channels = musicChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (newsChannels.isNotEmpty()) {
                item(key = "section_news") {
                    ChannelCarouselSection(
                        title = "Noticias en Español",
                        channels = newsChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            if (kidsChannels.isNotEmpty()) {
                item(key = "section_kids") {
                    ChannelCarouselSection(
                        title = "Infantil & Familia",
                        channels = kidsChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }
        }
    }
}

// ── Reusable Carousel Section ─────────────────────────────────────────────────

@Composable
private fun ChannelCarouselSection(
    title: String,
    channels: List<Channel>,
    accentColor: Color = uTVColors.AccentElectricBlue,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        SectionHeader(
            title = title,
            count = channels.size,
            accentColor = accentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onFavorite = { onFavoriteToggle(channel) },
                    modifier = Modifier.width(200.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
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
                    .size(width = 4.dp, height = 18.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = uTVColors.TextPrimary,
            )
        }

        if (count != null) {
            Text(
                text = "$count canales",
                style = MaterialTheme.typography.labelSmall,
                color = uTVColors.TextSecondary,
            )
        }
    }
}

// ── Tab 2: Guía TV (Diales 100 - 799) ─────────────────────────────────────────

@Composable
private fun GuideTabContent(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }

    val sortedChannels = remember(channels, searchQuery, selectedCategoryFilter) {
        channels
            .filter { ch ->
                val matchesQuery = searchQuery.isBlank() ||
                    ch.name.contains(searchQuery, ignoreCase = true) ||
                    ch.dialNumber.toString().contains(searchQuery) ||
                    ch.countryCode.contains(searchQuery, ignoreCase = true)

                val matchesCat = selectedCategoryFilter == null || ch.category == selectedCategoryFilter
                matchesQuery && matchesCat
            }
            .sortedBy { it.dialNumber }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Search and Title Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Guía TV en Vivo",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                color = uTVColors.TextPrimary,
            )
            Text(
                text = "Diales 100 – 799 estilo DirecTV / SimpleTV",
                style = MaterialTheme.typography.bodyMedium,
                color = uTVColors.TextSecondary,
            )

            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Buscar por nombre, dial (ej. 301) o país...",
                        style = MaterialTheme.typography.bodyMedium,
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

            Spacer(Modifier.height(10.dp))

            // Quick Category Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    FilterChip(
                        label = "Todos",
                        isSelected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                    )
                }
                items(Category.entries.filter { it != Category.UNDEFINED }) { cat ->
                    FilterChip(
                        label = cat.displayName,
                        isSelected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                    )
                }
            }
        }

        // Channels List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(sortedChannels, key = { it.id }) { channel ->
                GuideChannelRow(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onFavoriteToggle = { onFavoriteToggle(channel) },
                )
            }
        }
    }
}

@Composable
private fun GuideChannelRow(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = uTVColors.SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, uTVColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dial Badge
            Surface(
                color = uTVColors.BackgroundSecondary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(width = 46.dp, height = 36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.dialNumber.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = uTVColors.TextPrimary,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Logo or Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(uTVColors.BackgroundPrimary),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = uTVColors.AccentElectricBlue,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = uTVColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = channel.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = channel.countryCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = uTVColors.AccentElectricBlue,
                    )
                }
            }

            // Favorite button
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (channel.isFavorite) uTVColors.FavoriteGold else uTVColors.TextSecondary,
                )
            }
        }
    }
}

// ── Tab 3: Anime Hub ──────────────────────────────────────────────────────────

@Composable
private fun AnimeHubTabContent(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
) {
    val featuredAnime = remember(channels) { channels.firstOrNull() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Anime Hero Banner
        item(key = "anime_hero") {
            HeroFeaturedBanner(
                channel = featuredAnime,
                onPlayClick = { featuredAnime?.let(onChannelClick) },
                onFavoriteClick = { featuredAnime?.let(onFavoriteToggle) },
            )
        }

        item(key = "anime_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(uTVColors.AccentAnimeOrange),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Anime & Geek Hub",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
                        color = uTVColors.TextPrimary,
                    )
                }
                Text(
                    text = "Transmisiones continuas 24/7 de animación japonesa y cultura geek",
                    style = MaterialTheme.typography.bodyMedium,
                    color = uTVColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // Grid of Anime Channels
        items(channels.chunked(2), key = { it.first().id }) { rowChannels ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowChannels.forEach { ch ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChannelCard(
                            channel = ch,
                            onClick = { onChannelClick(ch) },
                            onFavorite = { onFavoriteToggle(ch) },
                        )
                    }
                }
                if (rowChannels.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Tab 4: Favoritos ─────────────────────────────────────────────────────────

@Composable
private fun FavoritesTabContent(
    favorites: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = uTVColors.FavoriteGold.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = uTVColors.FavoriteGold,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Aún no tienes favoritos",
                    style = MaterialTheme.typography.titleLarge,
                    color = uTVColors.TextPrimary,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Toca la estrella ★ en cualquier canal para agregarlo aquí y acceder rápidamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = uTVColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader(
                    title = "Mis Canales Favoritos",
                    count = favorites.size,
                    accentColor = uTVColors.FavoriteGold,
                )
            }

            items(favorites.chunked(2), key = { it.first().id }) { rowChannels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowChannels.forEach { ch ->
                        Box(modifier = Modifier.weight(1f)) {
                            ChannelCard(
                                channel = ch,
                                onClick = { onChannelClick(ch) },
                                onFavorite = { onFavoriteToggle(ch) },
                            )
                        }
                    }
                    if (rowChannels.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Country Picker Bottom Sheet Content ──────────────────────────────────────

@Composable
private fun CountryPickerContent(
    countries: List<Pair<String, Int>>,
    activeCountry: String?,
    onCountrySelected: (String) -> Unit,
    onClearCountry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TV por País (Latinoamérica y España)",
                style = MaterialTheme.typography.titleLarge,
                color = uTVColors.TextPrimary,
            )

            if (activeCountry != null) {
                Text(
                    text = "Restablecer",
                    style = MaterialTheme.typography.labelSmall,
                    color = uTVColors.AccentElectricBlue,
                    modifier = Modifier.clickable(onClick = onClearCountry),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(countries, key = { it.first }) { (code, count) ->
                val isSelected = code.equals(activeCountry, ignoreCase = true)
                Surface(
                    color = if (isSelected) uTVColors.AccentElectricBlue.copy(alpha = 0.2f) else uTVColors.SurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCountrySelected(code) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = countryDisplayName(code),
                            style = MaterialTheme.typography.titleMedium,
                            color = uTVColors.TextPrimary,
                        )
                        Text(
                            text = "$count canales",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) uTVColors.AccentElectricBlue else uTVColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ── Filter Chip Composable ───────────────────────────────────────────────────

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) uTVColors.AccentElectricBlue else uTVColors.SurfaceCard,
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) uTVColors.TextPrimary else uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

// ── Loading & Error States ───────────────────────────────────────────────────

@Composable
private fun LoadingView() {
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
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Sintonizando catálogo FAST...",
                style = MaterialTheme.typography.bodyMedium,
                color = uTVColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No se pudieron cargar los canales",
                style = MaterialTheme.typography.titleLarge,
                color = uTVColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = uTVColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = uTVColors.AccentElectricBlue,
                    contentColor = uTVColors.TextPrimary,
                ),
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = "Reintentar conexión")
            }
        }
    }
}

// ── Country Flag Helper ──────────────────────────────────────────────────────

fun countryDisplayName(code: String): String = when (code.uppercase()) {
    "AR" -> "🇦🇷 Argentina"
    "BO" -> "🇧🇴 Bolivia"
    "CL" -> "🇨🇱 Chile"
    "CO" -> "🇨🇴 Colombia"
    "CR" -> "🇨🇷 Costa Rica"
    "DO" -> "🇩🇴 Rep. Dominicana"
    "EC" -> "🇪🇨 Ecuador"
    "ES" -> "🇪🇸 España"
    "GT" -> "🇬🇹 Guatemala"
    "HN" -> "🇭🇳 Honduras"
    "MX" -> "🇲🇽 México"
    "NI" -> "🇳🇮 Nicaragua"
    "PA" -> "🇵🇦 Panamá"
    "PE" -> "🇵🇪 Perú"
    "PY" -> "🇵🇾 Paraguay"
    "SV" -> "🇸🇻 El Salvador"
    "UY" -> "🇺🇾 Uruguay"
    "VE" -> "🇻🇪 Venezuela"
    else -> "🌐 $code"
}
