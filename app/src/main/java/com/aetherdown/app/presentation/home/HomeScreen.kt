package com.aetherdown.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aetherdown.app.domain.model.ExtractResult
import com.aetherdown.app.domain.model.StreamInfo
import com.aetherdown.app.ui.theme.GradientEnd
import com.aetherdown.app.ui.theme.GradientStart
import kotlinx.coroutines.delay

data class PlatformLogo(
    val name: String,
    val logoUrl: String,
    val color: Color
)

private val platforms = listOf(
    PlatformLogo("YouTube", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/YouTube_full-color_icon_%282017%29.svg/240px-YouTube_full-color_icon_%282017%29.svg.png", Color(0xFFFF0000)),
    PlatformLogo("TikTok", "https://upload.wikimedia.org/wikipedia/zh/thumb/9/9a/TikTok_Logo_2021.svg/240px-TikTok_Logo_2021.svg.png", Color(0xFF000000)),
    PlatformLogo("Instagram", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Instagram_logo_2016.svg/240px-Instagram_logo_2016.svg.png", Color(0xFFE4405F)),
    PlatformLogo("X / Twitter", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/X_logo_2023.svg/240px-X_logo_2023.svg.png", Color(0xFF000000)),
    PlatformLogo("Reddit", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Reddit_logo_2023.svg/240px-Reddit_logo_2023.svg.png", Color(0xFFFF4500)),
    PlatformLogo("SoundCloud", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/SoundCloud_logo_2021.svg/240px-SoundCloud_logo_2021.svg.png", Color(0xFFFF3300)),
    PlatformLogo("Facebook", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/Facebook_Logo_%282019%29.png/240px-Facebook_Logo_%282019%29.png", Color(0xFF1877F2)),
    PlatformLogo("Twitch", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/Twitch_logo_2022.svg/240px-Twitch_logo_2022.svg.png", Color(0xFF9146FF)),
    PlatformLogo("Vimeo", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9c/Vimeo_Logo_2021.svg/240px-Vimeo_Logo_2021.svg.png", Color(0xFF1AB7EA)),
    PlatformLogo("Dailymotion", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/Dailymotion_2021_logo.svg/240px-Dailymotion_2021_logo.svg.png", Color(0xFF00D2F3))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedUrl: String = "",
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.checkClipboard()
    }

    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank()) {
            viewModel.handleSharedUrl(sharedUrl)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    )
                    .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Text(
                        text = "AetherDown",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Universal download manager",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20.dp))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = state.urlInput,
                            onValueChange = { viewModel.updateUrl(it) },
                            label = { Text("Paste URL") },
                            placeholder = { Text("https://example.com/video", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (state.urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateUrl("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    viewModel.extractAndDownload()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.extractAndDownload()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = state.urlInput.isNotBlank() && !state.isExtracting,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (state.isExtracting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Extracting...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Extract Streams",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.showClipboardBanner && state.clipboardDetectedUrls.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "URL detected in clipboard",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.dismissClipboardBanner() }) {
                                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            state.clipboardDetectedUrls.forEach { match ->
                                AssistChip(
                                    onClick = { viewModel.acceptClipboardUrl(match.url) },
                                    label = { Text(match.url.take(50), style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = state.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                state.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                state.extractResult?.let { result ->
                    StreamSelectionSheet(result, viewModel)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    "Supported Platforms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PlatformCarousel()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Enter any video, music, or torrent URL to start downloading",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                if (state.downloadStarted) {
                    SweetAlertDialog(
                        icon = Icons.Filled.CheckCircle,
                        title = "Download Added!",
                        message = state.lastFileName,
                        onDismiss = { viewModel.resetDownloadStarted() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformCarousel() {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { platforms.size * 100 }
    )

    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val infiniteInfinitely = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteInfinitely.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentPadding = PaddingValues(horizontal = 60.dp),
            pageSpacing = 16.dp
        ) { page ->
            val platform = platforms[page % platforms.size]
            val pageOffset = kotlin.math.abs(pagerState.currentPage - page).coerceAtMost(2)

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pof = pageOffset.toFloat()
                        val scale = 1f - (pof * 0.15f)
                        scaleX = scale.coerceIn(0.7f, 1f)
                        scaleY = scale.coerceIn(0.7f, 1f)
                        alpha = (1f - pof * 0.3f).coerceIn(0.4f, 1f)
                    },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = (4f + (1f - pageOffset.coerceAtMost(1).toFloat()) * 4f).dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        platform.color.copy(alpha = 0.8f),
                                        platform.color.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .then(
                                Modifier.graphicsLayer {
                                    this.alpha = if (pageOffset == 0) 1f else 1f - pageOffset.toFloat() * 0.3f
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = platform.logoUrl,
                            contentDescription = platform.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = platform.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.graphicsLayer { alpha = glowAlpha }
        ) {
            repeat(platforms.size) { index ->
                val isSelected = pagerState.currentPage % platforms.size == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun StreamSelectionSheet(result: ExtractResult, viewModel: HomeViewModel) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Select Format",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (result.thumbnailUrl != null) {
                                AsyncImage(
                                    model = result.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp, 48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (result.platform.isNotEmpty()) {
                                    Text(
                                        result.platform,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (result.streams.isEmpty()) {
                        Text(
                            "No streams found. Try direct download.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val videoStreams = result.streams.filter { it.isVideo }
                        val audioStreams = result.streams.filter { it.isAudio }

                        if (videoStreams.isNotEmpty()) {
                            Text(
                                "Video",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            videoStreams.forEach { stream ->
                                StreamItem(stream) {
                                    viewModel.startDownload(stream)
                                    showDialog = false
                                }
                            }
                        }

                        if (audioStreams.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Audio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            audioStreams.forEach { stream ->
                                StreamItem(stream) {
                                    viewModel.startDownload(stream)
                                    showDialog = false
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        viewModel.startDownloadDirectly(result.url)
                        showDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Direct Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
private fun StreamItem(stream: StreamInfo, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (stream.isVideo) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (stream.isVideo) Icons.Outlined.VideoFile else Icons.Outlined.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (stream.isVideo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stream.quality,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = buildString {
                            append(stream.format.uppercase())
                            if (stream.fileSize > 0) {
                                append(" · ")
                                append(formatFileSize(stream.fileSize))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SweetAlertDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * animProgress.value))
            .clickable(enabled = animProgress.value > 0.5f) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(40.dp)
                .graphicsLayer {
                    scaleX = animProgress.value.coerceIn(0.3f, 1f)
                    scaleY = animProgress.value.coerceIn(0.3f, 1f)
                    alpha = animProgress.value
                },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                val scale = if (animProgress.value > 0.5f) 1f else animProgress.value * 2f
                                scaleX = scale.coerceIn(0f, 1f)
                                scaleY = scale.coerceIn(0f, 1f)
                            }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes > 1_000_000_000 -> "${bytes / 1_000_000_000} GB"
        bytes > 1_000_000 -> "${bytes / 1_000_000} MB"
        bytes > 1_000 -> "${bytes / 1_000} KB"
        else -> "$bytes B"
    }
}
