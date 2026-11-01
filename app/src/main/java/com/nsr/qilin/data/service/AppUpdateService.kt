package com.nsr.qilin.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String
)

data class AppUpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val asset: ReleaseAsset,
    val isNewer: Boolean
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val info: AppUpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object AppUpdateService {
    private const val TAG = "AppUpdateService"
    private const val GITHUB_REPO = "NSR-XINE/Qilin"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Qilin-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API error: ${response.code}"))
            }

            val bodyStr = response.body.string()
            if (bodyStr.isBlank()) return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(bodyStr)

            val tagName = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "No release notes provided.")
            val publishedAt = json.optString("published_at", "")
            val versionName = tagName.removePrefix("v").removePrefix("V")

            val assetsJson = json.optJSONArray("assets")
            val assetsList = mutableListOf<ReleaseAsset>()
            if (assetsJson != null) {
                for (i in 0 until assetsJson.length()) {
                    val a = assetsJson.getJSONObject(i)
                    val name = a.optString("name")
                    val size = a.optLong("size")
                    val downloadUrl = a.optString("browser_download_url")
                    if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                        assetsList.add(ReleaseAsset(name, size, downloadUrl))
                    }
                }
            }

            if (assetsList.isEmpty()) {
                return@withContext Result.failure(Exception("No APK assets found in release $tagName"))
            }

            val bestAsset = selectBestAsset(assetsList)
                ?: return@withContext Result.failure(Exception("No compatible APK found for device"))

            val isNewer = isVersionNewer(remoteTag = tagName, localVersionName = currentVersionName)

            val info = AppUpdateInfo(
                versionName = versionName,
                tagName = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                asset = bestAsset,
                isNewer = isNewer
            )

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            Result.failure(e)
        }
    }

    fun isVersionNewer(remoteTag: String, localVersionName: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanLocal = localVersionName.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote.equals(cleanLocal, ignoreCase = true)) return false

        // Extract semantic numeric version parts
        val remoteCore = cleanRemote.split("-", "_")[0]
        val localCore = cleanLocal.split("-", "_")[0]

        val remoteParts = remoteCore.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localCore.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        // Compare pre-release tags: non-beta is newer than beta
        val remoteIsBeta = cleanRemote.contains("beta", ignoreCase = true)
        val localIsBeta = cleanLocal.contains("beta", ignoreCase = true)
        if (localIsBeta && !remoteIsBeta) return true

        return false
    }

    fun selectBestAsset(
        assets: List<ReleaseAsset>,
        supportedAbis: Array<String> = Build.SUPPORTED_ABIS
    ): ReleaseAsset? {
        val abis = supportedAbis.map { it.lowercase() }

        // 1. Check for 64-bit arm64-v8a
        if (abis.any { it.contains("arm64") || it.contains("aarch64") }) {
            val match = assets.find {
                val n = it.name.lowercase()
                n.contains("arm64") || n.contains("64bit")
            }
            if (match != null) return match
        }

        // 2. Check for 32-bit armeabi-v7a
        if (abis.any { it.contains("v7a") || it.contains("armeabi") }) {
            val match = assets.find {
                val n = it.name.lowercase()
                n.contains("armv7") || n.contains("32bit")
            }
            if (match != null) return match
        }

        // 3. Fallback to universal
        val universalMatch = assets.find { it.name.lowercase().contains("universal") }
        if (universalMatch != null) return universalMatch

        // 4. Fallback to any APK
        return assets.firstOrNull()
    }

    suspend fun downloadApk(
        context: Context,
        asset: ReleaseAsset,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, asset.name)

            // If file already exists and is complete size, return it directly!
            if (apkFile.exists() && apkFile.length() == asset.size && asset.size > 0) {
                onProgress(1.0f, asset.size, asset.size)
                return@withContext Result.success(apkFile)
            }

            // Temp file while downloading
            val tempFile = File(updateDir, "${asset.name}.part")
            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder()
                .url(asset.downloadUrl)
                .header("User-Agent", "Qilin-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
            }

            val body = response.body
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else asset.size

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 100 || totalRead == totalBytes) {
                            lastUpdate = now
                            val progress = if (totalBytes > 0) (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                            onProgress(progress, totalRead, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists()) {
                if (apkFile.exists()) apkFile.delete()
                tempFile.renameTo(apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            Result.failure(e)
        }
    }

    fun promptInstall(context: Context, apkFile: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permIntent)
                    return false
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            false
        }
    }
}
