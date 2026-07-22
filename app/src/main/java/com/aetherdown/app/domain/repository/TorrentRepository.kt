package com.aetherdown.app.domain.repository

import com.aetherdown.app.data.local.entity.TorrentEntity
import com.aetherdown.app.data.local.entity.TorrentFileEntity
import com.aetherdown.app.data.local.entity.TorrentStatus
import kotlinx.coroutines.flow.Flow

interface TorrentRepository {
    fun getAllTorrents(): Flow<List<TorrentEntity>>
    fun getTorrentById(id: Long): Flow<TorrentEntity?>
    suspend fun getTorrentByIdOnce(id: Long): TorrentEntity?
    fun getTorrentsByStatus(status: TorrentStatus): Flow<List<TorrentEntity>>
    suspend fun insertTorrent(torrent: TorrentEntity): Long
    suspend fun updateTorrent(torrent: TorrentEntity)
    suspend fun updateTorrentStatus(id: Long, status: TorrentStatus)
    suspend fun updateTorrentProgress(id: Long, downloaded: Long, uploaded: Long, progress: Int, dlSpeed: Long, upSpeed: Long, seeders: Int, leechers: Int)
    suspend fun deleteTorrentById(id: Long)
    suspend fun deleteAllTorrents()
    fun getFilesForTorrent(torrentId: Long): Flow<List<TorrentFileEntity>>
    suspend fun getFilesForTorrentOnce(torrentId: Long): List<TorrentFileEntity>
    suspend fun insertTorrentFiles(files: List<TorrentFileEntity>)
    suspend fun updateFileSelection(fileId: Long, selected: Boolean)
    suspend fun deleteFilesForTorrent(torrentId: Long)
}
