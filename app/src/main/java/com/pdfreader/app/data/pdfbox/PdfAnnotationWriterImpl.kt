package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.domain.repository.PdfAnnotationSaver
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implements [PdfAnnotationSaver] using Apache PDFBox.
 *
 * Strategy:
 * 1. Load a fresh [PDDocument] from the original [pdfBytes] (avoids mutating the
 *    live PDFium document, which lives in native memory).
 * 2. Delegate to [PdfAnnotationWriter] to add standard PDF annotation objects.
 * 3. Save the modified document to [outputFile].
 * 4. Return [outputFile] so the ViewModel can hand it to [SafPdfSyncManager].
 */
class PdfAnnotationWriterImpl : PdfAnnotationSaver {

    override suspend fun saveAnnotations(
        pdfBytes: ByteArray,
        strokesByPage: Map<Int, List<FreehandStroke>>,
        highlightsByPage: Map<Int, List<TextHighlight>>,
        textAnnotationsByPage: Map<Int, List<TextAnnotation>>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val document = PDDocument.load(pdfBytes)
        try {
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = strokesByPage,
                highlightsByPage = highlightsByPage,
                textAnnotationsByPage = textAnnotationsByPage
            )
            document.save(outputFile)
        } finally {
            document.close()
        }
        outputFile
    }
}
