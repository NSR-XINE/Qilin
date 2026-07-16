package com.mydonghua.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
                ),
                modifier = Modifier
                    .hazeChild(state = hazeState)
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
            GlassSection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md, horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themesList = listOf(
                        Triple("light", "Light", Color.White),
                        Triple("dark", "Dark", Color(0xFF2C2C2C)),
                        Triple("amoled", "AMOLED", Color.Black)
                    )
                    themesList.forEach { (modeKey, modeName, circleColor) ->
                        val isSelected = uiState.themeMode.equals(modeKey, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .clickable { viewModel.setThemeMode(modeKey) }
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val checkColor = if (modeKey == "light") Color.Black else Color.White
                                    Text("\u2713", color = checkColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Text(
                                text = modeName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            SectionHeader("Accent Themes Colors")
            GlassSection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = Spacing.md, horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accentPresets = listOf(
                        Triple("default", "Default", Color(0xFF6750A4)),
                        Triple("dynamic", "Dynamic", Color.Transparent),
                        Triple("cyan", "Cyan", Color(0xFF00B4D8)),
                        Triple("purple", "Purple", Color(0xFF7F3DFF)),
                        Triple("rose", "Rose", Color(0xFFE56B6F)),
                        Triple("green", "Green", Color(0xFF40916C)),
                        Triple("orange", "Orange", Color(0xFFF77F00)),
                        Triple("blue", "Blue", Color(0xFF0096C7)),
                        Triple("teal", "Teal", Color(0xFF009688)),
                        Triple("pink", "Pink", Color(0xFFE91E63))
                    )
                    accentPresets.forEach { (accentKey, accentName, presetColor) ->
                        val isSelected = uiState.accentColor.equals(accentKey, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .clickable { viewModel.setAccentColor(accentKey) }
                                .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (accentKey == "dynamic") {
                                            Brush.sweepGradient(
                                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                            )
                                        } else {
                                            Brush.solidColor(presetColor)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val checkColor = if (accentKey == "cyan" || accentKey == "rose" || accentKey == "orange" || accentKey == "dynamic") Color.Black else Color.White
                                    Text("\u2713", color = checkColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Text(
                                text = accentName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            SectionHeader("Smart Home Dashboard Elements")
            GlassSection {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TogglePreferenceItem(
                        title = "Show Hero Countdown Banner",
                        checked = uiState.showBanner,
                        onCheckedChange = { viewModel.setShowBanner(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TogglePreferenceItem(
                        title = "Show Continue Watching",
                        checked = uiState.showContinue,
                        onCheckedChange = { viewModel.setShowContinue(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TogglePreferenceItem(
                        title = "Show Recently Updated",
                        checked = uiState.showRecent,
                        onCheckedChange = { viewModel.setShowRecent(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TogglePreferenceItem(
                        title = "Show Weekly Schedule",
                        checked = uiState.showSchedule,
                        onCheckedChange = { viewModel.setShowSchedule(it) }
                    )
                }
            }

            SectionHeader("App Tools & Features")
            GlassSection {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionItem("Detailed Stats & Analytics", iconRes = R.drawable.ic_stats, onClick = onNavigateToStats)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsActionItem("Scheduled Releases Calendar", iconRes = R.drawable.ic_calendar, onClick = onNavigateToCalendar)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsActionItem("Date-Grouped Watch History", iconRes = R.drawable.ic_history, onClick = onNavigateToHistory)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsActionItem("Grouped Show Collections", iconRes = R.drawable.ic_collections, onClick = onNavigateToCollections)
                }
            }

            SectionHeader("Data Backups Management")
            GlassSection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
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
                            .height(ButtonHeight.lg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = buttonShape
                    ) {
                        Text("Export Backup JSON", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("application/json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(ButtonHeight.lg),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        shape = buttonShape
                    ) {
                        Text("Import Backup JSON", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
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
        modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs)
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("\u2192", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }
}