package com.aetherdown.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aetherdown.app.data.local.entity.TorrentEntity
import com.aetherdown.app.data.local.entity.TorrentFileEntity
import com.aetherdown.app.data.local.entity.TorrentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDao {
    @Query("SELECT * FROM torrents ORDER BY added_at DESC")
    fun getAllTorrents(): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE id = :id")
    fun getTorrentById(id: Long): Flow<TorrentEntity?>

    @Query("SELECT * FROM torrents WHERE id = :id")
    suspend fun getTorrentByIdOnce(id: Long): TorrentEntity?

    @Query("SELECT * FROM torrents WHERE status = :status")
    fun getTorrentsByStatus(status: TorrentStatus): Flow<List<TorrentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrent(torrent: TorrentEntity): Long

    @Update
    suspend fun updateTorrent(torrent: TorrentEntity)

    @Query("UPDATE torrents SET status = :status WHERE id = :id")
    suspend fun updateTorrentStatus(id: Long, status: TorrentStatus)

    @Query("UPDATE torrents SET downloaded_bytes = :downloaded, uploaded_bytes = :uploaded, progress = :progress, download_speed = :dlSpeed, upload_speed = :upSpeed, seeders = :seeders, leechers = :leechers WHERE id = :id")
    suspend fun updateTorrentProgress(id: Long, downloaded: Long, uploaded: Long, progress: Int, dlSpeed: Long, upSpeed: Long, seeders: Int, leechers: Int)

    @Query("DELETE FROM torrents WHERE id = :id")
    suspend fun deleteTorrentById(id: Long)

    @Query("DELETE FROM torrents")
    suspend fun deleteAllTorrents()

    @Query("SELECT * FROM torrent_files WHERE torrent_id = :torrentId")
    fun getFilesForTorrent(torrentId: Long): Flow<List<TorrentFileEntity>>

    @Query("SELECT * FROM torrent_files WHERE torrent_id = :torrentId")
    suspend fun getFilesForTorrentOnce(torrentId: Long): List<TorrentFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrentFiles(files: List<TorrentFileEntity>)

    @Query("UPDATE torrent_files SET is_selected = :selected WHERE id = :fileId")
    suspend fun updateFileSelection(fileId: Long, selected: Boolean)

    @Query("DELETE FROM torrent_files WHERE torrent_id = :torrentId")
    suspend fun deleteFilesForTorrent(torrentId: Long)
}
