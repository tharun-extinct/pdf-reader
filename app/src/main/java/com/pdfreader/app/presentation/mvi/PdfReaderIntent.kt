package com.pdfreader.app.presentation.mvi

import android.graphics.Bitmap
import android.net.Uri

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
}
