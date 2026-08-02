package com.mydonghua.app.data.voice

import com.mydonghua.app.domain.voice.VoiceSearchController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder for the future voice-search backend. Reports unavailable and
 * emits nothing so the UI can surface a "coming soon" hint.
 */
@Singleton
class VoiceSearchControllerImpl @Inject constructor() : VoiceSearchController {
    override val isAvailable: Boolean = false

    override fun startListening(): Flow<String> = emptyFlow()
}
