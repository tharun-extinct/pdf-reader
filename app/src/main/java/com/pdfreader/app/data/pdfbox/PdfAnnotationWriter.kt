package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationInk
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup

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
 *   pdfX = normalizedX × pageWidthPt
 *   pdfY = (1 − normalizedY) × pageHeightPt
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
        val pw = page.mediaBox.width
        val ph = page.mediaBox.height
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
                // Convert corners to PDF points (bottom-left origin)
                val pdfLeft   = rect.left   * pw
                val pdfRight  = rect.right  * pw
                val pdfTop    = (1f - rect.top)    * ph  // top in screen → high Y in PDF
                val pdfBottom = (1f - rect.bottom) * ph  // bottom in screen → low Y in PDF

                // QuadPoints order (PDF spec): upper-left, upper-right, lower-left, lower-right
                quadPoints[qIdx++] = pdfLeft;  quadPoints[qIdx++] = pdfTop    // upper-left
                quadPoints[qIdx++] = pdfRight; quadPoints[qIdx++] = pdfTop    // upper-right
                quadPoints[qIdx++] = pdfLeft;  quadPoints[qIdx++] = pdfBottom // lower-left
                quadPoints[qIdx++] = pdfRight; quadPoints[qIdx++] = pdfBottom // lower-right

                if (pdfLeft   < minX) minX = pdfLeft
                if (pdfBottom < minY) minY = pdfBottom
                if (pdfRight  > maxX) maxX = pdfRight
                if (pdfTop    > maxY) maxY = pdfTop
            }

            val annot = PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT)
            annot.rectangle = PDRectangle(minX, minY, maxX - minX, maxY - minY)
            annot.quadPoints = quadPoints
            annot.color = highlight.color.toRgbPdColor()
            // Transparency: ARGB alpha is stored in the upper 8 bits of the Long
            annot.constantOpacity = ((highlight.color ushr 24) and 0xFF).toFloat() / 255f

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
        val pw = page.mediaBox.width
        val ph = page.mediaBox.height
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
                val pdfX = point.x * pw
                val pdfY = (1f - point.y) * ph
                path[i * 2]     = pdfX
                path[i * 2 + 1] = pdfY
                if (pdfX < minX) minX = pdfX
                if (pdfY < minY) minY = pdfY
                if (pdfX > maxX) maxX = pdfX
                if (pdfY > maxY) maxY = pdfY
            }

            // Add a small margin around the bounding box to ensure the stroke cap
            // is fully inside the annotation rectangle
            val margin = (stroke.strokeWidth / 2f).coerceAtLeast(2f)

            val annot = PDAnnotationInk()
            annot.rectangle = PDRectangle(
                minX - margin, minY - margin,
                (maxX - minX) + margin * 2,
                (maxY - minY) + margin * 2
            )
            annot.inkList = listOf(path)
            annot.color = stroke.color.toRgbPdColor()

            // Store the stroke width in the border style array so viewers honour it
            val bs = com.tom_roush.pdfbox.cos.COSDictionary()
            bs.setInt(com.tom_roush.pdfbox.cos.COSName.W, stroke.strokeWidth.toInt().coerceAtLeast(1))
            annot.cosObject.setItem(com.tom_roush.pdfbox.cos.COSName.BS, bs)

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
        val pw = page.mediaBox.width
        val ph = page.mediaBox.height
        val annotations = page.annotations

        for (ta in textAnnotations) {
            if (ta.text.isBlank()) continue

            val pdfX = ta.position.x * pw
            val pdfY = (1f - ta.position.y) * ph

            // Standard note icon is 16×16 pt
            val iconSize = 16f
            val annot = PDAnnotationText()
            annot.rectangle = PDRectangle(pdfX, pdfY - iconSize, iconSize, iconSize)
            annot.contents = ta.text
            annot.color = ta.color.toRgbPdColor()
            annot.setName(PDAnnotationText.NAME_NOTE)
            annot.setOpen(false)

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
        textAnnotationsByPage: Map<Int, List<TextAnnotation>>
    ) {
        val allPageIndices = (strokesByPage.keys + highlightsByPage.keys + textAnnotationsByPage.keys).toSet()
        for (pageIndex in allPageIndices) {
            if (pageIndex < 0 || pageIndex >= document.numberOfPages) continue
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
     * annotation's CA entry via [PDAnnotation.constantOpacity].
     */
    private fun Long.toRgbPdColor(): PDColor {
        val r = ((this ushr 16) and 0xFF).toFloat() / 255f
        val g = ((this ushr  8) and 0xFF).toFloat() / 255f
        val b = ( this          and 0xFF).toFloat() / 255f
        return PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
    }
}
