package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Pure hit-testing policy used by the ViewModel and JVM tests. */
object HighlightHitTester {
    fun select(position: Offset, candidates: List<SelectedHighlight>): SelectedHighlight? {
        return candidates
            .filter { highlight -> highlight.rects.any { it.inflateForHitTarget().contains(position) } }
            .minByOrNull { highlight -> highlight.rects.sumOf { (it.width * it.height).toDouble() } }
    }
}

private fun Rect.inflateForHitTarget(): Rect = Rect(
    left = (left - 0.01f).coerceIn(0f, 1f),
    top = (top - 0.01f).coerceIn(0f, 1f),
    right = (right + 0.01f).coerceIn(0f, 1f),
    bottom = (bottom + 0.01f).coerceIn(0f, 1f)
)
