package com.pdfreader.app.domain.repository

import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import java.io.File

/**
 * Domain-level interface for baking in-memory annotations into the physical PDF file.
 * Implementations use PDFBox to write standard PDF annotation objects (Highlight,
 * Ink, Text) so they are visible in any compliant PDF viewer.
 */
interface PdfAnnotationSaver {

    /**
     * Writes all in-memory annotations into a new PDF derived from [pdfBytes],
     * saves the result to [outputFile], and returns it.
     *
     * @param pdfBytes         Raw bytes of the currently-open PDF.
     * @param strokesByPage    Map of page index → freehand ink strokes.
     * @param highlightsByPage Map of page index → text highlights.
     * @param textAnnotationsByPage Map of page index → sticky-note text annotations.
     * @param outputFile       Destination file (typically in cacheDir).
     */
    suspend fun saveAnnotations(
        pdfBytes: ByteArray,
        strokesByPage: Map<Int, List<FreehandStroke>>,
        highlightsByPage: Map<Int, List<TextHighlight>>,
        textAnnotationsByPage: Map<Int, List<TextAnnotation>>,
        deletedEmbeddedHighlightIdsByPage: Map<Int, Set<String>>,
        deletedEmbeddedInkIdsByPage: Map<Int, Set<String>>,
        deletedEmbeddedTextAnnotationIdsByPage: Map<Int, Set<String>>,
        outputFile: File
    ): File
}
