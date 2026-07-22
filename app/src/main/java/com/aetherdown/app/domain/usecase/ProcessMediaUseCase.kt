package com.aetherdown.app.domain.usecase

import android.net.Uri
import android.os.Environment
import com.aetherdown.app.data.transformer.Media3TransformerImpl
import com.aetherdown.app.data.transformer.TransformationState
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class ProcessMediaUseCase @Inject constructor(
    private val transformer: Media3TransformerImpl
) {
    suspend operator fun invoke(inputUri: Uri, outputName: String): Flow<TransformationState> {
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AetherDown")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        
        val outputFile = File(downloadDir, "$outputName.m4a")
        return transformer.extractAudio(inputUri, outputFile.absolutePath)
    }
}
