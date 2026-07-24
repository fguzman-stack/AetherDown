package com.aetherdown.app.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherdown.app.R
import kotlinx.coroutines.delay

private data class SmartFeature(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    var enabled: Boolean = true
)

@Composable
private fun getSmartFeatures() = listOf(
    SmartFeature(
        "auto_classify",
        R.string.smart_feature_classify,
        R.string.smart_feature_classify_desc,
        Icons.Filled.TravelExplore
    ),
    SmartFeature(
        "auto_name",
        R.string.smart_feature_naming,
        R.string.smart_feature_naming_desc,
        Icons.Filled.AutoAwesome
    ),
    SmartFeature(
        "transcribe",
        R.string.smart_feature_transcribe,
        R.string.smart_feature_transcribe_desc,
        Icons.Filled.ClosedCaption
    ),
    SmartFeature(
        "dedup",
        R.string.smart_feature_dedup,
        R.string.smart_feature_dedup_desc,
        Icons.Filled.CompareArrows
    ),
    SmartFeature(
        "auto_organize",
        R.string.smart_feature_organize,
        R.string.smart_feature_organize_desc,
        Icons.Filled.FolderSpecial
    ),
    SmartFeature(
        "summarize",
        R.string.smart_feature_summarize,
        R.string.smart_feature_summarize_desc,
        Icons.Filled.Article
    ),
    SmartFeature(
        "semantic_search",
        R.string.smart_feature_search,
        R.string.smart_feature_search_desc,
        Icons.Filled.Search
    )
)

@Composable
fun SmartModeDialog(
    onDismiss: () -> Unit
) {
    val features = getSmartFeatures()
    var animProgress by remember { mutableStateOf(0f) }
    var smartEnabled by remember { mutableStateOf(true) }
    var featureToggles by remember {
        mutableStateOf(features.associate { it.id to it.enabled })
    }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animProgress = 1f
        delay(4000)
    }

    if (dismissed) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * animProgress))
            .clickable(enabled = animProgress > 0.5f) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = animProgress.coerceIn(0.3f, 1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = animProgress
                },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.smart_mode_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    stringResource(R.string.smart_mode_enable_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (smartEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { smartEnabled = !smartEnabled }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (smartEnabled) Icons.Filled.ToggleOn else Icons.Filled.ToggleOff,
                        contentDescription = null,
                        tint = if (smartEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.smart_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (smartEnabled) stringResource(R.string.smart_mode_enabled_desc) else stringResource(R.string.smart_mode_disabled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = smartEnabled,
                        onCheckedChange = { smartEnabled = it }
                    )
                }

                if (smartEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.smart_mode_choose_features),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    features.forEach { feature ->
                        val isChecked = featureToggles[feature.id] ?: true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    featureToggles = featureToggles.toMutableMap().apply {
                                        put(feature.id, !isChecked)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                feature.icon,
                                contentDescription = null,
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(feature.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(feature.descRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    featureToggles = featureToggles.toMutableMap().apply {
                                        put(feature.id, checked)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                FilledTonalButton(
                    onClick = {
                        dismissed = true
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (smartEnabled) stringResource(R.string.smart_mode_activate) else stringResource(R.string.smart_mode_continue_standard),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
