package com.pdfreader.app.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var currentParagraphs: List<String> = emptyList()
    private var currentParagraphIndex = 0

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _ttsState.value = TtsState.Error("Language not supported")
            } else {
                isInitialized = true
                setupProgressListener()
            }
        } else {
            _ttsState.value = TtsState.Error("Initialization failed")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _ttsState.value = TtsState.Playing(currentParagraphIndex)
            }

            override fun onDone(utteranceId: String?) {
                playNextParagraph()
            }

            override fun onError(utteranceId: String?) {
                _ttsState.value = TtsState.Error("Playback error")
            }
        })
    }

    fun play(text: String) {
        if (!isInitialized) return
        
        // Split text into paragraphs
        currentParagraphs = text.split("\n\n").filter { it.isNotBlank() }
        currentParagraphIndex = 0
        
        if (currentParagraphs.isNotEmpty()) {
            speakCurrentParagraph()
        }
    }

    fun pause() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            _ttsState.value = TtsState.Paused(currentParagraphIndex)
        }
    }

    fun resume() {
        if (_ttsState.value is TtsState.Paused) {
            speakCurrentParagraph()
        }
    }

    fun stop() {
        tts?.stop()
        currentParagraphs = emptyList()
        currentParagraphIndex = 0
        _ttsState.value = TtsState.Idle
    }

    private fun playNextParagraph() {
        currentParagraphIndex++
        if (currentParagraphIndex < currentParagraphs.size) {
            speakCurrentParagraph()
        } else {
            _ttsState.value = TtsState.Idle
        }
    }

    private fun speakCurrentParagraph() {
        val text = currentParagraphs[currentParagraphIndex]
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "paragraph_$currentParagraphIndex")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

sealed class TtsState {
    object Idle : TtsState()
    data class Playing(val paragraphIndex: Int) : TtsState()
    data class Paused(val paragraphIndex: Int) : TtsState()
    data class Error(val message: String) : TtsState()
}
