package com.pdfreader.app.domain.tts

import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.presentation.mvi.PdfTextBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTextNavigatorTest {
    @Test
    fun `spoken text and box mapping stay character aligned`() {
        val boxes = listOf(
            box("Hello", 0.10f, 0.10f, 0.20f, 0.14f),
            box("world", 0.22f, 0.10f, 0.32f, 0.14f)
        )

        val chunk = TtsTextNavigator.buildChunks(boxes).single()

        assertEquals("Hello world", chunk.text)
        assertEquals(chunk.text.length, chunk.textToBoxIndices.size)
        assertEquals(List(5) { 0 } + listOf(-1) + List(5) { 1 }, chunk.textToBoxIndices)
    }

    @Test
    fun `vertical paragraph gap creates navigable paragraph chunks`() {
        val boxes = listOf(
            box("First", 0.10f, 0.10f, 0.18f, 0.14f),
            box("paragraph.", 0.20f, 0.10f, 0.36f, 0.14f),
            box("Second", 0.10f, 0.20f, 0.20f, 0.24f)
        )

        val chunks = TtsTextNavigator.buildChunks(boxes)

        assertEquals(listOf(0, 1), chunks.map { it.paragraphIndex })
        assertEquals(listOf("First paragraph.", "Second"), chunks.map { it.text })
    }

    @Test
    fun `active word expands to one synchronized line rect`() {
        val boxes = listOf(
            box("First", 0.10f, 0.10f, 0.18f, 0.14f),
            box("line", 0.20f, 0.10f, 0.28f, 0.14f),
            box("Next", 0.10f, 0.18f, 0.18f, 0.22f)
        )

        val rects = TtsTextNavigator.lineHighlightRects(boxes, setOf(1))

        assertEquals(1, rects.size)
        assertEquals(Rect(0.10f, 0.10f, 0.28f, 0.14f), rects.single())
        assertTrue(rects.single().bottom < boxes.last().bounds.top)
    }

    private fun box(text: String, left: Float, top: Float, right: Float, bottom: Float) =
        PdfTextBox(0, text, Rect(left, top, right, bottom))
}
