package com.mydonghua.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.common.ColorOSDialog
import com.mydonghua.app.ui.common.ColorOSOutlinedButton
import com.mydonghua.app.ui.common.ColorOSPillButton
import com.mydonghua.app.ui.common.ColorOSSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scrollState = rememberScrollState()

    val aniListUser by viewModel.aniListUser.collectAsState()
    val aniListAutoSync by viewModel.aniListAutoSync.collectAsState()
    val isImportingAniList by viewModel.isImportingAniList.collectAsState()
    val isPushingToAniList by viewModel.isPushingToAniList.collectAsState()
    val isTwoWaySyncing by viewModel.isTwoWaySyncing.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()

    var showAniListConnectDialog by remember { mutableStateOf(false) }
    var showAniListWebView by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var isConnectingAniList by remember { mutableStateOf(false) }
    var aniListConnectError by remember { mutableStateOf<String?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 84.dp
    val screenBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(top = headerTopPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Account & Cloud Sync",
                    subtitle = "AniList tracking & watch progression"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (aniListUser == null) {
                            // Disconnected State
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF02A9FF), Color(0xFF0066CC))
                                            )
                                        )
                                        .border(0.75.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AL",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AniList Cloud Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Not Connected",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Connect your AniList account to auto-track watched episodes, 2-way sync your watch progression, and import your library across all devices.",
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ColorOSPillButton(
                                    text = "Log In with AniList",
                                    onClick = {
                                        showAniListWebView = true
                                    },
                                    modifier = Modifier.weight(1.3f),
                                    height = 44.dp,
                                    containerColor = Color(0xFF02A9FF),
                                    contentColor = Color.White
                                )

                                ColorOSOutlinedButton(
                                    text = "Manual Token",
                                    onClick = {
                                        aniListConnectError = null
                                        tokenInput = ""
                                        showAniListConnectDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    height = 44.dp,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            // Connected State
                            val user = aniListUser!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!user.avatarUrl.isNullOrEmpty()) {
                                    SubcomposeAsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    ) {
                                        val state = painter.state
                                        if (state is coil.compose.AsyncImagePainter.State.Success) {
                                            SubcomposeAsyncImageContent()
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF02A9FF).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFF02A9FF)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF02A9FF))
                                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF00C853).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Connected",
                                                color = Color(0xFF00C853),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val syncStatusText = if (lastSyncedTime > 0) {
                                        val diff = System.currentTimeMillis() - lastSyncedTime
                                        val mins = diff / (60 * 1000)
                                        val timeStr = when {
                                            mins < 1 -> "Just now"
                                            mins < 60 -> "${mins}m ago"
                                            mins < 1440 -> "${mins / 60}h ago"
                                            else -> "${mins / 1440}d ago"
                                        }
                                        "Last synced: $timeStr"
                                    } else {
                                        "Auto-sync active"
                                    }

                                    Text(
                                        text = syncStatusText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // User stats 4-box grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statBoxBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                                val statBoxBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

                                listOf(
                                    Triple("Shows", "${user.animeCount}", "📺"),
                                    Triple("Watched", "${user.episodesWatched}", "🍿"),
                                    Triple("Time", user.daysWatched, "⏳"),
                                    Triple("Mean", if (user.meanScore > 0) "${user.meanScore.toInt()}%" else "--", "⭐")
                                ).forEach { (label, value, icon) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(statBoxBg)
                                            .border(1.dp, statBoxBorder, RoundedCornerShape(12.dp))
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = icon,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = value,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // Auto sync switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAniListAutoSync(!aniListAutoSync) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-sync Episodes",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Update AniList watch progress whenever you mark an episode",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ColorOSSwitch(
                                    checked = aniListAutoSync,
                                    onCheckedChange = { viewModel.setAniListAutoSync(it) }
                                )
                            }

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // Action buttons: 2-Way Sync (prominent), Push & Import, Profile & Disconnect
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 2-Way Sync All button
                                ColorOSPillButton(
                                    text = if (isTwoWaySyncing) "Syncing with AniList..." else "2-Way Sync All",
                                    onClick = {
                                        if (!isTwoWaySyncing && !isImportingAniList && !isPushingToAniList) {
                                            viewModel.twoWaySyncAll { pushed, pulled, imported, err ->
                                                if (err != null) {
                                                    Toast.makeText(context, "Sync failed: $err", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Sync complete! Pushed $pushed, Pulled $pulled, Imported $imported shows",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    height = 44.dp,
                                    containerColor = Color(0xFF0082FB),
                                    contentColor = Color.White
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ColorOSOutlinedButton(
                                        text = if (isImportingAniList) "Importing..." else "Import from Cloud",
                                        onClick = {
                                            if (!isImportingAniList && !isTwoWaySyncing) {
                                                viewModel.importLibraryFromAniList { imported, updated, err ->
                                                    if (err != null) {
                                                        Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Imported $imported new shows, updated $updated from AniList!",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 40.dp,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )

                                    ColorOSOutlinedButton(
                                        text = if (isPushingToAniList) "Pushing..." else "Push to Cloud",
                                        onClick = {
                                            if (!isPushingToAniList && !isTwoWaySyncing) {
                                                viewModel.pushLocalLibraryToAniList { synced, failed, err ->
                                                    if (err != null) {
                                                        Toast.makeText(context, "Push failed: $err", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Pushed progress for $synced shows to AniList ($failed failed)",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 40.dp,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ColorOSOutlinedButton(
                                        text = "View Profile",
                                        onClick = {
                                            val url = "https://anilist.co/user/${user.name}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 38.dp,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    ColorOSOutlinedButton(
                                        text = "Disconnect",
                                        onClick = { showDisconnectConfirmDialog = true },
                                        modifier = Modifier.weight(1f),
                                        height = 38.dp,
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        GlassHeader(
            title = "AniList",
            backdrop = screenBackdrop,
            isDark = isDark,
            onBackClick = onNavigateBack
        )

        if (showAniListConnectDialog) {
            ColorOSDialog(
                onDismissRequest = {
                    if (!isConnectingAniList) showAniListConnectDialog = false
                },
                title = "Connect AniList",
                confirmText = if (isConnectingAniList) "Verifying..." else "Authorize",
                dismissText = "Cancel",
                confirmButtonColor = Color(0xFF02A9FF),
                onConfirm = {
                    if (tokenInput.isBlank()) {
                        aniListConnectError = "Please paste or enter your AniList token"
                        return@ColorOSDialog
                    }
                    isConnectingAniList = true
                    aniListConnectError = null
                    viewModel.connectAniList(tokenInput.trim()) { res ->
                        isConnectingAniList = false
                        if (res.isSuccess) {
                            val connected = res.getOrThrow()
                            Toast.makeText(context, "Connected as @${connected.name}!", Toast.LENGTH_SHORT).show()
                            showAniListConnectDialog = false
                        } else {
                            aniListConnectError = res.exceptionOrNull()?.localizedMessage ?: "Failed to verify token"
                        }
                    }
                },
                customContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Follow these steps to connect your AniList account:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ColorOSPillButton(
                            text = "Log In with AniList (Automatic)",
                            onClick = {
                                showAniListWebView = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 46.dp,
                            containerColor = Color(0xFF02A9FF),
                            contentColor = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f))
                            Text(
                                text = "  OR  ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f))
                        }

                        ColorOSOutlinedButton(
                            text = "Open in External Browser",
                            onClick = {
                                val url = "https://anilist.co/api/v2/oauth/authorize?client_id=24757&response_type=token"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 40.dp
                        )

                        Text(
                            text = "Paste your Access Token or full address bar URL below:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = {
                                tokenInput = it
                                aniListConnectError = null
                            },
                            label = { Text("AniList Access Token") },
                            placeholder = { Text("Paste Bearer token here...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!text.isNullOrBlank()) {
                                            tokenInput = text.trim()
                                            aniListConnectError = null
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        )

                        if (aniListConnectError != null) {
                            Text(
                                text = aniListConnectError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        }

        if (showAniListWebView) {
            Dialog(
                onDismissRequest = { showAniListWebView = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarTop + 10.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) Color(0xFF141923) else Color.White)
                        .border(1.dp, Color.White.copy(alpha = if (isDark) 0.15f else 0.35f), RoundedCornerShape(26.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AniList Account Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showAniListWebView = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                    webViewClient = object : WebViewClient() {
                                        private var handled = false

                                        private fun checkUrl(url: String?) {
                                            if (!handled && url != null && url.contains("access_token=")) {
                                                handled = true
                                                val token = url.substringAfter("access_token=").substringBefore("&")
                                                showAniListWebView = false
                                                showAniListConnectDialog = false
                                                viewModel.connectAniList(token) { res ->
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Connected as @${res.getOrThrow().name}!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Authentication failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            checkUrl(url)
                                        }

                                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                            super.doUpdateVisitedHistory(view, url, isReload)
                                            checkUrl(url)
                                        }
                                    }
                                    loadUrl("https://anilist.co/api/v2/oauth/authorize?client_id=24757&response_type=token")
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (showDisconnectConfirmDialog) {
            ColorOSDialog(
                onDismissRequest = { showDisconnectConfirmDialog = false },
                title = "Disconnect AniList?",
                message = "Your local shows and watch progress will remain intact. Cloud tracking and auto-sync will be deactivated.",
                confirmText = "Disconnect",
                dismissText = "Cancel",
                confirmButtonColor = MaterialTheme.colorScheme.error,
                onConfirm = {
                    viewModel.disconnectAniList()
                    showDisconnectConfirmDialog = false
                    Toast.makeText(context, "AniList disconnected", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
