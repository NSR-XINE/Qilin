package com.nsr.qilin.data.service

import androidx.annotation.Keep

@Keep
data class DownloadModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String = "",
    var progress: Int = 0,
    var speed: String = "0 KB/s",
    var status: String = "DOWNLOADING",
    var filePath: String = "",
    var downloadedSize: String = "0 MB",
    var totalSize: String = "0 MB",
    var eta: String = "--:--",
    var duration: String = "",
    var formatName: String = "",
    var resolution: String = "",
    var statusDetail: String = "",
    var url: String = "",
    var formatId: String = "",
    var referer: String = "",
    var isAudioOnly: Boolean = false,
    var isHls: Boolean = false,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long = 0L,
    var errorMessage: String = ""
)
