package com.iptv.app.ui.tv

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.components.osd.MiniGuideOverlay
import com.iptv.app.ui.components.osd.SignalErrorOverlay
import com.iptv.app.ui.components.osd.SimpleTvInfoBanner
import com.iptv.app.ui.shared.PlayerViewModel
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay

/**
 * Android TV / Google TV Player Screen — implements guidelines.md §3.4, §4.3, §4.4, §5:
 * - Fullscreen Media3 ExoPlayer surface.
 * - 100% Remote D-Pad Navigation & Shortcuts:
 *     - D-Pad UP / Channel UP: zap next channel (dial + 1).
 *     - D-Pad DOWN / Channel DOWN: zap previous channel (dial - 1).
 *     - D-Pad LEFT: toggle translucent MiniGuideOverlay (35% screen width) without cutting video.
 *     - D-Pad CENTER / ENTER: show SimpleTvInfoBanner (3-block DirecTV/SimpleTV OSD).
 *     - Direct Numeric Keypad (0–9): accumulates digits (e.g., 301) with visual OSD and debounced tuning.
 *     - BACK: close MiniGuide if open, else return to TvHomeScreen.
 * - SignalErrorOverlay: handles 403 (GeoBlocked), 404/Timeout (OffAir), and generic player errors without crashing player.
 */
@Composable
fun TvPlayerScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // Direct numeric dial keypad tuning state (e.g. typing "301" on TV remote)
    var dialedDigits by remember { mutableStateOf("") }
    var dialFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var favoriteFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss favorite feedback toast
    LaunchedEffect(favoriteFeedbackMessage) {
        if (favoriteFeedbackMessage != null) {
            delay(2000)
            favoriteFeedbackMessage = null
        }
    }

    // Initial channel load
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    // Auto-request focus on the root container so D-Pad keys are immediately intercepted
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounced direct dial execution: when user types digits, wait 1.8s then zap
    LaunchedEffect(dialedDigits) {
        if (dialedDigits.isNotEmpty()) {
            delay(1800)
            val targetDial = dialedDigits.toIntOrNull()
            if (targetDial != null) {
                val match = allChannels.firstOrNull { it.dialNumber == targetDial }
                if (match != null) {
                    viewModel.playChannel(match)
                    dialFeedbackMessage = null
                } else {
                    dialFeedbackMessage = "Canal $targetDial no encontrado"
                    delay(2000)
                    dialFeedbackMessage = null
                }
            }
            dialedDigits = ""
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Pause/Resume on app backgrounding and fully stop stream when leaving player
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.pausePlayback()
                Lifecycle.Event.ON_START -> viewModel.resumePlayback()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPlayback()
        }
    }

    val handleBack = {
        viewModel.stopPlayback()
        onBack()
    }

    // Handle physical TV Back button
    BackHandler {
        if (uiState.isMiniGuideVisible) {
            viewModel.closeMiniGuide()
            focusRequester.requestFocus()
        } else if (dialedDigits.isNotEmpty()) {
            dialedDigits = ""
            dialFeedbackMessage = null
        } else {
            handleBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false

                val keyCode = keyEvent.nativeKeyEvent.keyCode

                // Numeric keys (0–9) for direct dial tuning
                if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                    val digit = keyCode - KeyEvent.KEYCODE_0
                    if (dialedDigits.length < 3) {
                        dialedDigits += digit.toString()
                        dialFeedbackMessage = null
                    }
                    return@onKeyEvent true
                }

                // D-Pad and channel controls
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (!uiState.isMiniGuideVisible) {
                            viewModel.zapNext()
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (!uiState.isMiniGuideVisible) {
                            viewModel.zapPrevious()
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!uiState.isMiniGuideVisible) {
                            viewModel.toggleMiniGuide()
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (uiState.isMiniGuideVisible) {
                            viewModel.closeMiniGuide()
                            focusRequester.requestFocus()
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (!uiState.isMiniGuideVisible) {
                            val current = uiState.currentChannel
                            if (current != null) {
                                val willBeFavorite = !current.isFavorite
                                viewModel.toggleFavorite()
                                viewModel.showOsd()
                                favoriteFeedbackMessage = if (willBeFavorite) "★ Agregado a favoritos" else "☆ Eliminado de favoritos"
                            }
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_BACK -> {
                        if (uiState.isMiniGuideVisible) {
                            viewModel.closeMiniGuide()
                            focusRequester.requestFocus()
                            true
                        } else if (dialedDigits.isNotEmpty()) {
                            dialedDigits = ""
                            dialFeedbackMessage = null
                            true
                        } else {
                            handleBack()
                            true
                        }
                    }

                    else -> false
                }
            },
    ) {
        // ── 1. Media3 ExoPlayer Video Surface ────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.player
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── 2. Buffering Spinner ─────────────────────────────────────────────
        if (uiState.isBuffering && uiState.errorState == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = uTVColors.AccentElectricBlue,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 5.dp,
                )
            }
        }

        // ── 3. Signal Error Overlay (403 GeoBlocked, 404 OffAir, PlayerError) ─
        SignalErrorOverlay(
            errorState = uiState.errorState,
            modifier = Modifier.fillMaxSize(),
        )

        // ── 4. Direct Dial Number Badge Overlay (Top Right) ──────────────────
        AnimatedVisibility(
            visible = dialedDigits.isNotEmpty() || dialFeedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 48.dp, top = 36.dp),
        ) {
            Surface(
                color = uTVColors.SurfaceGlassOSD,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, uTVColors.AccentElectricBlue),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dialedDigits.isNotEmpty()) {
                        Text(
                            text = "Canal ",
                            style = MaterialTheme.typography.titleMedium,
                            color = uTVColors.TextSecondary,
                        )
                        Text(
                            text = dialedDigits,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = uTVColors.AccentElectricBlue,
                            ),
                        )
                    } else if (dialFeedbackMessage != null) {
                        Text(
                            text = dialFeedbackMessage ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = uTVColors.LiveBadgeRed,
                        )
                    }
                }
            }
        }

        // ── 4b. Favorite Feedback Badge Overlay (Top Center) ─────────────────
        AnimatedVisibility(
            visible = favoriteFeedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp),
        ) {
            Surface(
                color = uTVColors.SurfaceGlassOSD,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, uTVColors.FavoriteGold),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = favoriteFeedbackMessage ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.FavoriteGold,
                    )
                }
            }
        }

        // ── 5. SimpleTvInfoBanner (Bottom 3-block OSD) ────────────────────────
        uiState.currentChannel?.let { currentChannel ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                SimpleTvInfoBanner(
                    channel = currentChannel,
                    quality = uiState.quality,
                    isVisible = uiState.isOsdVisible,
                    onDismiss = { viewModel.dismissOsd() },
                    onFavoriteClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── 6. MiniGuideOverlay (36% Left Translucent Panel) ─────────────────
        MiniGuideOverlay(
            isVisible = uiState.isMiniGuideVisible,
            channels = allChannels,
            currentChannelId = uiState.currentChannel?.id,
            onChannelSelected = { selected ->
                viewModel.playChannel(selected)
                viewModel.closeMiniGuide()
                focusRequester.requestFocus()
            },
            onClose = {
                viewModel.closeMiniGuide()
                focusRequester.requestFocus()
            },
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}
