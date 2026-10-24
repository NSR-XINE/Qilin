package com.mydonghua.app.data.service

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
    var statusDetail: String = ""
)
