package com.nsr.qilin.data.service.downloader

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Coroutine-based segment downloader for streaming media segments (HLS/DASH).
 * Ported from super-video-downloader.
 *
 * Automatically checks for pause/cancel flags, handles network retries with exponential backoff,
 * tolerates missing 404 ad segments without breaking the stream, and skips already completed segments.
 */
class SegmentDownloader(
    private val client: OkHttpClient,
    private val headers: Map<String, String> = emptyMap(),
    private val controller: FileBasedDownloadController,
    private val onProgress: ((bytes: Long) -> Unit)? = null
) {
    companion object {
        private const val TAG = "SegmentDownloader"
        private const val RETRY_COUNT = 5
        private const val BUFFER_SIZE = 64 * 1024
    }

    suspend fun download(
        segmentUrl: String,
        outputFile: File,
        logPrefix: String = "Segment",
        segmentIdentifier: Any = 0
    ): Long {
        if (outputFile.exists() && outputFile.length() > 0) {
            return outputFile.length()
        }

        var lastException: Throwable? = null
        for (attempt in 1..RETRY_COUNT) {
            if (controller.isInterrupted()) {
                throw CancellationException("Download interrupted by user.")
            }

            try {
                val requestBuilder = Request.Builder().url(segmentUrl).applySafeHeaders(headers)

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.code == 404 || response.code == 410) {
                        Log.w(TAG, "$logPrefix: Segment $segmentIdentifier returned HTTP ${response.code} (skipping missing or expired segment)")
                        return 0L
                    }

                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} downloading segment $segmentIdentifier")
                    }

                    val body = response.body
                    var bytesCopied = 0L

                    body.byteStream().use { input ->
                        outputFile.outputStream().use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (input.read(buffer).also { read = it } >= 0) {
                                if (controller.isInterrupted()) {
                                    throw CancellationException("Download interrupted during segment transfer.")
                                }
                                output.write(buffer, 0, read)
                                bytesCopied += read
                            }
                            output.flush()
                        }
                    }

                    onProgress?.invoke(bytesCopied)
                    return bytesCopied
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    if (outputFile.exists()) outputFile.delete()
                    throw e
                }

                lastException = e
                Log.w(TAG, "$logPrefix: Segment $segmentIdentifier attempt $attempt/$RETRY_COUNT failed: ${e.message}")
                if (outputFile.exists()) outputFile.delete()

                if (attempt < RETRY_COUNT) {
                    delay(500L * attempt)
                }
            }
        }

        throw IOException("Failed to download segment $segmentIdentifier after $RETRY_COUNT attempts", lastException)
    }
}
