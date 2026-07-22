package com.aetherdown.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_at DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getDownloadById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadByIdOnce(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status NOT IN (:excluded) ORDER BY priority DESC, created_at ASC")
    fun getActiveDownloads(excluded: List<DownloadStatus> = listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED)): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' LIMIT 1")
    suspend fun getCurrentlyDownloading(): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADING'")
    fun getActiveDownloadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET downloaded_bytes = :downloaded, progress = :progress, speed = :speed, eta = :eta, updated_at = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, downloaded: Long, progress: Int, speed: Long, eta: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("SELECT SUM(downloaded_bytes) FROM downloads")
    fun getTotalDownloadedBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>
}
