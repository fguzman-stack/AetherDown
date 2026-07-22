package com.aetherdown.app.di

import com.aetherdown.app.data.repository.DownloadRepositoryImpl
import com.aetherdown.app.data.repository.HistoryRepositoryImpl
import com.aetherdown.app.data.repository.SettingsRepositoryImpl
import com.aetherdown.app.data.repository.TorrentRepositoryImpl
import com.aetherdown.app.domain.repository.DownloadRepository
import com.aetherdown.app.domain.repository.HistoryRepository
import com.aetherdown.app.domain.repository.SettingsRepository
import com.aetherdown.app.domain.repository.TorrentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindTorrentRepository(impl: TorrentRepositoryImpl): TorrentRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
