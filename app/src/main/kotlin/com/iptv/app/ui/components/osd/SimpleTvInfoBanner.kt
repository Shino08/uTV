package com.iptv.app.ui.components.osd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.app.core.model.Channel
import com.iptv.app.core.model.StreamQuality
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * OSD info bar following guidelines.md §4.3.
 *
 * Auto-dismisses after [visibleDurationMs] ms (default 4 s).
 * Slides in from the bottom + fade in; slides out + fades out.
 *
 * Three-block layout (Left | Center | Right):
 * - Left:   Dial number + channel logo
 * - Center: Channel name + category pill + country + EN VIVO badge
 * - Right:  Current time + quality badge + favorite icon
 *
 * @param channel          Currently playing channel.
 * @param quality          Detected stream quality (passed from PlayerViewModel).
 * @param isVisible        Whether the banner should be shown.
 * @param onDismiss        Called when the auto-hide timer fires.
 * @param onFavoriteClick  User pressed the favorite star.
 * @param visibleDurationMs Auto-dismiss delay in milliseconds.
 */
@Composable
fun SimpleTvInfoBanner(
    channel: Channel,
    quality: StreamQuality,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    visibleDurationMs: Long = 4_000L,
    modifier: Modifier = Modifier,
) {
    // Auto-dismiss timer
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(visibleDurationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = uTVColors.SurfaceGlassOSD,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
                .border(
                    width = 1.dp,
                    color = uTVColors.BorderSubtle,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── Block Left: Dial + Logo ───────────────────────────────────
                OsdLeftBlock(channel = channel)

                // ── Block Center: Name + Category + Country + LIVE ───────────
                OsdCenterBlock(
                    channel = channel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )

                // ── Block Right: Time + Quality + Favorite ───────────────────
                OsdRightBlock(
                    quality = quality,
                    isFavorite = channel.isFavorite,
                    onFavoriteClick = onFavoriteClick,
                )
            }
        }
    }
}

// ── Private blocks ────────────────────────────────────────────────────────────

@Composable
private fun OsdLeftBlock(channel: Channel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Giant dial number
        Text(
            text = channel.dialNumber.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = uTVColors.TextPrimary,
        )

        // Channel logo in rounded square container
        Surface(
            modifier = Modifier.size(56.dp),
            color = uTVColors.SurfaceCard,
            shape = RoundedCornerShape(8.dp),
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = uTVColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OsdCenterBlock(channel: Channel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Channel name
        Text(
            text = channel.name,
            style = MaterialTheme.typography.displayLarge,
            color = uTVColors.TextPrimary,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Category pill
            Surface(
                color = uTVColors.AccentElectricBlue.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50.dp),
            ) {
                Text(
                    text = channel.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = uTVColors.AccentElectricBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            // Country
            Text(
                text = channel.countryCode,
                style = MaterialTheme.typography.labelSmall,
                color = uTVColors.TextSecondary,
            )

            // EN VIVO badge
            LiveBadge()
        }
    }
}

@Composable
private fun OsdRightBlock(
    quality: StreamQuality,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Current time
        val time = remember {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        Text(
            text = time,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp),
            color = uTVColors.TextPrimary,
        )

        // Quality badge
        if (quality != StreamQuality.UNKNOWN) {
            Surface(
                color = uTVColors.AccentElectricBlue,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = quality.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = uTVColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        // Favorite icon
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) uTVColors.FavoriteGold else uTVColors.TextSecondary,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50.dp)),
        )
    }
}

/** Pulsing ● EN VIVO badge. */
@Composable
private fun LiveBadge() {
    Surface(
        color = uTVColors.LiveBadgeRed.copy(alpha = 0.9f),
        shape = RoundedCornerShape(50.dp),
    ) {
        Text(
            text = "● EN VIVO",
            style = MaterialTheme.typography.labelSmall,
            color = uTVColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
