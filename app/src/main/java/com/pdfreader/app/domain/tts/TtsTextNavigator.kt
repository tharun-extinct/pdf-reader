package com.pdfreader.app.domain.tts

import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.presentation.mvi.PdfTextBox
import kotlin.math.abs
import kotlin.math.max

internal data class TtsChunkDraft(
    val text: String,
    val textToBoxIndices: List<Int>,
    val paragraphIndex: Int
)

/** Pure text/geometry helpers kept separate from Android TextToSpeech for unit testing. */
internal object TtsTextNavigator {
    private const val MAX_CHUNK_LENGTH = 3_000

    fun buildChunks(textBoxes: List<PdfTextBox>): List<TtsChunkDraft> {
        if (textBoxes.isEmpty()) return emptyList()

        val paragraphByBox = paragraphIndices(textBoxes)
        val chunks = mutableListOf<TtsChunkDraft>()
        var paragraphStart = 0

        while (paragraphStart < textBoxes.size) {
            val paragraphIndex = paragraphByBox[paragraphStart]
            var paragraphEnd = paragraphStart + 1
            while (paragraphEnd < textBoxes.size && paragraphByBox[paragraphEnd] == paragraphIndex) {
                paragraphEnd++
            }
            chunks += chunkParagraph(textBoxes, paragraphStart, paragraphEnd, paragraphIndex)
            paragraphStart = paragraphEnd
        }

        return chunks
    }

    fun lineHighlightRects(textBoxes: List<PdfTextBox>, activeBoxIndices: Set<Int>): List<Rect> {
        if (activeBoxIndices.isEmpty()) return emptyList()

        val activeBoxes = activeBoxIndices.mapNotNull { textBoxes.getOrNull(it) }
        val lineBoxes = textBoxes.filter { candidate ->
            activeBoxes.any { active -> isSameLine(active.bounds, candidate.bounds) }
        }

        val lines = mutableListOf<MutableList<Rect>>()
        lineBoxes.forEach { box ->
            val line = lines.firstOrNull { isSameLine(it.first(), box.bounds) }
            if (line == null) lines += mutableListOf(box.bounds) else line += box.bounds
        }
        return lines.map { line -> line.reduce { bounds, rect -> bounds.union(rect) } }
    }

    private fun paragraphIndices(textBoxes: List<PdfTextBox>): IntArray {
        val result = IntArray(textBoxes.size)
        var paragraphIndex = 0
        var previousLineStart = 0
        var previousLineEnd = 0
        var currentLineStart = 0

        for (index in 1 until textBoxes.size) {
            if (isSameLine(textBoxes[currentLineStart].bounds, textBoxes[index].bounds)) continue

            previousLineStart = currentLineStart
            previousLineEnd = index - 1
            currentLineStart = index

            val previousLineBounds = unionBounds(textBoxes, previousLineStart, previousLineEnd + 1)
            val currentBounds = textBoxes[index].bounds
            val verticalGap = currentBounds.top - previousLineBounds.bottom
            val lineHeight = max(previousLineBounds.height, currentBounds.height)
            val isIndentedAfterSentence =
                currentBounds.left - previousLineBounds.left > 0.025f &&
                    textBoxes[previousLineEnd].text.trimEnd().lastOrNull() in setOf('.', '?', '!')

            if (verticalGap > lineHeight * 0.75f || isIndentedAfterSentence) {
                paragraphIndex++
            }
            result[index] = paragraphIndex
        }

        // Fill boxes after each line's first box with that line/paragraph assignment.
        for (index in 1 until result.size) {
            if (result[index] == 0 && isSameLine(textBoxes[index - 1].bounds, textBoxes[index].bounds)) {
                result[index] = result[index - 1]
            } else if (result[index] == 0 && result[index - 1] > 0) {
                result[index] = result[index - 1]
            }
        }
        return result
    }

    private fun chunkParagraph(
        textBoxes: List<PdfTextBox>,
        start: Int,
        endExclusive: Int,
        paragraphIndex: Int
    ): List<TtsChunkDraft> {
        val result = mutableListOf<TtsChunkDraft>()
        var text = StringBuilder()
        var mapping = mutableListOf<Int>()

        fun flush() {
            if (text.isNotEmpty()) {
                result += TtsChunkDraft(text.toString(), mapping.toList(), paragraphIndex)
                text = StringBuilder()
                mapping = mutableListOf()
            }
        }

        for (boxIndex in start until endExclusive) {
            val boxText = textBoxes[boxIndex].text.trim()
            if (boxText.isEmpty()) continue

            if (text.isNotEmpty()) {
                if (text.length + 1 + boxText.length > MAX_CHUNK_LENGTH) flush() else {
                    text.append(' ')
                    mapping += -1
                }
            }

            var offset = 0
            while (offset < boxText.length) {
                val capacity = MAX_CHUNK_LENGTH - text.length
                if (capacity == 0) {
                    flush()
                    continue
                }
                val count = minOf(capacity, boxText.length - offset)
                text.append(boxText, offset, offset + count)
                repeat(count) { mapping += boxIndex }
                offset += count
                if (offset < boxText.length) flush()
            }
        }
        flush()
        return result
    }

    private fun unionBounds(textBoxes: List<PdfTextBox>, start: Int, endExclusive: Int): Rect =
        textBoxes.subList(start, endExclusive)
            .map { it.bounds }
            .reduce { bounds, rect -> bounds.union(rect) }

    private fun isSameLine(first: Rect, second: Rect): Boolean {
        val firstCenter = (first.top + first.bottom) / 2f
        val secondCenter = (second.top + second.bottom) / 2f
        return abs(firstCenter - secondCenter) <= max(first.height, second.height) * 0.6f
    }
}
