package com.aetherdown.app.domain.repository

import android.net.Uri
import com.aetherdown.app.domain.model.DownloadRequest

interface MediaDownloadGateway {
    suspend fun download(request: DownloadRequest): Result<Uri>
}
