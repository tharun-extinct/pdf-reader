package com.pdfreader.app.data.pdfbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.test.core.app.ApplicationProvider
import com.pdfreader.app.presentation.mvi.AnnotationSaveMode
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
import com.tom_roush.pdfbox.text.PDFTextStripper
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
    fun editableHighlightSavesOnPageWithoutResources() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))

            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = mapOf(
                    0 to listOf(
                        TextHighlight(
                            id = 1L,
                            pageIndex = 0,
                            color = 0x80FFEB3BL,
                            rects = listOf(Rect(0.1f, 0.1f, 0.4f, 0.15f))
                        )
                    )
                ),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                saveMode = AnnotationSaveMode.Editable
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
    fun flattenedTextNoteWritesVisibleTextAndRemovesAnnotation() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = mapOf(
                    0 to listOf(
                        TextAnnotation(
                            id = 1L,
                            pageIndex = 0,
                            position = Offset(0.1f, 0.1f),
                            color = 0xFFFFC107L,
                            text = "Review this section"
                        )
                    )
                ),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                saveMode = AnnotationSaveMode.Flattened
            )

            assertTrue(document.getPage(0).annotations.isEmpty())
            val savedPdf = ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
            PDDocument.load(savedPdf).use { reopened ->
                assertTrue(reopened.getPage(0).annotations.isEmpty())
                assertTrue(PDFTextStripper().getText(reopened).contains("Review this section"))
            }
        }
    }

    @Test
    fun flattenedInkUsesConfiguredStrokeWidth() {
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
                            points = listOf(Offset(0.1f, 0.1f), Offset(0.3f, 0.3f))
                        )
                    )
                ),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                saveMode = AnnotationSaveMode.Flattened
            )

            assertTrue(document.getPage(0).annotations.isEmpty())
            val content = document.getPage(0).contents.bufferedReader(Charsets.ISO_8859_1).use { it.readText() }
            val lineWidths = Regex("""(?:^|\s)([0-9]+(?:\.[0-9]+)?)\s+w(?:\s|$)""")
                .findAll(content)
                .map { it.groupValues[1].toFloat() }
                .toList()
            assertEquals(6.12f, lineWidths.single(), 0.001f)
        }
    }

    @Test
    fun editableInkKeepsConfiguredWidthAndRoundedAppearance() {
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
                            points = listOf(
                                Offset(0.1f, 0.1f), Offset(0.2f, 0.25f), Offset(0.3f, 0.1f)
                            )
                        )
                    )
                ),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = emptyMap(),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                saveMode = AnnotationSaveMode.Editable
            )

            val annotation = document.getPage(0).annotations.single()
            val borderStyle = annotation.cosObject.getDictionaryObject(COSName.BS) as COSDictionary
            assertEquals(6.12f, borderStyle.getFloat(COSName.W), 0.001f)
            val appearance = requireNotNull(annotation.normalAppearanceStream)
            val content = appearance.cosObject.createInputStream()
                .bufferedReader(Charsets.ISO_8859_1)
                .use { it.readText() }
            assertTrue(content.contains("1 J"))
            assertTrue(content.contains("1 j"))
        }
    }

    @Test
    fun flattenedTextNoteRetainsEditableAnnotationWhenTextCannotBeEncoded() {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            val contents = "Review ✓"
            PdfAnnotationWriter.writeAll(
                document = document,
                strokesByPage = emptyMap(),
                highlightsByPage = emptyMap(),
                textAnnotationsByPage = mapOf(
                    0 to listOf(
                        TextAnnotation(
                            id = 1L,
                            pageIndex = 0,
                            position = Offset(0.1f, 0.1f),
                            color = 0xFFFFC107L,
                            text = contents
                        )
                    )
                ),
                deletedEmbeddedHighlightIdsByPage = emptyMap(),
                saveMode = AnnotationSaveMode.Flattened
            )

            val remainingAnnotation = document.getPage(0).annotations.single()
            assertEquals(contents, remainingAnnotation.contents)
        }
    }
}
