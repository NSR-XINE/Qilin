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
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
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
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }
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
            SectionHeader("Theme Mode Options")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                listOf("light" to "Light Mode", "dark" to "Dark Mode", "amoled" to "AMOLED Black").forEach { (modeKey, modeName) ->
                    val isSelected = uiState.themeMode.equals(modeKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.sm))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(CornerRadius.sm)
                            )
                            .clickable { viewModel.setThemeMode(modeKey) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(modeName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (isSelected) {
                            Text("\u2713", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SectionHeader("Accent Themes Colors")
            Column(modifier = Modifier.fillMaxWidth()) {
                val accentPresets = listOf(
                    "default" to "Default", "dynamic" to "Dynamic", "cyan" to "Cyan", "purple" to "Purple",
                    "rose" to "Rose", "green" to "Green", "orange" to "Orange", "blue" to "Blue",
                    "teal" to "Teal", "pink" to "Pink"
                )
                val rows = accentPresets.chunked(3)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        rowItems.forEach { (accentKey, accentName) ->
                            val isSelected = uiState.accentColor.equals(accentKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(CornerRadius.sm))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(CornerRadius.sm)
                                    )
                                    .clickable { viewModel.setAccentColor(accentKey) }
                                    .padding(vertical = Spacing.md),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = accentName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            SectionHeader("Smart Home Dashboard Elements")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.sm))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )
                    .padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                TogglePreferenceItem(
                    title = "Show Hero Countdown Banner",
                    checked = uiState.showBanner,
                    onCheckedChange = { viewModel.setShowBanner(it) }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                TogglePreferenceItem(
                    title = "Show Continue Watching",
                    checked = uiState.showContinue,
                    onCheckedChange = { viewModel.setShowContinue(it) }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                TogglePreferenceItem(
                    title = "Show Recently Updated",
                    checked = uiState.showRecent,
                    onCheckedChange = { viewModel.setShowRecent(it) }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                TogglePreferenceItem(
                    title = "Show Weekly Schedule",
                    checked = uiState.showSchedule,
                    onCheckedChange = { viewModel.setShowSchedule(it) }
                )
            }

            SectionHeader("App Tools & Features")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.sm))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )
                    .padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                SettingsActionItem("Detailed Stats & Analytics", iconRes = R.drawable.ic_stats, onClick = onNavigateToStats)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Scheduled Releases Calendar", iconRes = R.drawable.ic_calendar, onClick = onNavigateToCalendar)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Date-Grouped Watch History", iconRes = R.drawable.ic_history, onClick = onNavigateToHistory)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Grouped Show Collections", iconRes = R.drawable.ic_collections, onClick = onNavigateToCollections)
            }

            SectionHeader("Data Backups Management")
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xxxl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
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
                    modifier = Modifier.weight(1f).height(ButtonHeight.lg),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(CornerRadius.sm)
                ) {
                    Text("Export Backup JSON", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("application/json")
                    },
                    modifier = Modifier.weight(1f).height(ButtonHeight.lg),
                    shape = RoundedCornerShape(CornerRadius.sm)
                ) {
                    Text("Import Backup JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.sm)
    )
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
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text("\u2192", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}