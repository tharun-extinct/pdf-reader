package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

/** Turns a drag into contiguous text ranges, including the text between endpoints on wrapped lines. */
object TextHighlightSelector {
    fun select(textBoxes: List<PdfTextBox>, start: Offset, end: Offset): List<Rect> {
        val characters = textBoxes
            .flatMap { it.characterBounds }
            .filter { it.width > 0f && it.height > 0f }
        if (characters.isEmpty()) return emptyList()

        val lines = characters
            .sortedWith(compareBy<Rect> { it.top }.thenBy { it.left })
            .fold(mutableListOf<MutableList<Rect>>()) { result, character ->
                val line = result.lastOrNull()
                if (line == null || !isOnSameLine(line.first(), character)) {
                    result.add(mutableListOf())
                }
                result.last() += character
                result
            }
            .map { it.sortedBy { rect -> rect.left } }

        var first = cursorAt(lines, start)
        var last = cursorAt(lines, end)
        if (first > last) {
            val temporary = first
            first = last
            last = temporary
        }

        return (first.lineIndex..last.lineIndex).mapNotNull { lineIndex ->
            val line = lines[lineIndex]
            val firstCharacter = if (lineIndex == first.lineIndex) first.characterIndex else 0
            val lastCharacter = if (lineIndex == last.lineIndex) last.characterIndex else line.lastIndex
            val selected = line.subList(firstCharacter, lastCharacter + 1)
            Rect(
                left = selected.first().left,
                top = selected.minOf { rect -> rect.top },
                right = selected.last().right,
                bottom = selected.maxOf { rect -> rect.bottom }
            )
        }
    }

    private fun isOnSameLine(first: Rect, candidate: Rect): Boolean {
        val centerDifference = abs(first.center.y - candidate.center.y)
        return centerDifference <= maxOf(first.height, candidate.height) * 0.55f
    }

    private fun cursorAt(lines: List<List<Rect>>, point: Offset): Cursor {
        val lineIndex = lines.indices.minByOrNull { index ->
            abs(lines[index].first().center.y - point.y)
        } ?: 0
        val line = lines[lineIndex]
        val characterIndex = line.indices.minByOrNull { index ->
            abs(line[index].center.x - point.x)
        } ?: 0
        return Cursor(lineIndex, characterIndex)
    }

    private data class Cursor(val lineIndex: Int, val characterIndex: Int) : Comparable<Cursor> {
        override fun compareTo(other: Cursor): Int =
            if (lineIndex != other.lineIndex) lineIndex.compareTo(other.lineIndex)
            else characterIndex.compareTo(other.characterIndex)
    }
}
