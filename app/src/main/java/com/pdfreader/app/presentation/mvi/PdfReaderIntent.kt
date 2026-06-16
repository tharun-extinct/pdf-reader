package com.pdfreader.app.presentation.mvi

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset

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

    data class SelectTool(val tool: AnnotationTool) : PdfReaderIntent()

    data class SelectPenColor(val index: Int) : PdfReaderIntent()

    data class SelectHighlighterColor(val index: Int) : PdfReaderIntent()

    object ToggleAnnotationSettings : PdfReaderIntent()

    data class SavePenColors(val colors: List<Long>) : PdfReaderIntent()

    data class SaveHighlighterColors(val colors: List<Long>) : PdfReaderIntent()

    data class AddStroke(val stroke: FreehandStroke) : PdfReaderIntent()

    data class RemoveStrokeAt(val pageIndex: Int, val position: Offset) : PdfReaderIntent()

    data class AddTextAnnotation(val pageIndex: Int, val position: Offset) : PdfReaderIntent()

    data class UpdateTextAnnotation(val annotationId: Long, val text: String) : PdfReaderIntent()
}
