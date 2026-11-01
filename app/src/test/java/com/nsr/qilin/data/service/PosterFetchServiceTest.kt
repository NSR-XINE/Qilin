package com.nsr.qilin.data.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterFetchServiceTest {

    @Test
    fun testFormatCustomUrl_withTitlePlaceholder() {
        val template = "https://example.com/search?q={title}&category=donghua"
        val formatted = PosterFetchService.formatCustomUrl(template, "Soul Land")
        assertEquals("https://example.com/search?q=Soul+Land&category=donghua", formatted)
    }

    @Test
    fun testFormatCustomUrl_withQueryPlaceholder() {
        val template = "https://example.com/?query={query}"
        val formatted = PosterFetchService.formatCustomUrl(template, "Battle Through The Heavens")
        assertEquals("https://example.com/?query=Battle+Through+The+Heavens", formatted)
    }

    @Test
    fun testFormatCustomUrl_withSearchParameterEnd() {
        val template = "https://donghuastream.org/?s="
        val formatted = PosterFetchService.formatCustomUrl(template, "Perfect World")
        assertEquals("https://donghuastream.org/?s=Perfect+World", formatted)
    }

    @Test
    fun testFormatCustomUrl_withoutScheme() {
        val template = "donghuaworld.net/?s="
        val formatted = PosterFetchService.formatCustomUrl(template, "A Will Eternal")
        assertEquals("https://donghuaworld.net/?s=A+Will+Eternal", formatted)
    }

    @Test
    fun testFormatCustomUrl_bareDomain() {
        val template = "https://animeschedule.net"
        val formatted = PosterFetchService.formatCustomUrl(template, "Renegade Immortal")
        assertEquals("https://animeschedule.net/?s=Renegade+Immortal", formatted)
    }

    @Test
    fun testResolveWatchUrl_perShowCustomUrlTakesPrecedence() {
        val customWatchUrl = "https://custom-site.com/episodes/soul-land-1"
        val result = PosterFetchService.resolveWatchUrl(
            showTitle = "Soul Land",
            customWatchUrl = customWatchUrl,
            providerPreference = "direct",
            customProviderUrl = "https://global-provider.com/?s="
        )
        assertEquals("https://custom-site.com/episodes/soul-land-1", result)
    }

    @Test
    fun testResolveWatchUrl_usesCustomProviderWhenShowUrlBlank() {
        val result = PosterFetchService.resolveWatchUrl(
            showTitle = "Soul Land",
            customWatchUrl = "",
            providerPreference = "direct",
            customProviderUrl = "https://my-donghua.site/?s="
        )
        assertEquals("https://my-donghua.site/?s=Soul+Land", result)
    }

    @Test
    fun testResolveWatchUrl_fallbackToDonghuaStreamWhenAllBlank() {
        val result = PosterFetchService.resolveWatchUrl(
            showTitle = "Soul Land",
            customWatchUrl = null,
            providerPreference = "direct",
            customProviderUrl = null
        )
        assertEquals("https://donghuastream.org/?s=Soul+Land", result)
    }

    @Test
    fun testResolveWatchUrl_builtInDonghuaStream() {
        val result = PosterFetchService.resolveWatchUrl(
            showTitle = "Soul Land",
            customWatchUrl = null,
            providerPreference = "donghuastream"
        )
        assertEquals("https://donghuastream.org/?s=Soul+Land", result)
    }

    @Test
    fun testResolveWatchUrl_builtInLuciferDonghua() {
        val result = PosterFetchService.resolveWatchUrl(
            showTitle = "Soul Land",
            customWatchUrl = null,
            providerPreference = "luciferdonghua"
        )
        assertEquals("https://luciferdonghua.in/?s=Soul+Land", result)
    }
}
