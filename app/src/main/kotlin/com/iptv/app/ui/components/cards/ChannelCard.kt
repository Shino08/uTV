package com.iptv.app.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.theme.LocalIsTV
import com.iptv.app.ui.theme.uTVColors

/**
 * Reusable 16:9 channel card following guidelines.md §4.1.
 *
 * Mobile: responds to tap + shows hover shadow.
 * TV: scale to 1.08f and illuminated border on D-Pad focus.
 *
 * @param channel    Domain model to display.
 * @param onClick    Invoked when the user selects the card.
 * @param onFavorite Invoked when the user taps the star icon.
 * @param modifier   Optional layout modifier from caller.
 */
@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: (() -> Unit)? = null,
) {
    val isTV = LocalIsTV.current
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isTV && isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "card_scale",
    )

    val borderColor = when {
        isFocused && channel.category.name == "ANIMATION" -> uTVColors.AccentAnimeOrange
        isFocused -> uTVColors.AccentElectricBlue
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            }
            .focusable()
            .clickable(onClick = onClick),
        color = uTVColors.SurfaceCard,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Channel logo ─────────────────────────────────────────────────
            ChannelLogoOrInitials(
                name = channel.name,
                logoUrl = channel.logoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )

            // ── Bottom gradient scrim ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, uTVColors.SurfaceCard),
                        )
                    ),
            )

            // ── Top row: Dial badge + Country badge ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DialBadge(number = channel.dialNumber)
                CountryBadge(countryCode = channel.countryCode)
            }

            // ── Bottom row: Name + Category + Favorite ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = uTVColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = uTVColors.TextSecondary,
                        maxLines = 1,
                    )
                }
                FavoriteIcon(
                    isFavorite = channel.isFavorite,
                    onClick = onFavorite,
                )
            }
        }
    }
}

// ── Private sub-components ────────────────────────────────────────────────────

@Composable
private fun ChannelLogoOrInitials(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (!logoUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val imageRequest = remember(logoUrl) {
            ImageRequest.Builder(context)
                .data(logoUrl)
                .size(240, 135) // Downsample to card bounds to save memory and eliminate lag
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        InitialsPlaceholder(name = name, modifier = modifier)
    }
}

@Composable
private fun InitialsPlaceholder(name: String, modifier: Modifier = Modifier) {
    val initials = name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = initials.ifBlank { "?" },
            style = MaterialTheme.typography.headlineLarge,
            color = uTVColors.TextSecondary,
        )
    }
}

@Composable
private fun DialBadge(number: Int) {
    Surface(
        color = uTVColors.BackgroundPrimary.copy(alpha = 0.75f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = MaterialTheme.typography.labelSmall.fontSize * 2,
            ),
            color = uTVColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CountryBadge(countryCode: String) {
    if (countryCode == "XX") return
    Surface(
        color = uTVColors.BackgroundPrimary.copy(alpha = 0.75f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = countryCode,
            style = MaterialTheme.typography.labelSmall,
            color = uTVColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FavoriteIcon(isFavorite: Boolean, onClick: () -> Unit) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        tint = if (isFavorite) uTVColors.FavoriteGold else uTVColors.TextSecondary,
        modifier = Modifier
            .size(20.dp)
            .clickable(onClick = onClick),
    )
}
