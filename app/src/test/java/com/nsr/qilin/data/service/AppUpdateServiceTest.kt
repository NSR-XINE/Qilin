package com.nsr.qilin.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateServiceTest {

    @Test
    fun isVersionNewer_majorVersionBump() {
        assertTrue(AppUpdateService.isVersionNewer("7.0.0", "6.8-beta"))
        assertTrue(AppUpdateService.isVersionNewer("v7.0", "v6.8"))
        assertFalse(AppUpdateService.isVersionNewer("5.9.9", "6.0"))
    }

    @Test
    fun isVersionNewer_minorVersionBump() {
        assertTrue(AppUpdateService.isVersionNewer("6.9-beta", "6.8-beta"))
        assertTrue(AppUpdateService.isVersionNewer("v6.9.0", "6.8-beta"))
        assertFalse(AppUpdateService.isVersionNewer("6.7-beta", "6.8-beta"))
    }

    @Test
    fun isVersionNewer_patchVersionBump() {
        assertTrue(AppUpdateService.isVersionNewer("6.8.1", "6.8"))
        assertTrue(AppUpdateService.isVersionNewer("6.8.1-beta", "6.8-beta"))
        assertFalse(AppUpdateService.isVersionNewer("6.8.0", "6.8.1"))
    }

    @Test
    fun isVersionNewer_identicalVersions_returnsFalse() {
        assertFalse(AppUpdateService.isVersionNewer("6.8-beta", "6.8-beta"))
        assertFalse(AppUpdateService.isVersionNewer("v6.8-beta", "6.8-beta"))
        assertFalse(AppUpdateService.isVersionNewer("6.8", "6.8"))
    }

    @Test
    fun selectBestAsset_prefersMatchingArchitecture() {
        val assets = listOf(
            ReleaseAsset("Qilin-v6.9-beta-armv7a-32bit-release-12345.apk", 30000000L, "https://example.com/armv7a.apk"),
            ReleaseAsset("Qilin-v6.9-beta-arm64-64bit-release-12345.apk", 35000000L, "https://example.com/arm64.apk"),
            ReleaseAsset("Qilin-v6.9-beta-universal-release-12345.apk", 60000000L, "https://example.com/universal.apk")
        )

        // arm64 preference
        val arm64Choice = AppUpdateService.selectBestAsset(assets, arrayOf("arm64-v8a", "armeabi-v7a"))
        assertNotNull(arm64Choice)
        assertTrue(arm64Choice!!.name.contains("arm64-64bit"))

        // armv7a preference
        val armv7aChoice = AppUpdateService.selectBestAsset(assets, arrayOf("armeabi-v7a", "armeabi"))
        assertNotNull(armv7aChoice)
        assertTrue(armv7aChoice!!.name.contains("armv7a-32bit"))

        // fallback to universal if x86 device
        val x86Choice = AppUpdateService.selectBestAsset(assets, arrayOf("x86_64", "x86"))
        assertNotNull(x86Choice)
        assertTrue(x86Choice!!.name.contains("universal"))
    }

    @Test
    fun selectBestAsset_handlesSingleOrPartialAssetList() {
        val singleUniversal = listOf(
            ReleaseAsset("Qilin-v6.9-beta-universal-release-12345.apk", 60000000L, "https://example.com/universal.apk")
        )
        val selected = AppUpdateService.selectBestAsset(singleUniversal, arrayOf("arm64-v8a"))
        assertNotNull(selected)
        assertEquals("Qilin-v6.9-beta-universal-release-12345.apk", selected!!.name)
    }
}
