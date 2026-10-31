package com.nsr.qilin.data.service.downloader

import okhttp3.Request

/**
 * Utility to sanitize HTTP headers passed to OkHttp, preventing IllegalArgumentException
 * caused by invalid control characters, newlines, or carriage returns from CookieManager.
 */
object HttpHeaderSanitizer {

    fun isSafeHeaderName(name: String): Boolean {
        if (name.isEmpty()) return false
        for (i in 0 until name.length) {
            val c = name[i]
            if (c <= '\u0020' || c >= '\u007f') return false
        }
        return true
    }

    fun isSafeHeaderValue(value: String): Boolean {
        for (i in 0 until value.length) {
            val c = value[i]
            if ((c < '\u0020' && c != '\t') || c >= '\u007f') return false
        }
        return true
    }

    fun sanitizeHeaderValue(value: String): String {
        val filtered = StringBuilder(value.length)
        for (i in 0 until value.length) {
            val c = value[i]
            if (c == '\r' || c == '\n') continue
            if ((c >= '\u0020' || c == '\t') && c < '\u007f') {
                filtered.append(c)
            }
        }
        return filtered.toString().trim()
    }

    fun applySafeHeaders(builder: Request.Builder, headers: Map<String, String>): Request.Builder {
        for ((rawKey, rawVal) in headers) {
            val key = rawKey.trim()
            if (!isSafeHeaderName(key)) continue
            val safeVal = sanitizeHeaderValue(rawVal)
            if (isSafeHeaderValue(safeVal)) {
                try {
                    builder.header(key, safeVal)
                } catch (ignored: Exception) {}
            }
        }
        return builder
    }
}

fun Request.Builder.applySafeHeaders(headers: Map<String, String>): Request.Builder {
    return HttpHeaderSanitizer.applySafeHeaders(this, headers)
}
