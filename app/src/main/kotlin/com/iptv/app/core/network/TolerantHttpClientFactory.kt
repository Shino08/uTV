package com.iptv.app.core.network

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Factory providing an [OkHttpClient] configured to handle public IPTV streams and playlists.
 *
 * Technical Rationale:
 * Public IPTV stream sources and community playlists often use:
 * 1. Let's Encrypt certificates whose root CA (ISRG Root X1) is missing from older Android
 *    or AOSP TV box trust stores (DST Root CA X3 expired in Sep 2021).
 * 2. Incomplete TLS certificate chains (missing intermediate CA bundles).
 * 3. Self-signed or IP-based TLS endpoints on broadcast servers.
 *
 * This factory builds an SSLContext with a permissive X509TrustManager and permissive HostnameVerifier,
 * preventing `CertPathValidatorException: Trust anchor for certification path not found` across
 * all Android versions and TV hardware devices.
 */
object TolerantHttpClientFactory {

    @SuppressLint("CustomX509TrustManager")
    fun create(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("TrustAllX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
