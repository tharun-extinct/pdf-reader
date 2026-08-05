package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

enum class AnnotationTool {
    None,
    ReadAloud,
    Pen,
    Highlighter,
    Eraser,
    AddText
}

enum class HighlightSource {
    Session,
    Embedded
}

data class AnnotationPalette(
    val colors: List<Long>
)

data class FreehandStroke(
    val id: Long,
    val pageIndex: Int,
    val tool: AnnotationTool,
    val color: Long,
    /** Stroke width as a fraction of the displayed page width. */
    val normalizedStrokeWidth: Float,
    val points: List<Offset>
)

data class TextAnnotation(
    val id: Long,
    val pageIndex: Int,
    val position: Offset,
    val bounds: Rect,
    val color: Long,
    val text: String
)

/** An editable /Text note read from the opened PDF. */
data class EmbeddedTextAnnotation(
    /** Negative, page-scoped UI identity that cannot collide with pending note IDs. */
    val id: Long,
    /** Stable PDF identity used to remove or replace this annotation on save. */
    val embeddedId: String,
    val pageIndex: Int,
    val position: Offset,
    val iconBounds: Rect,
    val color: Long,
    val text: String,
    /** Original pending-note ID persisted by NoxReader, when available. */
    val sourceAnnotationId: Long? = null
)

enum class TextAnnotationHandle {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

data class PdfTextBox(
    val pageIndex: Int,
    val text: String,
    val bounds: Rect,
    /** Per-character geometry used to create continuous, line-aware text selections. */
    val characterBounds: List<Rect> = listOf(bounds)
)

data class TextHighlight(
    val id: Long,
    val pageIndex: Int,
    val color: Long,
    val rects: List<Rect>
)

/** A page-scoped highlight read from the opened PDF, cached off the main thread. */
data class EmbeddedTextHighlight(
    val id: String,
    val pageIndex: Int,
    val color: Long,
    val rects: List<Rect>
)

enum class InkSource {
    Session,
    Embedded
}

/** A page-scoped editable /Ink annotation read from the opened PDF. */
data class EmbeddedInkAnnotation(
    val id: String,
    val pageIndex: Int,
    val color: Long,
    val normalizedStrokeWidth: Float,
    val paths: List<List<Offset>>
)

data class SelectedInk(
    val id: String,
    val pageIndex: Int,
    val source: InkSource,
    val color: Long,
    val normalizedStrokeWidth: Float,
    val paths: List<List<Offset>>
)

data class SelectedHighlight(
    val id: String,
    val pageIndex: Int,
    val source: HighlightSource,
    val color: Long,
    val rects: List<Rect>
)

fun parseHexColor(hex: String): Long? {
    val normalized = hex.trim().removePrefix("#")
    if (normalized.length != 6 && normalized.length != 8) {
        return null
    }

    return try {
        val value = normalized.toLong(16)
        if (normalized.length == 6) {
            0xFF000000 or value
        } else {
            value
        }
    } catch (_: NumberFormatException) {
        null
    }
}

fun formatHexColor(color: Long): String {
    return "#%08X".format(color)
}
