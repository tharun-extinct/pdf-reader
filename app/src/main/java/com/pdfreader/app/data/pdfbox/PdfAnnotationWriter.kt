package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
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
            val strokeWidth = PdfCoordinateMapper.toPdfStrokeWidth(page, stroke.normalizedStrokeWidth)

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
            val margin = (strokeWidth / 2f).coerceAtLeast(2f)

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
            val bs = COSDictionary()
            bs.setFloat(COSName.W, strokeWidth.coerceAtLeast(MIN_INK_STROKE_WIDTH))
            annot.cosObject.setItem(COSName.BS, bs)
            annot.createInkAppearance(document, path, strokeWidth)

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
            annot.cosObject.setString(NOX_READER_TEXT_ANNOTATION_ID, ta.id.toString())
            annot.constructAppearances(document)

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
        deletedEmbeddedInkIdsByPage: Map<Int, Set<String>>,
        deletedEmbeddedTextAnnotationIdsByPage: Map<Int, Set<String>>
    ) {
        val allPageIndices = (
            strokesByPage.keys + highlightsByPage.keys + textAnnotationsByPage.keys +
                deletedEmbeddedHighlightIdsByPage.keys + deletedEmbeddedInkIdsByPage.keys +
                deletedEmbeddedTextAnnotationIdsByPage.keys
            ).toSet()
        for (pageIndex in allPageIndices) {
            if (pageIndex < 0 || pageIndex >= document.numberOfPages) continue
            removeDeletedEmbeddedAnnotations(
                page = document.getPage(pageIndex),
                pageIndex = pageIndex,
                highlightIds = deletedEmbeddedHighlightIdsByPage[pageIndex].orEmpty(),
                inkIds = deletedEmbeddedInkIdsByPage[pageIndex].orEmpty(),
                textIds = deletedEmbeddedTextAnnotationIdsByPage[pageIndex].orEmpty()
            )
            writeHighlights(document, pageIndex, highlightsByPage[pageIndex].orEmpty())
            writeInkStrokes(document, pageIndex, strokesByPage[pageIndex].orEmpty())
            writeTextAnnotations(document, pageIndex, textAnnotationsByPage[pageIndex].orEmpty())
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

    private fun removeDeletedEmbeddedAnnotations(
        page: PDPage,
        pageIndex: Int,
        highlightIds: Set<String>,
        inkIds: Set<String>,
        textIds: Set<String>
    ) {
        val highlightIndexes = highlightIds.mapNotNull { id ->
            id.removePrefix("embedded:$pageIndex:").toIntOrNull()
        }.toSet()
        val inkIndexes = inkIds.mapNotNull { id ->
            id.removePrefix("embedded-ink:$pageIndex:").toIntOrNull()
        }.toSet()
        val textIndexes = textIds.mapNotNull { id ->
            id.removePrefix("embedded-text:$pageIndex:").toIntOrNull()
        }.toSet()
        if (highlightIndexes.isEmpty() && inkIndexes.isEmpty() && textIndexes.isEmpty()) return
        page.annotations = page.annotations.filterIndexed { index, annotation ->
            !(
                index in highlightIndexes && annotation.subtype == PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT ||
                    index in inkIndexes && annotation.subtype == PDAnnotationMarkup.SUB_TYPE_INK ||
                    index in textIndexes && annotation is PDAnnotationText
                )
        }.toMutableList()
    }

    /** Supplies a width-exact, rounded normal appearance instead of viewer-specific defaults. */
    private fun PDAnnotationMarkup.createInkAppearance(
        document: PDDocument,
        path: FloatArray,
        strokeWidth: Float
    ) {
        val rect = rectangle ?: return
        if (path.size < 4) return
        val appearanceStream = PDAppearanceStream(document).apply {
            bBox = PDRectangle(0f, 0f, rect.width, rect.height)
            resources = PDResources()
        }
        PDPageContentStream(document, appearanceStream).use { stream ->
            stream.setGraphicsStateParameters(PDExtendedGraphicsState().apply {
                strokingAlphaConstant = constantOpacity
            })
            stream.setStrokingColor(color ?: return@use)
            stream.setLineWidth(strokeWidth)
            stream.setLineCapStyle(1)
            stream.setLineJoinStyle(1)
            stream.addSmoothInkPath(path, rect.lowerLeftX, rect.lowerLeftY)
            stream.stroke()
        }
        setAppearance(PDAppearanceDictionary().apply { setNormalAppearance(appearanceStream) })
    }

    /** Mirrors the Compose midpoint spline using cubic PDF path operators. */
    private fun PDPageContentStream.addSmoothInkPath(
        path: FloatArray,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        val pointCount = path.size / 2
        if (pointCount == 0) return
        var currentX = path[0] - offsetX
        var currentY = path[1] - offsetY
        moveTo(currentX, currentY)
        for (index in 1 until pointCount - 1) {
            val controlX = path[index * 2] - offsetX
            val controlY = path[index * 2 + 1] - offsetY
            val nextX = path[(index + 1) * 2] - offsetX
            val nextY = path[(index + 1) * 2 + 1] - offsetY
            val endX = (controlX + nextX) / 2f
            val endY = (controlY + nextY) / 2f
            curveTo(
                currentX + (controlX - currentX) * 2f / 3f,
                currentY + (controlY - currentY) * 2f / 3f,
                endX + (controlX - endX) * 2f / 3f,
                endY + (controlY - endY) * 2f / 3f,
                endX,
                endY
            )
            currentX = endX
            currentY = endY
        }
        lineTo(path[(pointCount - 1) * 2] - offsetX, path[(pointCount - 1) * 2 + 1] - offsetY)
    }

    /** Supplies a transparent, quad-based normal appearance so it never obscures page text. */
    private fun PDAnnotationTextMarkup.createHighlightAppearance(document: PDDocument, quadPoints: FloatArray) {
        val rect = rectangle ?: return
        val appearanceStream = PDAppearanceStream(document)
        appearanceStream.bBox = PDRectangle(0f, 0f, rect.width, rect.height)
        // PDFBox Android does not create a resource dictionary for a fresh
        // appearance stream. Opacity is stored as an ExtGState resource, so
        // setGraphicsStateParameters would otherwise dereference null.
        appearanceStream.resources = PDResources()
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

    private const val MIN_INK_STROKE_WIDTH = 0.5f

}

internal val NOX_READER_TEXT_ANNOTATION_ID: COSName =
    COSName.getPDFName("NoxReaderTextAnnotationId")
