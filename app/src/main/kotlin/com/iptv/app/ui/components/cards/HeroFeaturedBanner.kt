package com.iptv.app.ui.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.theme.LocalIsTV
import com.iptv.app.ui.theme.uTVColors

/**
 * Cinematic hero banner displayed at the top of the Home screen.
 *
 * Layout (from back to front):
 * 1. Full-bleed logo / background image (AsyncImage, ContentScale.Crop)
 * 2. Left-to-right gradient scrim (#0B0C10 → transparent)
 * 3. Content column: category pill + channel name + country + CTA button + favorite
 *
 * Adapts text sizes for TV (LocalIsTV).
 *
 * @param channel         Featured channel (can be null while loading).
 * @param onPlayClick     User pressed the "Ver ahora" CTA.
 * @param onFavoriteClick User pressed the favorite icon.
 * @param modifier        External layout modifier.
 */
@Composable
fun HeroFeaturedBanner(
    channel: Channel?,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTV = LocalIsTV.current
    val bannerHeight = if (isTV) 320.dp else 220.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .background(uTVColors.BackgroundPrimary),
    ) {
        AnimatedVisibility(
            visible = channel != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            channel ?: return@AnimatedVisibility

            Box(modifier = Modifier.fillMaxSize()) {
                // ── Background art ────────────────────────────────────────────────
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // ── Gradient scrim (left side) ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to uTVColors.BackgroundPrimary,
                                    0.55f to uTVColors.BackgroundPrimary.copy(alpha = 0.85f),
                                    1.0f to Color.Transparent,
                                ),
                            )
                        ),
                )

                // ── Content ───────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = if (isTV) 48.dp else 24.dp, end = 120.dp)
                        .fillMaxWidth(0.6f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Category pill
                    Surface(
                        color = channel.category.heroAccentColor(),
                        shape = RoundedCornerShape(50.dp),
                    ) {
                        Text(
                            text = channel.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = uTVColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Channel name
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (isTV) 32.sp else 24.sp,
                        ),
                        color = uTVColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Country + Dial
                    Text(
                        text = "${channel.countryCode}  •  Canal ${channel.dialNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextSecondary,
                    )

                    Spacer(Modifier.height(16.dp))

                    // CTA row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onPlayClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = uTVColors.AccentElectricBlue,
                                contentColor = uTVColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "▶  Ver ahora",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Filled.Star
                                else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (channel.isFavorite) uTVColors.FavoriteGold
                                else uTVColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Returns an accent color for the hero category pill based on the channel's category. */
private fun com.iptv.app.core.model.Category.heroAccentColor(): Color = when (this) {
    com.iptv.app.core.model.Category.ANIMATION,
    com.iptv.app.core.model.Category.KIDS      -> uTVColors.AccentAnimeOrange
    com.iptv.app.core.model.Category.SPORTS    -> Color(0xFF1A6B1A)
    com.iptv.app.core.model.Category.MOVIES    -> Color(0xFF4B2BA0)
    com.iptv.app.core.model.Category.NEWS      -> uTVColors.TextSecondary
    com.iptv.app.core.model.Category.MUSIC     -> Color(0xFF8B1A8B)
    else                                        -> uTVColors.AccentElectricBlue
}
