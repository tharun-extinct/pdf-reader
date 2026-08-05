package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class TextAnnotationReselectionTest {
    @Test
    fun promotedNoteResolvesCallbacksFromItsOriginalEmbeddedIdentity() {
        val replacement = TextAnnotation(
            id = 42L,
            pageIndex = 0,
            position = Offset(0.2f, 0.3f),
            bounds = Rect(0.2f, 0.3f, 0.56f, 0.44f),
            color = 0xFF336699L,
            text = "Editing",
            sourceEmbeddedAnnotationId = -7L
        )
        val state = PdfReaderState(textAnnotationsByPage = mapOf(0 to listOf(replacement)))

        assertEquals(42L, state.resolvePendingTextAnnotationId(-7L))
        assertEquals(42L, state.resolvePendingTextAnnotationId(42L))
    }

    @Test
    fun persistedSourceIdentityWinsWhenContentsAreDuplicated() {
        val target = TextAnnotationReselectionTarget(
            pageIndex = 0,
            position = Offset(0.2f, 0.3f),
            text = "Same text",
            expectedSourceAnnotationId = 42L
        )
        val wrong = embedded(id = -1L, embeddedId = "embedded-text:0:0", sourceId = 7L)
        val expected = embedded(id = -2L, embeddedId = "embedded-text:0:1", sourceId = 42L)

        assertEquals(expected, findReselectedTextAnnotation(target, listOf(wrong, expected)))
    }

    @Test
    fun externalNoteFallsBackToSameContentsAtNearestAnchor() {
        val target = TextAnnotationReselectionTarget(
            pageIndex = 0,
            position = Offset(0.2f, 0.3f),
            text = "External note",
            expectedSourceAnnotationId = null
        )
        val expected = embedded(
            id = -3L,
            embeddedId = "embedded-text:0:2",
            sourceId = null,
            position = Offset(0.205f, 0.305f),
            text = "External note"
        )

        assertEquals(expected, findReselectedTextAnnotation(target, listOf(expected)))
    }

    private fun embedded(
        id: Long,
        embeddedId: String,
        sourceId: Long?,
        position: Offset = Offset(0.2f, 0.3f),
        text: String = "Same text"
    ) = EmbeddedTextAnnotation(
        id = id,
        embeddedId = embeddedId,
        pageIndex = 0,
        position = position,
        iconBounds = Rect(position.x, position.y, position.x + 0.03f, position.y + 0.03f),
        color = 0xFF336699L,
        text = text,
        sourceAnnotationId = sourceId
    )
}
