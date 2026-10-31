package com.nsr.qilin.data.service.downloader

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * High-performance multi-threaded chunked downloader ported from super-video-downloader.
 *
 * Divides large files into parallel byte ranges using HTTP 'Range: bytes=start-end' requests
 * and writes directly into pre-allocated positions via NIO FileChannel.
 * Supports resume via per-chunk byte tracking files and falls back cleanly to single-connection
 * streaming when Range requests are not supported by the upstream server.
 */
class CustomFileDownloader(
    private val url: URL,
    private val file: File,
    private val threadCount: Int = 16,
    private val headers: Map<String, String> = emptyMap(),
    private val client: OkHttpClient,
    private val controller: FileBasedDownloadController? = null,
    private val listener: DownloadListener? = null,
    private val isForceStreamDownloadMode: Boolean = false
) {
    companion object {
        private const val TAG = "CustomFileDownloader"
        const val PAUSE_ACTION = "PAUSE_ACTION"
        const val STOPPED_AND_SAVE_ACTION = "STOPPED_AND_SAVE_ACTION"
        const val CANCELED_ACTION = "CANCELED_ACTION"
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024 // 64KB buffer
        private const val CALLBACK_INTERVAL_MS = 500L
        private const val MAX_CHUNK_RETRIES = 3

        suspend fun downloadSuspend(
            url: String,
            file: File,
            threadCount: Int = 16,
            headers: Map<String, String> = emptyMap(),
            client: OkHttpClient,
            controller: FileBasedDownloadController? = null,
            onProgress: ((downloadedBytes: Long, totalBytes: Long, speedBps: Long) -> Unit)? = null
        ): Unit = suspendCancellableCoroutine { continuation ->
            val listener = object : DownloadListener {
                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onFailure(e: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long, speedBps: Long) {
                    onProgress?.invoke(downloadedBytes, totalBytes, speedBps)
                }
            }

            val downloader = CustomFileDownloader(
                url = URL(url),
                file = file,
                threadCount = threadCount,
                headers = headers,
                client = client,
                controller = controller,
                listener = listener
            )

            continuation.invokeOnCancellation {
                controller?.requestCancel()
            }

            try {
                downloader.download()
            } catch (e: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }

    private val effectiveThreads = threadCount.coerceIn(1, 32)
    private val executorService: ExecutorService = Executors.newFixedThreadPool(effectiveThreads)
    private val isPaused = AtomicBoolean(false)
    private val isSaved = AtomicBoolean(false)
    private val isCanceled = AtomicBoolean(false)

    private val lastProgressUpdate = AtomicLong(0L)
    private val lastSpeedSampleTime = AtomicLong(SystemClock.elapsedRealtime())
    private val lastSpeedSampleBytes = AtomicLong(0L)
    private val currentSpeedBps = AtomicLong(0L)

    private val totalBytesAll = AtomicLong(0L)
    private val totalBytesChunks = AtomicLongArray(effectiveThreads)
    private val copiedBytesChunks = AtomicLongArray(effectiveThreads)

    private val totalCopiedBytes: Long
        get() {
            var sum = 0L
            for (i in 0 until copiedBytesChunks.length()) {
                sum += copiedBytesChunks.get(i)
            }
            return sum
        }

    fun download() {
        val parentDir = file.parentFile ?: File(".")
        if (!parentDir.exists()) parentDir.mkdirs()

        if (controller?.isInterrupted() == true) {
            handleInterrupted(controller)
            return
        }

        val contentSize = try {
            getContentLength()
        } catch (e: Throwable) {
            this.onFailure(e)
            return
        }

        val randomAccessFile = try {
            RandomAccessFile(file, "rw")
        } catch (e: Throwable) {
            this.onFailure(e)
            return
        }
        val fileChannel = randomAccessFile.channel

        totalBytesAll.set(contentSize)

        controller?.start()

        val isRangeSupported = isUrlSupportingBytesRangeHeader()

        if (!isRangeSupported || (effectiveThreads == 1 && isForceStreamDownloadMode) || contentSize <= 0L) {
            Log.d(TAG, "Range header not supported or single-thread mode, falling back to streaming download.")
            val result = executorService.submit {
                downloadRegularStream(fileChannel)
            }
            try {
                result.get()
            } catch (e: Throwable) {
                this.onFailure(e)
            } finally {
                try { fileChannel.close() } catch (ignored: Exception) {}
                try { randomAccessFile.close() } catch (ignored: Exception) {}
            }
            return
        }

        val chunkSize = contentSize / effectiveThreads
        val ranges = (0 until effectiveThreads).map { index ->
            val start = index * chunkSize
            val end = if (index == effectiveThreads - 1) contentSize - 1 else (index + 1) * chunkSize - 1
            start..end
        }

        val chunkFutureMap = mutableMapOf<Int, Future<*>>()
        Log.d(TAG, "Starting parallel download: file=${file.name}, threads=$effectiveThreads, size=$contentSize")

        ranges.forEachIndexed { index, range ->
            chunkFutureMap[index] = executorService.submit {
                downloadChunkWithRetries(range, fileChannel, index * chunkSize, index)
            }
        }

        var allPartsSucceeded = true
        var failureCause: Throwable? = null

        chunkFutureMap.forEach { (index, future) ->
            try {
                future.get()
            } catch (e: Throwable) {
                allPartsSucceeded = false
                failureCause = e.cause ?: e
                Log.w(TAG, "Chunk $index failed: ${failureCause?.message}")
                listener?.onChunkFailure(failureCause!!, index)
            }
        }

        try { fileChannel.close() } catch (ignored: Exception) {}
        try { randomAccessFile.close() } catch (ignored: Exception) {}

        val isPausedLocal = isPaused.get() || controller?.isPauseRequested() == true
        val isCanceledLocal = isCanceled.get() || controller?.isCancelRequested() == true
        val isSavedLocal = isSaved.get() || controller?.isStopAndSaveRequested() == true

        when {
            allPartsSucceeded && !isPausedLocal && !isCanceledLocal && !isSavedLocal -> {
                cleanChunkFiles()
                this.onSuccess()
            }
            isSavedLocal -> {
                Log.d(TAG, "Download stopped and saved")
                this.onFailure(Exception(STOPPED_AND_SAVE_ACTION))
            }
            isPausedLocal -> {
                Log.d(TAG, "Download paused")
                this.onFailure(Exception(PAUSE_ACTION))
            }
            isCanceledLocal -> {
                Log.d(TAG, "Download cancelled")
                this.onFailure(Exception(CANCELED_ACTION))
            }
            else -> {
                Log.e(TAG, "Download incomplete or failed", failureCause)
                this.onFailure(failureCause ?: Exception("Chunked download incomplete"))
            }
        }
    }

    private fun handleInterrupted(controller: FileBasedDownloadController) {
        when {
            controller.isPauseRequested() -> onFailure(Exception(PAUSE_ACTION))
            controller.isCancelRequested() -> onFailure(Exception(CANCELED_ACTION))
            controller.isStopAndSaveRequested() -> onFailure(Exception(STOPPED_AND_SAVE_ACTION))
            else -> onFailure(Exception(CANCELED_ACTION))
        }
    }

    private fun downloadRegularStream(fileChannel: FileChannel) {
        val req = getOkRequest()
        val res = client.newCall(req).execute()

        if (!res.isSuccessful) {
            res.close()
            this.onFailure(IOException("Failed to download stream: HTTP ${res.code}"))
            return
        }

        val body = res.body ?: run {
            res.close()
            this.onFailure(IOException("Empty response body"))
            return
        }

        val contentLength = body.contentLength()
        if (contentLength > 0L) {
            totalBytesAll.set(contentLength)
        }

        val inputStream = body.byteStream()
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        var bytesCopied = 0L

        copiedBytesChunks[0] = 0L
        totalBytesChunks[0] = contentLength

        try {
            inputStream.use { stream ->
                while (!isSaved.get() && !isPaused.get() && !isCanceled.get() &&
                    (controller?.isInterrupted() != true)
                ) {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead < 0) break
                    fileChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead), bytesCopied)
                    bytesCopied += bytesRead
                    copiedBytesChunks[0] = bytesCopied
                    onProgressUpdate(bytesCopied, totalBytesAll.get())
                }
            }

            when {
                controller?.isPauseRequested() == true || isPaused.get() -> this.onFailure(Exception(PAUSE_ACTION))
                controller?.isStopAndSaveRequested() == true || isSaved.get() -> this.onFailure(Exception(STOPPED_AND_SAVE_ACTION))
                controller?.isCancelRequested() == true || isCanceled.get() -> this.onFailure(Exception(CANCELED_ACTION))
                else -> this.onSuccess()
            }
        } catch (e: Throwable) {
            this.onFailure(e)
        } finally {
            res.close()
        }
    }

    private fun downloadChunkWithRetries(
        range: LongRange,
        fileChannel: FileChannel,
        offset: Long,
        chunkIndex: Int
    ) {
        var lastEx: Throwable? = null
        for (attempt in 1..MAX_CHUNK_RETRIES) {
            if (isCanceled.get() || isPaused.get() || controller?.isInterrupted() == true) {
                return
            }
            try {
                downloadChunk(range, fileChannel, offset, chunkIndex)
                return
            } catch (e: Throwable) {
                lastEx = e
                if (isCanceled.get() || isPaused.get() || controller?.isInterrupted() == true) {
                    throw e
                }
                Log.w(TAG, "Chunk $chunkIndex attempt $attempt/$MAX_CHUNK_RETRIES failed: ${e.message}")
                if (attempt < MAX_CHUNK_RETRIES) {
                    try { Thread.sleep(500L * attempt) } catch (ignored: InterruptedException) {}
                }
            }
        }
        throw lastEx ?: IOException("Chunk $chunkIndex failed after $MAX_CHUNK_RETRIES attempts")
    }

    private fun downloadChunk(
        range: LongRange,
        fileChannel: FileChannel,
        offset: Long,
        chunkIndex: Int
    ) {
        val chunkFile = File(file.parentFile, "${file.nameWithoutExtension}_chunk_$chunkIndex")
        var bytesCopied = 0L

        if (chunkFile.exists()) {
            bytesCopied = try {
                chunkFile.readText().trim().toLongOrNull() ?: 0L
            } catch (e: Throwable) {
                0L
            }
        }

        copiedBytesChunks[chunkIndex] = bytesCopied
        val chunkTotalBytes = range.last - range.first + 1L
        totalBytesChunks[chunkIndex] = chunkTotalBytes

        if (range.first + bytesCopied >= range.last) {
            copiedBytesChunks[chunkIndex] = chunkTotalBytes
            return
        }

        val requestStart = range.first + bytesCopied
        val req = getOkRequestRange(requestStart, range.last)

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful && res.code != 206) {
                throw IOException("HTTP ${res.code} while downloading chunk $chunkIndex")
            }

            val body = res.body ?: throw IOException("Empty body for chunk $chunkIndex")
            val inputStream = body.byteStream()
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)

            RandomAccessFile(chunkFile, "rw").use { raf ->
                inputStream.use { stream ->
                    while (!isSaved.get() && !isPaused.get() && !isCanceled.get() &&
                        (controller?.isInterrupted() != true)
                    ) {
                        val bytesRead = stream.read(buffer)
                        if (bytesRead < 0) break
                        fileChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead), offset + bytesCopied)
                        bytesCopied += bytesRead
                        raf.seek(0)
                        raf.write("$bytesCopied".toByteArray())
                        raf.setLength("$bytesCopied".toByteArray().size.toLong())
                        this.onChunkProgressUpdate(bytesCopied, chunkTotalBytes, chunkIndex)
                    }

                    if (controller?.isPauseRequested() == true || isPaused.get()) {
                        throw Exception(PAUSE_ACTION)
                    }
                    if (controller?.isStopAndSaveRequested() == true || isSaved.get()) {
                        throw Exception(STOPPED_AND_SAVE_ACTION)
                    }
                    if (controller?.isCancelRequested() == true || isCanceled.get()) {
                        throw Exception(CANCELED_ACTION)
                    }
                }
            }
        }
    }

    private fun cleanChunkFiles() {
        try {
            for (i in 0 until effectiveThreads) {
                val chunkFile = File(file.parentFile, "${file.nameWithoutExtension}_chunk_$i")
                if (chunkFile.exists()) chunkFile.delete()
            }
        } catch (ignored: Exception) {}
    }

    private fun onSuccess() {
        executorService.shutdown()
        Log.d(TAG, "Download succeeded: ${file.absolutePath}")
        listener?.onSuccess()
    }

    private fun onFailure(e: Throwable) {
        executorService.shutdown()
        Log.e(TAG, "Download failed: ${e.message}")
        if (e.message == CANCELED_ACTION || controller?.isCancelRequested() == true) {
            cleanChunkFiles()
            try { file.delete() } catch (ignored: Exception) {}
        }
        listener?.onFailure(e)
    }

    private fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long) {
        val now = SystemClock.elapsedRealtime()
        val lastUpdate = lastProgressUpdate.get()
        if (now - lastUpdate >= CALLBACK_INTERVAL_MS) {
            if (lastProgressUpdate.compareAndSet(lastUpdate, now)) {
                if (controller?.isPauseRequested() == true) isPaused.set(true)
                if (controller?.isCancelRequested() == true) isCanceled.set(true)
                if (controller?.isStopAndSaveRequested() == true) isSaved.set(true)

                val lastSample = lastSpeedSampleTime.get()
                val deltaMs = now - lastSample
                if (deltaMs >= 1000L) {
                    val lastBytes = lastSpeedSampleBytes.get()
                    val deltaBytes = downloadedBytes - lastBytes
                    if (deltaBytes >= 0L) {
                        val speed = (deltaBytes * 1000.0 / deltaMs).toLong()
                        currentSpeedBps.set(speed)
                    }
                    lastSpeedSampleTime.set(now)
                    lastSpeedSampleBytes.set(downloadedBytes)
                }

                listener?.onProgressUpdate(downloadedBytes, totalBytes, currentSpeedBps.get())
            }
        }
    }

    private fun onChunkProgressUpdate(downloadedBytes: Long, allBytes: Long, chunkIndex: Int) {
        copiedBytesChunks[chunkIndex] = downloadedBytes
        onProgressUpdate(totalCopiedBytes, totalBytesAll.get())
        listener?.onChunkProgressUpdate(downloadedBytes, allBytes, chunkIndex)
    }

    private fun isUrlSupportingBytesRangeHeader(): Boolean {
        var res: Response? = null
        return try {
            val req = getOkRequestRange(0, 0)
            res = client.newCall(req).execute()
            res.code == 206
        } catch (e: Throwable) {
            false
        } finally {
            try { res?.close() } catch (ignored: Exception) {}
        }
    }

    private fun getOkRequest(): Request {
        val builder = Request.Builder().url(url)
        if (headers.isNotEmpty()) {
            builder.headers(headers.toHeaders())
        }
        return builder.build()
    }

    private fun getOkRequestRange(startByte: Long, endByte: Long?): Request {
        val rangeVal = if (endByte != null) "bytes=$startByte-$endByte" else "bytes=$startByte-"
        val builder = Request.Builder().url(url)
        if (headers.isNotEmpty()) {
            builder.headers(headers.toHeaders())
        }
        builder.header("Range", rangeVal)
        return builder.build()
    }

    private fun getContentLength(): Long {
        val req = getOkRequest()
        client.newCall(req).execute().use { res ->
            return res.body?.contentLength() ?: -1L
        }
    }
}
