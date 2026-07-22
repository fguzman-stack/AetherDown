package com.aetherdown.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aetherdown.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM download_history ORDER BY completed_at DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE platform = :platform ORDER BY completed_at DESC")
    fun getHistoryByPlatform(platform: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY completed_at DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?

    @Insert
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM download_history")
    suspend fun deleteAllHistory()

    @Query("SELECT COUNT(*) FROM download_history")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT DISTINCT platform FROM download_history WHERE platform != ''")
    fun getDistinctPlatforms(): Flow<List<String>>
}
