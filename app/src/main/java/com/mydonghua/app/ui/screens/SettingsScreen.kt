package com.mydonghua.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
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
                        Text("←", fontSize = 24.sp)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Mode Section
            SectionHeader("Theme Mode Options")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("light" to "Light Mode", "dark" to "Dark Mode", "amoled" to "AMOLED Black").forEach { (modeKey, modeName) ->
                    val isSelected = uiState.themeMode.equals(modeKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.setThemeMode(modeKey) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(modeName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if (isSelected) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Theme Accent Presets
            SectionHeader("Accent Themes Colors")
            Column(modifier = Modifier.fillMaxWidth()) {
                val accentPresets = listOf(
                    "default" to "Default", "dynamic" to "Dynamic", "cyan" to "Cyan", "purple" to "Purple",
                    "rose" to "Rose", "green" to "Green", "orange" to "Orange", "blue" to "Blue",
                    "teal" to "Teal", "pink" to "Pink"
                )
                // Grid representation 3 columns
                val rows = accentPresets.chunked(3)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (accentKey, accentName) ->
                            val isSelected = uiState.accentColor.equals(accentKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { viewModel.setAccentColor(accentKey) }
                                    .padding(vertical = 12.dp),
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
                        // Fill empty cells if row not full
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Smart Home Dashboard Toggles
            SectionHeader("Smart Home Dashboard Elements")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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

            // App Tools & Features
            SectionHeader("App Tools & Features")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingsActionItem("Detailed Stats & Analytics", iconRes = R.drawable.ic_stats, onClick = onNavigateToStats)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Scheduled Releases Calendar", iconRes = R.drawable.ic_calendar, onClick = onNavigateToCalendar)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Date-Grouped Watch History", iconRes = R.drawable.ic_history, onClick = onNavigateToHistory)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionItem("Grouped Show Collections", iconRes = R.drawable.ic_collections, onClick = onNavigateToCollections)
            }

            // Backup & Import/Export
            SectionHeader("Data Backups Management")
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Export Backup JSON", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("application/json")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
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
        modifier = Modifier.padding(top = 8.dp)
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
