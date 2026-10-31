package com.nsr.qilin.di

import com.nsr.qilin.data.repository.SettingsRepositoryImpl
import com.nsr.qilin.data.repository.ShowRepositoryImpl
import com.nsr.qilin.data.repository.WatchHistoryRepositoryImpl
import com.nsr.qilin.data.voice.VoiceSearchControllerImpl
import com.nsr.qilin.domain.repository.SettingsRepository
import com.nsr.qilin.domain.repository.ShowRepository
import com.nsr.qilin.domain.repository.WatchHistoryRepository
import com.nsr.qilin.domain.voice.VoiceSearchController
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
