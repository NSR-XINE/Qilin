package com.nsr.qilin.data.service.detector

/**
 * Type of detected media resource.
 */
enum class ContentType {
    M3U8,
    MPD,
    VIDEO,
    AUDIO,
    OTHER
}

/**
 * Quality rendition variant parsed from an HLS or MPD manifest.
 */
data class VideoQualityVariant(
    val url: String,
    val quality: String, // e.g. "1080p", "720p", "480p", "360p"
    val bandwidth: Long = 0L,
    val resolution: String? = null, // e.g. "1920x1080"
    val codecs: String? = null
)

/**
 * Represents a video or audio stream detected on a website.
 */
data class DetectedVideo(
    val url: String,
    val title: String? = null,
    val contentType: ContentType = ContentType.VIDEO,
    val quality: String? = null,
    val contentLength: Long = 0L,
    val durationSec: Double = 0.0,
    val headers: Map<String, String> = emptyMap(),
    val variants: List<VideoQualityVariant> = emptyList(),
    val detectedAt: Long = System.currentTimeMillis()
)
