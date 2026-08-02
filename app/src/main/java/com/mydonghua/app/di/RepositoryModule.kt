package com.mydonghua.app.di

import com.mydonghua.app.data.repository.SettingsRepositoryImpl
import com.mydonghua.app.data.repository.ShowRepositoryImpl
import com.mydonghua.app.data.repository.WatchHistoryRepositoryImpl
import com.mydonghua.app.data.voice.VoiceSearchControllerImpl
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import com.mydonghua.app.domain.voice.VoiceSearchController
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
    abstract fun bindShowRepository(
        showRepositoryImpl: ShowRepositoryImpl
    ): ShowRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(
        watchHistoryRepositoryImpl: WatchHistoryRepositoryImpl
    ): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindVoiceSearchController(
        voiceSearchControllerImpl: VoiceSearchControllerImpl
    ): VoiceSearchController
}
