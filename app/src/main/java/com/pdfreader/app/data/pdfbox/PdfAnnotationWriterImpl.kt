package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.domain.repository.PdfAnnotationSaver
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
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
        deletedEmbeddedHighlightIdsByPage: Map<Int, Set<String>>,
        deletedEmbeddedInkIdsByPage: Map<Int, Set<String>>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val document = ByteArrayInputStream(pdfBytes).use { input ->
            PDDocument.load(input, MemoryUsageSetting.setupMixed(PDFBOX_MEMORY_LIMIT_BYTES))
        }
        try {
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = strokesByPage,
                highlightsByPage = highlightsByPage,
                textAnnotationsByPage = textAnnotationsByPage,
                deletedEmbeddedHighlightIdsByPage = deletedEmbeddedHighlightIdsByPage,
                deletedEmbeddedInkIdsByPage = deletedEmbeddedInkIdsByPage
            )
            document.save(outputFile)
        } finally {
            document.close()
        }
        outputFile
    }
}

private const val PDFBOX_MEMORY_LIMIT_BYTES = 50L * 1024L * 1024L
