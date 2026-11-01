package com.nsr.qilin.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.rememberLiquidGlassChipBorder
import com.nsr.qilin.ui.GlassHeader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.WatchActivity
import com.nsr.qilin.data.service.DownloadEngine
import com.nsr.qilin.data.service.DownloadStatus
import com.nsr.qilin.data.service.DownloadTask
import com.nsr.qilin.ui.common.ColorOSDialog
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.JustSansFamily
import com.nsr.qilin.ui.theme.Spacing
import com.nsr.qilin.ui.theme.colorOSPressBounce
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tasks by DownloadEngine.tasksState.collectAsState()

    var offlineFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var storageSpace by remember { mutableStateOf(Pair(0L, 0L)) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var newFileNameText by remember { mutableStateOf("") }

    suspend fun refreshOfflineFiles() = withContext(Dispatchers.IO) {
        val dirs = DownloadEngine.getAllStorageDirs(context)
        val files = mutableListOf<File>()
        for (dir in dirs) {
            if (dir.exists()) {
                val list = dir.listFiles { _, name ->
                    name.endsWith(".mp4", ignoreCase = true) ||
                    name.endsWith(".mkv", ignoreCase = true) ||
                    name.endsWith(".ts", ignoreCase = true) ||
                    name.endsWith(".mp3", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true) ||
                    name.endsWith(".webm", ignoreCase = true) ||
                    name.endsWith(".flv", ignoreCase = true)
                }
                if (list != null) {
                    files.addAll(list)
                }
            }
        }
        val sortedFiles = files.distinctBy { it.name }.sortedByDescending { it.lastModified() }
        val storage = try {
            val primaryDir = DownloadEngine.getPrimaryStorageDir(context)
            val stat = StatFs(primaryDir.path)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(free, total)
        } catch (e: Exception) {
            try {
                val stat = StatFs(Environment.getExternalStorageDirectory().path)
                val free = stat.availableBlocksLong * stat.blockSizeLong
                val total = stat.blockCountLong * stat.blockSizeLong
                Pair(free, total)
            } catch (ex: Exception) {
                Pair(0L, 0L)
            }
        }
        withContext(Dispatchers.Main) {
            offlineFiles = sortedFiles
            storageSpace = storage
        }
    }

    LaunchedEffect(Unit) {
        refreshOfflineFiles()
    }

    LaunchedEffect(tasks) {
        if (tasks.any { it.status == DownloadStatus.COMPLETED }) {
            refreshOfflineFiles()
        }
    }

    val activeTasks = remember(tasks) {
        tasks.filter {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.PAUSED
        }
    }

    val totalDownloadsBytes = remember(offlineFiles) {
        offlineFiles.sumOf { it.length() }
    }

    val (freeBytes, totalDeviceBytes) = storageSpace
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Delete Confirmation Dialog (ColorOS 17 Dialog)
    fileToDelete?.let { file ->
        ColorOSDialog(
            onDismissRequest = { fileToDelete = null },
            title = "Delete Download?",
            message = "Are you sure you want to delete '${file.nameWithoutExtension}'? This file will be permanently removed.",
            confirmText = "Delete",
            dismissText = "Cancel",
            confirmButtonColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                val targetFile = file
                fileToDelete = null
                coroutineScope.launch(Dispatchers.IO) {
                    val tmp = File(targetFile.parentFile, "${targetFile.name}.tmp")
                    if (tmp.exists()) tmp.delete()
                    targetFile.delete()
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                    } catch (ignored: Exception) {}
                    refreshOfflineFiles()
                }
            },
            onDismiss = { fileToDelete = null }
        )
    }

    // Rename Video Dialog
    fileToRename?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            containerColor = if (isLight) Color(0xFFF7F9FC) else Color(0xFF1E2638),
            shape = RoundedCornerShape(CornerRadius.dialog),
            title = {
                Text(
                    text = "Rename Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JustSansFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a new name for this offline episode:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newFileNameText,
                        onValueChange = { newFileNameText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0082FB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = file
                        val clean = newFileNameText.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
                        fileToRename = null
                        if (clean.isNotBlank() && clean != target.nameWithoutExtension) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val ext = target.extension
                                val newFile = File(target.parentFile, if (ext.isNotEmpty()) "$clean.$ext" else clean)
                                if (!newFile.exists()) {
                                    if (target.renameTo(newFile)) {
                                        try {
                                            android.media.MediaScannerConnection.scanFile(
                                                context,
                                                arrayOf(target.absolutePath, newFile.absolutePath),
                                                null,
                                                null
                                            )
                                        } catch (ignored: Exception) {}
                                        refreshOfflineFiles()
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Rename",
                        color = Color(0xFF0082FB),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 84.dp
    val screenBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = headerTopPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Storage Overview Card
            item {
                StorageOverviewCard(
                    downloadsBytes = totalDownloadsBytes,
                    freeBytes = freeBytes,
                    totalBytes = totalDeviceBytes,
                    filesCount = offlineFiles.size
                )
            }

            // Active Downloads Section
            if (activeTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = Spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0082FB))
                        )
                        Text(
                            text = "ACTIVE DOWNLOADS (${activeTasks.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0082FB),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                items(activeTasks, key = { it.id }) { task ->
                    ActiveDownloadCard(
                        task = task,
                        onCancel = { DownloadEngine.cancelDownload(task.id) }
                    )
                }
            }

            // Saved Offline Videos Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0082FB))
                        )
                        Text(
                            text = "SAVED VIDEOS (${offlineFiles.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0082FB),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Movies/Qilin",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (offlineFiles.isEmpty() && activeTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(CornerRadius.surface))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .border(
                                        width = 0.6.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(CornerRadius.surface)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "No Offline Downloads",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Episodes you download while watching will appear here for fast offline playback with zero buffering.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontWeight = FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(offlineFiles, key = { it.absolutePath }) { file ->
                    OfflineFileCard(
                        file = file,
                        onPlay = {
                            val intent = Intent(context, WatchActivity::class.java).apply {
                                putExtra("watch_url", file.absolutePath)
                                putExtra("show_title", file.nameWithoutExtension)
                            }
                            context.startActivity(intent)
                            (context as? Activity)?.let { act ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    act.overrideActivityTransition(
                                        Activity.OVERRIDE_TRANSITION_OPEN,
                                        com.nsr.qilin.R.anim.oplus_open_slide_enter,
                                        com.nsr.qilin.R.anim.oplus_open_slide_exit
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    act.overridePendingTransition(
                                        com.nsr.qilin.R.anim.oplus_open_slide_enter,
                                        com.nsr.qilin.R.anim.oplus_open_slide_exit
                                    )
                                }
                            }
                        },
                        onRename = {
                            fileToRename = file
                            newFileNameText = file.nameWithoutExtension
                        },
                        onDelete = {
                            fileToDelete = file
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        GlassHeader(
            title = "Offline Downloads",
            subtitle = "${offlineFiles.size} saved • ${activeTasks.size} active",
            backdrop = screenBackdrop,
            isDark = !isLight,
            onBackClick = onNavigateBack,
            actions = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f))
                        .clickable { coroutineScope.launch { refreshOfflineFiles() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Downloads",
                        tint = if (isLight) Color(0xFF1E1E1E) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun StorageOverviewCard(
    downloadsBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    filesCount: Int
) {
    val downloadsMb = downloadsBytes.toFloat() / (1024 * 1024)
    val downloadsStr = if (downloadsMb >= 1024) {
        String.format("%.2f GB", downloadsMb / 1024)
    } else {
        String.format("%.1f MB", downloadsMb)
    }

    val freeGb = freeBytes.toFloat() / (1024 * 1024 * 1024)
    val totalGb = totalBytes.toFloat() / (1024 * 1024 * 1024)

    val usedFraction = if (totalBytes > 0) {
        ((totalBytes - freeBytes).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val downloadsFraction = if (totalBytes > 0) {
        (downloadsBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(
                if (isLight) Color.White.copy(alpha = 0.65f)
                else Color(0xFF1E2638).copy(alpha = 0.48f)
            )
            .border(
                width = 0.85.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .padding(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0082FB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = Color(0xFF0082FB),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Storage Used by Donghua",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JustSansFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$filesCount offline items stored",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = downloadsStr,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = JustSansFamily,
                    color = Color(0xFF0082FB)
                )
            }

            // Dual tone storage bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                // Total used space
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usedFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                )
                // Donghua downloads portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth(downloadsFraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color(0xFF0082FB))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f GB free", freeGb),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f GB total device", totalGb),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    task: DownloadTask,
    onCancel: () -> Unit
) {
    val speedMb = String.format("%.1f MB/s", task.speedBps.toFloat() / (1024 * 1024))
    val progressInt = task.progress.toInt()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(
                if (isLight) Color.White.copy(alpha = 0.65f)
                else Color(0xFF1E2638).copy(alpha = 0.48f)
            )
            .border(
                width = 0.85.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .padding(Spacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val res = task.resolution ?: when {
                            task.title.contains("1080", ignoreCase = true) -> "1080p"
                            task.title.contains("720", ignoreCase = true) -> "720p"
                            task.title.contains("480", ignoreCase = true) -> "480p"
                            task.title.contains("360", ignoreCase = true) -> "360p"
                            task.isAudioOnly -> "AUDIO"
                            else -> null
                        }
                        if (!res.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0082FB).copy(alpha = 0.16f))
                                    .border(0.6.dp, Color(0xFF0082FB).copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JustSansFamily,
                                    color = Color(0xFF0082FB)
                                )
                            }
                        }

                        val fmt = task.format ?: if (task.isAudioOnly) "M4A" else "MP4"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = fmt,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JustSansFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val badgeText = if (task.isHls) "HLS Stream" else if (task.numThreads > 1) "${task.numThreads} Threads" else "Direct Stream"
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = JustSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.status == DownloadStatus.PAUSED) {
                            Text(
                                text = "• PAUSED",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = LocalContext.current
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        IconButton(
                            onClick = { DownloadEngine.pauseDownload(context, task.id) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.75.dp, rememberLiquidGlassChipBorder(isLight), CircleShape)
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else if (task.status == DownloadStatus.PAUSED) {
                        IconButton(
                            onClick = { DownloadEngine.resumeDownload(context, task.id) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0082FB).copy(alpha = 0.16f))
                                .border(0.75.dp, Color(0xFF0082FB).copy(alpha = 0.35f), CircleShape)
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color(0xFF0082FB),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .border(0.75.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), CircleShape)
                            .colorOSPressBounce(pressedScale = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel Download",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Signature ColorOS 17 Dual-Tone Liquid Progress Pill (Matches Picture 2)
            val progressFraction = (task.progress / 100f).coerceIn(0f, 1f)
            val displayText = "Downloading... $progressInt% • $speedMb"
            val pillShape = CircleShape
            val primaryBlue = Color(0xFF0082FB)
            val remainingWhite = Color(0xFFE2F0FE)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(pillShape)
                    .background(remainingWhite),
                contentAlignment = Alignment.Center
            ) {
                // Background Layer Text (Blue text on frosted sky-white background)
                Text(
                    text = displayText,
                    fontSize = 13.5.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryBlue
                )

                // Filled Section with clipped white text overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val clipWidth = size.width * progressFraction
                            drawRect(
                                color = primaryBlue,
                                size = androidx.compose.ui.geometry.Size(clipWidth, size.height)
                            )
                            clipRect(right = clipWidth) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        fontSize = 13.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            if (task.etaSeconds > 0) {
                Text(
                    text = "${task.etaSeconds}s remaining",
                    fontSize = 11.5.sp,
                    fontFamily = JustSansFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OfflineFileCard(
    file: File,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeMb = String.format("%.1f MB", file.length().toFloat() / (1024 * 1024))
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(
                if (isLight) Color.White.copy(alpha = 0.65f)
                else Color(0xFF1E2638).copy(alpha = 0.48f)
            )
            .border(
                width = 0.85.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .colorOSPressBounce(
                pressedScale = 0.98f,
                onClick = onPlay
            )
            .padding(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0082FB).copy(alpha = 0.15f))
                    .border(
                        width = 0.75.dp,
                        color = Color(0xFF0082FB).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play Episode",
                    tint = Color(0xFF0082FB),
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.nameWithoutExtension,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val nameLower = file.nameWithoutExtension.lowercase()
                    val res = when {
                        nameLower.contains("1080") -> "1080p"
                        nameLower.contains("720") -> "720p"
                        nameLower.contains("480") -> "480p"
                        nameLower.contains("360") -> "360p"
                        nameLower.contains("2160") || nameLower.contains("4k") -> "4K"
                        file.extension in setOf("m4a", "mp3", "aac") -> "AUDIO"
                        else -> null
                    }
                    if (res != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF0082FB).copy(alpha = 0.16f))
                                .border(0.6.dp, Color(0xFF0082FB).copy(alpha = 0.40f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = res,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JustSansFamily,
                                color = Color(0xFF0082FB)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = file.extension.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JustSansFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = sizeMb,
                        fontSize = 12.sp,
                        fontFamily = JustSansFamily,
                        color = Color(0xFF0082FB),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ready Offline",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0082FB).copy(alpha = 0.10f))
                        .border(0.75.dp, Color(0xFF0082FB).copy(alpha = 0.25f), CircleShape)
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename Video",
                        tint = Color(0xFF0082FB),
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .border(0.75.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), CircleShape)
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete File",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
