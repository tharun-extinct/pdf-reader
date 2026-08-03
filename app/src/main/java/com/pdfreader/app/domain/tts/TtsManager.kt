package com.pdfreader.app.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.R
import com.pdfreader.app.presentation.mvi.PdfTextBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var currentPageIndex: Int = -1
    private var currentTextBoxes: List<PdfTextBox> = emptyList()
    private var chunks: List<TtsChunk> = emptyList()
    private var currentChunkIndex = 0
    private var speechRate = 1f
    private var playbackSession = 0
    private var activeUtteranceId: String? = null

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _ttsState.value = TtsState.Error(
                    appContext.getString(R.string.tts_error_language_not_supported)
                )
            } else {
                isInitialized = true
                tts?.setSpeechRate(speechRate)
                setupProgressListener()
            }
        } else {
            _ttsState.value = TtsState.Error(
                appContext.getString(R.string.tts_error_initialization)
            )
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val chunk = currentChunkFor(utteranceId)
                if (chunk != null) {
                    val firstBoxIndex = chunk.textToBoxIndices.firstOrNull { it >= 0 }
                    val rects = if (firstBoxIndex != null) {
                        TtsTextNavigator.lineHighlightRects(currentTextBoxes, setOf(firstBoxIndex))
                    } else emptyList()
                    _ttsState.value = playingState(rects)
                }
            }

            override fun onDone(utteranceId: String?) {
                if (currentChunkFor(utteranceId) != null) playNextChunk()
            }

            override fun onError(utteranceId: String?) {
                if (currentChunkFor(utteranceId) != null) {
                    _ttsState.value = TtsState.Error(
                        appContext.getString(R.string.tts_error_playback)
                    )
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                
                val chunk = currentChunkFor(utteranceId) ?: return
                if (start < 0 || start >= chunk.textToBoxIndices.size) return
                
                // Find all unique text boxes that overlap with this word's character range
                val activeBoxIndices = mutableSetOf<Int>()
                val safeEnd = minOf(end, chunk.textToBoxIndices.size)
                
                for (i in start until safeEnd) {
                    val boxIndex = chunk.textToBoxIndices[i]
                    if (boxIndex >= 0) {
                        activeBoxIndices.add(boxIndex)
                    }
                }
                
                val rects = TtsTextNavigator.lineHighlightRects(currentTextBoxes, activeBoxIndices)
                if (rects.isNotEmpty()) {
                    _ttsState.value = playingState(rects)
                }
            }
        })
    }

    fun play(pageIndex: Int, textBoxes: List<PdfTextBox>) {
        if (!isInitialized) return
        if (textBoxes.isEmpty()) {
            _ttsState.value = TtsState.PageCompleted(pageIndex)
            return
        }
        
        currentPageIndex = pageIndex
        currentTextBoxes = textBoxes
        
        playbackSession++
        chunks = TtsTextNavigator.buildChunks(textBoxes).mapIndexed { index, draft ->
            TtsChunk(
                utteranceId = "tts_${playbackSession}_$index",
                text = draft.text,
                textToBoxIndices = draft.textToBoxIndices,
                paragraphIndex = draft.paragraphIndex
            )
        }
        currentChunkIndex = 0
        
        if (chunks.isNotEmpty()) {
            speakCurrentChunk()
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.6f, 1.6f)
        if (isInitialized) {
            tts?.setSpeechRate(speechRate)
        }
    }

    fun pause() {
        if (tts?.isSpeaking == true) {
            activeUtteranceId = null
            tts?.stop()
            val currentState = _ttsState.value
            val rects = if (currentState is TtsState.Playing) currentState.highlightRects else emptyList()
            val chunk = chunks.getOrNull(currentChunkIndex)
            _ttsState.value = TtsState.Paused(
                currentPageIndex,
                rects,
                chunk?.paragraphIndex ?: 0,
                paragraphCount()
            )
        }
    }

    fun resume() {
        if (_ttsState.value is TtsState.Paused) {
            speakCurrentChunk()
        }
    }

    fun stop() {
        activeUtteranceId = null
        playbackSession++
        tts?.stop()
        chunks = emptyList()
        currentChunkIndex = 0
        currentTextBoxes = emptyList()
        currentPageIndex = -1
        _ttsState.value = TtsState.Idle
    }

    private fun playNextChunk() {
        activeUtteranceId = null
        currentChunkIndex++
        if (currentChunkIndex < chunks.size) {
            speakCurrentChunk()
        } else {
            _ttsState.value = TtsState.PageCompleted(currentPageIndex)
        }
    }

    fun previousParagraph() = moveToParagraph(-1)

    fun nextParagraph() = moveToParagraph(1)

    private fun moveToParagraph(offset: Int) {
        val current = chunks.getOrNull(currentChunkIndex) ?: return
        val targetParagraph = current.paragraphIndex + offset
        val targetChunkIndex = chunks.indexOfFirst { it.paragraphIndex == targetParagraph }
        if (targetChunkIndex >= 0) {
            activeUtteranceId = null
            tts?.stop()
            currentChunkIndex = targetChunkIndex
            speakCurrentChunk()
        } else if (offset > 0) {
            activeUtteranceId = null
            tts?.stop()
            _ttsState.value = TtsState.PageCompleted(currentPageIndex)
        }
    }

    private fun speakCurrentChunk() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        activeUtteranceId = chunk.utteranceId
        tts?.speak(chunk.text, TextToSpeech.QUEUE_FLUSH, null, chunk.utteranceId)
    }

    private fun currentChunkFor(utteranceId: String?): TtsChunk? {
        if (utteranceId == null || utteranceId != activeUtteranceId) return null
        return chunks.getOrNull(currentChunkIndex)?.takeIf { it.utteranceId == utteranceId }
    }

    private fun paragraphCount(): Int = chunks.maxOfOrNull { it.paragraphIndex + 1 } ?: 0

    private fun playingState(rects: List<Rect>): TtsState.Playing {
        val chunk = chunks.getOrNull(currentChunkIndex)
        return TtsState.Playing(
            currentPageIndex,
            rects,
            chunk?.paragraphIndex ?: 0,
            paragraphCount()
        )
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

data class TtsChunk(
    val utteranceId: String,
    val text: String,
    val textToBoxIndices: List<Int>,
    val paragraphIndex: Int
)

sealed class TtsState {
    object Idle : TtsState()
    data class Playing(
        val pageIndex: Int,
        val highlightRects: List<Rect>,
        val paragraphIndex: Int,
        val paragraphCount: Int
    ) : TtsState()
    data class Paused(
        val pageIndex: Int,
        val highlightRects: List<Rect>,
        val paragraphIndex: Int,
        val paragraphCount: Int
    ) : TtsState()
    data class PageCompleted(val pageIndex: Int) : TtsState()
    data class Error(val message: String) : TtsState()
}
