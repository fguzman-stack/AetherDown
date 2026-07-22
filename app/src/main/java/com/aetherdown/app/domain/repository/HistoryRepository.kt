package com.aetherdown.app.domain.repository

import com.aetherdown.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryEntity>>
    fun getHistoryByPlatform(platform: String): Flow<List<HistoryEntity>>
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    suspend fun getHistoryById(id: Long): HistoryEntity?
    suspend fun insertHistory(history: HistoryEntity): Long
    suspend fun deleteHistoryById(id: Long)
    suspend fun deleteAllHistory()
    fun getHistoryCount(): Flow<Int>
    fun getDistinctPlatforms(): Flow<List<String>>
}
