package com.aetherdown.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aetherdown.app.domain.model.Language
import com.aetherdown.app.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsGroup("Download", Icons.Filled.Download) {
                    SliderSetting(
                        title = "Max concurrent downloads",
                        subtitle = "${settings.maxConcurrentDownloads} at once",
                        value = settings.maxConcurrentDownloads.toFloat(),
                        range = 1f..10f,
                        onValueChange = { viewModel.updateMaxConcurrentDownloads(it.toInt()) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SliderSetting(
                        title = "Max connections per download",
                        subtitle = "${settings.defaultMaxConnections} connections",
                        value = settings.defaultMaxConnections.toFloat(),
                        range = 1f..16f,
                        onValueChange = { viewModel.updateDefaultMaxConnections(it.toInt()) }
                    )
                }

                SettingsGroup("Network", Icons.Filled.Wifi) {
                    SwitchSetting("Wi-Fi only", "Only download on Wi-Fi networks", settings.wifiOnly) { viewModel.updateWifiOnly(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting("Allow roaming", "Download while roaming", settings.roamingAllowed) { viewModel.updateRoamingAllowed(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting("Metered networks", "Allow download on metered connections", settings.meteredNetworkAllowed) { viewModel.updateMeteredNetworkAllowed(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting("Only on charger", "Pause downloads when not charging", settings.onlyOnCharging) { viewModel.updateOnlyOnCharging(it) }
                }

                SettingsGroup("Appearance", Icons.Filled.Palette) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.darkTheme == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.SYSTEM -> "System"
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchSetting("Dynamic colors", "Use Material You dynamic colors (Android 12+)", settings.useDynamicColors) { viewModel.updateUseDynamicColors(it) }
                }

                SettingsGroup("Privacy", Icons.Filled.PrivacyTip) {
                    SwitchSetting("Incognito mode", "Don't save download history", settings.incognitoMode) { viewModel.updateIncognitoMode(it) }
                }

                SettingsGroup("Organization", Icons.Filled.Folder) {
                    SwitchSetting("Organize by platform", "Save files in platform-specific folders", settings.organizeByPlatform) { viewModel.updateOrganizeByPlatform(it) }
                }

                SettingsGroup("Notifications", Icons.Filled.Notifications) {
                    SwitchSetting("Show progress", "Show download progress in notifications", settings.notificationProgress) { viewModel.updateNotificationProgress(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting("Completion alerts", "Show notification when download completes", settings.completedNotification) { viewModel.updateCompletedNotification(it) }
                }

                SettingsGroup("Clipboard", Icons.Filled.ContentPaste) {
                    SwitchSetting("Auto-detect URLs", "Automatically check clipboard for URLs", settings.autoExtractClipboard) { viewModel.updateAutoExtractClipboard(it) }
                }

                SettingsGroup("Language", Icons.Filled.Language) {
                    Text(
                        "Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val languageOptions = listOf(
                        Language.SYSTEM to "System default",
                        Language.ENGLISH to "English",
                        Language.SPANISH to "Español",
                        Language.FRENCH to "Français",
                        Language.GERMAN to "Deutsch",
                        Language.ITALIAN to "Italiano",
                        Language.PORTUGUESE to "Português"
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        languageOptions.forEach { (lang, label) ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { viewModel.updateLanguage(lang) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Gavel,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Legal Disclaimer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "AetherDown is a general-purpose download tool. Users are solely responsible for ensuring they have the legal right to download and use any content accessed through this application.\n\n" +
                            "The developers do not condone piracy, copyright infringement, or any illegal use of this software. This application does not host, store, or distribute any copyrighted content.\n\n" +
                            "Downloading copyrighted material without permission may violate applicable laws in your jurisdiction. Use at your own risk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "AetherDown v2.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

@Composable
private fun SettingsGroup(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt() - 1
        )
    }
}
