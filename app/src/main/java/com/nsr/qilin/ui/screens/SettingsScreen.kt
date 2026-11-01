package com.nsr.qilin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.CornerRadius
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.nsr.qilin.R
import com.nsr.qilin.ui.GlassHeader
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.common.ColorOSSwitch
import com.nsr.qilin.ui.common.ColorOSPillButton
import com.nsr.qilin.ui.common.ColorOSOutlinedButton
import com.nsr.qilin.ui.theme.Haptics
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

            // 2. PERSONALIZATION (THEME & ACCENT)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Personalization",
                    subtitle = "Theme mode and accent color styling"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
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

                        val themeShape = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                }
                                .clip(themeShape)
                                .background(cardBg, themeShape)
                                .border(if (isSelected) 1.75.dp else 0.8.dp, specularBorder, themeShape)
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

                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                )

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

                    val dynamicShape = RoundedCornerShape(18.dp)
                    val dynamicBorderColor = if (isDynamicSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = dynamicScale
                                scaleY = dynamicScale
                            }
                            .clip(dynamicShape)
                            .background(dynamicCardBg, dynamicShape)
                            .border(if (isDynamicSelected) 1.75.dp else 0.8.dp, dynamicBorderColor, dynamicShape)
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

                            Text(
                                text = "Material You Dynamic",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

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
            }

            // 3. HOME DASHBOARD
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Home Dashboard",
                    subtitle = "Feeds and carousel banners on home screen"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OxygenTogglePreferenceItem(
                            title = "Trending Hero Banner",
                            subtitle = "Top trending donghua carousel on home screen",
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
                            subtitle = "Quick horizontal resume carousel of your in-progress series",
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
                            subtitle = "Latest releases and status updates feed",
                            iconRes = R.drawable.ic_tv,
                            iconGradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                            checked = uiState.showRecent,
                            onCheckedChange = { viewModel.setShowRecent(it) }
                        )
                    }
                }
            }

            // 4. HAPTICS & VIBRATION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Haptics & Vibration",
                    subtitle = "Tactile feedback for gestures and controls"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OxygenTogglePreferenceItem(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on player controls, switches, and key gestures",
                            iconVector = Icons.Rounded.Vibration,
                            iconGradient = listOf(Color(0xFFFF9800), Color(0xFFFFB74D)),
                            checked = uiState.hapticsEnabled,
                            onCheckedChange = { viewModel.setHapticsEnabled(it) }
                        )
                    }
                }
            }

            // 5. STREAMING & SEARCH
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Streaming & Search",
                    subtitle = "Player sources, ad blocker, and search behavior"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OxygenTogglePreferenceItem(
                            title = "Ad & Popup Blocker",
                            subtitle = "Block invasive stream ads, redirects, and popups",
                            iconVector = Icons.Rounded.Shield,
                            iconGradient = listOf(Color(0xFF0082FB), Color(0xFF52A9FF)),
                            checked = uiState.adBlockerEnabled,
                            onCheckedChange = { viewModel.setAdBlockerEnabled(it) }
                        )

                        HorizontalDivider(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                        )

                        OxygenTogglePreferenceItem(
                            title = "Auto-open Keyboard",
                            subtitle = "Automatically focus search field when opening Search screen",
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
                                text = "Default Streaming Provider",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val providers = listOf(
                                Triple("donghuastream", "DonghuaStream", "donghuastream.org"),
                                Triple("luciferdonghua", "LuciferDonghua", "luciferdonghua.in"),
                                Triple(
                                    "direct",
                                    "Custom Streaming URL",
                                    if (uiState.customStreamUrl.isNotBlank()) uiState.customStreamUrl else "Direct show URL or custom provider template"
                                )
                            )

                            val clipboardManager = LocalClipboardManager.current
                            var customUrlInput by remember(uiState.customStreamUrl) { mutableStateOf(uiState.customStreamUrl) }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                providers.forEach { (key, title, domain) ->
                                    val isSelected = uiState.defaultStreamProvider.equals(key, ignoreCase = true) ||
                                            (key == "direct" && uiState.defaultStreamProvider.equals("custom", ignoreCase = true))
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

                                    if (key == "direct") {
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            OxygenCustomUrlCard(
                                                customUrl = customUrlInput,
                                                isDark = isDark,
                                                onUrlChange = { newUrl ->
                                                    customUrlInput = newUrl
                                                    viewModel.setCustomStreamUrl(newUrl)
                                                },
                                                onPaste = {
                                                    val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                                    if (clipText.isNotBlank()) {
                                                        customUrlInput = clipText
                                                        viewModel.setCustomStreamUrl(clipText)
                                                        Haptics.tick()
                                                        Toast.makeText(context, "URL pasted from clipboard", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onClear = {
                                                    customUrlInput = ""
                                                    viewModel.setCustomStreamUrl("")
                                                    Haptics.tick()
                                                    Toast.makeText(context, "Custom URL cleared (using direct show URLs)", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. DATA BACKUPS & STORAGE
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "Data & Storage",
                    subtitle = "Offline backups and export tools"
                )
                OxygenGlassGroup(isDark = isDark) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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

                            ColorOSPillButton(
                                text = "Export Backup",
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
                                modifier = Modifier.weight(1f),
                                height = 46.dp,
                                icon = {
                                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )

                            ColorOSOutlinedButton(
                                text = "Import Backup",
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                height = 46.dp,
                                contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                icon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // 6. ABOUT QILIN
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(
                    title = "About",
                    subtitle = "App info and source repository"
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
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.10f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Qilin",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version ${com.nsr.qilin.BuildConfig.VERSION_NAME} (Build 2026.10)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                        )

                        OxygenSettingsActionItem(
                            title = "GitHub Repository",
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OxygenSectionHeader(title = "Developers")
                OxygenGlassGroup(isDark = isDark) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DeveloperRow(
                            name = "NSR-XINE",
                            subtitle = "github.com/NSR-XINE",
                            avatarRes = R.drawable.developer_nsr_xine,
                            isDark = isDark,
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NSR-XINE"))
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open web browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        HorizontalDivider(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            modifier = Modifier.padding(start = 74.dp, end = 16.dp)
                        )
                        DeveloperRow(
                            name = "TenSei てんせい",
                            subtitle = "github.com/Koustubh12345",
                            avatarRes = R.drawable.developer_tensei,
                            isDark = isDark,
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Koustubh12345"))
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open web browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        GlassHeader(
            title = "Settings",
            backdrop = screenBackdrop,
            isDark = isDark,
            onBackClick = onNavigateBack
        )

    }
}

// ---------------------------------------------------------------------------
// ColorOS 17 & OxygenOS 17 Liquid Glass Helper UI Components
// ---------------------------------------------------------------------------

@Composable
internal fun OxygenSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.5.dp, height = 13.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }
        if (!subtitle.isNullOrBlank()) {
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
internal fun OxygenGlassGroup(
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

    val isLight = !isDark
    val glassBorder = rememberLiquidGlassBorder(isLight)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg, shape)
            .border(width = 1.dp, brush = glassBorder, shape = shape)
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

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
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

        // Official ColorOS 17 Fluid Toggle Switch
        ColorOSSwitch(
            checked = checked,
            onCheckedChange = null,
            activeColor = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
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

    val fontShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(fontShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.40f)
                else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
                fontShape
            )
            .border(if (isSelected) 1.5.dp else 0.8.dp, specularBorder, fontShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
                    Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun OxygenCustomUrlCard(
    customUrl: String,
    isDark: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    val specularBorder = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f),
            if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (isDark) Color(0xFF161B22).copy(alpha = 0.70f)
                else Color(0xFFF6F8FA).copy(alpha = 0.90f),
                cardShape
            )
            .border(1.dp, specularBorder, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Custom Donghua Stream URL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (customUrl.isNotBlank()) Color(0xFF43A047).copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (customUrl.isNotBlank()) "URL Active" else "Per-Show Link",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customUrl.isNotBlank()) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = customUrl,
            onValueChange = onUrlChange,
            placeholder = {
                Text(
                    text = "e.g. https://donghuastream.org/?s= or https://site.com/?s={title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                focusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.6f),
                unfocusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.4f)
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (customUrl.isNotBlank()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPaste,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )

        Text(
            text = "💡 Use {title} as query placeholder or leave query blank to append show title automatically. If this field is empty, the player will use each donghua's individual custom watch link.",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )

        // Presets quick chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "DonghuaWorld" to "https://donghuaworld.net/?s=",
                "AnimeSchedule" to "https://animeschedule.net",
                "DonghuaStream" to "https://donghuastream.org/?s=",
                "LuciferDonghua" to "https://luciferdonghua.in/?s="
            )

            presets.forEach { (label, presetUrl) ->
                val isCurrent = customUrl.equals(presetUrl, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CornerRadius.pill))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                        )
                        .border(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                            RoundedCornerShape(CornerRadius.pill)
                        )
                        .clickable {
                            Haptics.tick()
                            onUrlChange(presetUrl)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperRow(
    name: String,
    subtitle: String,
    avatarRes: Int,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "developerRowScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    Haptics.tick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = name,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}
