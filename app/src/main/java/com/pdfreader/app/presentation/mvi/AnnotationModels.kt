package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset

enum class AnnotationTool {
    ReadAloud,
    Pen,
    Highlighter,
    Eraser,
    AddText
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

private val DefaultPenColors = listOf(
    0xFFE53935,
    0xFF1E88E5,
    0xFF43A047,
    0xFFFDD835
)

private val DefaultHighlighterColors = listOf(
    0x66FFEB3B,
    0x668E24AA,
    0x664CAF50,
    0x66FB8C00
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
