package com.mydonghua.app.domain.voice

import kotlinx.coroutines.flow.Flow

/**
 * Contract for speech-to-text search input. The current build ships a stub
 * implementation ([isAvailable] == false); a real backend (on-device SpeechRecognizer
 * or a cloud STT provider) can swap in behind this interface without touching the UI.
 */
interface VoiceSearchController {
    val isAvailable: Boolean

    /**
     * Starts a listening session and emits recognized text fragments (e.g. one
     * emission per partial/final result) to be fed straight into the search box.
     */
    fun startListening(): Flow<String>

    fun stopListening() {}
}
