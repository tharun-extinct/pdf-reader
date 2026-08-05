package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextAnnotationGeometryTest {
    @Test
    fun createBoundsKeepsTextBoxInsidePage() {
        assertEquals(
            Rect(0.84f, 0.92f, 1f, 1f),
            TextAnnotationGeometry.createBounds(Offset(0.98f, 0.99f))
        )
    }

    @Test
    fun resizeClampsToPageAndMinimumSize() {
        val bounds = Rect(0.2f, 0.2f, 0.6f, 0.5f)

        assertEquals(
            Rect(0.44f, 0.42f, 0.6f, 0.5f),
            TextAnnotationGeometry.resize(
                bounds,
                TextAnnotationHandle.TopLeft,
                Offset(0.8f, 0.8f)
            )
        )
        assertEquals(
            Rect(0.2f, 0.2f, 1f, 1f),
            TextAnnotationGeometry.resize(
                bounds,
                TextAnnotationHandle.BottomRight,
                Offset(0.8f, 0.8f)
            )
        )
    }

    @Test
    fun selectReturnsTopmostOverlappingTextBox() {
        val lower = annotation(1L, Rect(0.1f, 0.1f, 0.5f, 0.5f))
        val upper = annotation(2L, Rect(0.2f, 0.2f, 0.6f, 0.6f))

        assertEquals(
            upper,
            TextAnnotationGeometry.select(Offset(0.3f, 0.3f), listOf(lower, upper))
        )
        assertNull(TextAnnotationGeometry.select(Offset(0.9f, 0.9f), listOf(lower, upper)))
    }

    private fun annotation(id: Long, bounds: Rect) = TextAnnotation(
        id = id,
        pageIndex = 0,
        position = bounds.topLeft,
        bounds = bounds,
        color = 0xFF000000,
        text = "Note"
    )
}