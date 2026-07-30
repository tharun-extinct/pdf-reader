package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.pdfreader.app.presentation.mvi.AnnotationSaveMode
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState

/**
 * Pure utility object that converts normalized in-memory annotations
 * (0..1 coordinates, top-left origin) into standard PDFBox annotation objects
 * (PDF points, bottom-left origin) and appends them to the given [PDPage].
 *
 * Coordinate system mapping
 * ─────────────────────────
 * The app stores all annotation coordinates as **normalized values 0→1**
 * measured from the **top-left** corner of the page.
 *
 * PDF spec uses **points** (1pt = 1/72 inch) measured from the **bottom-left**
 * corner.  The conversion is:
 *
 * For an unrotated page, `pdfX = CropBox.left + normalizedX × CropBox.width`
 * and `pdfY = CropBox.bottom + (1 − normalizedY) × CropBox.height`. The helper
 * below also maps 90° increments of page rotation.
 */
object PdfAnnotationWriter {

    // ─── public entry points ────────────────────────────────────────────────

    /**
     * Appends highlight annotations for every [TextHighlight] on [pageIndex].
     * Uses the `/Highlight` subtype of PDF text-markup annotations so all
     * major viewers (Adobe, Drive, etc.) render them correctly.
     */
    fun writeHighlights(
        document: PDDocument,
        pageIndex: Int,
        highlights: List<TextHighlight>
    ) {
        if (highlights.isEmpty()) return
        val page = document.getPage(pageIndex)
        val annotations = page.annotations

        for (highlight in highlights) {
            if (highlight.rects.isEmpty()) continue

            // The outer bounding rectangle is the union of all highlight rects
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            // Each rect in [highlight.rects] is a normalized Compose Rect
            // (left, top, right, bottom) in top-left origin.
            val quadPoints = FloatArray(highlight.rects.size * 8)
            var qIdx = 0

            for (rect in highlight.rects) {
                val corners = listOf(
                    PdfCoordinateMapper.toPdfPoint(page, androidx.compose.ui.geometry.Offset(rect.left, rect.top)),
                    PdfCoordinateMapper.toPdfPoint(page, androidx.compose.ui.geometry.Offset(rect.right, rect.top)),
                    PdfCoordinateMapper.toPdfPoint(page, androidx.compose.ui.geometry.Offset(rect.left, rect.bottom)),
                    PdfCoordinateMapper.toPdfPoint(page, androidx.compose.ui.geometry.Offset(rect.right, rect.bottom))
                )

                // Preserve the display-space quad order. The page transform accounts for
                // CropBox and rotation before these points are written in PDF user space.
                for (corner in corners) {
                    quadPoints[qIdx++] = corner.x
                    quadPoints[qIdx++] = corner.y
                    minX = minOf(minX, corner.x)
                    minY = minOf(minY, corner.y)
                    maxX = maxOf(maxX, corner.x)
                    maxY = maxOf(maxY, corner.y)
                }
            }

            val annot = PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT)
            annot.rectangle = PDRectangle(minX, minY, maxX - minX, maxY - minY)
            annot.quadPoints = quadPoints
            annot.color = highlight.color.toRgbPdColor()
            // Transparency: ARGB alpha is stored in the upper 8 bits of the Long
            annot.setConstantOpacity(((highlight.color ushr 24) and 0xFF).toFloat() / 255f)
            annot.createHighlightAppearance(document, quadPoints)

            annotations.add(annot)
        }
        page.annotations = annotations
    }

    /**
     * Appends ink (freehand) annotations for every [FreehandStroke] on [pageIndex].
     * Uses the `/Ink` annotation type so strokes are vector-based inside the PDF.
     */
    fun writeInkStrokes(
        document: PDDocument,
        pageIndex: Int,
        strokes: List<FreehandStroke>
    ) {
        if (strokes.isEmpty()) return
        val page = document.getPage(pageIndex)
        val annotations = page.annotations

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            // Each stroke is a single continuous path — one float[] in the inkList
            val path = FloatArray(stroke.points.size * 2)
            for ((i, point) in stroke.points.withIndex()) {
                val pdfPoint = PdfCoordinateMapper.toPdfPoint(page, point)
                path[i * 2] = pdfPoint.x
                path[i * 2 + 1] = pdfPoint.y
                minX = minOf(minX, pdfPoint.x)
                minY = minOf(minY, pdfPoint.y)
                maxX = maxOf(maxX, pdfPoint.x)
                maxY = maxOf(maxY, pdfPoint.y)
            }

            // Add a small margin around the bounding box to ensure the stroke cap
            // is fully inside the annotation rectangle
            val margin = (stroke.strokeWidth / 2f).coerceAtLeast(2f)

            // PdfBox-Android 2.x represents ink with the generic markup annotation.
            // The dedicated PDAnnotationInk class was added in newer upstream PDFBox versions.
            val annot = PDAnnotationMarkup()
            annot.cosObject.setName(com.tom_roush.pdfbox.cos.COSName.SUBTYPE, PDAnnotationMarkup.SUB_TYPE_INK)
            annot.rectangle = PDRectangle(
                minX - margin, minY - margin,
                (maxX - minX) + margin * 2,
                (maxY - minY) + margin * 2
            )
            annot.setInkList(arrayOf(path))
            annot.color = stroke.color.toRgbPdColor()
            annot.setConstantOpacity(((stroke.color ushr 24) and 0xFF).toFloat() / 255f)

            // Store the stroke width in the border style array so viewers honour it
            val bs = com.tom_roush.pdfbox.cos.COSDictionary()
            bs.setInt(com.tom_roush.pdfbox.cos.COSName.W, stroke.strokeWidth.toInt().coerceAtLeast(1))
            annot.cosObject.setItem(com.tom_roush.pdfbox.cos.COSName.BS, bs)
            annot.constructAppearances(document)

            annotations.add(annot)
        }
        page.annotations = annotations
    }

    /**
     * Appends text (sticky-note) annotations for every [TextAnnotation] on [pageIndex].
     * Uses `/Text` annotation type — renders as a note icon in standard viewers.
     */
    fun writeTextAnnotations(
        document: PDDocument,
        pageIndex: Int,
        textAnnotations: List<TextAnnotation>
    ) {
        if (textAnnotations.isEmpty()) return
        val page = document.getPage(pageIndex)
        val annotations = page.annotations

        for (ta in textAnnotations) {
            if (ta.text.isBlank()) continue

            val pdfPoint = PdfCoordinateMapper.toPdfPoint(page, ta.position)

            // Standard note icon is 16×16 pt
            val iconSize = 16f
            val annot = PDAnnotationText()
            annot.rectangle = PDRectangle(pdfPoint.x, pdfPoint.y - iconSize, iconSize, iconSize)
            annot.contents = ta.text
            annot.color = ta.color.toRgbPdColor()
            annot.setName(PDAnnotationText.NAME_NOTE)
            annot.setOpen(false)
            annot.createRectangleAppearance(document)

            annotations.add(annot)
        }
        page.annotations = annotations
    }

    /**
     * Convenience: write all annotation types for every page that has annotations.
     */
    fun writeAll(
        document: PDDocument,
        strokesByPage: Map<Int, List<FreehandStroke>>,
        highlightsByPage: Map<Int, List<TextHighlight>>,
        textAnnotationsByPage: Map<Int, List<TextAnnotation>>,
        deletedEmbeddedHighlightIdsByPage: Map<Int, Set<String>>,
        saveMode: AnnotationSaveMode
    ) {
        val allPageIndices = (
            strokesByPage.keys + highlightsByPage.keys + textAnnotationsByPage.keys +
                deletedEmbeddedHighlightIdsByPage.keys
            ).toSet()
        for (pageIndex in allPageIndices) {
            if (pageIndex < 0 || pageIndex >= document.numberOfPages) continue
            removeDeletedEmbeddedHighlights(document.getPage(pageIndex), pageIndex, deletedEmbeddedHighlightIdsByPage[pageIndex].orEmpty())
            val annotationCountBeforeWrite = document.getPage(pageIndex).annotations.size
            writeHighlights(document, pageIndex, highlightsByPage[pageIndex].orEmpty())
            writeInkStrokes(document, pageIndex, strokesByPage[pageIndex].orEmpty())
            writeTextAnnotations(document, pageIndex, textAnnotationsByPage[pageIndex].orEmpty())
            if (saveMode == AnnotationSaveMode.Flattened) {
                val page = document.getPage(pageIndex)
                val newAnnotations = page.annotations.drop(annotationCountBeforeWrite)
                flattenAnnotations(document, page, newAnnotations)
                page.annotations = page.annotations.dropLast(newAnnotations.size).toMutableList()
            }
        }
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    /**
     * Converts a packed ARGB [Long] color (as stored in [FreehandStroke.color],
     * [TextHighlight.color], [TextAnnotation.color]) to a [PDColor] in DeviceRGB space.
     *
     * Note: PDColor only carries RGB — the alpha (opacity) is set separately on the
     * annotation's CA entry via [PDAnnotationMarkup.setConstantOpacity].
     */
    private fun Long.toRgbPdColor(): PDColor {
        val r = ((this ushr 16) and 0xFF).toFloat() / 255f
        val g = ((this ushr  8) and 0xFF).toFloat() / 255f
        val b = ( this          and 0xFF).toFloat() / 255f
        return PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
    }

    private fun removeDeletedEmbeddedHighlights(page: PDPage, pageIndex: Int, ids: Set<String>) {
        if (ids.isEmpty()) return
        val indexes = ids.mapNotNull { id ->
            id.removePrefix("embedded:$pageIndex:").toIntOrNull()
        }.toSet()
        if (indexes.isEmpty()) return
        page.annotations = page.annotations.filterIndexed { index, annotation ->
            index !in indexes || annotation.subtype != PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
        }.toMutableList()
    }

    /** Paints supported annotations into page contents before removing their /Annots entries. */
    private fun flattenAnnotations(document: PDDocument, page: PDPage, annotations: List<PDAnnotation>) {
        if (annotations.isEmpty()) return
        PDPageContentStream(
            document,
            page,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        ).use { stream ->
            annotations.forEach { annotation ->
                val color = annotation.color ?: return@forEach
                val opacity = (annotation as? PDAnnotationMarkup)?.getConstantOpacity() ?: 1f
                stream.saveGraphicsState()
                stream.setGraphicsStateParameters(PDExtendedGraphicsState().apply {
                    nonStrokingAlphaConstant = opacity
                    strokingAlphaConstant = opacity
                })
                when (annotation) {
                    is PDAnnotationTextMarkup -> {
                        annotation.quadPoints?.asList()?.chunked(8)?.forEach quadLoop@{ quad ->
                            if (quad.size != 8) return@quadLoop
                            stream.setNonStrokingColor(color)
                            stream.moveTo(quad[0], quad[1])
                            stream.lineTo(quad[2], quad[3])
                            stream.lineTo(quad[6], quad[7])
                            stream.lineTo(quad[4], quad[5])
                            stream.closePath()
                            stream.fill()
                        }
                    }
                    is PDAnnotationMarkup -> {
                        if (annotation.subtype != PDAnnotationMarkup.SUB_TYPE_INK) return@forEach
                        stream.setStrokingColor(color)
                        stream.setLineWidth(2f)
                        annotation.getInkList().forEach pathLoop@{ path ->
                            if (path.size < 4) return@pathLoop
                            stream.moveTo(path[0], path[1])
                            path.asList().chunked(2).drop(1).forEach { point ->
                                if (point.size == 2) stream.lineTo(point[0], point[1])
                            }
                            stream.stroke()
                        }
                    }
                    is PDAnnotationText -> {
                        val rect = annotation.rectangle
                        stream.setNonStrokingColor(color)
                        stream.addRect(rect.lowerLeftX, rect.lowerLeftY, rect.width, rect.height)
                        stream.fill()
                    }
                }
                stream.restoreGraphicsState()
            }
        }
    }

    /** Supplies a transparent, quad-based normal appearance so it never obscures page text. */
    private fun PDAnnotationTextMarkup.createHighlightAppearance(document: PDDocument, quadPoints: FloatArray) {
        val rect = rectangle ?: return
        val appearanceStream = PDAppearanceStream(document)
        appearanceStream.bBox = PDRectangle(0f, 0f, rect.width, rect.height)
        PDPageContentStream(document, appearanceStream).use { stream ->
            val color = color ?: return@use
            stream.setGraphicsStateParameters(PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = constantOpacity
            })
            stream.setNonStrokingColor(color)
            quadPoints.asList().chunked(8).forEach { quad ->
                if (quad.size != 8) return@forEach
                stream.moveTo(quad[0] - rect.lowerLeftX, quad[1] - rect.lowerLeftY)
                stream.lineTo(quad[2] - rect.lowerLeftX, quad[3] - rect.lowerLeftY)
                stream.lineTo(quad[6] - rect.lowerLeftX, quad[7] - rect.lowerLeftY)
                stream.lineTo(quad[4] - rect.lowerLeftX, quad[5] - rect.lowerLeftY)
                stream.closePath()
                stream.fill()
            }
        }
        val dictionary = PDAppearanceDictionary()
        dictionary.setNormalAppearance(appearanceStream)
        setAppearance(dictionary)
    }

}
