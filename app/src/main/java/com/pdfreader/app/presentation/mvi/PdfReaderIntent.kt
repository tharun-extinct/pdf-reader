package com.pdfreader.app.presentation.mvi

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.pdfreader.app.domain.model.ThemeMode

/**
 * Represents the user intents/actions for the PDF Reader.
 */
sealed class PdfReaderIntent {
    /** Intent to open a PDF file from a given URI */
    data class OpenPdf(val uri: Uri) : PdfReaderIntent()
    
    /** Intent to close the currently opened PDF */
    object ClosePdf : PdfReaderIntent()

    /** Intent to sync changes back to the source URI (e.g., Google Drive) */
    data class SyncPdf(val localFile: java.io.File) : PdfReaderIntent()
    
    /** 
     * Intent to request the rendering of a specific page.
     * We pass a callback instead of saving bitmaps in the main state 
     * to prevent out-of-memory errors and keep the state lightweight.
     */
    data class RequestPageRender(
        val pageIndex: Int,
        val width: Int,
        val height: Int,
        val onRendered: (Bitmap?) -> Unit
    ) : PdfReaderIntent()

    data class RequestPageText(
        val pageIndex: Int,
        val onExtracted: (List<PdfTextBox>) -> Unit
    ) : PdfReaderIntent()

    data class PageChanged(val pageIndex: Int) : PdfReaderIntent()

    object ToggleBookmark : PdfReaderIntent()

    object ClearRecentDocuments : PdfReaderIntent()

    object DismissError : PdfReaderIntent()

    data class SetThemeMode(val mode: ThemeMode) : PdfReaderIntent()

    data class SetKeepScreenOn(val enabled: Boolean) : PdfReaderIntent()

    data class SetSpeechRate(val rate: Float) : PdfReaderIntent()

    data class RequestPageHighlights(
        val pageIndex: Int,
        val onLoaded: (List<EmbeddedTextHighlight>) -> Unit = {}
    ) : PdfReaderIntent()

    data class RequestPageInk(val pageIndex: Int) : PdfReaderIntent()

    data class RequestPageTextAnnotations(val pageIndex: Int) : PdfReaderIntent()

    data class SelectTool(val tool: AnnotationTool) : PdfReaderIntent()

    data class SelectPenColor(val index: Int) : PdfReaderIntent()

    data class SelectHighlighterColor(val index: Int) : PdfReaderIntent()

    data class SavePenColors(val colors: List<Long>) : PdfReaderIntent()

    data class SaveHighlighterColors(val colors: List<Long>) : PdfReaderIntent()

    data class AddStroke(val stroke: FreehandStroke) : PdfReaderIntent()

    data class AddTextHighlight(val highlight: TextHighlight) : PdfReaderIntent()

    data class RemoveStrokeAt(val pageIndex: Int, val position: Offset) : PdfReaderIntent()

    data class AddTextAnnotation(val pageIndex: Int, val position: Offset) : PdfReaderIntent()

    data class UpdateTextAnnotation(val annotationId: Long, val text: String) : PdfReaderIntent()

    data class SelectTextAnnotation(val annotationId: Long) : PdfReaderIntent()

    data class ResizeTextAnnotation(
        val annotationId: Long,
        val handle: TextAnnotationHandle,
        val normalizedDelta: Offset
    ) : PdfReaderIntent()

    data class SelectAnnotationAt(val pageIndex: Int, val position: Offset) : PdfReaderIntent()
    object ClearAnnotationSelection : PdfReaderIntent()
    object DeleteSelectedAnnotation : PdfReaderIntent()

    data class PlayTts(val pageIndex: Int, val textBoxes: List<PdfTextBox>) : PdfReaderIntent()
    object PauseTts : PdfReaderIntent()
    object ResumeTts : PdfReaderIntent()
    object PreviousTtsParagraph : PdfReaderIntent()
    object NextTtsParagraph : PdfReaderIntent()
    object StopTts : PdfReaderIntent()

    /**
     * Bakes all in-memory annotations into the PDF file and syncs it back
     * to the original source URI via the Storage Access Framework.
     */
    object SaveAnnotations : PdfReaderIntent()
}
