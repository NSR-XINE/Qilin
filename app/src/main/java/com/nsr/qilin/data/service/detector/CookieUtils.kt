package com.nsr.qilin.data.service.detector

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Cookie aggregation utility ported from super-video-downloader.
 * Combines cookies for the target URL, referer URL, host domain, and top-level domain
 * to ensure that video streams loaded via CDN subdomains have all required session and auth cookies.
 */
object CookieUtils {

    fun getAggregatedCookies(targetUrl: String, referer: String? = null): String {
        return try {
            val cookieManager = try {
                CookieManager.getInstance()
            } catch (t: Throwable) {
                null
            } ?: return ""

            val targetUri = targetUrl.toHttpUrlOrNull()

            val targetCookies = cookieManager.getCookie(targetUrl) ?: ""
            val refererCookies = referer?.let { cookieManager.getCookie(it) } ?: ""

            val targetHost = targetUri?.host
            val targetHostCookies = if (!targetHost.isNullOrEmpty()) {
                cookieManager.getCookie("https://$targetHost") ?: ""
            } else ""

            val refererHost = referer?.toHttpUrlOrNull()?.host
            val refererHostCookies = if (!refererHost.isNullOrEmpty()) {
                cookieManager.getCookie("https://$refererHost") ?: ""
            } else ""

            val topDomainCookies = try {
                val domainToTry = refererHost ?: targetHost
                if (!domainToTry.isNullOrEmpty()) {
                    val parts = domainToTry.split(".")
                    val topDomain = if (parts.size >= 2) parts.takeLast(2).joinToString(".") else domainToTry
                    val c1 = cookieManager.getCookie("https://$topDomain") ?: ""
                    val c2 = cookieManager.getCookie("http://$topDomain") ?: ""
                    val c3 = cookieManager.getCookie("https://.$topDomain") ?: ""
                    "$c1; $c2; $c3"
                } else ""
            } catch (ignored: Throwable) {
                ""
            }

            listOf(targetCookies, refererCookies, targetHostCookies, refererHostCookies, topDomainCookies)
                .filter { it.isNotBlank() }
                .flatMap { it.split(";") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("; ")
        } catch (ignored: Throwable) {
            ""
        }
    }

    fun extractHeadersFromRequest(
        request: WebResourceRequest,
        pageUrl: String? = null
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        try {
            request.requestHeaders?.forEach { (k, v) ->
                if (!k.isNullOrBlank() && !v.isNullOrBlank()) {
                    headers[k] = v
                }
            }
        } catch (ignored: Throwable) {}

        val url = try { request.url?.toString().orEmpty() } catch (t: Throwable) { "" }
        val referer = pageUrl ?: headers["Referer"] ?: headers["referer"]
        if (!referer.isNullOrEmpty() && !headers.containsKey("Referer") && !headers.containsKey("referer")) {
            headers["Referer"] = referer
        }

        if (url.isNotEmpty()) {
            val cookies = getAggregatedCookies(url, referer)
            if (cookies.isNotEmpty()) {
                headers["Cookie"] = cookies
            }
        }

        return headers
    }
}
