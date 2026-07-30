package com.mydonghua.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCollections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val hazeState = LocalHazeState.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xs))

            SectionHeader("Theme Mode Options")
            SettingsGlassSection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themesList = listOf(
                        Triple("light", "Light Mode", Icons.Rounded.WbSunny to Pair(Color(0xFFF6F7F9), Color(0xFFFFFFFF))),
                        Triple("dark", "Dark Mode", Icons.Rounded.NightsStay to Pair(Color(0xFF0F141F), Color(0xFF1B2433))),
                        Triple("amoled", "AMOLED", Icons.Rounded.Contrast to Pair(Color(0xFF000000), Color(0xFF0C0F17)))
                    )
                    themesList.forEach { (modeKey, modeTitle, modeData) ->
                        val (modeIcon, colors) = modeData
                        val (bgPreview, cardPreview) = colors
                        val isSelected = uiState.themeMode.equals(modeKey, ignoreCase = true)
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                        val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f) else Color.Transparent

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setThemeMode(modeKey) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm)
                            } {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgPreview)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
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
                                                .fillMaxWidth(0.65f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(cardPreview.copy(alpha = 0.70f))
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.Center)
                                                .offset(y = Spacing.xs)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = modeTitle,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = modeTitle,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader("Accent Color Palette")
            SettingsGlassSection {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Material You Dynamic Palette Card
                    val isDynamicSelected = uiState.accentColor.equals("dynamic", ignoreCase = true)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isDynamicSelected) 2.dp else 1.dp,
                            color = if (isDynamicSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDynamicSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.setAccentColor("dynamic") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Dynamic Material You",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Spacing.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material You Dynamic",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extracts colors from system wallpaper",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isDynamicSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
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

                    // Curated Dual-Tone Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
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
                            Triple("pink", "Pink", Pair(Color(0xFFF472B6), Color(0xFFBE185D)))
                        )
                        accentPresets.forEach { (accentKey, accentName, colorPair) ->
                            val isSelected = uiState.accentColor.equals(accentKey, ignoreCase = true)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setAccentColor(accentKey) }
                                    .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(colorPair.first, colorPair.second)
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color.Black.copy(alpha = 0.40f)),
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
                    }
                }
            }

            SectionHeader("Dashboard Display Options")
            SettingsGlassSection {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TogglePreferenceItem(
                        title = "Hero Countdown Banner",
                        subtitle = "Show upcoming episode countdown card at top of Home Screen",
                        checked = uiState.showBanner,
                        onCheckedChange = { viewModel.setShowBanner(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    TogglePreferenceItem(
                        title = "Continue Watching Row",
                        subtitle = "Show horizontal carousel for currently watching series",
                        checked = uiState.showContinue,
                        onCheckedChange = { viewModel.setShowContinue(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    TogglePreferenceItem(
                        title = "Recently Updated Row",
                        subtitle = "Display cards for series with newly released episodes",
                        checked = uiState.showRecent,
                        onCheckedChange = { viewModel.setShowRecent(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    TogglePreferenceItem(
                        title = "Weekly Schedule Section",
                        subtitle = "Organized day-by-day release schedule section",
                        checked = uiState.showSchedule,
                        onCheckedChange = { viewModel.setShowSchedule(it) }
                    )
                }
            }

            SectionHeader("Search & Streaming Options")
            SettingsGlassSection {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs)
                ) {
                    TogglePreferenceItem(
                        title = "Auto-open Keyboard",
                        subtitle = "Automatically open soft keyboard when tapping the search button",
                        checked = uiState.autoOpenKeyboard,
                        onCheckedChange = { viewModel.setAutoOpenKeyboard(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "Default Watch Provider",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose which streaming provider to open when tapping \"Watch Online\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))

                        val providers = listOf(
                            Triple("donghuastream", "DonghuaStream", "donghuastream.org to search & stream"),
                            Triple("luciferdonghua", "LuciferDonghua", "luciferdonghua.in to search & stream"),
                            Triple("direct", "Custom Saved URL", "Direct watch link specified in show details")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            providers.forEach { (key, title, domain) ->
                                val isSelected = uiState.defaultStreamProvider.equals(key, ignoreCase = true)
                                val emoji = when (key) {
                                    "donghuastream" -> "🎬"
                                    "luciferdonghua" -> "⚡"
                                    else -> "🔗"
                                }

                                Surface(
                                    onClick = { viewModel.setDefaultStreamProvider(key) },
                                    shape = RoundedCornerShape(CornerRadius.sm),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                                        ) {
                                            val providerIcon: ImageVector = when (key) {
                                                "donghuastream" -> Icons.Rounded.Tv
                                                "luciferdonghua" -> Icons.Rounded.AutoAwesome
                                                else -> Icons.Rounded.Link
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = providerIcon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.Center) {
                                                Text(
                                                    text = title,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = domain,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader("App Tools & Features")
            SettingsGlassSection {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionItem("Detailed Stats & Analytics", subtitle = "Viewing metrics, total watch time & status charts", iconRes = R.drawable.ic_stats, onClick = onNavigateToStats)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SettingsActionItem("Releases Calendar", subtitle = "Full day-by-day airing schedule overview", iconRes = R.drawable.ic_calendar, onClick = onNavigateToCalendar)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SettingsActionItem("Watch History Log", subtitle = "Chronological viewing history & date logs", iconRes = R.drawable.ic_history, onClick = onNavigateToHistory)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SettingsActionItem("Grouped Collections", subtitle = "Custom show groupings & categories", iconRes = R.drawable.ic_collections, onClick = onNavigateToCollections)
                }
            }

            SectionHeader("Data Backups Management")
            SettingsGlassSection {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "Export your shows, watch progress, and custom collections to a JSON backup file or restore from a previous backup.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        val buttonShape = RoundedCornerShape(CornerRadius.sm)
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            shape = buttonShape,
                            contentPadding = PaddingValues(horizontal = Spacing.sm)
                        ) {
                            Text("Export Backup JSON", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("application/json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            shape = buttonShape,
                            contentPadding = PaddingValues(horizontal = Spacing.sm)
                        ) {
                            Text("Import Backup JSON", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            SectionHeader("About My Donghua")
            SettingsGlassSection {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(Spacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "My Donghua",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Surface(
                                    shape = RoundedCornerShape(CornerRadius.xs),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "v2.4.0",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Smart Donghua Tracker • Build 2026.07",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SettingsInfoItem(
                        title = "Developer & License",
                        value = "NSR-XINE • Open Source"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SettingsActionItem(
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

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(top = Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .clip(RoundedCornerShape(CornerRadius.xs))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TogglePreferenceItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
            }
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(CornerRadius.sm))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.md),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                }
            }
        }
        Text("\u2192", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsGlassSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surfaceBg = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = surfaceBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            content = content
        )
    }
}