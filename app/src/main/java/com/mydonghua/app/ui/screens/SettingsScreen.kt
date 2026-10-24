package com.mydonghua.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.mydonghua.app.ui.theme.ColorOSMotion
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mydonghua.app.R
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}
            viewModel.importBackup(
                context = context,
                uri = uri,
                onSuccess = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                onFailure = { err ->
                    Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    var storageGranted by remember { mutableStateOf(hasStoragePermission(context)) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "settingsPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // ColorOS 17 Liquid Glass Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val isBackPressed by backInteraction.collectIsPressedAsState()
                    val backScale by animateFloatAsState(
                        targetValue = if (isBackPressed) 0.88f else 1f,
                        animationSpec = ColorOSMotion.TouchBounceSpring,
                        label = "backScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = backScale
                                scaleY = backScale
                            }
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.10f)
                                else Color.Black.copy(alpha = 0.06f)
                            )
                            .border(
                                width = 0.75.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.20f)
                                else Color.Black.copy(alpha = 0.10f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = backInteraction,
                                indication = null,
                                onClick = onNavigateBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Settings",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preferences, appearance & system",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ColorOS 17 Aurora Engine Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFFE50914).copy(alpha = 0.18f)
                            else Color(0xFFE50914).copy(alpha = 0.10f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha * 0.45f
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFE50914))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914))
                        )
                    }
                    Text(
                        text = "COLOROS 17",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isDark) Color(0xFFFF5252) else Color(0xFFC60000)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. THEME MODE SELECTION
            OxygenSectionHeader(
                title = "APPEARANCE & THEME",
                subtitle = "Choose interface style and ambient illumination"
            )
            OxygenGlassGroup(isDark = isDark) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themesList = listOf(
                        Triple("light", "Light", Icons.Rounded.WbSunny to Pair(Color(0xFFF6F7F9), Color(0xFFFFFFFF))),
                        Triple("dark", "Dark", Icons.Rounded.NightsStay to Pair(Color(0xFF0F141F), Color(0xFF1B2433))),
                        Triple("amoled", "AMOLED", Icons.Rounded.Contrast to Pair(Color(0xFF000000), Color(0xFF0C0F17)))
                    )
                    themesList.forEach { (modeKey, modeTitle, modeData) ->
                        val (modeIcon, colors) = modeData
                        val (bgPreview, cardPreview) = colors
                        val isSelected = uiState.themeMode.equals(modeKey, ignoreCase = true)

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val cardScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.94f else 1f,
                            animationSpec = ColorOSMotion.TouchBounceSpring,
                            label = "themeScale"
                        )

                        val cardBg = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.45f)
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                        }

                        // ColorOS 17 Prismatic Aurora Specular Border
                        val specularBorder = if (isSelected) {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF26C6DA),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f),
                                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.02f)
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.75.dp else 0.8.dp, specularBorder),
                            color = cardBg,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { viewModel.setThemeMode(modeKey) }
                                )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                // Live Mockup Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgPreview)
                                        .border(
                                            0.5.dp,
                                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.80f))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(cardPreview)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.60f)
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(cardPreview.copy(alpha = 0.70f))
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(1.5.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = modeTitle,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = modeTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. ACCENT COLOR PALETTE
            OxygenSectionHeader(
                title = "COLOR ACCENT & SYSTEM GLOW",
                subtitle = "Fluid dynamic wallpaper extraction or curated shades"
            )
            OxygenGlassGroup(isDark = isDark) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Material You Dynamic Card
                    val isDynamicSelected = uiState.accentColor.equals("dynamic", ignoreCase = true)
                    val dynamicInteraction = remember { MutableInteractionSource() }
                    val isDynamicPressed by dynamicInteraction.collectIsPressedAsState()
                    val dynamicScale by animateFloatAsState(
                        targetValue = if (isDynamicPressed) 0.965f else 1f,
                        animationSpec = ColorOSMotion.TouchBounceSpring,
                        label = "dynamicScale"
                    )

                    val dynamicCardBg = if (isDynamicSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.45f)
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isDynamicSelected) 1.75.dp else 0.8.dp,
                            color = if (isDynamicSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                        ),
                        color = dynamicCardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = dynamicScale
                                scaleY = dynamicScale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = dynamicInteraction,
                                indication = null,
                                onClick = { viewModel.setAccentColor("dynamic") }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFFF3366),
                                                Color(0xFFFF9933),
                                                Color(0xFF33CC66),
                                                Color(0xFF3399FF),
                                                Color(0xFF9933FF),
                                                Color(0xFFFF3366)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Dynamic Material You",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material You Dynamic",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time extraction from your device wallpaper",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isDynamicSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Liquid Gemstone Droplets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accentPresets = listOf(
                            Triple("default", "Cyan", Pair(Color(0xFF00F2FE), Color(0xFF00B4D8))),
                            Triple("purple", "Purple", Pair(Color(0xFF9D4EDD), Color(0xFF7B2CBF))),
                            Triple("rose", "Rose", Pair(Color(0xFFFF2A5F), Color(0xFFD90429))),
                            Triple("green", "Green", Pair(Color(0xFF3FB950), Color(0xFF2EA043))),
                            Triple("orange", "Orange", Pair(Color(0xFFF0883E), Color(0xFFD97706))),
                            Triple("blue", "Blue", Pair(Color(0xFF58A6FF), Color(0xFF2563EB))),
                            Triple("teal", "Teal", Pair(Color(0xFF2DD4BF), Color(0xFF0D9488))),
                            Triple("pink", "Pink", Pair(Color(0xFFF472B6), Color(0xFFBE185D))),
                            Triple("red", "Red", Pair(Color(0xFFFF5252), Color(0xFFC62828))),
                            Triple("amber", "Amber", Pair(Color(0xFFFFCA28), Color(0xFFFF8F00))),
                            Triple("lime", "Lime", Pair(Color(0xFFBEF264), Color(0xFF84CC16))),
                            Triple("emerald", "Emerald", Pair(Color(0xFF34D399), Color(0xFF059669))),
                            Triple("indigo", "Indigo", Pair(Color(0xFF818CF8), Color(0xFF4F46E5))),
                            Triple("violet", "Violet", Pair(Color(0xFFA78BFA), Color(0xFF7C3AED))),
                            Triple("gold", "Gold", Pair(Color(0xFFFDE047), Color(0xFFEAB308))),
                            Triple("sky", "Sky", Pair(Color(0xFF7DD3FC), Color(0xFF0284C7))),
                            Triple("fuchsia", "Fuchsia", Pair(Color(0xFFF0ABFC), Color(0xFFC026D3))),
                            Triple("coral", "Coral", Pair(Color(0xFFFF8A80), Color(0xFFF45454))),
                            Triple("slate", "Slate", Pair(Color(0xFFCBD5E1), Color(0xFF64748B))),
                            Triple("cocoa", "Cocoa", Pair(Color(0xFFD7CCC8), Color(0xFFA1887F)))
                        )
                        accentPresets.forEach { (accentKey, accentName, colorPair) ->
                            val isSelected = uiState.accentColor.equals(accentKey, ignoreCase = true)
                            OxygenDropletItem(
                                accentName = accentName,
                                colorPair = colorPair,
                                isSelected = isSelected,
                                isDark = isDark,
                                onClick = { viewModel.setAccentColor(accentKey) }
                            )
                        }
                    }
                }
            }

            // 3. DASHBOARD DISPLAY PREFERENCES
            OxygenSectionHeader(
                title = "HOME DASHBOARD DISPLAY",
                subtitle = "Customize interactive widgets and feeds on home screen"
            )
            OxygenGlassGroup(isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OxygenTogglePreferenceItem(
                        title = "Hero Countdown Banner",
                        subtitle = "Featured next-airing show countdown at top of Home Screen",
                        iconRes = R.drawable.ic_clock,
                        iconGradient = listOf(Color(0xFF0288D1), Color(0xFF26C6DA)),
                        checked = uiState.showBanner,
                        onCheckedChange = { viewModel.setShowBanner(it) }
                    )
                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                    )
                    OxygenTogglePreferenceItem(
                        title = "Continue Watching Row",
                        subtitle = "Horizontal carousel for currently active series",
                        iconRes = R.drawable.ic_play,
                        iconGradient = listOf(Color(0xFFFF7043), Color(0xFFFF8A65)),
                        checked = uiState.showContinue,
                        onCheckedChange = { viewModel.setShowContinue(it) }
                    )
                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                    )
                    OxygenTogglePreferenceItem(
                        title = "Recently Updated Row",
                        subtitle = "Quick cards for newly released donghua episodes",
                        iconRes = R.drawable.ic_tv,
                        iconGradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                        checked = uiState.showRecent,
                        onCheckedChange = { viewModel.setShowRecent(it) }
                    )
                }
            }

            // 4. SEARCH & STREAMING OPTIONS
            OxygenSectionHeader(
                title = "SEARCH & STREAMING PROVIDER",
                subtitle = "Keyboard behavior and preferred streaming search engine"
            )
            OxygenGlassGroup(isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OxygenTogglePreferenceItem(
                        title = "Auto-open Keyboard",
                        subtitle = "Automatically pop soft keyboard on search tab entry",
                        iconVector = Icons.Rounded.Search,
                        iconGradient = listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)),
                        checked = uiState.autoOpenKeyboard,
                        onCheckedChange = { viewModel.setAutoOpenKeyboard(it) }
                    )

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Default Watch Online Provider",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select preferred streaming catalog when tapping \"Watch Online\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val providers = listOf(
                            Triple("donghuastream", "DonghuaStream", "donghuastream.org • Fast HD Direct Streaming"),
                            Triple("luciferdonghua", "LuciferDonghua", "luciferdonghua.in • Multi-Server Mirrored"),
                            Triple("direct", "Custom Saved URL", "Direct watch link specified in show details")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            providers.forEach { (key, title, domain) ->
                                val isSelected = uiState.defaultStreamProvider.equals(key, ignoreCase = true)
                                val providerIcon: ImageVector = when (key) {
                                    "donghuastream" -> Icons.Rounded.Tv
                                    "luciferdonghua" -> Icons.Rounded.AutoAwesome
                                    else -> Icons.Rounded.Link
                                }
                                val providerGradient = when (key) {
                                    "donghuastream" -> listOf(Color(0xFF0288D1), Color(0xFF26C6DA))
                                    "luciferdonghua" -> listOf(Color(0xFFF57C00), Color(0xFFFFB74D))
                                    else -> listOf(Color(0xFF43A047), Color(0xFF66BB6A))
                                }

                                OxygenProviderItem(
                                    key = key,
                                    title = title,
                                    domain = domain,
                                    icon = providerIcon,
                                    gradient = providerGradient,
                                    isSelected = isSelected,
                                    isDark = isDark,
                                    onClick = { viewModel.setDefaultStreamProvider(key) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. DATA BACKUPS & STORAGE
            OxygenSectionHeader(
                title = "DATA BACKUPS & MIGRATION",
                subtitle = "Export database or restore your full watchlist safely"
            )
            OxygenGlassGroup(isDark = isDark) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Safely export your watchlist, episodes history, and custom collections into an encrypted JSON file, or restore existing backups.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (!storageGranted) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.20f else 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.75.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Storage Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Grant storage permission to allow exporting backups to device storage or importing backup files.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        storagePermissionLauncher.launch(currentStoragePermission())
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val exportInteraction = remember { MutableInteractionSource() }
                        val isExportPressed by exportInteraction.collectIsPressedAsState()
                        val exportScale by animateFloatAsState(
                            targetValue = if (isExportPressed) 0.95f else 1f,
                            animationSpec = ColorOSMotion.TouchBounceSpring,
                            label = "exportScale"
                        )

                        val importInteraction = remember { MutableInteractionSource() }
                        val isImportPressed by importInteraction.collectIsPressedAsState()
                        val importScale by animateFloatAsState(
                            targetValue = if (isImportPressed) 0.95f else 1f,
                            animationSpec = ColorOSMotion.TouchBounceSpring,
                            label = "importScale"
                        )

                        val buttonShape = RoundedCornerShape(14.dp)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val file = viewModel.exportBackup(context)
                                    if (file != null) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup JSON"))
                                    } else {
                                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            interactionSource = exportInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .graphicsLayer {
                                    scaleX = exportScale
                                    scaleY = exportScale
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = buttonShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Export Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            interactionSource = importInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .graphicsLayer {
                                    scaleX = importScale
                                    scaleY = importScale
                                },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                            ),
                            shape = buttonShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Import Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 6. ABOUT MY DONGHUA (COLOROS & OXYGENOS SYSTEM CARD)
            OxygenSectionHeader(
                title = "ABOUT SYSTEM & APP",
                subtitle = "Build engine, software version and repository"
            )
            OxygenGlassGroup(isDark = isDark) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.10f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "My Donghua",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE50914).copy(alpha = if (isDark) 0.20f else 0.12f)
                                ) {
                                    Text(
                                        text = "ColorOS 17",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFF5252) else Color(0xFFC60000),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Liquid Glass Edition • v6.8.8 (Build 2026.10)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    )

                    OxygenSettingsInfoItem(
                        title = "Architecture",
                        value = "Clean Architecture + Kotlin 2.0"
                    )

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    )

                    OxygenSettingsInfoItem(
                        title = "Motion & UI Engine",
                        value = "ColorOS 17 Aurora + Jetpack Compose"
                    )

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    )

                    OxygenSettingsActionItem(
                        title = "GitHub Source Repository",
                        subtitle = "github.com/NSR-XINE/my_donguha",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NSR-XINE/my_donguha"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open web browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// ColorOS 17 & OxygenOS 17 Liquid Glass Helper UI Components
// ---------------------------------------------------------------------------

@Composable
private fun OxygenSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 13.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.1.sp
            )
        }
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun OxygenGlassGroup(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val cardBg = if (isDark) {
        Color(0xFF131722).copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    // ColorOS 17 Prismatic Aurora Specular Border
    val specularBorder = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.22f),
            Color(0xFF26C6DA).copy(alpha = if (isDark) 0.10f else 0.12f),
            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(0.85.dp, specularBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun OxygenTogglePreferenceItem(
    title: String,
    subtitle: String? = null,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    iconGradient: List<Color>,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "toggleItemScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(iconGradient))
                    .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ColorOS 17 Liquid Switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White.copy(alpha = 0.70f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun OxygenSettingsActionItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "actionItemScale"
    )
    val arrowOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = ColorOSMotion.SnappyDp,
        label = "arrowOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = arrowOffset)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "›",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OxygenSettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OxygenDropletItem(
    accentName: String,
    colorPair: Pair<Color, Color>,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "dropletScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(colorPair.first, colorPair.second)
                    )
                )
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // 3D Glass Specular Reflection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.40f), Color.Transparent),
                            center = Offset(14f, 14f),
                            radius = 28f
                        )
                    )
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(
            text = accentName,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OxygenProviderItem(
    key: String,
    title: String,
    domain: String,
    icon: ImageVector,
    gradient: List<Color>,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "providerScale"
    )

    // ColorOS 17 Aurora Rim Border
    val specularBorder = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF26C6DA),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
            )
        )
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.40f)
        else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, specularBorder),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = domain,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun currentStoragePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasStoragePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, currentStoragePermission()) == PackageManager.PERMISSION_GRANTED
