package com.nsr.qilin.data.service.downloader

import java.io.File
import java.io.IOException

/**
 * File-based download controller ported from super-video-downloader.
 * Uses atomic flag files in the download working directory to signal pause, cancel, and stop events
 * across concurrent threads and processes.
 */
class FileBasedDownloadController(private val downloadDir: File) {

    companion object {
        const val PAUSE_FLAG_FILENAME = "pause"
        const val CANCEL_FLAG_FILENAME = "cancel"
        const val STOP_AND_SAVE_FLAG_FILENAME = "stop_and_save"
    }

    private val pauseFlag = File(downloadDir, PAUSE_FLAG_FILENAME)
    private val cancelFlag = File(downloadDir, CANCEL_FLAG_FILENAME)
    private val stopAndSaveFlag = File(downloadDir, STOP_AND_SAVE_FLAG_FILENAME)

    /**
     * Initializes the controller for a new download, ensuring no old flags exist.
     */
    fun start() {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        pauseFlag.delete()
        cancelFlag.delete()
        stopAndSaveFlag.delete()
    }

    /**
     * Signals the downloader to pause by creating the pause flag file.
     */
    @Throws(IOException::class)
    fun requestPause() {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        pauseFlag.createNewFile()
    }

    /**
     * Signals the downloader to cancel by creating the cancel flag file.
     */
    @Throws(IOException::class)
    fun requestCancel() {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        if (isPauseRequested()) {
            pauseFlag.delete()
        }
        cancelFlag.createNewFile()
    }

    /**
     * Signals the downloader to stop and save existing segments by creating the stop_and_save flag.
     */
    @Throws(IOException::class)
    fun requestStopAndSave() {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        stopAndSaveFlag.createNewFile()
    }

    fun isPauseRequested(): Boolean = pauseFlag.exists()

    fun isCancelRequested(): Boolean = cancelFlag.exists()

    fun isStopAndSaveRequested(): Boolean = stopAndSaveFlag.exists()

    /**
     * Checks if any stop or interruption action has been requested.
     */
    fun isInterrupted(): Boolean {
        return isPauseRequested() || isCancelRequested() || isStopAndSaveRequested()
    }

    fun clear() {
        try {
            pauseFlag.delete()
            cancelFlag.delete()
            stopAndSaveFlag.delete()
        } catch (ignored: Exception) {}
    }
}
