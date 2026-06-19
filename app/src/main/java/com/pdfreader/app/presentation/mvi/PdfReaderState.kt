package com.pdfreader.app.presentation.mvi

import android.net.Uri
import androidx.compose.ui.geometry.Offset

/**
 * Represents the immutable state of the PDF Reader UI.
 */
data class PdfReaderState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isPdfLoaded: Boolean = false,
    val pageCount: Int = 0,
    val openedUri: Uri? = null,
    val errorMessage: String? = null,
    val activeTool: AnnotationTool = AnnotationTool.None,
    val penPalette: AnnotationPalette = defaultPenPalette(),
    val highlighterPalette: AnnotationPalette = defaultHighlighterPalette(),
    val selectedPenColorIndex: Int = 0,
    val selectedHighlighterColorIndex: Int = 0,
    val isAnnotationSettingsOpen: Boolean = false,
    val strokesByPage: Map<Int, List<FreehandStroke>> = emptyMap(),
    val highlightsByPage: Map<Int, List<TextHighlight>> = emptyMap(),
    val textBoxesByPage: Map<Int, List<PdfTextBox>> = emptyMap(),
    val textAnnotationsByPage: Map<Int, List<TextAnnotation>> = emptyMap(),
    val selectedTextPositionByPage: Map<Int, Offset> = emptyMap()
)
