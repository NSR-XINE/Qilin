package com.nsr.qilin.di

import android.content.Context
import com.nsr.qilin.data.db.AppDatabase
import com.nsr.qilin.data.db.ShowDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideShowDao(database: AppDatabase): ShowDao {
        return database.showDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): com.nsr.qilin.data.db.SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: AppDatabase): com.nsr.qilin.data.db.WatchHistoryDao {
        return database.watchHistoryDao()
    }
}
