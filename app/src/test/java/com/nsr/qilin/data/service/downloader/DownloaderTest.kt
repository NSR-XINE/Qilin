package com.nsr.qilin.data.service.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFileBasedDownloadController_lifecycle() {
        val dir = tempFolder.newFolder("controller_test")
        val controller = FileBasedDownloadController(dir)

        controller.start()
        assertFalse(controller.isPauseRequested())
        assertFalse(controller.isCancelRequested())
        assertFalse(controller.isStopAndSaveRequested())
        assertFalse(controller.isInterrupted())

        controller.requestPause()
        assertTrue(controller.isPauseRequested())
        assertTrue(controller.isInterrupted())

        controller.requestCancel()
        assertTrue(controller.isCancelRequested())
        assertFalse(controller.isPauseRequested()) // pause flag cleared on cancel
        assertTrue(controller.isInterrupted())

        controller.clear()
        assertFalse(controller.isInterrupted())

        controller.requestStopAndSave()
        assertTrue(controller.isStopAndSaveRequested())
        assertTrue(controller.isInterrupted())

        controller.start()
        assertFalse(controller.isInterrupted())
    }

    @Test
    fun testHlsPlaylistParser_masterPlaylist() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.4d401e,mp4a.40.2"
            360p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2"
            720p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
            1080p/index.m3u8
        """.trimIndent()

        val baseUri = "https://example.com/vod/master.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Master)
        val master = (result as HlsPlaylistParser.ParsedHlsResult.Master).master

        assertEquals(3, master.variants.size)
        // Check sorted by bandwidth descending
        assertEquals(5000000L, master.variants[0].bandwidth)
        assertEquals("1920x1080", master.variants[0].resolution)
        assertEquals("https://example.com/vod/1080p/index.m3u8", master.variants[0].url)

        assertEquals(2500000L, master.variants[1].bandwidth)
        assertEquals("https://example.com/vod/720p/index.m3u8", master.variants[1].url)

        assertEquals(800000L, master.variants[2].bandwidth)
        assertEquals("https://example.com/vod/360p/index.m3u8", master.variants[2].url)
    }

    @Test
    fun testHlsPlaylistParser_mediaPlaylistStandardTs() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:0
            #EXTINF:5.000,
            segment_0.ts
            #EXTINF:6.000,
            segment_1.ts
            #EXTINF:4.500,
            https://cdn.example.com/absolute/segment_2.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val baseUri = "https://example.com/stream/index.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Media)
        val media = (result as HlsPlaylistParser.ParsedHlsResult.Media).media

        assertEquals(6, media.targetDuration)
        assertTrue(media.hasEndList)
        assertFalse(media.isFmp4)
        assertEquals(3, media.segments.size)

        assertEquals("https://example.com/stream/segment_0.ts", media.segments[0].url)
        assertEquals(5.0, media.segments[0].duration, 0.001)
        assertEquals(0, media.segments[0].index)

        assertEquals("https://example.com/stream/segment_1.ts", media.segments[1].url)
        assertEquals(6.0, media.segments[1].duration, 0.001)
        assertEquals(1, media.segments[1].index)

        assertEquals("https://cdn.example.com/absolute/segment_2.ts", media.segments[2].url)
        assertEquals(4.5, media.segments[2].duration, 0.001)
        assertEquals(2, media.segments[2].index)

        assertEquals(15.5, media.totalDuration, 0.001)
    }

    @Test
    fun testHlsPlaylistParser_fmp4InitializationSegment() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:4
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4.000,
            chunk_0.m4s
            #EXTINF:4.000,
            chunk_1.m4s
            #EXT-X-ENDLIST
        """.trimIndent()

        val baseUri = "https://example.com/hls/playlist.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Media)
        val media = (result as HlsPlaylistParser.ParsedHlsResult.Media).media

        assertTrue(media.isFmp4)
        assertEquals("https://example.com/hls/init.mp4", media.initSegmentUrl)
        assertEquals(2, media.segments.size)
        assertTrue(media.segments[0].isFmp4)
        assertEquals("https://example.com/hls/chunk_0.m4s", media.segments[0].url)
    }

    @Test
    fun testHlsPlaylistParser_encryptionKey() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:5
            #EXT-X-KEY:METHOD=AES-128,URI="enc.key",IV=0x1234567890abcdef
            #EXTINF:5.000,
            enc_seg_0.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val baseUri = "https://example.com/encrypted/index.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Media)
        val media = (result as HlsPlaylistParser.ParsedHlsResult.Media).media

        assertEquals("https://example.com/encrypted/enc.key", media.keyUrl)
        assertEquals(1, media.segments.size)
        assertEquals("https://example.com/encrypted/enc.key", media.segments[0].keyUrl)
        assertEquals("0x1234567890abcdef", media.segments[0].keyIv)
    }

    @Test
    fun testResolveUrl_edgeCases() {
        val base = "https://example.com/a/b/c/playlist.m3u8?token=xyz"
        assertEquals("https://example.com/a/b/c/segment.ts", HlsPlaylistParser.resolveUrl(base, "segment.ts"))
        assertEquals("https://example.com/root.ts", HlsPlaylistParser.resolveUrl(base, "/root.ts"))
        assertEquals("https://other.com/ext.ts", HlsPlaylistParser.resolveUrl(base, "https://other.com/ext.ts"))
        assertEquals("http://other.com/ext.ts", HlsPlaylistParser.resolveUrl(base, "http://other.com/ext.ts"))
    }

    @Test
    fun testHttpHeaderSanitizer() {
        val dirtyHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14)\r\n",
            "Cookie" to "cf_clearance=abc123\n; session=xyz\r",
            "Referer" to "https://example.com/watch?id=123",
            "Invalid\nKey" to "some_value",
            "" to "empty_key"
        )

        val reqBuilder = okhttp3.Request.Builder().url("https://example.com")
        reqBuilder.applySafeHeaders(dirtyHeaders)
        val request = reqBuilder.build()

        assertEquals("Mozilla/5.0 (Linux; Android 14)", request.header("User-Agent"))
        assertEquals("cf_clearance=abc123; session=xyz", request.header("Cookie"))
        assertEquals("https://example.com/watch?id=123", request.header("Referer"))
        assertFalse(request.headers.names().contains("Invalid\nKey"))
        assertFalse(request.headers.names().contains(""))
    }

    @Test
    fun testHlsPlaylistParser_byteRangeAndSlidingWindow() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:100
            #EXTINF:6.0,
            #EXT-X-BYTERANGE:500000@0
            video.mp4
            #EXTINF:6.0,
            #EXT-X-BYTERANGE:450000@500000
            video.mp4
        """.trimIndent() // Note: No #EXT-X-ENDLIST (sliding window live/rolling stream)

        val baseUri = "https://example.com/vod/playlist.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Media)
        val media = (result as HlsPlaylistParser.ParsedHlsResult.Media).media

        assertFalse(media.hasEndList)
        assertEquals(2, media.segments.size)

        val seg0 = media.segments[0]
        assertEquals("https://example.com/vod/video.mp4", seg0.url)
        assertEquals(500000L, seg0.byteRangeLength)
        assertEquals(0L, seg0.byteRangeOffset)

        val seg1 = media.segments[1]
        assertEquals("https://example.com/vod/video.mp4", seg1.url)
        assertEquals(450000L, seg1.byteRangeLength)
        assertEquals(500000L, seg1.byteRangeOffset)
    }

    @Test
    fun testHlsPlaylistParser_masterPlaylistWithDemuxedAudio() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_default",NAME="English",DEFAULT=YES,AUTOSELECT=YES,URI="audio/aac_q2/manifest.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_commentary",NAME="Commentary",DEFAULT=NO,AUTOSELECT=NO,URI="audio/commentary/manifest.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,CODECS="avc1.640028",AUDIO="audio_default"
            video/1080p/manifest.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2200000,RESOLUTION=1280x720,CODECS="avc1.4d401f",AUDIO="audio_default"
            video/720p/manifest.m3u8
        """.trimIndent()

        val baseUri = "https://example.com/stream/master.m3u8"
        val result = HlsPlaylistParser.parse(m3u8, baseUri)

        assertTrue(result is HlsPlaylistParser.ParsedHlsResult.Master)
        val master = (result as HlsPlaylistParser.ParsedHlsResult.Master).master

        assertEquals(2, master.variants.size)
        assertEquals(2, master.audioTracks.size)

        val defaultAudio = master.audioTracks.first { it.isDefault }
        assertEquals("audio_default", defaultAudio.groupId)
        assertEquals("English", defaultAudio.name)
        assertEquals("https://example.com/stream/audio/aac_q2/manifest.m3u8", defaultAudio.url)

        val var1080 = master.variants[0]
        assertEquals("1920x1080", var1080.resolution)
        assertEquals("audio_default", var1080.audioGroupId)
        assertEquals("https://example.com/stream/audio/aac_q2/manifest.m3u8", var1080.audioUrl)

        val var720 = master.variants[1]
        assertEquals("1280x720", var720.resolution)
        assertEquals("https://example.com/stream/audio/aac_q2/manifest.m3u8", var720.audioUrl)
    }

    @Test
    fun testMediaMuxerHelper_handlesEmptyOrMissingFileGracefully() {
        val nonExistent = File(tempFolder.root, "non_existent.mp4")
        val out = File(tempFolder.root, "out.mp4")
        val result = MediaMuxerHelper.mux(nonExistent, null, out)
        assertFalse(result)
        assertFalse(out.exists())
    }
}
