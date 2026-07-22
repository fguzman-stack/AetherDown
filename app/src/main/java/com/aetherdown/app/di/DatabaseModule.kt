package com.aetherdown.app.di

import android.content.Context
import androidx.room.Room
import com.aetherdown.app.data.local.AetherDatabase
import com.aetherdown.app.data.local.dao.DownloadDao
import com.aetherdown.app.data.local.dao.HistoryDao
import com.aetherdown.app.data.local.dao.TorrentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AetherDatabase {
        return Room.databaseBuilder(
            context,
            AetherDatabase::class.java,
            "aetherdown_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDownloadDao(db: AetherDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideHistoryDao(db: AetherDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideTorrentDao(db: AetherDatabase): TorrentDao = db.torrentDao()
}
