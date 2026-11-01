package com.nsr.qilin.data.service.downloader

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * High-performance container muxer leveraging bundled FFmpeg (libffmpeg.so) as the primary engine
 * with seamless fallback to Android's native MediaMuxer and MediaExtractor.
 *
 * Combines separate video and audio streams (e.g. demuxed HLS tracks) or remuxes concatenated
 * fMP4 / MPEG-TS files into 100% compliant, seekable MP4, MKV, M4A, or MP3 files.
 *
 * Correctly preserves H.264/H.265 B-frames and presentation timestamps without doubling video duration
 * or introducing playback stutter/lag.
 */
object MediaMuxerHelper {
    private const val TAG = "MediaMuxerHelper"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    fun mux(
        videoFile: File,
        audioFile: File?,
        outputFile: File,
        context: Context? = null,
        format: String = "mp4"
    ): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e(TAG, "Video file missing or empty: ${videoFile.absolutePath}")
            return false
        }

        // 1. Prioritize FFmpeg: Handles B-frames, ctts box, proper timestamps, and A/V sync without lag
        if (context != null) {
            val ffmpegSuccess = tryFFmpegMux(context, videoFile, audioFile, outputFile, format)
            if (ffmpegSuccess && outputFile.exists() && outputFile.length() > 0L) {
                Log.i(TAG, "FFmpeg remux succeeded -> ${outputFile.name} (${outputFile.length()} bytes)")
                return true
            }
            Log.w(TAG, "FFmpeg remux failed or produced empty file; falling back to native MediaMuxer...")
        }

        // 2. Fallback to native MediaMuxer (only applicable for MP4 container)
        if (format.equals("mp4", ignoreCase = true)) {
            val nativeSuccess = tryNativeMux(videoFile, audioFile, outputFile)
            if (nativeSuccess && outputFile.exists() && outputFile.length() > 0L) {
                Log.i(TAG, "Native MediaMuxer succeeded -> ${outputFile.name} (${outputFile.length()} bytes)")
                return true
            }
        }

        return false
    }

    private fun tryFFmpegMux(
        context: Context,
        videoFile: File,
        audioFile: File?,
        outputFile: File,
        format: String
    ): Boolean {
        return try {
            try {
                com.yausername.ffmpeg.FFmpeg.getInstance().init(context.applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "FFmpeg init notice: ${e.message}")
            }

            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val ffmpegBin = File(nativeLibDir, "libffmpeg.so")
            if (!ffmpegBin.exists()) {
                Log.w(TAG, "libffmpeg.so not found at ${ffmpegBin.absolutePath}")
                return false
            }

            if (!ffmpegBin.canExecute()) {
                ffmpegBin.setExecutable(true, false)
            }

            val packagesRoot = File(context.noBackupFilesDir, "youtubedl-android/packages")
            val ffmpegLibDir = File(packagesRoot, "ffmpeg/usr/lib")
            val pythonLibDir = File(packagesRoot, "python/usr/lib")
            val aria2cLibDir = File(packagesRoot, "aria2c/usr/lib")

            val ldList = mutableListOf<String>()
            if (ffmpegLibDir.exists()) ldList.add(ffmpegLibDir.absolutePath)
            if (pythonLibDir.exists()) ldList.add(pythonLibDir.absolutePath)
            if (aria2cLibDir.exists()) ldList.add(aria2cLibDir.absolutePath)
            ldList.add(nativeLibDir)
            System.getenv("LD_LIBRARY_PATH")?.let { envLd ->
                if (envLd.isNotEmpty()) ldList.add(envLd)
            }
            val ldPath = ldList.joinToString(":")

            val isAudioOnly = format.equals("m4a", ignoreCase = true) || format.equals("mp3", ignoreCase = true) || format.equals("aac", ignoreCase = true)
            val hasSeparateAudio = audioFile != null && audioFile.exists() && audioFile.length() > 0L

            val cmd = mutableListOf<String>()
            cmd.add(ffmpegBin.absolutePath)
            cmd.add("-y")
            cmd.add("-fflags")
            cmd.add("+genpts+discardcorrupt")

            if (isAudioOnly) {
                val audioSource = if (hasSeparateAudio) audioFile else videoFile
                cmd.add("-i")
                cmd.add(audioSource.absolutePath)
                cmd.add("-vn")
                if (format.equals("mp3", ignoreCase = true)) {
                    cmd.add("-c:a")
                    cmd.add("libmp3lame")
                    cmd.add("-b:a")
                    cmd.add("320k")
                } else {
                    cmd.add("-c:a")
                    cmd.add("copy")
                }
            } else {
                cmd.add("-i")
                cmd.add(videoFile.absolutePath)
                if (hasSeparateAudio) {
                    cmd.add("-i")
                    cmd.add(audioFile.absolutePath)
                    cmd.add("-map")
                    cmd.add("0:v:0")
                    cmd.add("-map")
                    cmd.add("1:a:0?")
                } else {
                    cmd.add("-map")
                    cmd.add("0:v:0")
                    cmd.add("-map")
                    cmd.add("0:a?")
                }
                cmd.add("-c")
                cmd.add("copy")
                cmd.add("-avoid_negative_ts")
                cmd.add("make_zero")
                if (format.equals("mp4", ignoreCase = true)) {
                    cmd.add("-movflags")
                    cmd.add("+faststart")
                }
            }

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()
            cmd.add(outputFile.absolutePath)

            Log.d(TAG, "Starting FFmpeg remux: ${cmd.joinToString(" ")}")

            val pb = ProcessBuilder(cmd)
            pb.environment()["LD_LIBRARY_PATH"] = ldPath
            pb.environment()["PATH"] = "$nativeLibDir:${System.getenv("PATH") ?: "/system/bin"}"
            pb.environment()["TMPDIR"] = context.cacheDir.absolutePath
            pb.redirectErrorStream(true)

            val proc = pb.start()

            val logOutput = StringBuilder()
            val readerThread = Thread {
                try {
                    proc.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            synchronized(logOutput) {
                                if (logOutput.length < 16384) {
                                    logOutput.appendLine(line)
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }
            readerThread.start()

            val exitCode = proc.waitFor()
            readerThread.join(3000)

            if (exitCode != 0) {
                Log.w(TAG, "FFmpeg remux finished with error code $exitCode:\n$logOutput")
            } else {
                Log.d(TAG, "FFmpeg remux process completed successfully")
            }

            exitCode == 0 && outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "FFmpeg remux execution error: ${e.message}", e)
            false
        }
    }

    private fun tryNativeMux(
        videoFile: File,
        audioFile: File?,
        outputFile: File
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            videoExtractor = MediaExtractor().apply {
                setDataSource(videoFile.absolutePath)
            }

            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                Log.w(TAG, "No video track discovered in ${videoFile.name}")
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)

            // Determine audio track: separate audioFile or interleaved in videoFile
            var effectiveAudioExtractor: MediaExtractor? = null
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
                try {
                    val aExtractor = MediaExtractor().apply {
                        setDataSource(audioFile.absolutePath)
                    }
                    for (i in 0 until aExtractor.trackCount) {
                        val format = aExtractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                        if (mime.startsWith("audio/")) {
                            audioTrackIndex = i
                            audioFormat = format
                            audioExtractor = aExtractor
                            effectiveAudioExtractor = aExtractor
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open audio track from ${audioFile.name}: ${e.message}")
                }
            }

            if (audioTrackIndex == -1) {
                for (i in 0 until videoExtractor.trackCount) {
                    val format = videoExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        effectiveAudioExtractor = videoExtractor
                        break
                    }
                }
            }

            if (effectiveAudioExtractor != null && audioTrackIndex != -1) {
                effectiveAudioExtractor.selectTrack(audioTrackIndex)
            }

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val muxVideoTrack = muxer.addTrack(videoFormat)
            val muxAudioTrack = if (audioFormat != null) muxer.addTrack(audioFormat) else -1

            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // Pump video samples: only apply stream offset on genuine stream resets (>1 second discontinuity)
            // Never increment offset on normal B-frame fluctuations
            var lastVideoPts = -1L
            var videoPtsOffset = 0L

            while (true) {
                buffer.clear()
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = videoExtractor.sampleTime
                if (lastVideoPts != -1L && sampleTime < lastVideoPts && (lastVideoPts - sampleTime) > 1_000_000L) {
                    videoPtsOffset += (lastVideoPts - sampleTime) + 33_333L
                }
                lastVideoPts = sampleTime

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTime + videoPtsOffset
                bufferInfo.flags = videoExtractor.sampleFlags

                muxer.writeSampleData(muxVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Pump audio samples: only apply stream offset on genuine stream resets (>1 second discontinuity)
            if (muxAudioTrack != -1 && effectiveAudioExtractor != null) {
                var lastAudioPts = -1L
                var audioPtsOffset = 0L

                while (true) {
                    buffer.clear()
                    val sampleSize = effectiveAudioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTime = effectiveAudioExtractor.sampleTime
                    if (lastAudioPts != -1L && sampleTime < lastAudioPts && (lastAudioPts - sampleTime) > 1_000_000L) {
                        audioPtsOffset += (lastAudioPts - sampleTime) + 21_333L
                    }
                    lastAudioPts = sampleTime

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime + audioPtsOffset
                    bufferInfo.flags = effectiveAudioExtractor.sampleFlags

                    muxer.writeSampleData(muxAudioTrack, buffer, bufferInfo)
                    effectiveAudioExtractor.advance()
                }
            }

            muxer.stop()
            Log.d(TAG, "Native MediaMuxer success -> ${outputFile.name} (${outputFile.length()} bytes)")
            return outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "Native MediaMuxer failed: ${e.message}")
            return false
        } finally {
            try { videoExtractor?.release() } catch (ignored: Exception) {}
            try { audioExtractor?.release() } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }
}
