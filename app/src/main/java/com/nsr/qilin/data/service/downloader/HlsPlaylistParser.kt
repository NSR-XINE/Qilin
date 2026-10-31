package com.nsr.qilin.data.service.downloader

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

/**
 * Robust HLS Playlist Parser ported from super-video-downloader.
 * Supports Master and Media playlists, relative URI resolution,
 * fMP4 initialization segments, and AES-128 encryption keys.
 */
object HlsPlaylistParser {

    data class MediaSegment(
        val url: String,
        val duration: Double,
        val index: Int,
        val title: String? = null,
        val isFmp4: Boolean = false,
        val initSegmentUrl: String? = null,
        val keyUrl: String? = null,
        val keyIv: String? = null
    )

    data class Variant(
        val url: String,
        val bandwidth: Long,
        val resolution: String?,
        val codecs: String?,
        val audioGroupId: String? = null
    )

    data class MasterPlaylist(
        val baseUri: String,
        val variants: List<Variant>,
        val audioUris: List<String>
    )

    data class MediaPlaylist(
        val baseUri: String,
        val segments: List<MediaSegment>,
        val targetDuration: Int,
        val hasEndList: Boolean,
        val isFmp4: Boolean,
        val initSegmentUrl: String?,
        val keyUrl: String?
    ) {
        val totalDuration: Double by lazy { segments.sumOf { it.duration } }
    }

    sealed class ParsedHlsResult {
        data class Master(val master: MasterPlaylist) : ParsedHlsResult()
        data class Media(val media: MediaPlaylist) : ParsedHlsResult()
    }

    fun parse(content: String, baseUri: String): ParsedHlsResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.none { it.startsWith("#EXTM3U") }) {
            // Not a strict M3U8, but try to parse lines if EXTINF exists
        }

        val isMaster = lines.any { it.startsWith("#EXT-X-STREAM-INF") }
        return if (isMaster) {
            ParsedHlsResult.Master(parseMasterPlaylist(lines, baseUri))
        } else {
            ParsedHlsResult.Media(parseMediaPlaylist(lines, baseUri))
        }
    }

    private fun parseMasterPlaylist(lines: List<String>, baseUri: String): MasterPlaylist {
        val variants = mutableListOf<Variant>()
        val audioUris = mutableListOf<String>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val attrs = parseAttributes(line.substringAfter(':'))
                val bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L
                val resolution = attrs["RESOLUTION"]
                val codecs = attrs["CODECS"]
                val audioGroupId = attrs["AUDIO"]

                // Next non-comment line is the variant URI
                var j = i + 1
                while (j < lines.size && lines[j].startsWith("#")) {
                    j++
                }
                if (j < lines.size) {
                    val variantUrl = resolveUrl(baseUri, lines[j])
                    variants.add(
                        Variant(
                            url = variantUrl,
                            bandwidth = bandwidth,
                            resolution = resolution,
                            codecs = codecs,
                            audioGroupId = audioGroupId
                        )
                    )
                    i = j
                }
            } else if (line.startsWith("#EXT-X-MEDIA:") && line.contains("TYPE=AUDIO", ignoreCase = true)) {
                val attrs = parseAttributes(line.substringAfter(':'))
                val uri = attrs["URI"]
                if (!uri.isNullOrEmpty()) {
                    audioUris.add(resolveUrl(baseUri, uri))
                }
            }
            i++
        }

        return MasterPlaylist(
            baseUri = baseUri,
            variants = variants.sortedByDescending { it.bandwidth },
            audioUris = audioUris
        )
    }

    private fun parseMediaPlaylist(lines: List<String>, baseUri: String): MediaPlaylist {
        val segments = mutableListOf<MediaSegment>()
        var targetDuration = 10
        var hasEndList = false
        var currentInitSegment: String? = null
        var currentKeyUrl: String? = null
        var currentKeyIv: String? = null
        var segmentIndex = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.substringAfter(':').toIntOrNull() ?: 10
                }
                line.startsWith("#EXT-X-ENDLIST") -> {
                    hasEndList = true
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val attrs = parseAttributes(line.substringAfter(':'))
                    attrs["URI"]?.let { uri ->
                        currentInitSegment = resolveUrl(baseUri, uri)
                    }
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val attrs = parseAttributes(line.substringAfter(':'))
                    val method = attrs["METHOD"] ?: "NONE"
                    if (!method.equals("NONE", ignoreCase = true)) {
                        attrs["URI"]?.let { uri ->
                            currentKeyUrl = resolveUrl(baseUri, uri)
                        }
                        currentKeyIv = attrs["IV"]
                    } else {
                        currentKeyUrl = null
                        currentKeyIv = null
                    }
                }
                line.startsWith("#EXTINF:") -> {
                    val durationStr = line.substringAfter(':').substringBefore(',')
                    val duration = durationStr.toDoubleOrNull() ?: 0.0
                    val title = line.substringAfter(',', "").takeIf { it.isNotEmpty() }

                    var j = i + 1
                    while (j < lines.size && lines[j].startsWith("#")) {
                        if (lines[j].startsWith("#EXT-X-MAP:")) {
                            val attrs = parseAttributes(lines[j].substringAfter(':'))
                            attrs["URI"]?.let { uri ->
                                currentInitSegment = resolveUrl(baseUri, uri)
                            }
                        }
                        j++
                    }
                    if (j < lines.size) {
                        val segmentUrl = resolveUrl(baseUri, lines[j])
                        val isFmp4 = currentInitSegment != null || segmentUrl.endsWith(".m4s", ignoreCase = true)
                        segments.add(
                            MediaSegment(
                                url = segmentUrl,
                                duration = duration,
                                index = segmentIndex++,
                                title = title,
                                isFmp4 = isFmp4,
                                initSegmentUrl = currentInitSegment,
                                keyUrl = currentKeyUrl,
                                keyIv = currentKeyIv
                            )
                        )
                        i = j
                    }
                }
            }
            i++
        }

        return MediaPlaylist(
            baseUri = baseUri,
            segments = segments,
            targetDuration = targetDuration,
            hasEndList = hasEndList,
            isFmp4 = currentInitSegment != null || segments.any { it.isFmp4 },
            initSegmentUrl = currentInitSegment,
            keyUrl = currentKeyUrl
        )
    }

    private fun parseAttributes(attrString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|([^,]*))""")
        for (match in regex.findAll(attrString)) {
            val key = match.groups[1]?.value?.uppercase() ?: continue
            val value = match.groups[2]?.value ?: match.groups[3]?.value ?: ""
            map[key] = value
        }
        return map
    }

    fun resolveUrl(baseUri: String, relativeUrl: String): String {
        val cleanRel = relativeUrl.trim().replace("\"", "")
        if (cleanRel.startsWith("http://") || cleanRel.startsWith("https://")) {
            return cleanRel
        }
        return try {
            baseUri.toHttpUrlOrNull()?.resolve(cleanRel)?.toString()
                ?: URI(baseUri).resolve(cleanRel).toString()
        } catch (e: Exception) {
            val baseWithoutQuery = baseUri.substringBefore('?')
            val parent = baseWithoutQuery.substringBeforeLast('/', "")
            if (parent.isNotEmpty()) "$parent/$cleanRel" else cleanRel
        }
    }
}
