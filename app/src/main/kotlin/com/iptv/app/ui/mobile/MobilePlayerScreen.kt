package com.iptv.app.ui.mobile

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.iptv.app.core.model.AspectRatioPreference
import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import com.iptv.app.ui.components.osd.SignalErrorOverlay
import com.iptv.app.ui.components.osd.SimpleTvInfoBanner
import com.iptv.app.ui.shared.PlayerViewModel
import com.iptv.app.ui.theme.uTVColors

/**
 * Mobile Player Screen — implements guidelines.md §3.4, §4.3, §4.4, §5:
 * - Fullscreen Media3 ExoPlayer surface.
 * - Vertical swipe gestures for fast zapping: Swipe UP = Dial + 1, Swipe DOWN = Dial - 1.
 * - Tap anywhere to toggle OSD (auto-dismisses after 4s).
 * - Translucent Top Bar: Back, Channel info, MiniGuide toggle, Picture-in-Picture trigger.
 * - Bottom OSD: SimpleTvInfoBanner (3 blocks: dial/logo, metadata/live badge, quality/time/favorite).
 * - Mobile MiniGuide BottomSheet: browse all dials (100–799) with category tabs without stopping video.
 * - SignalErrorOverlay: handles 403 (GeoBlocked), 404/Timeout (OffAir), and generic player errors without crashing player.
 */
/**
 * Supported video scaling modes on mobile devices (portrait & landscape).
 */
enum class MobileResizeMode(val mode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Ajustar (16:9)"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Llenar pantalla"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Estirar"),
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun MobilePlayerScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()

    val userSettings by viewModel.userSettings.collectAsState()
    val initialResizeMode = remember(userSettings.aspectRatio) {
        when (userSettings.aspectRatio) {
            AspectRatioPreference.FIT -> MobileResizeMode.FIT
            AspectRatioPreference.FILL -> MobileResizeMode.FILL
            AspectRatioPreference.ZOOM -> MobileResizeMode.ZOOM
        }
    }

    var isInPipMode by remember { mutableStateOf(false) }
    var resizeMode by remember(initialResizeMode) { mutableStateOf(initialResizeMode) }
    var resizeFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val toggleResizeMode: () -> Unit = {
        resizeMode = when (resizeMode) {
            MobileResizeMode.FIT -> MobileResizeMode.ZOOM
            MobileResizeMode.ZOOM -> MobileResizeMode.FILL
            MobileResizeMode.FILL -> MobileResizeMode.FIT
        }
        resizeFeedbackMessage = resizeMode.label
    }

    LaunchedEffect(resizeFeedbackMessage) {
        if (resizeFeedbackMessage != null) {
            delay(1500)
            resizeFeedbackMessage = null
        }
    }

    val toggleOrientation: () -> Unit = {
        val activity = findActivity(context)
        if (activity != null) {
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = findActivity(context)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Load initial channel when navigated to
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    val handleBack = {
        viewModel.stopPlayback()
        onBack()
    }

    // Monitor Lifecycle events and Picture-in-Picture mode transitions
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val activity = findActivity(context)
            val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.isInPictureInPictureMode == true
            } else false
            isInPipMode = inPip

            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (!inPip) {
                        viewModel.pausePlayback()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    viewModel.resumePlayback()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val activity = findActivity(context)
            val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.isInPictureInPictureMode == true
            } else false
            if (!inPip) {
                viewModel.stopPlayback()
            }
        }
    }

    // Handle back button: if MiniGuide open, close it first; otherwise navigate back
    BackHandler {
        if (uiState.isMiniGuideVisible) {
            viewModel.closeMiniGuide()
        } else {
            handleBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── 1. Video Surface & Gesture Layer ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isInPipMode) {
                                viewModel.toggleOsd()
                            }
                        },
                        onDoubleTap = {
                            if (!isInPipMode) {
                                toggleResizeMode()
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 120f
                            if (totalDragY < -threshold) {
                                // Dragged UP -> next dial
                                viewModel.zapNext()
                            } else if (totalDragY > threshold) {
                                // Dragged DOWN -> previous dial
                                viewModel.zapPrevious()
                            }
                            totalDragY = 0f
                        },
                        onDragCancel = {
                            totalDragY = 0f
                        },
                    )
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                        resizeMode = resizeMode.mode
                        keepScreenOn = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = viewModel.player
                    playerView.resizeMode = resizeMode.mode
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── 2. Buffering Spinner ─────────────────────────────────────────────
        if (uiState.isBuffering && uiState.errorState == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = uTVColors.AccentElectricBlue,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 4.dp,
                )
            }
        }

        // ── 3. Signal Error Overlay (403, 404, Stream errors) ────────────────
        SignalErrorOverlay(
            errorState = uiState.errorState,
            modifier = Modifier.fillMaxSize(),
        )

        // ── 4. Ephemeral Resize Mode Feedback Badge ──────────────────────────
        AnimatedVisibility(
            visible = resizeFeedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                color = uTVColors.SurfaceGlassOSD,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, uTVColors.BorderSubtle),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = resizeFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.TextPrimary,
                    )
                }
            }
        }

        // ── 5. Interactive Overlays (Hidden in Picture-in-Picture) ───────────
        if (!isInPipMode) {
            // Top Bar Overlay
            AnimatedVisibility(
                visible = uiState.isOsdVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                PlayerTopBar(
                    channel = uiState.currentChannel,
                    onBack = handleBack,
                    onMiniGuideClick = { viewModel.toggleMiniGuide() },
                    onPipClick = { enterPipMode(context) },
                    onZapPrevious = { viewModel.zapPrevious() },
                    onZapNext = { viewModel.zapNext() },
                    onToggleResizeMode = toggleResizeMode,
                    onToggleOrientation = toggleOrientation,
                )
            }

            // Bottom OSD Banner (SimpleTvInfoBanner)
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
        }

        // ── 5. Mobile MiniGuide Bottom Sheet ─────────────────────────────────
        if (uiState.isMiniGuideVisible && !isInPipMode) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeMiniGuide() },
                sheetState = sheetState,
                containerColor = uTVColors.SurfaceGlassOSD,
                contentColor = uTVColors.TextPrimary,
            ) {
                MobileMiniGuideContent(
                    channels = allChannels,
                    currentChannelId = uiState.currentChannel?.id,
                    onChannelSelected = { selected ->
                        viewModel.playChannel(selected)
                        viewModel.closeMiniGuide()
                    },
                    onFavoriteToggle = { ch ->
                        if (ch.id == uiState.currentChannel?.id) {
                            viewModel.toggleFavorite()
                        }
                    },
                )
            }
        }
    }
}

// ── Top Bar Overlay ──────────────────────────────────────────────────────────

@Composable
private fun PlayerTopBar(
    channel: Channel?,
    onBack: () -> Unit,
    onMiniGuideClick: () -> Unit,
    onPipClick: () -> Unit,
    onZapPrevious: () -> Unit,
    onZapNext: () -> Unit,
    onToggleResizeMode: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        uTVColors.BackgroundPrimary.copy(alpha = 0.95f),
                        uTVColors.BackgroundPrimary.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Back button + Dial/Channel Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = uTVColors.TextPrimary,
                    )
                }

                if (channel != null) {
                    Surface(
                        color = uTVColors.SurfaceCard,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = channel.dialNumber.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = uTVColors.AccentElectricBlue,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = uTVColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Right: Resize, Rotate, Quick Zap, MiniGuide & PiP buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Resize Mode toggle (Fit 16:9 / Zoom / Fill)
                IconButton(
                    onClick = onToggleResizeMode,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Modo de escalado",
                        tint = uTVColors.TextPrimary,
                    )
                }

                // Screen Orientation toggle (Landscape / Portrait)
                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Rotar pantalla",
                        tint = uTVColors.TextPrimary,
                    )
                }

                // Zap Down (Previous dial)
                IconButton(
                    onClick = onZapPrevious,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Canal anterior",
                        tint = uTVColors.TextPrimary,
                    )
                }

                // Zap Up (Next dial)
                IconButton(
                    onClick = onZapNext,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Canal siguiente",
                        tint = uTVColors.TextPrimary,
                    )
                }

                // MiniGuide trigger
                IconButton(
                    onClick = onMiniGuideClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Mini Guía",
                        tint = uTVColors.AccentElectricBlue,
                    )
                }

                // Picture-in-Picture trigger (Android 8.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(
                        onClick = onPipClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture-in-Picture",
                            tint = uTVColors.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

// ── Mobile MiniGuide Bottom Sheet Content ────────────────────────────────────

@Composable
private fun MobileMiniGuideContent(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
) {
    var activeCategory by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }

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

    // Auto-scroll to currently playing channel
    LaunchedEffect(currentChannelId) {
        if (currentChannelId != null) {
            val idx = filteredChannels.indexOfFirst { it.id == currentChannelId }
            if (idx >= 0) listState.scrollToItem(idx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Guía de Canales en Vivo",
                style = MaterialTheme.typography.titleLarge,
                color = uTVColors.TextPrimary,
            )
            Text(
                text = "${filteredChannels.size} canales",
                style = MaterialTheme.typography.labelSmall,
                color = uTVColors.AccentElectricBlue,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Buscar canal o dial (ej. HBO, Anime, 301)...",
                    style = MaterialTheme.typography.labelSmall,
                    color = uTVColors.TextSecondary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
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
                unfocusedBorderColor = uTVColors.BorderSubtle,
                focusedTextColor = uTVColors.TextPrimary,
                unfocusedTextColor = uTVColors.TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        // Horizontal Category Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                GuideCategoryChip(
                    label = "Todos",
                    isSelected = activeCategory == null,
                    onClick = { activeCategory = null },
                )
            }
            items(Category.entries.filter { it != Category.UNDEFINED }) { cat ->
                GuideCategoryChip(
                    label = cat.displayName,
                    isSelected = activeCategory == cat,
                    onClick = {
                        activeCategory = if (activeCategory == cat) null else cat
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Channels list ordered by dialNumber
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(filteredChannels, key = { it.id }) { channel ->
                val isPlaying = channel.id == currentChannelId
                Surface(
                    color = if (isPlaying) uTVColors.AccentElectricBlue.copy(alpha = 0.25f) else uTVColors.SurfaceCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isPlaying) uTVColors.AccentElectricBlue else uTVColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChannelSelected(channel) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Dial Number
                        Text(
                            text = channel.dialNumber.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPlaying) uTVColors.AccentElectricBlue else uTVColors.TextSecondary,
                            ),
                            modifier = Modifier.width(42.dp),
                        )

                        // Channel Logo or Placeholder
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(uTVColors.BackgroundPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp),
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

                        // Name + Country
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = uTVColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = uTVColors.TextSecondary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "• ${channel.countryCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = uTVColors.AccentElectricBlue,
                                )
                            }
                        }

                        // Favorite star
                        if (channel.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorito",
                                tint = uTVColors.FavoriteGold,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onFavoriteToggle(channel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) uTVColors.AccentElectricBlue else uTVColors.BackgroundSecondary,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

// ── PiP Helper ───────────────────────────────────────────────────────────────

private fun enterPipMode(context: Context) {
    val activity = findActivity(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(params)
    }
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
