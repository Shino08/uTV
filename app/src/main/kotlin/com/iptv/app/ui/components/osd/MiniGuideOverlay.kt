package com.iptv.app.ui.components.osd

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay

/**
 * Android TV & Mobile Quick Channel Guide Overlay (Left Panel — 36% screen width):
 * - Top magnifying glass icon (Search): pressing D-Pad LEFT again while inside the guide
 *   jumps directly to the search bar for instant channel lookup during streaming.
 * - D-Pad remote accessible: navigates UP/DOWN smoothly while current channel continues streaming.
 */
@Composable
fun MiniGuideOverlay(
    isVisible: Boolean,
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onClose: () -> Unit = {},
    searchJumpTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    var activeCategory by remember { mutableStateOf<Category?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val searchInputFocusRequester = remember { FocusRequester() }
    val searchButtonFocusRequester = remember { FocusRequester() }
    val activeItemFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }

    val filteredChannels = remember(channels, activeCategory, searchQuery) {
        channels.filter { channel ->
            val matchesCat = activeCategory == null || channel.category == activeCategory
            val matchesQuery = searchQuery.isBlank() ||
                channel.name.contains(searchQuery, ignoreCase = true) ||
                channel.dialNumber.toString().contains(searchQuery) ||
                channel.countryCode.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val listState = rememberLazyListState()

    // Reset search when overlay closes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            isSearchExpanded = false
            searchQuery = ""
        }
    }

    // When user presses D-Pad LEFT again while guide is already open, jump to the Search Magnifying Glass
    LaunchedEffect(searchJumpTrigger) {
        if (isVisible && searchJumpTrigger > 0) {
            isSearchExpanded = true
            delay(80)
            try {
                searchInputFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-scroll and focus the currently playing channel when the guide opens
    LaunchedEffect(isVisible, activeCategory) {
        if (isVisible && !isSearchExpanded && filteredChannels.isNotEmpty()) {
            val targetIndex = filteredChannels.indexOfFirst { it.id == currentChannelId }
            if (targetIndex >= 0) {
                listState.scrollToItem(maxOf(0, targetIndex - 2))
            } else {
                listState.scrollToItem(0)
            }
            delay(120)
            try {
                if (targetIndex >= 0) {
                    activeItemFocusRequester.requestFocus()
                } else {
                    firstItemFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }
    }

    val activateSearch = {
        isSearchExpanded = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.38f)
                .background(color = uTVColors.SurfaceGlassOSD)
                .border(
                    width = 1.dp,
                    color = uTVColors.BorderSubtle,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header + Magnifying Glass Button ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guía Rápida",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = uTVColors.TextPrimary,
                        )
                        Text(
                            text = "Pulsa ◀ otra vez para buscar • ▶ salir",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = uTVColors.TextSecondary,
                        )
                    }

                    // Magnifying Glass Button (Top Right of Guide)
                    var isSearchBtnFocused by remember { mutableStateOf(false) }
                    Surface(
                        color = when {
                            isSearchBtnFocused -> uTVColors.AccentAnimeOrange
                            isSearchExpanded -> uTVColors.AccentElectricBlue
                            else -> uTVColors.SurfaceCard
                        },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSearchBtnFocused) 2.dp else 1.dp,
                            color = if (isSearchBtnFocused) Color.White else uTVColors.AccentElectricBlue,
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (isSearchBtnFocused) 1.1f else 1f)
                            .focusRequester(searchButtonFocusRequester)
                            .onFocusChanged { isSearchBtnFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) searchQuery = ""
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar canal",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // ── Expandable Quick Search Input ─────────────────────────────
                AnimatedVisibility(visible = isSearchExpanded) {
                    LaunchedEffect(Unit) {
                        delay(60)
                        try {
                            searchInputFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Canal o dial (ej. HBO, Anime, 301)...",
                                style = MaterialTheme.typography.labelSmall,
                                color = uTVColors.TextSecondary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = uTVColors.AccentElectricBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = uTVColors.TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = uTVColors.SurfaceCard,
                            unfocusedContainerColor = uTVColors.SurfaceCard,
                            focusedBorderColor = uTVColors.AccentAnimeOrange,
                            unfocusedBorderColor = uTVColors.AccentElectricBlue,
                            focusedTextColor = uTVColors.TextPrimary,
                            unfocusedTextColor = uTVColors.TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .focusRequester(searchInputFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
                                    filteredChannels.isNotEmpty()
                                ) {
                                    try {
                                        firstItemFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                    true
                                } else false
                            },
                    )
                }

                // ── Category filter tabs ──────────────────────────────────────
                val visibleCategories = remember {
                    Category.entries.filter { it != Category.UNDEFINED }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    item {
                        MiniGuideCategoryTab(
                            label = "Todos",
                            isSelected = activeCategory == null,
                            onClick = { activeCategory = null },
                            onClose = onClose,
                            onJumpToSearch = activateSearch,
                        )
                    }
                    items(visibleCategories.size) { index ->
                        val category = visibleCategories[index]
                        MiniGuideCategoryTab(
                            label = category.displayName,
                            isSelected = activeCategory == category,
                            onClick = { activeCategory = if (activeCategory == category) null else category },
                            onClose = onClose,
                            onJumpToSearch = activateSearch,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Channel list ──────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    itemsIndexed(
                        items = filteredChannels,
                        key = { _, channel -> channel.id },
                    ) { index, channel ->
                        val isActive = channel.id == currentChannelId
                        val itemFocusRequester = when {
                            index == 0 -> firstItemFocusRequester
                            isActive -> activeItemFocusRequester
                            else -> null
                        }

                        MiniGuideChannelRow(
                            channel = channel,
                            isActive = isActive,
                            focusRequester = itemFocusRequester,
                            onClick = { onChannelSelected(channel) },
                            onClose = onClose,
                            onJumpToSearch = {
                                isSearchExpanded = true
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Private sub-components ────────────────────────────────────────────────────

@Composable
private fun MiniGuideCategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onJumpToSearch: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = when {
            isFocused -> uTVColors.AccentElectricBlue.copy(alpha = 0.40f)
            isSelected -> uTVColors.AccentElectricBlue
            else -> uTVColors.SurfaceCard
        },
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) uTVColors.AccentElectricBlue else Color.Transparent,
        ),
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            onJumpToSearch()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected || isFocused) Color.White else uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MiniGuideChannelRow(
    channel: Channel,
    isActive: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onJumpToSearch: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val rowModifier = Modifier
        .fillMaxWidth()
        .scale(if (isFocused) 1.03f else 1.0f)
        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .clickable(onClick = onClick)
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        onJumpToSearch()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onClose()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            } else false
        }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isFocused -> uTVColors.AccentElectricBlue.copy(alpha = 0.35f)
            isActive -> uTVColors.AccentElectricBlue.copy(alpha = 0.15f)
            else -> uTVColors.SurfaceCard.copy(alpha = 0.5f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else if (isActive) 1.dp else 0.dp,
            color = when {
                isFocused -> uTVColors.AccentElectricBlue
                isActive -> uTVColors.AccentElectricBlue.copy(alpha = 0.6f)
                else -> Color.Transparent
            },
        ),
        modifier = rowModifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Surface(
                color = if (isFocused || isActive) uTVColors.AccentElectricBlue else uTVColors.SurfaceCard,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = channel.dialNumber.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = if (isFocused || isActive) Color.White else uTVColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                modifier = Modifier.size(32.dp),
                color = uTVColors.BackgroundSecondary,
                shape = RoundedCornerShape(4.dp),
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(3.dp),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = uTVColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isFocused || isActive) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isFocused) Color.White else if (isActive) uTVColors.AccentElectricBlue else uTVColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isActive) {
                    Text(
                        text = "● EN VIVO AHORA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = uTVColors.AccentAnimeOrange,
                    )
                }
            }

            if (channel.isFavorite) {
                Text(
                    text = "★",
                    color = uTVColors.FavoriteGold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
