package com.iptv.app.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.iptv.app.BuildConfig
import com.iptv.app.core.network.AppUpdateChecker
import com.iptv.app.core.network.AppUpdateState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.app.core.model.AspectRatioPreference
import com.iptv.app.core.model.BufferDurationMode
import com.iptv.app.core.model.DecoderMode
import com.iptv.app.core.model.FontSizePreference
import com.iptv.app.core.model.StreamQualityPreference
import com.iptv.app.core.model.ThemePreference
import com.iptv.app.core.model.ZappingSpeedPreference
import com.iptv.app.ui.shared.SettingsViewModel
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay

@Composable
fun MobileSettingsTab(
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
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showClearFavoritesConfirm by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Header
            item(key = "settings_header") {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "Opciones de reproducción, red, privacidad y apariencia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // ── Grupo 1: Reproducción y Video ────────────────────────────────
            item(key = "group_playback") {
                SettingsSectionCard(title = "Reproducción y Video", icon = Icons.Default.PlayCircleOutline) {
                    SettingsNavigationRow(
                        title = "Calidad de stream",
                        subtitle = settings.streamQuality.label,
                        icon = Icons.Default.HighQuality,
                        onClick = { showQualityDialog = true },
                    )
                    SettingsNavigationRow(
                        title = "Tamaño de buffer",
                        subtitle = settings.bufferMode.label,
                        icon = Icons.Default.Storage,
                        onClick = { showBufferDialog = true },
                    )
                    SettingsNavigationRow(
                        title = "Modo de decodificación",
                        subtitle = settings.decoderMode.label,
                        icon = Icons.Default.Speed,
                        onClick = { showDecoderDialog = true },
                    )
                    SettingsNavigationRow(
                        title = "Relación de aspecto",
                        subtitle = settings.aspectRatio.label,
                        icon = Icons.Default.AspectRatio,
                        onClick = { showAspectRatioDialog = true },
                    )
                    SettingsSwitchRow(
                        title = "Auto-play siguiente canal",
                        subtitle = "Salta automáticamente al siguiente canal ante caídas de señal",
                        checked = settings.autoPlayNext,
                        onCheckedChange = { viewModel.setAutoPlayNext(it) },
                    )
                    SettingsSwitchRow(
                        title = "Iniciar en silencio",
                        subtitle = "Silencia el reproductor al sintonizar cualquier canal",
                        checked = settings.startMuted,
                        onCheckedChange = { viewModel.setStartMuted(it) },
                    )
                    VolumeSliderRow(
                        currentVolume = settings.defaultVolume,
                        onVolumeChange = { viewModel.setDefaultVolume(it) },
                    )
                }
            }

            // ── Grupo 2: Red y Datos ─────────────────────────────────────────
            item(key = "group_network") {
                SettingsSectionCard(title = "Red y Datos", icon = Icons.Default.NetworkWifi) {
                    SettingsSwitchRow(
                        title = "Modo bajo consumo de datos",
                        subtitle = "Limita la resolución máxima a 480p SD para ahorrar ancho de banda",
                        checked = settings.lowDataMode,
                        onCheckedChange = { viewModel.setLowDataMode(it) },
                    )
                    SettingsSwitchRow(
                        title = "Solo Wi-Fi",
                        subtitle = "Evita la reproducción cuando el dispositivo use datos móviles",
                        checked = settings.onlyWifi,
                        onCheckedChange = { viewModel.setOnlyWifi(it) },
                    )
                    SettingsNavigationRow(
                        title = "Timeout de conexión",
                        subtitle = "${settings.connectionTimeoutSeconds} segundos",
                        icon = Icons.Default.Timer,
                        onClick = { showTimeoutDialog = true },
                    )
                    SettingsNavigationRow(
                        title = "User-Agent personalizado",
                        subtitle = if (settings.customUserAgent.isBlank()) "Por defecto (uTV/ExoPlayer)" else settings.customUserAgent,
                        icon = Icons.Default.Info,
                        onClick = { showUserAgentDialog = true },
                    )
                }
            }

            // ── Grupo 3: Privacidad y Mantenimiento ──────────────────────────
            item(key = "group_privacy") {
                SettingsSectionCard(title = "Privacidad y Catálogo", icon = Icons.Default.Security) {
                    SettingsSwitchRow(
                        title = "Modo privado",
                        subtitle = "No registra canales en el historial de visualización",
                        checked = settings.privateMode,
                        onCheckedChange = { viewModel.setPrivateMode(it) },
                    )
                    SettingsActionRow(
                        title = "Borrar historial de reproducción",
                        subtitle = "Limpia la sección de 'Recientemente vistos'",
                        icon = Icons.Default.DeleteSweep,
                        actionLabel = "Limpiar",
                        isDestructive = true,
                        onClick = { showClearHistoryConfirm = true },
                    )
                    SettingsActionRow(
                        title = "Borrar favoritos",
                        subtitle = "Elimina todos los canales marcados con estrella",
                        icon = Icons.Default.DeleteSweep,
                        actionLabel = "Borrar",
                        isDestructive = true,
                        onClick = { showClearFavoritesConfirm = true },
                    )
                    SettingsActionRow(
                        title = "Sincronizar catálogo M3U",
                        subtitle = "Fuerza la descarga limpia de la lista pública de canales",
                        icon = Icons.Default.Refresh,
                        actionLabel = if (isSyncing) "Sincronizando..." else "Recargar",
                        isLoading = isSyncing,
                        onClick = { viewModel.forceSync() },
                    )
                }
            }

            // ── Grupo 4: Apariencia y Controles ──────────────────────────────
            item(key = "group_appearance") {
                SettingsSectionCard(title = "Apariencia y Controles", icon = Icons.Default.Palette) {
                    SettingsNavigationRow(
                        title = "Tema de la aplicación",
                        subtitle = settings.themePreference.label,
                        icon = Icons.Default.Palette,
                        onClick = { showThemeDialog = true },
                    )
                    SettingsNavigationRow(
                        title = "Tamaño de texto",
                        subtitle = settings.fontSizePreference.label,
                        icon = Icons.Default.Info,
                        onClick = { showFontSizeDialog = true },
                    )
                    SettingsSwitchRow(
                        title = "Reducir animaciones",
                        subtitle = "Acelera la respuesta en dispositivos de menor rendimiento",
                        checked = settings.reduceAnimations,
                        onCheckedChange = { viewModel.setReduceAnimations(it) },
                    )
                    SettingsNavigationRow(
                        title = "Velocidad de zapping",
                        subtitle = settings.zappingSpeed.label,
                        icon = Icons.Default.Speed,
                        onClick = { showZappingDialog = true },
                    )
                }
            }

            // ── Grupo 5: Información, Actualizaciones y Aviso Legal ─────────────
            item(key = "group_legal") {
                SettingsSectionCard(title = "Acerca de uTV", icon = Icons.Default.Info) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "uTV v${BuildConfig.VERSION_NAME} (Compilación ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = uTVColors.TextPrimary,
                            )
                            Text(
                                text = "Reproductor nativo de televisión abierta y FAST para Android TV y Móviles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = uTVColors.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = uTVColors.BorderSubtle)

                    // Verificar actualizaciones en GitHub Releases
                    val updateSubtitle = when (val state = updateState) {
                        is AppUpdateState.Checking -> "Consultando GitHub Releases..."
                        is AppUpdateState.UpToDate -> "uTV está al día (v${state.currentVersion})"
                        is AppUpdateState.UpdateAvailable -> "¡Nueva versión v${state.latestVersion} disponible! Toca para descargar el APK."
                        is AppUpdateState.Error -> "Error: ${state.message} (Toca para reintentar)"
                        is AppUpdateState.Idle -> "Buscar nueva versión en GitHub Releases"
                    }
                    SettingsNavigationRow(
                        title = "Verificar actualizaciones",
                        subtitle = updateSubtitle,
                        icon = Icons.Default.SystemUpdate,
                        onClick = {
                            when (val state = updateState) {
                                is AppUpdateState.UpdateAvailable -> {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.downloadUrl))
                                    context.startActivity(intent)
                                }
                                else -> viewModel.checkForUpdates()
                            }
                        },
                    )

                    HorizontalDivider(color = uTVColors.BorderSubtle)

                    SettingsNavigationRow(
                        title = "Créditos y Librerías",
                        subtitle = "iptv-org, Media3 / ExoPlayer, OkHttp, Coil, Room",
                        icon = Icons.Default.Group,
                        onClick = { showCreditsDialog = true },
                    )

                    HorizontalDivider(color = uTVColors.BorderSubtle)

                    SettingsNavigationRow(
                        title = "Código fuente en GitHub",
                        subtitle = "github.com/${AppUpdateChecker.GITHUB_OWNER}/${AppUpdateChecker.GITHUB_REPO}",
                        icon = Icons.Default.Code,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateChecker.GITHUB_REPO_URL))
                            context.startActivity(intent)
                        },
                    )

                    HorizontalDivider(color = uTVColors.BorderSubtle)

                    SettingsNavigationRow(
                        title = "Reportar problema o sugerencia",
                        subtitle = "Abre un issue para reportar canales caídos o errores",
                        icon = Icons.Default.BugReport,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateChecker.GITHUB_ISSUES_URL))
                            context.startActivity(intent)
                        },
                    )

                    HorizontalDivider(color = uTVColors.BorderSubtle)

                    SettingsNavigationRow(
                        title = "Aviso Legal y Deslinde de Responsabilidad",
                        subtitle = "uTV no aloja ni distribuye contenido multimedia.",
                        icon = Icons.Default.Security,
                        onClick = { showLegalDialog = true },
                    )
                }
            }
        }

        // Ephemeral feedback notification
        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 24.dp, end = 24.dp),
        ) {
            Surface(
                color = uTVColors.SurfaceGlassOSD,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, uTVColors.AccentElectricBlue),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = feedbackMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = uTVColors.TextPrimary,
                    )
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showQualityDialog) {
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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
        SelectOptionDialog(
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

    if (showUserAgentDialog) {
        var uaInput by remember { mutableStateOf(settings.customUserAgent) }
        AlertDialog(
            onDismissRequest = { showUserAgentDialog = false },
            containerColor = uTVColors.SurfaceGlassOSD,
            title = {
                Text(
                    text = "User-Agent personalizado",
                    color = uTVColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column {
                    Text(
                        text = "Deja en blanco para utilizar el User-Agent por defecto del reproductor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    OutlinedTextField(
                        value = uaInput,
                        onValueChange = { uaInput = it },
                        placeholder = { Text("Ej: Mozilla/5.0 ...", color = uTVColors.TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = uTVColors.AccentElectricBlue,
                            unfocusedBorderColor = uTVColors.BorderSubtle,
                            focusedTextColor = uTVColors.TextPrimary,
                            unfocusedTextColor = uTVColors.TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCustomUserAgent(uaInput)
                        showUserAgentDialog = false
                    },
                ) {
                    Text("Guardar", color = uTVColors.AccentElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserAgentDialog = false }) {
                    Text("Cancelar", color = uTVColors.TextSecondary)
                }
            },
        )
    }

    if (showClearHistoryConfirm) {
        ConfirmationDialog(
            title = "¿Borrar historial de reproducción?",
            message = "Esta acción reiniciará el contador de visualizaciones y vaciará la lista de canales recientemente vistos.",
            confirmText = "Borrar",
            onConfirm = {
                viewModel.clearWatchHistory()
                showClearHistoryConfirm = false
            },
            onDismiss = { showClearHistoryConfirm = false },
        )
    }

    if (showClearFavoritesConfirm) {
        ConfirmationDialog(
            title = "¿Borrar todos los favoritos?",
            message = "Se eliminará la marca de estrella de todos los canales guardados en tu lista de favoritos.",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                    )
                    Text(
                        text = "Aviso Legal y Deslinde",
                        color = uTVColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "uTV es exclusivamente una aplicación cliente reproductora de transmisiones multimedia en código abierto (open-source).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextPrimary,
                    )
                    Text(
                        text = "1. uTV no aloja, produce, retransmite, controla ni distribuye ningún contenido audiovisual ni señal en vivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "2. Todos los enlaces, metadatos y señales sintonizados provienen de listas de acceso público gratuitas mantenidas por comunidades globales de terceros (proyecto iptv-org).",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                    Text(
                        text = "3. Los derechos de autor, marcas y transmisiones pertenecen íntegramente a sus respectivos dueños y televisoras.",
                        style = MaterialTheme.typography.bodySmall,
                        color = uTVColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text("Entendido", color = uTVColors.AccentElectricBlue, fontWeight = FontWeight.Bold)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = uTVColors.AccentElectricBlue,
                    )
                    Text(
                        text = "Créditos y Librerías",
                        color = uTVColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = "uTV es un reproductor libre construido gracias a estos proyectos de código abierto:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = uTVColors.TextPrimary,
                    )
                    CreditItem(
                        name = "iptv-org / iptv",
                        desc = "Listas de canales públicas mundiales (Unlicense - Dominio Público)",
                    )
                    CreditItem(
                        name = "Media3 / ExoPlayer",
                        desc = "Motor de reproducción HLS y DASH para Android (Apache 2.0)",
                    )
                    CreditItem(
                        name = "OkHttp",
                        desc = "Cliente HTTP para descarga de listas y streams (Apache 2.0)",
                    )
                    CreditItem(
                        name = "Coil",
                        desc = "Carga y cache optimizado de logos de canales (Apache 2.0)",
                    )
                    CreditItem(
                        name = "Room & DataStore",
                        desc = "Almacenamiento local del catálogo, favoritos y preferencias (Apache 2.0)",
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
}

@Composable
private fun CreditItem(
    name: String,
    desc: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = uTVColors.AccentAnimeOrange,
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = uTVColors.TextSecondary,
        )
    }
}

// ── Reusable Settings Row Components ─────────────────────────────────────────

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = uTVColors.AccentElectricBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp,
                ),
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
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = uTVColors.TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = uTVColors.AccentElectricBlue,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = uTVColors.TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = uTVColors.AccentElectricBlue,
                uncheckedThumbColor = uTVColors.TextSecondary,
                uncheckedTrackColor = uTVColors.BackgroundSecondary,
            ),
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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
            onClick = onClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            color = if (isDestructive) uTVColors.LiveBadgeRed.copy(alpha = 0.15f) else uTVColors.SurfaceGlassOSD,
            border = BorderStroke(
                1.dp,
                if (isDestructive) uTVColors.LiveBadgeRed.copy(alpha = 0.5f) else uTVColors.BorderSubtle,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = uTVColors.AccentElectricBlue,
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) uTVColors.LiveBadgeRed else uTVColors.AccentElectricBlue,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDestructive) uTVColors.LiveBadgeRed else uTVColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun VolumeSliderRow(
    currentVolume: Int,
    onVolumeChange: (Int) -> Unit,
) {
    var sliderValue by remember(currentVolume) { mutableFloatStateOf(currentVolume.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = uTVColors.AccentElectricBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Volumen inicial",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = uTVColors.TextPrimary,
                )
            }
            Text(
                text = "${sliderValue.toInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = uTVColors.AccentElectricBlue,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onVolumeChange(sliderValue.toInt()) },
            valueRange = 0f..100f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = uTVColors.AccentElectricBlue,
                activeTrackColor = uTVColors.AccentElectricBlue,
                inactiveTrackColor = uTVColors.BackgroundSecondary,
            ),
        )
    }
}

// ── Generic Dialogs ──────────────────────────────────────────────────────────

@Composable
private fun <T> SelectOptionDialog(
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (option, label) ->
                    val isSelected = option == selectedOption
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) uTVColors.AccentElectricBlue.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, uTVColors.AccentElectricBlue) else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (isSelected) uTVColors.TextPrimary else uTVColors.TextSecondary,
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = uTVColors.AccentElectricBlue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = uTVColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun ConfirmationDialog(
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = uTVColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = uTVColors.LiveBadgeRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = uTVColors.TextSecondary)
            }
        },
    )
}
