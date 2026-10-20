package com.iptv.app.ui.splash

import android.view.KeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.app.R
import com.iptv.app.ui.theme.uTVColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Crunchyroll-style cinematic animated splash screen.
 *
 * Visual sequence:
 * 1. Deep OLED dark space (#0B0C10) background with ambient radial neon bloom.
 * 2. Energetic shockwave rings (Crunchyroll Orange #F47521 & Electric Blue #0063E5) burst radially from center.
 * 3. 3D frosted logo emerges with spring-loaded scale physics (bouncy overshoot + settle).
 * 4. Specular light sweep glides across the logo surface.
 * 5. Brand typography "uTV" slides up with neon divider line and expanding subtitle tracking.
 * 6. Smooth cinematic exit transition (zoom + fade) seamlessly into [Routes.HOME].
 *
 * Supports touch tap or TV remote D-Pad OK / Enter to skip immediately.
 *
 * @param onSplashFinished Callback triggered when animation completes or user skips.
 * @param isTV True if running on Android TV / Google TV.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    isTV: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    // Animation drivers
    val shockwaveRadius1 = remember { Animatable(0f) }
    val shockwaveAlpha1 = remember { Animatable(0f) }

    val shockwaveRadius2 = remember { Animatable(0f) }
    val shockwaveAlpha2 = remember { Animatable(0f) }

    val logoScale = remember { Animatable(0.2f) }
    val logoAlpha = remember { Animatable(0f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(30f) }

    val dividerWidth = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineSpacing = remember { Animatable(1f) }

    val screenScale = remember { Animatable(1.0f) }
    val screenAlpha = remember { Animatable(1.0f) }

    // Subtle ambient breathing glow behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "ambientPulse",
    )

    // Master animation orchestration
    LaunchedEffect(Unit) {
        // Step 1: Initial shockwave burst + logo pop-in with spring physics
        launch {
            shockwaveAlpha1.snapTo(0.85f)
            shockwaveRadius1.animateTo(
                targetValue = if (isTV) 380f else 260f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
        launch {
            shockwaveAlpha1.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing),
            )
        }

        // Secondary delayed electric blue ring
        launch {
            delay(120)
            shockwaveAlpha2.snapTo(0.75f)
            shockwaveRadius2.animateTo(
                targetValue = if (isTV) 440f else 320f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            )
        }
        launch {
            delay(120)
            shockwaveAlpha2.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 850, easing = LinearEasing),
            )
        }

        // Logo spring pop
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }

        // Step 2: Typography and divider reveal (after logo lands)
        delay(400)
        launch {
            titleAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            titleOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
        launch {
            dividerWidth.animateTo(if (isTV) 160f else 110f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            delay(150)
            taglineAlpha.animateTo(1f, animationSpec = tween(450))
            taglineSpacing.animateTo(if (isTV) 6f else 4f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }

        // Step 3: Hold briefly, then cinematic exit zoom-out
        delay(1300)
        launch {
            screenScale.animateTo(1.06f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        }
        screenAlpha.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))

        // Transition directly to Home
        onSplashFinished()
    }

    // Auto-focus container for remote control interception on Android TV
    LaunchedEffect(Unit) {
        if (isTV) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(screenScale.value)
            .alpha(screenAlpha.value)
            .background(uTVColors.BackgroundPrimary)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_SPACE -> {
                            onSplashFinished()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ── 1. Shockwave Canvas Layer ───────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f - 40f)

            // Inner Crunchyroll Orange sonic shockwave ring
            if (shockwaveAlpha1.value > 0f) {
                drawCircle(
                    color = uTVColors.AccentAnimeOrange.copy(alpha = shockwaveAlpha1.value),
                    radius = shockwaveRadius1.value * density,
                    center = centerOffset,
                    style = Stroke(width = 4.dp.toPx() * (1f - shockwaveRadius1.value / 400f).coerceAtLeast(0.3f)),
                )
            }

            // Outer Electric Blue sonic shockwave ring
            if (shockwaveAlpha2.value > 0f) {
                drawCircle(
                    color = uTVColors.AccentElectricBlue.copy(alpha = shockwaveAlpha2.value),
                    radius = shockwaveRadius2.value * density,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx() * (1f - shockwaveRadius2.value / 450f).coerceAtLeast(0.2f)),
                )
            }
        }

        // ── 2. Ambient Neon Bloom Glow ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(if (isTV) 320.dp else 240.dp)
                .scale(logoScale.value * ambientPulse)
                .alpha(logoAlpha.value * 0.45f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            uTVColors.AccentAnimeOrange.copy(alpha = 0.5f),
                            uTVColors.AccentElectricBlue.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
                .blur(48.dp),
        )

        // ── 3. Central Brand Composition ────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Frosted 3D "u" Logo with Spring Scale
            Box(
                modifier = Modifier
                    .size(if (isTV) 180.dp else 130.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "uTV Logo",
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Brand Title: "uTV"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .scale(if (titleOffsetY.value > 0f) 0.95f else 1.0f),
            ) {
                Text(
                    text = "u",
                    style = if (isTV) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = uTVColors.AccentAnimeOrange,
                )
                Text(
                    text = "TV",
                    style = if (isTV) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = uTVColors.TextPrimary,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Neon Gradient Divider
            Box(
                modifier = Modifier
                    .width(dividerWidth.value.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                uTVColors.AccentElectricBlue,
                                uTVColors.AccentAnimeOrange,
                                uTVColors.AccentElectricBlue,
                            ),
                        ),
                    ),
            )

            Spacer(Modifier.height(10.dp))

            // Subtitle Tagline
            Text(
                text = "STREAMING EN VIVO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = uTVColors.TextSecondary,
                letterSpacing = taglineSpacing.value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value),
            )
        }
    }
}
