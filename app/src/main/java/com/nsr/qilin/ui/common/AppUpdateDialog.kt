package com.nsr.qilin.ui.common

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nsr.qilin.data.service.AppUpdateInfo
import com.nsr.qilin.data.service.UpdateState
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.ColorOSTransitions
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.JustSansFamily
import java.io.File

@Composable
fun AppUpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: (AppUpdateInfo) -> Unit,
    onInstallNow: (File, AppUpdateInfo) -> Unit,
    onRetry: () -> Unit
) {
    if (updateState is UpdateState.Idle) return

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val isDark = !isLight

    Dialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogShape = RoundedCornerShape(28.dp)
        val dialogBg = if (isLight) {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.98f), Color(0xFFF1F5F9).copy(alpha = 0.95f))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFF161A2B).copy(alpha = 0.96f), Color(0xFF0F121E).copy(alpha = 0.98f))
            )
        }
        val dialogBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isLight) 0.35f else 0.22f),
                Color.White.copy(alpha = if (isLight) 0.10f else 0.05f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(dialogShape)
                .background(brush = dialogBg, shape = dialogShape)
                .border(width = 1.dp, brush = dialogBorder, shape = dialogShape)
                .padding(22.dp)
        ) {
            when (updateState) {
                is UpdateState.Checking -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Color(0xFF02A9FF),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Checking GitHub for updates...",
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is UpdateState.UpToDate -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "You're Up to Date",
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Qilin v${updateState.currentVersion} is currently the latest available release.",
                            fontFamily = JustSansFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        ColorOSPillButton(
                            text = "Done",
                            modifier = Modifier.fillMaxWidth(),
                            height = 46.dp,
                            onClick = onDismiss
                        )
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    val info = updateState.info
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF02A9FF).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RocketLaunch,
                                        contentDescription = null,
                                        tint = Color(0xFF02A9FF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Update Available",
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Version ${info.versionName}",
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color(0xFF02A9FF)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                val mbSize = if (info.asset.size > 0) String.format("%.1f MB", info.asset.size / (1024f * 1024f)) else "APK"
                                Text(
                                    text = mbSize,
                                    fontFamily = JustSansFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Release notes section
                        Text(
                            text = "What's New:",
                            fontFamily = JustSansFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val notesShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 150.dp)
                                .clip(notesShape)
                                .background(if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.04f))
                                .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f), notesShape)
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                                fontFamily = JustSansFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorOSOutlinedButton(
                                text = "Later",
                                modifier = Modifier.weight(0.8f),
                                height = 46.dp,
                                onClick = onDismiss
                            )
                            ColorOSPillButton(
                                text = "Download & Install",
                                modifier = Modifier.weight(1.2f),
                                height = 46.dp,
                                onClick = { onDownloadAndInstall(info) }
                            )
                        }
                    }
                }

                is UpdateState.Downloading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF02A9FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color(0xFF02A9FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val percent = (updateState.progress * 100).toInt()
                        Text(
                            text = "Downloading Update... $percent%",
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { updateState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF02A9FF),
                            trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val dlMb = String.format("%.1f MB", updateState.downloadedBytes / (1024f * 1024f))
                        val totalMb = if (updateState.totalBytes > 0) String.format("%.1f MB", updateState.totalBytes / (1024f * 1024f)) else "-- MB"
                        Text(
                            text = "$dlMb / $totalMb",
                            fontFamily = JustSansFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is UpdateState.ReadyToInstall -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Download Complete",
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Qilin v${updateState.info.versionName} is ready to install.",
                            fontFamily = JustSansFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorOSOutlinedButton(
                                text = "Later",
                                modifier = Modifier.weight(0.8f),
                                height = 46.dp,
                                onClick = onDismiss
                            )
                            ColorOSPillButton(
                                text = "Install Now",
                                modifier = Modifier.weight(1.2f),
                                height = 46.dp,
                                onClick = { onInstallNow(updateState.apkFile, updateState.info) }
                            )
                        }
                    }
                }

                is UpdateState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Update Check Failed",
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = updateState.message,
                            fontFamily = JustSansFamily,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorOSOutlinedButton(
                                text = "Cancel",
                                modifier = Modifier.weight(1f),
                                height = 46.dp,
                                onClick = onDismiss
                            )
                            ColorOSPillButton(
                                text = "Retry",
                                modifier = Modifier.weight(1f),
                                height = 46.dp,
                                onClick = onRetry
                            )
                        }
                    }
                }

                is UpdateState.Idle -> {}
            }
        }
    }
}
