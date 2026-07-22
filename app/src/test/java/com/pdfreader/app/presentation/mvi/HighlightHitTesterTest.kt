package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightHitTesterTest {
    @Test
    fun choosesTheSmallestOverlappingHighlight() {
        val large = SelectedHighlight("large", 0, HighlightSource.Embedded, 0x66FFFF00, listOf(Rect(0.1f, 0.1f, 0.9f, 0.9f)))
        val small = SelectedHighlight("small", 0, HighlightSource.Embedded, 0x66FFFF00, listOf(Rect(0.4f, 0.4f, 0.6f, 0.6f)))

        val selected = HighlightHitTester.select(Offset(0.5f, 0.5f), listOf(large, small))

        assertEquals("small", selected?.id)
    }
}
