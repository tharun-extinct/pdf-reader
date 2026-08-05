package com.pdfreader.app.data.pdfbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.test.core.app.ApplicationProvider
import com.pdfreader.app.presentation.mvi.AnnotationTool
import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class PdfAnnotationWriterTest {
    @Before
    fun initializePdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun highlightSavesAsEditableAnnotationOnPageWithoutResources() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = mapOf(
                    0 to listOf(
                        TextHighlight(1L, 0, 0x80FFEB3BL, listOf(Rect(0.1f, 0.1f, 0.4f, 0.15f)))
                    )
                ),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )

            val savedPdf = ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
            PDDocument.load(savedPdf).use { reopened ->
                assertEquals(1, reopened.getPage(0).annotations.size)
            }
        }
    }

    @Test
    fun inkKeepsConfiguredWidthAndRoundedEditableAppearance() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = mapOf(
                    0 to listOf(
                        FreehandStroke(
                            id = 1L,
                            pageIndex = 0,
                            tool = AnnotationTool.Pen,
                            color = 0xFF1E88E5L,
                            normalizedStrokeWidth = 0.01f,
                            points = listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.25f), Offset(0.3f, 0.1f))
                        )
                    )
                ),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )

            val annotation = document.getPage(0).annotations.single()
            val borderStyle = annotation.cosObject.getDictionaryObject(COSName.BS) as COSDictionary
            assertEquals(6.12f, borderStyle.getFloat(COSName.W), 0.001f)
            val content = requireNotNull(annotation.normalAppearanceStream)
                .cosObject.createInputStream().bufferedReader(Charsets.ISO_8859_1).use { it.readText() }
            assertTrue(content.contains("1 J"))
            assertTrue(content.contains("1 j"))
        }
    }

    @Test
    fun deletesSelectedEmbeddedInkByStablePageAnnotationIndex() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            val stroke = FreehandStroke(
                id = 1L,
                pageIndex = 0,
                tool = AnnotationTool.Pen,
                color = 0xFF1E88E5L,
                normalizedStrokeWidth = 0.01f,
                points = listOf(Offset(0.1f, 0.1f), Offset(0.3f, 0.3f))
            )
            PdfAnnotationWriter.writeAll(
                document,
                strokesByPage = mapOf(0 to listOf(stroke)),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )
            PdfAnnotationWriter.writeAll(
                document,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = mapOf(0 to setOf("embedded-ink:0:0")),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )
            assertTrue(document.getPage(0).annotations.isEmpty())
        }
    }

    @Test
    fun deletesSelectedEmbeddedTextNoteByStablePageAnnotationIndex() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = mapOf(
                    0 to listOf(
                        TextHighlight(
                            id = 7L,
                            pageIndex = 0,
                            color = 0x80FFEB3BL,
                            rects = listOf(Rect(0.1f, 0.1f, 0.4f, 0.15f))
                        )
                    )
                ),
                textAnnotationsByPage = mapOf(
                    0 to listOf(
                        TextAnnotation(
                            id = 42L,
                            pageIndex = 0,
                            position = Offset(0.2f, 0.3f),
                            bounds = Rect(0.2f, 0.3f, 0.56f, 0.44f),
                            color = 0xFF336699L,
                            text = "Delete me"
                        )
                    )
                ),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = emptyMap()
            )
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                deletedEmbeddedInkIdsByPage = emptyMap(),
                deletedEmbeddedTextAnnotationIdsByPage = mapOf(
                    0 to setOf("embedded-text:0:1")
                )
            )

            val remaining = document.getPage(0).annotations.single()
            assertEquals(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT, remaining.subtype)
        }
    }
}
