package com.nsr.qilin.data.service.detector

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteVideoDetectorTest {

    @Test
    fun testVideoUtils_staticFilter() {
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/assets/app.js"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/styles/main.css?v=2"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/images/logo.png"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/font.woff2"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://google-analytics.com/analytics.js"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("data:image/png;base64,12345"))

        assertFalse(VideoUtils.isStaticOrNonMediaUrl("https://example.com/stream/index.m3u8"))
        assertFalse(VideoUtils.isStaticOrNonMediaUrl("https://example.com/video/movie.mp4"))
        assertFalse(VideoUtils.isStaticOrNonMediaUrl("https://example.com/dynamic_stream?id=123"))

        // Segments should be classified as static/non-media so they are not captured as independent streams
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/stream/seg-1.ts"))
        assertTrue(VideoUtils.isStaticOrNonMediaUrl("https://example.com/stream/chunk_001.m4s"))
    }

    @Test
    fun testVideoUtils_segmentDetection() {
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/hls/segment-001.ts?token=123"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/dash/video_chunk_001.m4s"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/enc.key"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/subtitles/sub.vtt"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/subtitles/sub.srt"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/hls/segment/42"))

        // Progressive chunk patterns that must be detected as segments (not full files)
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/chunk_1.mp4"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/frag-002.mp4"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/part1.mp4"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/video_0001.mp4"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/media.mp4?range=0-1048576"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/media.mp4?bytes=0-50000"))
        assertTrue(VideoUtils.isSegmentUrl("https://example.com/stream/media.mp4?start=0&end=1000"))

        assertFalse(VideoUtils.isSegmentUrl("https://example.com/stream/playlist.m3u8"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/dash/manifest.mpd"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/video.mp4"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/video.mkv"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/audio.m4a"))
    }

    @Test
    fun testWebsiteVideoDetector_formattingHelpers() {
        assertEquals("0 B", WebsiteVideoDetector.formatBytes(0))
        assertEquals("500 B", WebsiteVideoDetector.formatBytes(500))
        assertEquals("2 KB", WebsiteVideoDetector.formatBytes(1536))
        assertEquals("100 MB", WebsiteVideoDetector.formatBytes(100 * 1024 * 1024))
        assertEquals("1.5 GB", WebsiteVideoDetector.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))

        assertEquals("", WebsiteVideoDetector.formatDuration(0))
        assertEquals("00:45", WebsiteVideoDetector.formatDuration(45))
        assertEquals("24:15", WebsiteVideoDetector.formatDuration(1455))
        assertEquals("1:05:30", WebsiteVideoDetector.formatDuration(3930))
    }

    @Test
    fun testVideoUtils_quickDetect() {
        assertEquals(ContentType.M3U8, VideoUtils.quickDetectContentType("https://example.com/vod/playlist.m3u8"))
        assertEquals(ContentType.M3U8, VideoUtils.quickDetectContentType("https://example.com/hls/live/stream"))
        assertEquals(ContentType.MPD, VideoUtils.quickDetectContentType("https://example.com/dash/manifest.mpd"))
        assertEquals(ContentType.VIDEO, VideoUtils.quickDetectContentType("https://example.com/file.mp4?token=abc"))
        assertEquals(ContentType.VIDEO, VideoUtils.quickDetectContentType("https://example.com/file.mkv"))
        assertEquals(ContentType.AUDIO, VideoUtils.quickDetectContentType("https://example.com/track.m4a"))
        assertEquals(ContentType.OTHER, VideoUtils.quickDetectContentType("https://example.com/page.html"))
        // Segments within HLS paths must not be recognized as M3U8 playlists
        assertEquals(ContentType.OTHER, VideoUtils.quickDetectContentType("https://example.com/hls/live/segment-001.ts"))
        assertEquals(ContentType.OTHER, VideoUtils.quickDetectContentType("https://example.com/hls/live/segment_001.m4s"))
    }

    @Test
    fun testVideoUtils_probeContentType() {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/vnd.apple.mpegurl")
                    .body("#EXTM3U\n".toResponseBody("application/vnd.apple.mpegurl".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val detected = VideoUtils.probeContentType("https://example.com/stream_probe", client = mockClient)
        assertEquals(ContentType.M3U8, detected)
    }

    @Test
    fun testWebsiteVideoDetector_hlsMasterParsing() {
        val masterManifest = """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
            1080p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2200000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2"
            720p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1100000,RESOLUTION=854x480,CODECS="avc1.4d401e,mp4a.40.2"
            480p.m3u8
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/vnd.apple.mpegurl")
                    .body(masterManifest.toResponseBody("application/vnd.apple.mpegurl".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val masterUrl = "https://example.com/master.m3u8"
        val detector = WebsiteVideoDetector(mockClient)

        val capturedVideo = detector.inspectUrl(masterUrl, pageTitle = "Test Donghua")

        assertNotNull(capturedVideo)
        assertEquals(ContentType.M3U8, capturedVideo?.contentType)
        assertEquals("Test Donghua", capturedVideo?.title)
        assertEquals(3, capturedVideo?.variants?.size)

        val variants = capturedVideo!!.variants
        assertEquals("1080p", variants[0].quality)
        assertEquals(4500000L, variants[0].bandwidth)
        assertEquals("1920x1080", variants[0].resolution)

        assertEquals("720p", variants[1].quality)
        assertEquals(2200000L, variants[1].bandwidth)

        assertEquals("480p", variants[2].quality)
        assertEquals(1100000L, variants[2].bandwidth)

        val options = detector.toQualityOptions(capturedVideo, currentPlaybackHeight = 1080)
        assertEquals(3, options.size)
        assertEquals("1080p", options[0].badge)
        assertTrue(options[0].isPlayingNow)
        assertFalse(options[1].isPlayingNow)
    }

    @Test
    fun testWebsiteVideoDetector_deduplication() {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "video/mp4")
                    .body("".toResponseBody("video/mp4".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val detector = WebsiteVideoDetector(mockClient)
        val mockUrl = "https://example.com/video.mp4"

        val first = detector.inspectUrl(mockUrl)
        val second = detector.inspectUrl(mockUrl)

        assertNotNull(first)
        assertNotNull(second)
        // Ensure detector.detectedVideos has only 1 entry (not duplicated)
        assertEquals(1, detector.detectedVideos.value.size)
    }

    @Test
    fun testWebsiteVideoDetector_hlsMasterWithAudioTrack() {
        val masterManifest = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="0_aac_q2",NAME="Main",DEFAULT=YES,URI="audio/manifest.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,CODECS="avc1.640028",AUDIO="0_aac_q2"
            1080p.m3u8
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/vnd.apple.mpegurl")
                    .body(masterManifest.toResponseBody("application/vnd.apple.mpegurl".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val masterUrl = "https://example.com/master.m3u8"
        val detector = WebsiteVideoDetector(mockClient)
        val captured = detector.inspectUrl(masterUrl, pageTitle = "Test Donghua")

        assertNotNull(captured)
        assertEquals("https://example.com/audio/manifest.m3u8", captured?.defaultAudioUrl)
        assertEquals(1, captured?.variants?.size)
        assertEquals("https://example.com/audio/manifest.m3u8", captured?.variants?.get(0)?.audioUrl)

        val options = detector.toQualityOptions(captured!!)
        assertEquals(1, options.size)
        assertEquals("https://example.com/audio/manifest.m3u8", options[0].audioUrl)
        assertEquals(masterUrl, options[0].masterUrl)
    }

    @Test
    fun testWebsiteVideoDetector_childPlaylistInheritsMasterAudioAndDoesNotDisplaceMaster() {
        val masterManifest = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio1",NAME="Stereo",DEFAULT=YES,URI="audio/manifest.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,AUDIO="audio1"
            video/720p/manifest.m3u8
        """.trimIndent()

        val childManifest = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:1
            #EXTINF:6.0,
            seg_1.m4s
            #EXT-X-ENDLIST
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val bodyText = if (url.contains("master.m3u8")) masterManifest else childManifest
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/vnd.apple.mpegurl")
                    .body(bodyText.toResponseBody("application/vnd.apple.mpegurl".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val detector = WebsiteVideoDetector(mockClient)
        val masterUrl = "https://example.com/master.m3u8"
        val childUrl = "https://example.com/video/720p/manifest.m3u8"

        // 1. Inspect master first
        val capturedMaster = detector.inspectUrl(masterUrl, pageTitle = "Donghua Master")
        assertNotNull(capturedMaster)
        assertEquals(1, detector.detectedVideos.value.size)
        assertEquals("https://example.com/audio/manifest.m3u8", capturedMaster?.defaultAudioUrl)

        // 2. Web player starts playing child playlist -> inspect child
        val capturedChild = detector.inspectUrl(childUrl, pageTitle = "Donghua Child")
        assertNotNull(capturedChild)

        // Child media playlist should inherit cached master audio URL
        assertEquals("https://example.com/audio/manifest.m3u8", capturedChild?.defaultAudioUrl)

        // Options generated for child should have audioUrl and masterUrl pointing to master
        val childOptions = detector.toQualityOptions(capturedChild!!)
        assertTrue(childOptions.isNotEmpty())
        assertEquals("https://example.com/audio/manifest.m3u8", childOptions[0].audioUrl)
        assertEquals(masterUrl, childOptions[0].masterUrl)

        // pushDetectedVideo should NOT displace or add single-variant child if master is present
        assertEquals(1, detector.detectedVideos.value.size)
        assertEquals(masterUrl, detector.detectedVideos.value[0].url)
    }
}
