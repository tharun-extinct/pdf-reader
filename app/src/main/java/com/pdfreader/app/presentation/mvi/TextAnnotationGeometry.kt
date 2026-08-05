package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

object TextAnnotationGeometry {
    private const val DefaultWidth = 0.36f
    private const val DefaultHeight = 0.14f
    private const val MinWidth = 0.16f
    private const val MinHeight = 0.08f

    fun createBounds(anchor: Offset): Rect {
        val left = anchor.x.coerceIn(0f, 1f - MinWidth)
        val top = anchor.y.coerceIn(0f, 1f - MinHeight)
        return Rect(
            left = left,
            top = top,
            right = (left + DefaultWidth).coerceAtMost(1f),
            bottom = (top + DefaultHeight).coerceAtMost(1f)
        )
    }

    fun resize(bounds: Rect, handle: TextAnnotationHandle, delta: Offset): Rect {
        var left = bounds.left
        var top = bounds.top
        var right = bounds.right
        var bottom = bounds.bottom

        when (handle) {
            TextAnnotationHandle.TopLeft -> {
                left = (left + delta.x).coerceIn(0f, right - MinWidth)
                top = (top + delta.y).coerceIn(0f, bottom - MinHeight)
            }
            TextAnnotationHandle.TopRight -> {
                right = (right + delta.x).coerceIn(left + MinWidth, 1f)
                top = (top + delta.y).coerceIn(0f, bottom - MinHeight)
            }
            TextAnnotationHandle.BottomLeft -> {
                left = (left + delta.x).coerceIn(0f, right - MinWidth)
                bottom = (bottom + delta.y).coerceIn(top + MinHeight, 1f)
            }
            TextAnnotationHandle.BottomRight -> {
                right = (right + delta.x).coerceIn(left + MinWidth, 1f)
                bottom = (bottom + delta.y).coerceIn(top + MinHeight, 1f)
            }
        }

        return Rect(left, top, right, bottom)
    }

    fun select(position: Offset, annotations: List<TextAnnotation>): TextAnnotation? =
        annotations.asReversed().firstOrNull { it.bounds.contains(position) }
}