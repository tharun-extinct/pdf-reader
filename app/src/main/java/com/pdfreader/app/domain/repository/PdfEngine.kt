package com.pdfreader.app.domain.repository

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Size
import com.pdfreader.app.presentation.mvi.PdfTextBox
import com.pdfreader.app.presentation.mvi.EmbeddedTextHighlight
import com.pdfreader.app.presentation.mvi.EmbeddedInkAnnotation
import com.pdfreader.app.presentation.mvi.EmbeddedTextAnnotation

/**
 * Domain-level interface for the PDF Rendering Engine.
 * This hides the underlying implementation (PDFium) from the Domain/Presentation layers.
 */
interface PdfEngine {
    
    /**
     * Opens the document given a ParcelFileDescriptor (pointing to the PDF file).
     */
    fun openDocument(pfd: ParcelFileDescriptor, pdfBytes: ByteArray)
    
    /**
     * Returns the total number of pages in the loaded document.
     */
    fun getPageCount(): Int
    
    /**
     * Retrieves the width and height points for a specific page.
     */
    fun getPageSize(pageIndex: Int): Size
    
    /**
     * Renders a specific page into a Bitmap of the exact given dimensions.
     * This allows rendering large bitmaps for high zoom scales, or small tiles.
     */
    fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap

    /**
     * Extracts positioned text boxes for text selection and synchronized highlighting.
     * Bounds are normalized to the page: left/top/right/bottom are 0f..1f.
     */
    fun getTextBoxes(pageIndex: Int): List<PdfTextBox>

    /** Returns embedded text highlights in normalized display coordinates. */
    fun getEmbeddedHighlights(pageIndex: Int): List<EmbeddedTextHighlight>

    /** Returns editable embedded /Ink annotations in normalized display coordinates. */
    fun getEmbeddedInk(pageIndex: Int): List<EmbeddedInkAnnotation>

    /** Returns editable embedded /Text notes in normalized display coordinates. */
    fun getEmbeddedTextAnnotations(pageIndex: Int): List<EmbeddedTextAnnotation>
    
    /**
     * Returns the raw PDF bytes that were passed to [openDocument].
     * Used by the annotation writer to load a fresh PDDocument for writing.
     * Returns null if no document is currently open.
     */
    fun getPdfBytes(): ByteArray?

    /**
     * Closes the document and frees up memory allocations in the native code.
     */
    fun closeDocument()
}
