package com.iptv.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.iptv.app.ui.mobile.MobileHomeScreen
import com.iptv.app.ui.mobile.MobilePlayerScreen
import com.iptv.app.ui.splash.SplashScreen
import com.iptv.app.ui.tv.TvHomeScreen
import com.iptv.app.ui.tv.TvPlayerScreen

/** Route constants — single source of truth, avoids string literals across the codebase. */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val PLAYER = "player/{channelId}"

    fun playerRoute(channelId: String) = "player/$channelId"
}

/**
 * Root navigation graph.
 * Selects the Mobile or TV screen hierarchy based on [isTV].
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    isTV: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                isTV = isTV,
            )
        }
        composable(Routes.HOME) {
            if (isTV) {
                TvHomeScreen(
                    onChannelSelected = { channel ->
                        navController.navigate(Routes.playerRoute(channel.id))
                    },
                )
            } else {
                MobileHomeScreen(
                    onChannelSelected = { channel ->
                        navController.navigate(Routes.playerRoute(channel.id))
                    },
                )
            }
        }

        composable(Routes.PLAYER) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            if (isTV) {
                TvPlayerScreen(
                    channelId = channelId,
                    onBack = { navController.popBackStack() },
                )
            } else {
                MobilePlayerScreen(
                    channelId = channelId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
