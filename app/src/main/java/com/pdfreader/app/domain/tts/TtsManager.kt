package com.pdfreader.app.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.presentation.mvi.PdfTextBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var currentPageIndex: Int = -1
    private var currentTextBoxes: List<PdfTextBox> = emptyList()
    private var chunks: List<TtsChunk> = emptyList()
    private var currentChunkIndex = 0

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
                // We emit the first rect of the chunk when it starts.
                val chunk = chunks.getOrNull(currentChunkIndex)
                if (chunk != null) {
                    val firstBoxIndex = chunk.textToBoxIndices.firstOrNull { it >= 0 }
                    val rect = if (firstBoxIndex != null) listOf(currentTextBoxes[firstBoxIndex].bounds) else emptyList()
                    _ttsState.value = TtsState.Playing(currentPageIndex, rect)
                }
            }

            override fun onDone(utteranceId: String?) {
                playNextChunk()
            }

            override fun onError(utteranceId: String?) {
                _ttsState.value = TtsState.Error("Playback error")
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                
                val chunk = chunks.getOrNull(currentChunkIndex) ?: return
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
                
                val rects = activeBoxIndices.map { currentTextBoxes[it].bounds }
                if (rects.isNotEmpty()) {
                    _ttsState.value = TtsState.Playing(currentPageIndex, rects)
                }
            }
        })
    }

    fun play(pageIndex: Int, textBoxes: List<PdfTextBox>) {
        if (!isInitialized || textBoxes.isEmpty()) return
        
        currentPageIndex = pageIndex
        currentTextBoxes = textBoxes
        
        // Build chunks. For simplicity, we chunk by sentences (split by ". ")
        chunks = buildChunks(textBoxes)
        currentChunkIndex = 0
        
        if (chunks.isNotEmpty()) {
            speakCurrentChunk()
        }
    }

    private fun buildChunks(textBoxes: List<PdfTextBox>): List<TtsChunk> {
        val result = mutableListOf<TtsChunk>()
        
        var currentChunkBuilder = StringBuilder()
        var currentIndices = mutableListOf<Int>()
        var chunkIndex = 0
        
        for (i in textBoxes.indices) {
            val box = textBoxes[i]
            val text = box.text
            
            // Append the text
            for (char in text) {
                currentChunkBuilder.append(char)
                currentIndices.add(i)
            }
            
            // Add a space between boxes if needed
            currentChunkBuilder.append(" ")
            currentIndices.add(-1)
            
            // If the text ends with a period, question mark, or exclamation point, treat it as a sentence boundary
            if (text.endsWith(".") || text.endsWith("?") || text.endsWith("!")) {
                val chunkStr = currentChunkBuilder.toString().trimEnd()
                if (chunkStr.isNotBlank()) {
                    result.add(TtsChunk("chunk_$chunkIndex", chunkStr, currentIndices.toList()))
                    chunkIndex++
                }
                currentChunkBuilder.clear()
                currentIndices.clear()
            } else if (currentChunkBuilder.length > 3000) {
                // Fallback for extremely long sentences without punctuation to prevent TTS engine failure
                val chunkStr = currentChunkBuilder.toString()
                result.add(TtsChunk("chunk_$chunkIndex", chunkStr, currentIndices.toList()))
                chunkIndex++
                currentChunkBuilder.clear()
                currentIndices.clear()
            }
        }
        
        if (currentChunkBuilder.isNotBlank()) {
            val chunkStr = currentChunkBuilder.toString().trimEnd()
            result.add(TtsChunk("chunk_$chunkIndex", chunkStr, currentIndices.toList()))
        }
        
        return result
    }

    fun pause() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            val currentState = _ttsState.value
            val rects = if (currentState is TtsState.Playing) currentState.highlightRects else emptyList()
            _ttsState.value = TtsState.Paused(currentPageIndex, rects)
        }
    }

    fun resume() {
        if (_ttsState.value is TtsState.Paused) {
            speakCurrentChunk()
        }
    }

    fun stop() {
        tts?.stop()
        chunks = emptyList()
        currentChunkIndex = 0
        currentTextBoxes = emptyList()
        currentPageIndex = -1
        _ttsState.value = TtsState.Idle
    }

    private fun playNextChunk() {
        currentChunkIndex++
        if (currentChunkIndex < chunks.size) {
            speakCurrentChunk()
        } else {
            _ttsState.value = TtsState.Idle
        }
    }

    private fun speakCurrentChunk() {
        val chunk = chunks[currentChunkIndex]
        tts?.speak(chunk.text, TextToSpeech.QUEUE_FLUSH, null, chunk.utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

data class TtsChunk(
    val utteranceId: String,
    val text: String,
    val textToBoxIndices: List<Int>
)

sealed class TtsState {
    object Idle : TtsState()
    data class Playing(val pageIndex: Int, val highlightRects: List<Rect>) : TtsState()
    data class Paused(val pageIndex: Int, val highlightRects: List<Rect>) : TtsState()
    data class Error(val message: String) : TtsState()
}
