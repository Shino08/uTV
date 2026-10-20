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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
 * Android TV Quick Channel Guide Overlay (Left Panel — 35% screen width):
 * - D-Pad remote accessible: navigates UP/DOWN smoothly while current channel continues streaming.
 * - Auto-focuses on the currently playing channel row upon opening.
 * - Selecting any channel with OK (DPAD_CENTER / ENTER) tunes that channel immediately.
 * - Pressing D-Pad RIGHT or BACK closes the panel and returns focus to the player.
 * - Fast category filtering tabs above the list.
 */
@Composable
fun MiniGuideOverlay(
    isVisible: Boolean,
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeCategory by remember { mutableStateOf<Category?>(null) }

    val filteredChannels = remember(channels, activeCategory) {
        if (activeCategory == null) channels
        else channels.filter { it.category == activeCategory }
    }

    val listState = rememberLazyListState()
    val activeItemFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }

    // Auto-scroll and focus the currently playing channel when the guide opens
    LaunchedEffect(isVisible, activeCategory) {
        if (isVisible && filteredChannels.isNotEmpty()) {
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

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.36f)
                .background(color = uTVColors.SurfaceGlassOSD)
                .border(
                    width = 1.dp,
                    color = uTVColors.BorderSubtle,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Guía Rápida",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = uTVColors.TextPrimary,
                        )
                        Text(
                            text = "${filteredChannels.size} canales • Pulsa OK para cambiar",
                            style = MaterialTheme.typography.labelSmall,
                            color = uTVColors.TextSecondary,
                        )
                    }
                    Text(
                        text = "▶ / BACK para salir",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = uTVColors.TextSecondary,
                    )
                }

                // ── Category filter tabs ──────────────────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        MiniGuideCategoryTab(
                            label = "Todos",
                            isSelected = activeCategory == null,
                            onClick = { activeCategory = null },
                            onClose = onClose,
                        )
                    }
                    items(Category.entries.filter { it != Category.UNDEFINED }.size) { index ->
                        val category = Category.entries.filter { it != Category.UNDEFINED }[index]
                        MiniGuideCategoryTab(
                            label = category.displayName,
                            isSelected = activeCategory == category,
                            onClick = { activeCategory = category },
                            onClose = onClose,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

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
                            isActive -> activeItemFocusRequester
                            index == 0 -> firstItemFocusRequester
                            else -> null
                        }

                        MiniGuideChannelRow(
                            channel = channel,
                            isActive = isActive,
                            focusRequester = itemFocusRequester,
                            onClick = { onChannelSelected(channel) },
                            onClose = onClose,
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
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        onClose()
                        return@onKeyEvent true
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
            // Dial number badge
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

            // Channel Logo
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

            // Channel Name & Live state
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

            // Favorite star badge
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
