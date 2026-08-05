package com.pdfreader.app.data.pdfium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.test.core.app.ApplicationProvider
import com.pdfreader.app.data.pdfbox.PdfAnnotationWriter
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class PdfEmbeddedTextAnnotationReaderTest {
    @Before
    fun initializePdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun writtenTextNoteReopensWithContentAnchorAndSourceIdentity() {
        val savedPdf = PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = mapOf(
                    0 to listOf(
                        TextAnnotation(
                            id = 42L,
                            pageIndex = 0,
                            position = Offset(0.25f, 0.3f),
                            bounds = Rect(0.25f, 0.3f, 0.6f, 0.44f),
                            color = 0xFF336699L,
                            text = "Reopen me"
                        )
                    )
                ),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )
            ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }

        PDDocument.load(savedPdf).use { reopened ->
            val annotation = readEmbeddedTextAnnotations(reopened.getPage(0), 0).single()

            assertEquals(-1L, annotation.id)
            assertEquals("embedded-text:0:0", annotation.embeddedId)
            assertEquals(42L, annotation.sourceAnnotationId)
            assertEquals("Reopen me", annotation.text)
            assertEquals(0.25f, annotation.position.x, 0.0001f)
            assertEquals(0.3f, annotation.position.y, 0.0001f)
            assertEquals(0xFF336699L, annotation.color)
        }
    }
}
