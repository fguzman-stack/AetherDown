package com.aetherdown.app.data.repository

import com.aetherdown.app.data.local.dao.TorrentDao
import com.aetherdown.app.data.local.entity.TorrentEntity
import com.aetherdown.app.data.local.entity.TorrentFileEntity
import com.aetherdown.app.data.local.entity.TorrentStatus
import com.aetherdown.app.domain.repository.TorrentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepositoryImpl @Inject constructor(
    private val torrentDao: TorrentDao
) : TorrentRepository {

    override fun getAllTorrents(): Flow<List<TorrentEntity>> = torrentDao.getAllTorrents()

    override fun getTorrentById(id: Long): Flow<TorrentEntity?> = torrentDao.getTorrentById(id)

    override suspend fun getTorrentByIdOnce(id: Long): TorrentEntity? =
        torrentDao.getTorrentByIdOnce(id)

    override fun getTorrentsByStatus(status: TorrentStatus): Flow<List<TorrentEntity>> =
        torrentDao.getTorrentsByStatus(status)

    override suspend fun insertTorrent(torrent: TorrentEntity): Long =
        torrentDao.insertTorrent(torrent)

    override suspend fun updateTorrent(torrent: TorrentEntity) =
        torrentDao.updateTorrent(torrent)

    override suspend fun updateTorrentStatus(id: Long, status: TorrentStatus) =
        torrentDao.updateTorrentStatus(id, status)

    override suspend fun updateTorrentProgress(
        id: Long, downloaded: Long, uploaded: Long, progress: Int,
        dlSpeed: Long, upSpeed: Long, seeders: Int, leechers: Int
    ) = torrentDao.updateTorrentProgress(id, downloaded, uploaded, progress, dlSpeed, upSpeed, seeders, leechers)

    override suspend fun deleteTorrentById(id: Long) = torrentDao.deleteTorrentById(id)

    override suspend fun deleteAllTorrents() = torrentDao.deleteAllTorrents()

    override fun getFilesForTorrent(torrentId: Long): Flow<List<TorrentFileEntity>> =
        torrentDao.getFilesForTorrent(torrentId)

    override suspend fun getFilesForTorrentOnce(torrentId: Long): List<TorrentFileEntity> =
        torrentDao.getFilesForTorrentOnce(torrentId)

    override suspend fun insertTorrentFiles(files: List<TorrentFileEntity>) =
        torrentDao.insertTorrentFiles(files)

    override suspend fun updateFileSelection(fileId: Long, selected: Boolean) =
        torrentDao.updateFileSelection(fileId, selected)

    override suspend fun deleteFilesForTorrent(torrentId: Long) =
        torrentDao.deleteFilesForTorrent(torrentId)
}
