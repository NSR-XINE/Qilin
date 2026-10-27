package com.mydonghua.app.di

import android.content.Context
import androidx.room.Room
import com.mydonghua.app.data.db.AppDatabase
import com.mydonghua.app.data.db.ShowDao
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mydonghua.db"
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
    fun provideSettingsDao(database: AppDatabase): com.mydonghua.app.data.db.SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: AppDatabase): com.mydonghua.app.data.db.WatchHistoryDao {
        return database.watchHistoryDao()
    }
}
