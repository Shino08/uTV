package com.iptv.app.ui.components.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.iptv.app.ui.theme.LocalIsTV
import com.iptv.app.ui.theme.LocalReduceAnimations
import com.iptv.app.ui.theme.uTVColors

/**
 * Data class holding the visual configuration for each hub pill.
 * Keeps the composable stateless and easy to preview.
 */
data class CategoryHubPillData(
    val label: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val isSelected: Boolean = false,
)

/**
 * Horizontal pill button following guidelines.md §4.2.
 *
 * Each hub uses a distinct gradient derived from the design system.
 * On TV: focus triggers an elevated border matching the gradient end color.
 *
 * @param data       Visual configuration for this pill.
 * @param onClick    Invoked on tap / D-Pad select.
 * @param modifier   Optional external modifier.
 */
@Composable
fun CategoryHubPill(
    data: CategoryHubPillData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTV = LocalIsTV.current
    val reduceAnimations = LocalReduceAnimations.current
    var isFocused by remember { mutableStateOf(false) }

    val borderWidth by animateDpAsState(
        targetValue = when {
            data.isSelected -> 2.dp
            isTV && isFocused -> 2.dp
            else -> 0.dp
        },
        animationSpec = tween(if (reduceAnimations) 0 else 150),
        label = "pill_border",
    )

    val activeBorderColor = data.gradientColors.lastOrNull() ?: uTVColors.AccentElectricBlue

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .border(
                width = borderWidth,
                color = if (data.isSelected || isFocused) activeBorderColor else Color.Transparent,
                shape = RoundedCornerShape(50.dp),
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (data.isSelected || (isTV && isFocused)) {
                        data.gradientColors
                    } else {
                        listOf(
                            uTVColors.SurfaceCard,
                            uTVColors.BackgroundSecondary,
                        )
                    },
                ),
                shape = RoundedCornerShape(50.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = if (data.isSelected || (isTV && isFocused)) {
                    uTVColors.TextPrimary
                } else {
                    uTVColors.TextSecondary
                },
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = data.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (data.isSelected || (isTV && isFocused)) {
                    uTVColors.TextPrimary
                } else {
                    uTVColors.TextSecondary
                },
            )
        }
    }
}
