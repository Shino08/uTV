package com.iptv.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.iptv.app.ui.navigation.AppNavGraph
import com.iptv.app.ui.theme.uTVTheme

/**
 * Single activity — detects form factor on creation and routes to the
 * appropriate NavGraph (Mobile or TV).
 *
 * Detection strategy:
 *   [PackageManager.FEATURE_LEANBACK] → Android TV / Google TV
 *   Absence of the feature → Mobile / Tablet
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen API — keeps native splash visible until first frame
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        setContent {
            uTVTheme(isTV = isTV) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    isTV = isTV,
                )
            }
        }
    }
}
