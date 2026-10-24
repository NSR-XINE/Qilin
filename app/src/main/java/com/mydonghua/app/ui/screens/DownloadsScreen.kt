package com.mydonghua.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.WatchActivity
import com.mydonghua.app.data.service.DownloadEngine
import com.mydonghua.app.data.service.DownloadStatus
import com.mydonghua.app.data.service.DownloadTask
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tasks by DownloadEngine.tasksState.collectAsState()

    var offlineFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    fun refreshOfflineFiles() {
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "MyDonghua"
        )
        if (storageDir.exists()) {
            offlineFiles = storageDir.listFiles { _, name ->
                name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true)
            }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            offlineFiles = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refreshOfflineFiles()
    }

    val activeTasks = remember(tasks) {
        tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Offline Downloads",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshOfflineFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Downloads")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Active Downloads Section
            if (activeTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloading (${activeTasks.size})",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                        .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Videos (${offlineFiles.size})",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Movies/MyDonghua",
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
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No Downloads Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap the download icon while watching any episode to save it offline.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                fontWeight = FontWeight.Normal
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
                        },
                        onDelete = {
                            file.delete()
                            refreshOfflineFiles()
                        }
                    )
                }
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

    GlassSection(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                }
            }

            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$progressInt% • $speedMb",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (task.etaSeconds > 0) "${task.etaSeconds}s left" else "Calculating...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OfflineFileCard(
    file: File,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeMb = String.format("%.1f MB", file.length().toFloat() / (1024 * 1024))

    GlassSection(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(CornerRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$sizeMb • Offline Video",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete File",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
