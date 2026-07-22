package com.aetherdown.app.data.transformer

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class Media3TransformerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractAudio(inputUri: Uri, outputFilePath: String): Flow<TransformationState> = callbackFlow {
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()

        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setRemoveVideo(true)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                trySend(TransformationState.Completed)
                close()
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                cancel("Transformation failed", exportException)
            }
        }

        transformer.addListener(listener)
        transformer.start(editedMediaItem, outputFilePath)

        while (isActive) {
            val progressHolder = ProgressHolder()
            val progressState = transformer.getProgress(progressHolder)
            if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                trySend(TransformationState.Progress(progressHolder.progress))
            }
            delay(500)
        }
    }
}

sealed class TransformationState {
    data class Progress(val percentage: Int) : TransformationState()
    object Completed : TransformationState()
}
