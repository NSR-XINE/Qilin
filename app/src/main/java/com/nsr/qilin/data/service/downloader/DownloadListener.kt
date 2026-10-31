package com.nsr.qilin.data.service.downloader

/**
 * Listener interface for multi-threaded chunked downloads.
 * Ported from super-video-downloader.
 */
interface DownloadListener {
    fun onSuccess()

    fun onFailure(e: Throwable)

    fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long, speedBps: Long)

    fun onChunkProgressUpdate(downloadedBytes: Long, allBytesChunk: Long, chunkIndex: Int) {}

    fun onChunkFailure(e: Throwable, chunkIndex: Int) {}
}
