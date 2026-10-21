package com.iptv.app.ui.tv

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.app.core.model.AspectRatioPreference
import com.iptv.app.core.model.BufferDurationMode
import androidx.compose.ui.platform.LocalContext
import com.iptv.app.BuildConfig
import com.iptv.app.core.network.AppUpdateChecker
import com.iptv.app.core.network.AppUpdateState
import com.iptv.app.core.model.DecoderMode
import com.iptv.app.core.model.FontSizePreference
import com.iptv.app.core.model.StreamQualityPreference
import com.iptv.app.core.model.ThemePreference
import com.iptv.app.core.model.ZappingSpeedPreference
import com.iptv.app.ui.shared.SettingsViewModel
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay

@Composable
fun TvSettingsSection(
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    var showQualityDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showZappingDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showClearFavoritesConfirm by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showGitHubDialog by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2500)
            viewModel.dismissFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(uTVColors.BackgroundPrimary),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            item(key = "tv_settings_header") {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Configuración uTV",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "Ajustes de reproducción, búfer, aceleración de video y mantenimiento",
                        style = MaterialTheme.typography.bodyLarge,
                        color = uTVColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // ── Grupo 1: Reproducción y Video ────────────────────────────────
            item(key = "tv_group_playback") {
                TvSettingsGroupCard(title = "Reproducción y Video", icon = Icons.Default.PlayCircleOutline) {
                    TvSettingsItem(
                        title = "Calidad de stream",
                        value = settings.streamQuality.label,
                        onClick = { showQualityDialog = true },
                    )
                    TvSettingsItem(
                        title = "Tamaño de buffer",
                        value = settings.bufferMode.label,
                        onClick = { showBufferDialog = true },
                    )
                    TvSettingsItem(
                        title = "Modo de decodificación",
                        value = settings.decoderMode.label,
                        onClick = { showDecoderDialog = true },
                    )
                    TvSettingsItem(
                        title = "Relación de aspecto",
                        value = settings.aspectRatio.label,
                        onClick = { showAspectRatioDialog = true },
                    )
                    TvSettingsToggleItem(
                        title = "Auto-play siguiente canal",
                        subtitle = "Salta automáticamente al siguiente dial si la señal se desconecta",
                        checked = settings.autoPlayNext,
                        onToggle = { viewModel.setAutoPlayNext(!settings.autoPlayNext) },
                    )
                    TvSettingsToggleItem(
                        title = "Iniciar en silencio",
                        subtitle = "Sintoniza los canales con volumen silenciado",
                        checked = settings.startMuted,
                        onToggle = { viewModel.setStartMuted(!settings.startMuted) },
                    )
                }
            }

            // ── Grupo 2: Red y Rendimiento ───────────────────────────────────
            item(key = "tv_group_network") {
                TvSettingsGroupCard(title = "Red y Rendimiento", icon = Icons.Default.NetworkWifi) {
                    TvSettingsToggleItem(
                        title = "Modo bajo consumo de datos",
                        subtitle = "Limita la calidad a 480p para conexiones lentas o inestables",
                        checked = settings.lowDataMode,
                        onToggle = { viewModel.setLowDataMode(!settings.lowDataMode) },
                    )
                    TvSettingsItem(
                        title = "Timeout de conexión",
                        value = "${settings.connectionTimeoutSeconds} segundos",
                        onClick = { showTimeoutDialog = true },
                    )
                }
            }

            // ── Grupo 3: Apariencia y Controles ──────────────────────────────
            item(key = "tv_group_appearance") {
                TvSettingsGroupCard(title = "Apariencia y Controles", icon = Icons.Default.Palette) {
                    TvSettingsItem(
                        title = "Tema de la aplicación",
                        value = settings.themePreference.label,
                        onClick = { showThemeDialog = true },
                    )
                    TvSettingsItem(
                        title = "Tamaño de texto",
                        value = settings.fontSizePreference.label,
                        onClick = { showFontSizeDialog = true },
                    )
                    TvSettingsToggleItem(
                        title = "Reducir animaciones",
                        subtitle = "Aumenta la fluidez en TV Sticks y cajas con hardware modesto",
                        checked = settings.reduceAnimations,
                        onToggle = { viewModel.setReduceAnimations(!settings.reduceAnimations) },
                    )
                    TvSettingsItem(
                        title = "Velocidad de zapping",
                        value = settings.zappingSpeed.label,
                        onClick = { showZappingDialog = true },
                    )
                }
            }

            // ── Grupo 4: Privacidad y Mantenimiento ──────────────────────────
            item(key = "tv_group_privacy") {
                TvSettingsGroupCard(title = "Privacidad y Catálogo", icon = Icons.Default.Security) {
                    TvSettingsToggleItem(
                        title = "Modo privado",
                        subtitle = "No guardar canales en 'Recientemente vistos'",
                        checked = settings.privateMode,
                        onToggle = { viewModel.setPrivateMode(!settings.privateMode) },
                    )
                    TvSettingsActionItem(
                        title = "Borrar historial de visualización",
                        subtitle = "Reinicia la lista de canales vistos recientemente",
                        actionLabel = "Borrar",
                        isDestructive = true,
                        onClick = { showClearHistoryConfirm = true },
                    )
                    TvSettingsActionItem(
                        title = "Borrar favoritos",
                        subtitle = "Elimina todas las marcas de canales favoritos",
                        actionLabel = "Borrar",
                        isDestructive = true,
                        onClick = { showClearFavoritesConfirm = true },
                    )
                    TvSettingsActionItem(
                        title = "Sincronizar catálogo M3U",
                        subtitle = "Fuerza la recarga de canales desde la lista pública",
                        actionLabel = if (isSyncing) "Sincronizando..." else "Recargar",
                        isLoading = isSyncing,
                        onClick = { viewModel.forceSync() },
                    )
                }
            }

            // ── Grupo 5: Legal, Versión y Acerca de ──────────────────────────
            item(key = "tv_group_legal") {
                TvSettingsGroupCard(title = "Acerca de uTV y Legal", icon = Icons.Default.Info) {
                    TvSettingsItem(
                        title = "Versión de uTV",
                        value = "v${BuildConfig.VERSION_NAME} (Compilación ${BuildConfig.VERSION_CODE})",
                        onClick = {},
                    )

                    val updateLabel = when (val state = updateState) {
                        is AppUpdateState.Checking -> "Comprobando en GitHub..."
                        is AppUpdateState.UpToDate -> "Al día (v${state.currentVersion})"
                        is AppUpdateState.UpdateAvailable -> "¡Nueva v${state.latestVersion}! (Abrir descarga)"
                        is AppUpdateState.Error -> "Error al consultar (Reintentar)"
                        is AppUpdateState.Idle -> "Buscar en GitHub Releases"
                    }
                    TvSettingsItem(
                        title = "Verificar actualizaciones",
                        value = updateLabel,
                        onClick = {
                            when (val state = updateState) {
                                is AppUpdateState.UpdateAvailable -> {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.downloadUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        showGitHubDialog = true
                                    }
                                }
                                else -> viewModel.checkForUpdates()
                            }
                        },
                    )

                    TvSettingsItem(
                        title = "Créditos y Librerías Open Source",
                        value = "iptv-org, Media3, OkHttp, Coil, Room (Ver licencias)",
                        onClick = { showCreditsDialog = true },
                    )

                    TvSettingsItem(
                        title = "Código fuente y Reporte de Bugs en GitHub",
                        value = "github.com/${AppUpdateChecker.GITHUB_OWNER}/${AppUpdateChecker.GITHUB_REPO}",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateChecker.GITHUB_REPO_URL))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showGitHubDialog = true
                            }
                        },
                    )

                    TvSettingsItem(
                        title = "Aviso Legal y Deslinde de Responsabilidad",
                        value = "uTV no aloja ni distribuye contenido (Ver detalles)",
                        onClick = { showLegalDialog = true },
                    )
                }
            }
        }

        // Ephemeral notification
        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) {
            Surface(
                color = uTVColors.SurfaceGlassOSD,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, uTVColors.AccentElectricBlue),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = feedbackMessage ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.TextPrimary,
                    )
                }
            }
        }
    }

    // ── TV Dialogs ───────────────────────────────────────────────────────────

    if (showQualityDialog) {
        TvSelectDialog(
            title = "Calidad de stream",
            options = StreamQualityPreference.entries.map { it to it.label },
            selectedOption = settings.streamQuality,
            onSelect = {
                viewModel.setStreamQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false },
        )
    }

    if (showBufferDialog) {
        TvSelectDialog(
            title = "Tamaño de buffer",
            options = BufferDurationMode.entries.map { it to it.label },
            selectedOption = settings.bufferMode,
            onSelect = {
                viewModel.setBufferMode(it)
                showBufferDialog = false
            },
            onDismiss = { showBufferDialog = false },
        )
    }

    if (showDecoderDialog) {
        TvSelectDialog(
            title = "Modo de decodificación",
            options = DecoderMode.entries.map { it to it.label },
            selectedOption = settings.decoderMode,
            onSelect = {
                viewModel.setDecoderMode(it)
                showDecoderDialog = false
            },
            onDismiss = { showDecoderDialog = false },
        )
    }

    if (showAspectRatioDialog) {
        TvSelectDialog(
            title = "Relación de aspecto",
            options = AspectRatioPreference.entries.map { it to it.label },
            selectedOption = settings.aspectRatio,
            onSelect = {
                viewModel.setAspectRatio(it)
                showAspectRatioDialog = false
            },
            onDismiss = { showAspectRatioDialog = false },
        )
    }

    if (showTimeoutDialog) {
        TvSelectDialog(
            title = "Timeout de conexión",
            options = listOf(5 to "5 segundos", 8 to "8 segundos (Por defecto)", 15 to "15 segundos", 20 to "20 segundos"),
            selectedOption = settings.connectionTimeoutSeconds,
            onSelect = {
                viewModel.setConnectionTimeoutSeconds(it)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false },
        )
    }

    if (showZappingDialog) {
        TvSelectDialog(
            title = "Velocidad de zapping",
            options = ZappingSpeedPreference.entries.map { it to it.label },
            selectedOption = settings.zappingSpeed,
            onSelect = {
                viewModel.setZappingSpeed(it)
                showZappingDialog = false
            },
            onDismiss = { showZappingDialog = false },
        )
    }

    if (showThemeDialog) {
        TvSelectDialog(
            title = "Tema de la aplicación",
            options = ThemePreference.entries.map { it to it.label },
            selectedOption = settings.themePreference,
            onSelect = {
                viewModel.setThemePreference(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showFontSizeDialog) {
        TvSelectDialog(
            title = "Tamaño de texto",
            options = FontSizePreference.entries.map { it to it.label },
            selectedOption = settings.fontSizePreference,
            onSelect = {
                viewModel.setFontSizePreference(it)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false },
        )
    }

    if (showClearHistoryConfirm) {
        TvConfirmDialog(
            title = "¿Borrar historial de reproducción?",
            message = "Esta acción vaciará la lista de canales recientemente vistos.",
            confirmText = "Borrar",
            onConfirm = {
                viewModel.clearWatchHistory()
                showClearHistoryConfirm = false
            },
            onDismiss = { showClearHistoryConfirm = false },
        )
    }

    if (showClearFavoritesConfirm) {
        TvConfirmDialog(
            title = "¿Borrar todos los favoritos?",
            message = "Se eliminarán todas las estrellas guardadas en tu lista de canales favoritos.",
            confirmText = "Borrar",
            onConfirm = {
                viewModel.clearFavorites()
                showClearFavoritesConfirm = false
            },
            onDismiss = { showClearFavoritesConfirm = false },
        )
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            containerColor = uTVColors.SurfaceGlassOSD,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Aviso Legal y Deslinde de Responsabilidad",
                        color = uTVColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "uTV es exclusivamente un cliente reproductor open-source de transmisiones multimedia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "1. uTV no aloja, retransmite, controla ni distribuye ningún contenido multimedia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "2. Todos los canales y listas provienen de fuentes de acceso público provistas por comunidades de terceros (iptv-org).",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text("Cerrar", color = uTVColors.AccentElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            containerColor = uTVColors.SurfaceGlassOSD,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Créditos y Librerías",
                        color = uTVColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "uTV es un reproductor libre construido con:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "• iptv-org/iptv — Listas M3U globales en dominio público (Unlicense)",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "• Media3 / ExoPlayer — Motor HLS y DASH para TV (Apache 2.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "• OkHttp — Cliente HTTP resiliente con reconexión (Apache 2.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "• Coil — Carga y cache inteligente de logos de canales (Apache 2.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "• Room — Base de datos local para catálogo y favoritos (Apache 2.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text("Cerrar", color = uTVColors.AccentElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showGitHubDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubDialog = false },
            containerColor = uTVColors.SurfaceGlassOSD,
            title = {
                Text(
                    text = "GitHub / Releases",
                    color = uTVColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Visita el repositorio oficial en tu navegador o dispositivo:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = AppUpdateChecker.GITHUB_REPO_URL,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.AccentElectricBlue,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGitHubDialog = false }) {
                    Text("Aceptar", color = uTVColors.AccentElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

// ── TV Specific D-Pad Friendly Composable Rows ───────────────────────────────

@Composable
private fun TvSettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = uTVColors.AccentElectricBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = uTVColors.TextSecondary,
            )
        }
        Surface(
            color = uTVColors.SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, uTVColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun TvSettingsItem(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "scale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) uTVColors.AccentElectricBlue.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isFocused) BorderStroke(2.dp, uTVColors.AccentElectricBlue) else null,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = uTVColors.TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = uTVColors.AccentElectricBlue,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = uTVColors.TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TvSettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "scale")

    Surface(
        onClick = onToggle,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) uTVColors.AccentElectricBlue.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isFocused) BorderStroke(2.dp, uTVColors.AccentElectricBlue) else null,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = uTVColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = uTVColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = uTVColors.AccentElectricBlue,
                    uncheckedThumbColor = uTVColors.TextSecondary,
                    uncheckedTrackColor = uTVColors.BackgroundSecondary,
                ),
            )
        }
    }
}

@Composable
private fun TvSettingsActionItem(
    title: String,
    subtitle: String,
    actionLabel: String,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "scale")

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) {
            if (isDestructive) uTVColors.LiveBadgeRed.copy(alpha = 0.25f) else uTVColors.AccentElectricBlue.copy(alpha = 0.25f)
        } else Color.Transparent,
        border = if (isFocused) {
            BorderStroke(2.dp, if (isDestructive) uTVColors.LiveBadgeRed else uTVColors.AccentElectricBlue)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = uTVColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = uTVColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDestructive) uTVColors.LiveBadgeRed.copy(alpha = 0.2f) else uTVColors.SurfaceGlassOSD,
                border = BorderStroke(1.dp, if (isDestructive) uTVColors.LiveBadgeRed else uTVColors.BorderSubtle),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = uTVColors.AccentElectricBlue,
                        )
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDestructive) uTVColors.LiveBadgeRed else uTVColors.TextPrimary,
                    )
                }
            }
        }
    }
}

// ── TV Dialog Helpers ────────────────────────────────────────────────────────

@Composable
private fun <T> TvSelectDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = uTVColors.SurfaceGlassOSD,
        title = {
            Text(
                text = title,
                color = uTVColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (option, label) ->
                    val isSelected = option == selectedOption
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) uTVColors.AccentElectricBlue.copy(alpha = 0.3f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(2.dp, uTVColors.AccentElectricBlue) else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (isSelected) uTVColors.TextPrimary else uTVColors.TextSecondary,
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = uTVColors.AccentElectricBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = uTVColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun TvConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = uTVColors.SurfaceGlassOSD,
        title = {
            Text(
                text = title,
                color = uTVColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = uTVColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = uTVColors.LiveBadgeRed, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = uTVColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}
