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
 * High-performance container muxer using Android's native MediaMuxer and MediaExtractor.
 * Combines separate video and audio streams (or remuxes concatenated fMP4 / MPEG-TS files)
 * into a 100% compliant, seekable, unified MP4 file with monotonic timestamps.
 *
 * Includes an automated fallback to the bundled FFmpeg binary (libffmpeg.so) if native
 * hardware demuxers encounter unindexed fragments or non-standard container metadata.
 */
object MediaMuxerHelper {
    private const val TAG = "MediaMuxerHelper"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    fun mux(
        videoFile: File,
        audioFile: File?,
        outputFile: File,
        context: Context? = null
    ): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e(TAG, "Video file missing or empty: ${videoFile.absolutePath}")
            return false
        }

        // Try primary native MediaMuxer
        val success = tryNativeMux(videoFile, audioFile, outputFile)
        if (success && outputFile.exists() && outputFile.length() > 0L) {
            return true
        }

        // Fallback to bundled FFmpeg binary if available
        if (context != null) {
            Log.i(TAG, "Native MediaMuxer failed or produced empty file; attempting FFmpeg fallback...")
            val ffmpegSuccess = tryFFmpegMux(context, videoFile, audioFile, outputFile)
            if (ffmpegSuccess && outputFile.exists() && outputFile.length() > 0L) {
                return true
            }
        }

        return false
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

            // Pump video samples with monotonic PTS protection
            var lastVideoPts = -1L
            var videoPtsOffset = 0L

            while (true) {
                buffer.clear()
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = videoExtractor.sampleTime
                if (lastVideoPts != -1L && sampleTime < lastVideoPts) {
                    videoPtsOffset += (lastVideoPts - sampleTime) + 33_333L // ~30fps frame increment
                }
                lastVideoPts = sampleTime

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTime + videoPtsOffset
                bufferInfo.flags = videoExtractor.sampleFlags

                muxer.writeSampleData(muxVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Pump audio samples with monotonic PTS protection
            if (muxAudioTrack != -1 && effectiveAudioExtractor != null) {
                var lastAudioPts = -1L
                var audioPtsOffset = 0L

                while (true) {
                    buffer.clear()
                    val sampleSize = effectiveAudioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTime = effectiveAudioExtractor.sampleTime
                    if (lastAudioPts != -1L && sampleTime < lastAudioPts) {
                        audioPtsOffset += (lastAudioPts - sampleTime) + 21_333L // ~1024 samples at 48kHz
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

    private fun tryFFmpegMux(
        context: Context,
        videoFile: File,
        audioFile: File?,
        outputFile: File
    ): Boolean {
        return try {
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val ffmpegBin = File(nativeLibDir, "libffmpeg.so")
            if (!ffmpegBin.exists() || !ffmpegBin.canExecute()) {
                Log.w(TAG, "libffmpeg.so missing or not executable at ${ffmpegBin.absolutePath}")
                return false
            }

            val ffmpegPkgDir = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
            val ldPath = if (ffmpegPkgDir.exists()) {
                "${ffmpegPkgDir.absolutePath}:$nativeLibDir"
            } else {
                nativeLibDir
            }

            val cmd = mutableListOf<String>()
            cmd.add(ffmpegBin.absolutePath)
            cmd.add("-y")
            cmd.add("-i")
            cmd.add(videoFile.absolutePath)
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
                cmd.add("-i")
                cmd.add(audioFile.absolutePath)
            }
            cmd.add("-c")
            cmd.add("copy")
            cmd.add("-movflags")
            cmd.add("+faststart")
            cmd.add(outputFile.absolutePath)

            val pb = ProcessBuilder(cmd)
            pb.environment()["LD_LIBRARY_PATH"] = ldPath
            pb.redirectErrorStream(true)

            val proc = pb.start()
            val exitCode = proc.waitFor()
            Log.d(TAG, "FFmpeg remux finished with code $exitCode (size=${outputFile.length()})")
            exitCode == 0 && outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "FFmpeg remux execution error: ${e.message}")
            false
        }
    }
}
