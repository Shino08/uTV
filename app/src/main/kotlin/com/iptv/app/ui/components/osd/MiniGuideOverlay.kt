package com.iptv.app.ui.components.osd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.theme.uTVColors

/**
 * Mini-guide panel following guidelines.md §4.4.
 *
 * TV:     Left-side panel occupying 35% of screen width — slides in from the left.
 * Mobile: Thin left panel (same behavior) — BottomSheet implementation is in MobilePlayerScreen.
 *
 * Channel list is ordered by dialNumber (100→799).
 * Quick-filter tabs by category scroll horizontally above the list.
 * Current channel is highlighted with AccentElectricBlue border.
 *
 * @param isVisible         Whether the panel should be shown.
 * @param channels          Full sorted channel list.
 * @param currentChannelId  ID of the currently playing channel (for highlight).
 * @param onChannelSelected Invoked when user selects a channel to zap to.
 */
@Composable
fun MiniGuideOverlay(
    isVisible: Boolean,
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeCategory by remember { mutableStateOf<Category?>(null) }

    val filteredChannels = remember(channels, activeCategory) {
        if (activeCategory == null) channels
        else channels.filter { it.category == activeCategory }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to current channel when guide opens
    LaunchedEffect(isVisible, currentChannelId) {
        if (isVisible && currentChannelId != null) {
            val index = filteredChannels.indexOfFirst { it.id == currentChannelId }
            if (index >= 0) listState.animateScrollToItem(index)
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
                .fillMaxWidth(0.35f)   // 35% of screen width per spec
                .background(color = uTVColors.SurfaceGlassOSD)
                .border(
                    width = 1.dp,
                    color = uTVColors.BorderSubtle,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                Text(
                    text = "Guía de Canales",
                    style = MaterialTheme.typography.titleLarge,
                    color = uTVColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                // ── Category filter tabs ──────────────────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        CategoryTab(
                            label = "Todos",
                            isSelected = activeCategory == null,
                            onClick = { activeCategory = null },
                        )
                    }
                    items(Category.entries.filter { it != Category.UNDEFINED }) { category ->
                        CategoryTab(
                            label = category.displayName,
                            isSelected = activeCategory == category,
                            onClick = { activeCategory = category },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Channel list ──────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        MiniGuideChannelRow(
                            channel = channel,
                            isActive = channel.id == currentChannelId,
                            onClick = { onChannelSelected(channel) },
                        )
                    }
                }
            }
        }
    }
}

// ── Private sub-components ────────────────────────────────────────────────────

@Composable
private fun CategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) uTVColors.AccentElectricBlue
        else uTVColors.SurfaceCard,
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) uTVColors.TextPrimary else uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun MiniGuideChannelRow(
    channel: Channel,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) uTVColors.AccentElectricBlue.copy(alpha = 0.2f)
                else uTVColors.SurfaceCard.copy(alpha = 0f),
            )
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // Dial number
        Text(
            text = channel.dialNumber.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) uTVColors.AccentElectricBlue else uTVColors.TextSecondary,
            modifier = Modifier.width(32.dp),
        )

        // Logo
        Surface(
            modifier = Modifier.size(32.dp),
            color = uTVColors.BackgroundSecondary,
            shape = RoundedCornerShape(4.dp),
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Name
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) uTVColors.TextPrimary else uTVColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
