package com.iptv.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility to inspect network transport capabilities (Wi-Fi, Ethernet unmetered vs Cellular).
 */
object NetworkUtils {

    /**
     * Returns true if device is connected to Wi-Fi or Ethernet (unmetered home network).
     * Returns false if connected solely to Cellular data or disconnected.
     */
    fun isWifiOrEthernetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
