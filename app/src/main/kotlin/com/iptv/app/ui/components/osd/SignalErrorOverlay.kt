package com.iptv.app.ui.components.osd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iptv.app.core.model.StreamErrorState
import com.iptv.app.ui.theme.uTVColors

/**
 * Overlay composable rendered on top of the player when a [StreamErrorState] is active.
 *
 * Never releases the ExoPlayer — the user can keep zapping.
 *
 * States:
 * - [StreamErrorState.GeoBlocked] → Lock icon + "Señal bloqueada en tu región"
 * - [StreamErrorState.OffAir]     → No-signal icon + "Señal fuera del aire"
 * - [StreamErrorState.PlayerError]→ Generic error with optional message
 *
 * @param errorState  Current error (null hides the overlay entirely).
 * @param modifier    Optional layout modifier.
 */
@Composable
fun SignalErrorOverlay(
    errorState: StreamErrorState?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = errorState != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(uTVColors.SurfaceGlassOSD),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = uTVColors.BackgroundSecondary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = uTVColors.BorderSubtle,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 40.dp, vertical = 32.dp),
            ) {
                when (errorState) {
                    is StreamErrorState.GeoBlocked -> {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = uTVColors.LiveBadgeRed,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Señal bloqueada en tu región",
                            style = MaterialTheme.typography.titleLarge,
                            color = uTVColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Este canal no está disponible desde tu ubicación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = uTVColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is StreamErrorState.OffAir, null -> {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = uTVColors.TextSecondary,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Señal fuera del aire",
                            style = MaterialTheme.typography.titleLarge,
                            color = uTVColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "El canal no está transmitiendo en este momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = uTVColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is StreamErrorState.WifiOnlyRestricted -> {
                        Icon(
                            imageVector = Icons.Default.NetworkWifi,
                            contentDescription = null,
                            tint = uTVColors.AccentAnimeOrange,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Modo Solo Wi-Fi activo",
                            style = MaterialTheme.typography.titleLarge,
                            color = uTVColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Conéctate a una red Wi-Fi o desactiva la opción en Ajustes para reproducir con datos móviles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = uTVColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is StreamErrorState.PlayerError -> {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = uTVColors.TextSecondary,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Error de reproducción",
                            style = MaterialTheme.typography.titleLarge,
                            color = uTVColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        errorState.message?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = uTVColors.TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
