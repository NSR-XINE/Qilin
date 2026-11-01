package com.nsr.qilin.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.nsr.qilin.R
import com.nsr.qilin.ui.GlassHeader
import com.nsr.qilin.ui.common.ColorOSDialog
import com.nsr.qilin.ui.common.ColorOSOutlinedButton
import com.nsr.qilin.ui.common.ColorOSPillButton
import com.nsr.qilin.ui.common.ColorOSSwitch
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.Haptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isLight = !isDark
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
    val glassBorder = rememberLiquidGlassBorder(isLight)

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // -------------------------------------------------------------------
            // SECTION 1: HERO ACCOUNT CARD (Connected vs Disconnected)
            // -------------------------------------------------------------------
            val heroShape = RoundedCornerShape(26.dp)
            val heroBg = if (isDark) Color(0xFF131722).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)

            if (aniListUser == null) {
                // Disconnected State: Premium Invitation Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroShape)
                        .background(heroBg, heroShape)
                        .border(1.dp, glassBorder, heroShape)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF02A9FF).copy(alpha = if (isDark) 0.14f else 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF02A9FF), Color(0xFF0066CC))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AniList Cloud Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9800))
                                    )
                                    Text(
                                        text = "Not Connected",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Link your AniList account to auto-track completed episodes, sync watch progression 2-ways across devices, and import your cloud anime collection.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Feature Highlights
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "☁️ 2-Way Sync",
                                "📺 Auto Progress",
                                "🔒 Keystore Safe"
                            ).forEach { feature ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.06f)
                                            else Color.Black.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            0.75.dp,
                                            if (isDark) Color.White.copy(alpha = 0.08f)
                                            else Color.Black.copy(alpha = 0.06f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = feature,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Connect Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorOSPillButton(
                                text = "Log In with AniList",
                                onClick = {
                                    Haptics.confirm()
                                    showAniListWebView = true
                                },
                                modifier = Modifier.weight(1.3f),
                                height = 46.dp,
                                containerColor = Color(0xFF0082FB),
                                contentColor = Color.White
                            )

                            ColorOSOutlinedButton(
                                text = "Manual Token",
                                onClick = {
                                    Haptics.impact()
                                    aniListConnectError = null
                                    tokenInput = ""
                                    showAniListConnectDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                height = 46.dp,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Connected State: Profile Hero Card
                val user = aniListUser!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroShape)
                        .background(heroBg, heroShape)
                        .border(1.dp, glassBorder, heroShape)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF02A9FF).copy(alpha = if (isDark) 0.16f else 0.09f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Avatar with Specular Aura
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF02A9FF),
                                                Color(0xFF00E5FF),
                                                Color(0xFF00C853),
                                                Color(0xFF02A9FF)
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .padding(2.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatarUrl.isNullOrEmpty()) {
                                    SubcomposeAsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    ) {
                                        val state = painter.state
                                        if (state is coil.compose.AsyncImagePainter.State.Success) {
                                            SubcomposeAsyncImageContent()
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF02A9FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF02A9FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-0.4).sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF00C853).copy(alpha = 0.15f))
                                            .border(0.5.dp, Color(0xFF00C853).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00C853))
                                            )
                                            Text(
                                                text = "CONNECTED",
                                                color = Color(0xFF00C853),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "@${user.name} • AniList Member",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(3.dp))
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = syncStatusText,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------------
                // SECTION 2: BENTO WATCH STATISTICS
                // -------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OxygenSectionHeader(
                        title = "Watch Overview",
                        subtitle = "Official statistics linked from your AniList profile"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AniListStatBentoCard(
                            label = "Shows in Library",
                            value = "${user.animeCount}",
                            iconRes = R.drawable.ic_tv,
                            gradient = listOf(Color(0xFF0082FB), Color(0xFF00D2FF)),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        AniListStatBentoCard(
                            label = "Episodes Watched",
                            value = "${user.episodesWatched}",
                            iconRes = R.drawable.ic_play_circle,
                            gradient = listOf(Color(0xFF00C853), Color(0xFF69F0AE)),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AniListStatBentoCard(
                            label = "Time Invested",
                            value = user.daysWatched,
                            iconRes = R.drawable.ic_hourglass_half,
                            gradient = listOf(Color(0xFFFF9100), Color(0xFFFF5252)),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        AniListStatBentoCard(
                            label = "Average Rating",
                            value = if (user.meanScore > 0) "${user.meanScore.toInt()}%" else "—",
                            iconVector = Icons.Rounded.Star,
                            gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8)),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // -------------------------------------------------------------------
            // SECTION 3: CLOUD SYNCHRONIZATION CENTER
            // -------------------------------------------------------------------
            if (aniListUser != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OxygenSectionHeader(
                        title = "Cloud Synchronization",
                        subtitle = "Automated tracking & multi-device backup"
                    )

                    OxygenGlassGroup(isDark = isDark) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Auto-sync Episodes switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.impact()
                                        viewModel.setAniListAutoSync(!aniListAutoSync)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF0082FB), Color(0xFF00D2FF))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Auto-sync Episodes",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Update AniList watch progress automatically",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                ColorOSSwitch(
                                    checked = aniListAutoSync,
                                    onCheckedChange = { viewModel.setAniListAutoSync(it) }
                                )
                            }

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // 2-Way Sync All (Primary Action)
                            val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
                            val syncRotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(900, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "syncRotation"
                            )

                            ColorOSPillButton(
                                text = if (isTwoWaySyncing) "Synchronizing with Cloud..." else "2-Way Sync Library",
                                onClick = {
                                    if (!isTwoWaySyncing && !isImportingAniList && !isPushingToAniList) {
                                        Haptics.confirm()
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
                                height = 46.dp,
                                containerColor = Color(0xFF0082FB),
                                contentColor = Color.White,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                if (isTwoWaySyncing) {
                                                    rotationZ = syncRotation
                                                }
                                            }
                                    )
                                }
                            )

                            Text(
                                text = "Pushes local episode progress to AniList and updates local shows with your cloud watch status in one tap.",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // One-Way Actions Bento Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AniListActionBentoTile(
                                    title = if (isImportingAniList) "Importing..." else "Pull Library",
                                    subtitle = "Import from Cloud",
                                    icon = Icons.Rounded.CloudDownload,
                                    gradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isImportingAniList && !isTwoWaySyncing) {
                                            Haptics.impact()
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
                                    }
                                )

                                AniListActionBentoTile(
                                    title = if (isPushingToAniList) "Pushing..." else "Push Library",
                                    subtitle = "Upload to Cloud",
                                    icon = Icons.Rounded.CloudUpload,
                                    gradient = listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isPushingToAniList && !isTwoWaySyncing) {
                                            Haptics.impact()
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
                                    }
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------------
                // SECTION 4: SESSION & SECURITY
                // -------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OxygenSectionHeader(
                        title = "Session & Security",
                        subtitle = "Account credentials and security encryption"
                    )

                    OxygenGlassGroup(isDark = isDark) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Open Profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.impact()
                                        val url = "https://anilist.co/user/${aniListUser!!.name}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF02A9FF).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = null,
                                            tint = Color(0xFF02A9FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Open AniList Profile",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "View public profile on anilist.co",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // Security encryption footnote
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00C853).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hardware-Backed Security",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Protected with Android Keystore AES-256 GCM",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )

                            // Disconnect Account Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Haptics.impact()
                                        showDisconnectConfirmDialog = true
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Logout,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Disconnect Account",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Remove saved token and stop cloud tracking",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
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

        // -------------------------------------------------------------------
        // DIALOGS: Manual Token, WebView OAuth, Disconnect
        // -------------------------------------------------------------------
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFF02A9FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AL",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = "AniList Authorization",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
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

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString()
                                            checkUrl(url)
                                            val scheme = request?.url?.scheme?.lowercase() ?: ""
                                            if (scheme != "http" && scheme != "https") {
                                                return true
                                            }
                                            return super.shouldOverrideUrlLoading(view, request)
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
                            onRelease = { webView ->
                                webView.stopLoading()
                                webView.destroy()
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

@Composable
private fun AniListStatBentoCard(
    label: String,
    value: String,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    gradient: List<Color>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    val cardBg = if (isDark) Color(0xFF131722).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
    val glassBorder = rememberLiquidGlassBorder(!isDark)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(cardBg, cardShape)
            .border(1.dp, glassBorder, cardShape)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(gradient))
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1
                )
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AniListActionBentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "tileScale"
    )

    val shape = RoundedCornerShape(16.dp)
    val bg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    val border = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
