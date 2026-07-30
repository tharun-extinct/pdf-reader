package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class TextHighlightSelectorTest {
    @Test
    fun selectsContinuousRangesAcrossWrappedLines() {
        val firstLine = listOf(
            Rect(0.10f, 0.10f, 0.14f, 0.14f),
            Rect(0.15f, 0.10f, 0.19f, 0.14f),
            Rect(0.20f, 0.10f, 0.24f, 0.14f)
        )
        val secondLine = listOf(
            Rect(0.10f, 0.16f, 0.14f, 0.20f),
            Rect(0.15f, 0.16f, 0.19f, 0.20f),
            Rect(0.20f, 0.16f, 0.24f, 0.20f)
        )
        val boxes = listOf(
            PdfTextBox(0, "abc", Rect(0.10f, 0.10f, 0.24f, 0.14f), firstLine),
            PdfTextBox(0, "def", Rect(0.10f, 0.16f, 0.24f, 0.20f), secondLine)
        )

        val selection = TextHighlightSelector.select(
            boxes,
            start = Offset(0.16f, 0.12f),
            end = Offset(0.16f, 0.18f)
        )

        assertEquals(listOf(
            Rect(0.15f, 0.10f, 0.24f, 0.14f),
            Rect(0.10f, 0.16f, 0.19f, 0.20f)
        ), selection)
    }

    @Test
    fun supportsReverseDragDirection() {
        val characters = listOf(
            Rect(0.10f, 0.10f, 0.14f, 0.14f),
            Rect(0.15f, 0.10f, 0.19f, 0.14f),
            Rect(0.20f, 0.10f, 0.24f, 0.14f)
        )
        val boxes = listOf(PdfTextBox(0, "abc", Rect(0.10f, 0.10f, 0.24f, 0.14f), characters))

        val selection = TextHighlightSelector.select(boxes, Offset(0.22f, 0.12f), Offset(0.11f, 0.12f))

        assertEquals(listOf(Rect(0.10f, 0.10f, 0.24f, 0.14f)), selection)
    }
}
