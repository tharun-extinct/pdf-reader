package com.pdfreader.app.presentation.mvi

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.pdfreader.app.domain.tts.TtsState

/**
 * Represents the immutable state of the PDF Reader UI.
 */
data class PdfReaderState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isSavingAnnotations: Boolean = false,
    val isPdfLoaded: Boolean = false,
    val pageCount: Int = 0,
    val openedUri: Uri? = null,
    val documentTitle: String? = null,
    val errorMessage: String? = null,
    val activeTool: AnnotationTool = AnnotationTool.None,
    val penPalette: AnnotationPalette = defaultPenPalette(),
    val highlighterPalette: AnnotationPalette = defaultHighlighterPalette(),
    val selectedPenColorIndex: Int = 0,
    val selectedHighlighterColorIndex: Int = 0,
    val isAnnotationSettingsOpen: Boolean = false,
    val annotationSaveMode: AnnotationSaveMode = AnnotationSaveMode.Editable,
    val strokesByPage: Map<Int, List<FreehandStroke>> = emptyMap(),
    val highlightsByPage: Map<Int, List<TextHighlight>> = emptyMap(),
    val embeddedHighlightsByPage: Map<Int, List<EmbeddedTextHighlight>> = emptyMap(),
    val deletedEmbeddedHighlightIdsByPage: Map<Int, Set<String>> = emptyMap(),
    val selectedHighlight: SelectedHighlight? = null,
    val textBoxesByPage: Map<Int, List<PdfTextBox>> = emptyMap(),
    val textAnnotationsByPage: Map<Int, List<TextAnnotation>> = emptyMap(),
    val selectedTextPositionByPage: Map<Int, Offset> = emptyMap(),
    val ttsState: TtsState = TtsState.Idle,
    /**
     * Incremented each time annotations are baked into the PDF and the document is
     * re-opened. Composable pages observe this to know they must re-render their
     * bitmaps so the embedded annotations become visible in the rendered output.
     */
    val renderRevision: Int = 0
)
