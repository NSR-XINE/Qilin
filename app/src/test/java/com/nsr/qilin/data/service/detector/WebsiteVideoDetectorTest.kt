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

        assertFalse(VideoUtils.isSegmentUrl("https://example.com/stream/playlist.m3u8"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/dash/manifest.mpd"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/video.mp4"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/video.mkv"))
        assertFalse(VideoUtils.isSegmentUrl("https://example.com/audio.m4a"))
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
}
