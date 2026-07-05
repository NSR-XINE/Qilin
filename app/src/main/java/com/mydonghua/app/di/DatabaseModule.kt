package com.mydonghua.app.di

import android.content.Context
import androidx.room.Room
import com.mydonghua.app.data.db.AppDatabase
import com.mydonghua.app.data.db.ShowDao
import dagger.Module
import dagger.annotation.Beta
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
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideShowDao(database: AppDatabase): ShowDao {
        return database.showDao()
    }
}
