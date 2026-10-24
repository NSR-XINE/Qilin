package com.mydonghua.app.data.service

import android.content.Context
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.io.ByteArrayInputStream

object AdBlocker {

    private const val PREFS_NAME = "ad_blocker_prefs"
    private const val KEY_ENABLED = "ad_blocker_enabled"

    private val _enabledFlow = MutableStateFlow<Boolean?>(null)

    // High-frequency video stream and popup ad networks
    private val AD_HOSTS = hashSetOf(
        "popads",
        "adsterra",
        "doubleclick",
        "googlesyndication",
        "googleadservices",
        "adservice",
        "propellerads",
        "exoclick",
        "yandex",
        "monetag",
        "trafficjunky",
        "juicyads",
        "onclickrevenue",
        "adnxs",
        "bidswitch",
        "criteo",
        "taboola",
        "outbrain",
        "adcolony",
        "applovin",
        "unityads",
        "vungle",
        "inmobi",
        "chartboost",
        "bet365",
        "1xbet",
        "parimatch",
        "mostbet",
        "melbet",
        "adkeeper",
        "clickadu",
        "richaudience",
        "hilltopads",
        "evadav",
        "admaven",
        "galaksion",
        "zeroredirect",
        "popcash",
        "mgid",
        "adskeeper",
        "syndication",
        "adpushup",
        "adblade",
        "contextweb",
        "scorecardresearch",
        "moatads",
        "serving-sys",
        "revcontent",
        "contentexchange",
        "histats",
        "statcounter"
    )

    fun isEnabled(context: Context): Boolean {
        _enabledFlow.value?.let { return it }
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
        _enabledFlow.value = value
        return value
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _enabledFlow.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun enabledFlow(context: Context): Flow<Boolean> {
        if (_enabledFlow.value == null) {
            _enabledFlow.value = isEnabled(context)
        }
        return _enabledFlow.filterNotNull()
    }

    fun isAdUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return AD_HOSTS.any { lower.contains(it) }
    }

    fun isDisallowedScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.startsWith("intent:") ||
                lower.startsWith("market:") ||
                lower.startsWith("tg:") ||
                lower.startsWith("whatsapp:") ||
                lower.startsWith("viber:") ||
                lower.startsWith("line:") ||
                lower.startsWith("fb:") ||
                lower.startsWith("twitter:")
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
