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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.aetherdown.app.R
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
            SettingsGroup(stringResource(R.string.settings_download), Icons.Filled.Download) {
                    SliderSetting(
                        title = stringResource(R.string.max_concurrent_downloads),
                        subtitle = stringResource(R.string.max_concurrent_downloads_subtitle, settings.maxConcurrentDownloads),
                        value = settings.maxConcurrentDownloads.toFloat(),
                        range = 1f..10f,
                        onValueChange = { viewModel.updateMaxConcurrentDownloads(it.toInt()) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SliderSetting(
                        title = stringResource(R.string.max_connections),
                        subtitle = stringResource(R.string.max_connections_subtitle, settings.defaultMaxConnections),
                        value = settings.defaultMaxConnections.toFloat(),
                        range = 1f..16f,
                        onValueChange = { viewModel.updateDefaultMaxConnections(it.toInt()) }
                    )
                }

                SettingsGroup(stringResource(R.string.settings_network), Icons.Filled.Wifi) {
                    SwitchSetting(stringResource(R.string.wifi_only), stringResource(R.string.wifi_only_desc), settings.wifiOnly) { viewModel.updateWifiOnly(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting(stringResource(R.string.allow_roaming), stringResource(R.string.allow_roaming_desc), settings.roamingAllowed) { viewModel.updateRoamingAllowed(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting(stringResource(R.string.metered_networks), stringResource(R.string.metered_networks_desc), settings.meteredNetworkAllowed) { viewModel.updateMeteredNetworkAllowed(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting(stringResource(R.string.only_on_charger), stringResource(R.string.only_on_charger_desc), settings.onlyOnCharging) { viewModel.updateOnlyOnCharging(it) }
                }

                SettingsGroup(stringResource(R.string.settings_appearance), Icons.Filled.Palette) {
                    Text(
                        stringResource(R.string.theme),
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
                                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchSetting(stringResource(R.string.dynamic_colors), stringResource(R.string.dynamic_colors_desc), settings.useDynamicColors) { viewModel.updateUseDynamicColors(it) }
                }

                SettingsGroup(stringResource(R.string.settings_privacy), Icons.Filled.PrivacyTip) {
                    SwitchSetting(stringResource(R.string.incognito_mode), stringResource(R.string.incognito_desc), settings.incognitoMode) { viewModel.updateIncognitoMode(it) }
                }

                SettingsGroup(stringResource(R.string.settings_organization), Icons.Filled.Folder) {
                    SwitchSetting(stringResource(R.string.organize_by_platform), stringResource(R.string.organize_by_platform_desc), settings.organizeByPlatform) { viewModel.updateOrganizeByPlatform(it) }
                }

                SettingsGroup(stringResource(R.string.settings_notifications), Icons.Filled.Notifications) {
                    SwitchSetting(stringResource(R.string.show_progress), stringResource(R.string.show_progress_desc), settings.notificationProgress) { viewModel.updateNotificationProgress(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchSetting(stringResource(R.string.completion_alerts), stringResource(R.string.completion_alerts_desc), settings.completedNotification) { viewModel.updateCompletedNotification(it) }
                }

                SettingsGroup(stringResource(R.string.settings_clipboard), Icons.Filled.ContentPaste) {
                    SwitchSetting(stringResource(R.string.auto_detect_urls), stringResource(R.string.auto_detect_urls_desc), settings.autoExtractClipboard) { viewModel.updateAutoExtractClipboard(it) }
                }

                SettingsGroup(stringResource(R.string.settings_language), Icons.Filled.Language) {
                    Text(
                        stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val languageOptions = listOf(
                        Language.SYSTEM to stringResource(R.string.language_system_default),
                        Language.ENGLISH to stringResource(R.string.language_english),
                        Language.SPANISH to stringResource(R.string.language_spanish),
                        Language.FRENCH to stringResource(R.string.language_french),
                        Language.GERMAN to stringResource(R.string.language_german),
                        Language.ITALIAN to stringResource(R.string.language_italian),
                        Language.PORTUGUESE to stringResource(R.string.language_portuguese)
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
                                stringResource(R.string.disclaimer_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.disclaimer_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.app_version),
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
