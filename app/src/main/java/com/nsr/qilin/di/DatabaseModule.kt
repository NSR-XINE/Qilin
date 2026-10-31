package com.nsr.qilin.di

import android.content.Context
import androidx.room.Room
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
        AppDatabase.ensureDatabaseMigrated(context)
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
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
