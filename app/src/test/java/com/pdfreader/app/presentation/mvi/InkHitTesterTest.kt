package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InkHitTesterTest {
    private val stroke = SelectedInk(
        id = "ink",
        pageIndex = 0,
        source = InkSource.Embedded,
        color = 0xFF000000,
        normalizedStrokeWidth = 0.01f,
        paths = listOf(listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)))
    )

    @Test
    fun selectsStrokeNearSegmentBetweenSampledPoints() {
        assertEquals("ink", InkHitTester.select(Offset(0.5f, 0.505f), listOf(stroke))?.id)
    }

    @Test
    fun ignoresTapOutsideStrokeTolerance() {
        assertNull(InkHitTester.select(Offset(0.5f, 0.7f), listOf(stroke)))
    }
}
