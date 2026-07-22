package com.aetherdown.app.data.repository

import com.aetherdown.app.data.local.dao.HistoryDao
import com.aetherdown.app.data.local.entity.HistoryEntity
import com.aetherdown.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    override fun getHistoryByPlatform(platform: String): Flow<List<HistoryEntity>> =
        historyDao.getHistoryByPlatform(platform)

    override fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        historyDao.searchHistory(query)

    override suspend fun getHistoryById(id: Long): HistoryEntity? =
        historyDao.getHistoryById(id)

    override suspend fun insertHistory(history: HistoryEntity): Long =
        historyDao.insertHistory(history)

    override suspend fun deleteHistoryById(id: Long) =
        historyDao.deleteHistoryById(id)

    override suspend fun deleteAllHistory() = historyDao.deleteAllHistory()

    override fun getHistoryCount(): Flow<Int> = historyDao.getHistoryCount()

    override fun getDistinctPlatforms(): Flow<List<String>> = historyDao.getDistinctPlatforms()
}
