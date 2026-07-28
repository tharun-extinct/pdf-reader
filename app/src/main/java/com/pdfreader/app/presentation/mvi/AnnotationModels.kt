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

/** Controls whether newly saved annotations remain editable or are painted into page content. */
enum class AnnotationSaveMode {
    Editable,
    Flattened
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
    val strokeWidth: Float,
    val points: List<Offset>
)

data class TextAnnotation(
    val id: Long,
    val pageIndex: Int,
    val position: Offset,
    val color: Long,
    val text: String
)

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

data class SelectedHighlight(
    val id: String,
    val pageIndex: Int,
    val source: HighlightSource,
    val color: Long,
    val rects: List<Rect>
)

private val DefaultPenColors = listOf(
    0xFFE53935L,
    0xFF1E88E5L,
    0xFF43A047L,
    0xFFFDD835L
)

private val DefaultHighlighterColors = listOf(
    0x66FFEB3BL,
    0x668E24AAL,
    0x664CAF50L,
    0x66FB8C00L
)

fun defaultPenPalette(): AnnotationPalette = AnnotationPalette(DefaultPenColors)

fun defaultHighlighterPalette(): AnnotationPalette = AnnotationPalette(DefaultHighlighterColors)

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
