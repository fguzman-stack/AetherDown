package com.aetherdown.app.domain.usecase

import com.aetherdown.app.data.local.entity.TorrentEntity
import com.aetherdown.app.data.local.entity.TorrentFileEntity
import com.aetherdown.app.data.local.entity.TorrentStatus
import com.aetherdown.app.domain.repository.TorrentRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

class ManageTorrentUseCase @Inject constructor(
    private val torrentRepository: TorrentRepository
) {
    suspend fun addMagnet(magnetUri: String, savePath: String): Result<Long> {
        return try {
            val torrent = TorrentEntity(
                magnetUri = magnetUri,
                name = "Torrent",
                savePath = savePath,
                status = TorrentStatus.STOPPED
            )
            val id = torrentRepository.insertTorrent(torrent)
            Timber.d("Torrent added via magnet: $id")
            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add magnet link")
            Result.failure(e)
        }
    }

    suspend fun addTorrentFile(torrentPath: String, savePath: String): Result<Long> {
        return try {
            val torrent = TorrentEntity(
                torrentPath = torrentPath,
                name = torrentPath.substringAfterLast("/").substringBeforeLast("."),
                savePath = savePath,
                status = TorrentStatus.STOPPED
            )
            val id = torrentRepository.insertTorrent(torrent)
            Timber.d("Torrent added from file: $id")
            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add torrent file")
            Result.failure(e)
        }
    }

    fun getAllTorrents(): Flow<List<TorrentEntity>> = torrentRepository.getAllTorrents()

    suspend fun startTorrent(id: Long) {
        torrentRepository.updateTorrentStatus(id, TorrentStatus.DOWNLOADING)
    }

    suspend fun pauseTorrent(id: Long) {
        torrentRepository.updateTorrentStatus(id, TorrentStatus.PAUSED)
    }

    suspend fun stopTorrent(id: Long) {
        torrentRepository.updateTorrentStatus(id, TorrentStatus.STOPPED)
    }

    suspend fun deleteTorrent(id: Long) {
        torrentRepository.deleteTorrentById(id)
    }

    suspend fun updateFileSelection(fileId: Long, selected: Boolean) {
        torrentRepository.updateFileSelection(fileId, selected)
    }
}
