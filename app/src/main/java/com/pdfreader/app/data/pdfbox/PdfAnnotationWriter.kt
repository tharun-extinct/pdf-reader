package com.pdfreader.app.data.pdfbox

import com.pdfreader.app.presentation.mvi.FreehandStroke
import com.pdfreader.app.presentation.mvi.TextAnnotation
import com.pdfreader.app.presentation.mvi.TextHighlight
import com.pdfreader.app.presentation.mvi.AnnotationSaveMode
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
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
                val flattenedAnnotations = flattenAnnotations(document, page, newAnnotations)
                page.annotations = page.annotations
                    // PDFBox materializes fresh annotation wrapper objects every time
                    // `page.annotations` is read, so Kotlin object identity cannot
                    // identify the entries that were just flattened. The COS dictionary
                    // is the stable backing object shared by those wrappers.
                    .filterNot { annotation ->
                        flattenedAnnotations.any { it.cosObject === annotation.cosObject }
                    }
                    .toMutableList()
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

    /**
     * Paints supported annotations into page contents and returns only the annotations
     * that were represented without dropping payload. A note that cannot be encoded or
     * fitted remains editable instead of being silently removed.
     */
    private fun flattenAnnotations(
        document: PDDocument,
        page: PDPage,
        annotations: List<PDAnnotation>
    ): Set<PDAnnotation> {
        if (annotations.isEmpty()) return emptySet()
        val flattened = mutableSetOf<PDAnnotation>()
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
                val wasFlattened = when (annotation) {
                    is PDAnnotationTextMarkup -> {
                        var paintedQuad = false
                        annotation.quadPoints?.asList()?.chunked(8)?.forEach quadLoop@{ quad ->
                            if (quad.size != 8) return@quadLoop
                            stream.setNonStrokingColor(color)
                            stream.moveTo(quad[0], quad[1])
                            stream.lineTo(quad[2], quad[3])
                            stream.lineTo(quad[6], quad[7])
                            stream.lineTo(quad[4], quad[5])
                            stream.closePath()
                            stream.fill()
                            paintedQuad = true
                        }
                        paintedQuad
                    }
                    is PDAnnotationText -> flattenTextNote(stream, page, annotation, color)
                    is PDAnnotationMarkup -> {
                        if (annotation.subtype != PDAnnotationMarkup.SUB_TYPE_INK) {
                            false
                        } else {
                            var paintedPath = false
                            stream.setStrokingColor(color)
                            stream.setLineWidth(annotation.inkStrokeWidth())
                            stream.setLineCapStyle(1)
                            stream.setLineJoinStyle(1)
                            annotation.getInkList().forEach pathLoop@{ path ->
                                if (path.size < 4) return@pathLoop
                                stream.addSmoothInkPath(path)
                                stream.stroke()
                                paintedPath = true
                            }
                            paintedPath
                        }
                    }
                    else -> false
                }
                stream.restoreGraphicsState()
                if (wasFlattened) flattened += annotation
            }
        }
        return flattened
    }

    private fun PDAnnotationMarkup.inkStrokeWidth(): Float {
        val borderStyle = cosObject.getDictionaryObject(COSName.BS) as? COSDictionary
        return borderStyle
            ?.getFloat(COSName.W, DEFAULT_INK_STROKE_WIDTH)
            ?.coerceAtLeast(MIN_INK_STROKE_WIDTH)
            ?: DEFAULT_INK_STROKE_WIDTH
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

    /** Paints a readable, popup-like note box containing the complete note payload. */
    private fun flattenTextNote(
        stream: PDPageContentStream,
        page: PDPage,
        annotation: PDAnnotationText,
        accentColor: PDColor
    ): Boolean {
        val contents = annotation.contents?.takeIf { it.isNotBlank() } ?: return false
        val pageBox = page.cropBox ?: page.mediaBox
        val availableWidth = pageBox.width - NOTE_PAGE_MARGIN * 2
        val availableHeight = pageBox.height - NOTE_PAGE_MARGIN * 2
        if (availableWidth < NOTE_MIN_BOX_WIDTH || availableHeight < NOTE_LINE_HEIGHT + NOTE_PADDING * 2) {
            return false
        }

        val boxWidth = minOf(NOTE_MAX_BOX_WIDTH, availableWidth)
        val textWidth = boxWidth - NOTE_PADDING * 2
        val lines = wrapNoteText(contents, textWidth) ?: return false
        val boxHeight = NOTE_PADDING * 2 + NOTE_HEADER_HEIGHT + lines.size * NOTE_LINE_HEIGHT
        if (boxHeight > availableHeight) return false

        val rect = annotation.rectangle ?: return false
        val minX = pageBox.lowerLeftX + NOTE_PAGE_MARGIN
        val maxX = pageBox.upperRightX - NOTE_PAGE_MARGIN - boxWidth
        val minY = pageBox.lowerLeftY + NOTE_PAGE_MARGIN
        val maxY = pageBox.upperRightY - NOTE_PAGE_MARGIN - boxHeight
        val boxX = (rect.upperRightX + NOTE_ANCHOR_GAP).coerceIn(minX, maxX)
        val boxY = (rect.upperRightY - boxHeight).coerceIn(minY, maxY)

        stream.setNonStrokingColor(NOTE_BACKGROUND_COLOR)
        stream.addRect(boxX, boxY, boxWidth, boxHeight)
        stream.fill()

        stream.setStrokingColor(accentColor)
        stream.setLineWidth(NOTE_BORDER_WIDTH)
        stream.addRect(boxX, boxY, boxWidth, boxHeight)
        stream.stroke()

        stream.setNonStrokingColor(accentColor)
        stream.addRect(
            boxX,
            boxY + boxHeight - NOTE_HEADER_HEIGHT,
            boxWidth,
            NOTE_HEADER_HEIGHT
        )
        stream.fill()

        stream.setNonStrokingColor(NOTE_TEXT_COLOR)
        stream.beginText()
        stream.setFont(NOTE_FONT, NOTE_FONT_SIZE)
        stream.newLineAtOffset(
            boxX + NOTE_PADDING,
            boxY + boxHeight - NOTE_HEADER_HEIGHT - NOTE_PADDING - NOTE_FONT_SIZE
        )
        lines.forEachIndexed { index, line ->
            if (index > 0) stream.newLineAtOffset(0f, -NOTE_LINE_HEIGHT)
            stream.showText(line)
        }
        stream.endText()
        return true
    }

    /**
     * Wraps by glyph so every encodable character is retained. Unsupported glyphs or
     * a single glyph wider than the box return null, causing the editable note to stay.
     */
    private fun wrapNoteText(contents: String, maxWidth: Float): List<String>? {
        return try {
            val lines = mutableListOf<String>()
            val paragraphs = contents.replace("\r\n", "\n").replace('\r', '\n').split('\n')
            paragraphs.forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    lines += ""
                    return@forEach
                }

                val currentLine = StringBuilder()
                var currentWidth = 0f
                paragraph.forEach { character ->
                    val glyph = if (character == '\t') "    " else character.toString()
                    val glyphWidth = NOTE_FONT.getStringWidth(glyph) / 1000f * NOTE_FONT_SIZE
                    if (glyphWidth > maxWidth) return null
                    if (currentLine.isNotEmpty() && currentWidth + glyphWidth > maxWidth) {
                        lines += currentLine.toString()
                        currentLine.clear()
                        currentWidth = 0f
                    }
                    currentLine.append(glyph)
                    currentWidth += glyphWidth
                }
                lines += currentLine.toString()
            }
            lines
        } catch (_: IllegalArgumentException) {
            null
        }
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

    private val NOTE_FONT = PDType1Font.HELVETICA
    private val NOTE_BACKGROUND_COLOR = PDColor(floatArrayOf(1f, 1f, 1f), PDDeviceRGB.INSTANCE)
    private val NOTE_TEXT_COLOR = PDColor(floatArrayOf(0f, 0f, 0f), PDDeviceRGB.INSTANCE)
    private const val DEFAULT_INK_STROKE_WIDTH = 2f
    private const val MIN_INK_STROKE_WIDTH = 0.5f
    private const val NOTE_FONT_SIZE = 9f
    private const val NOTE_LINE_HEIGHT = 11f
    private const val NOTE_PADDING = 6f
    private const val NOTE_HEADER_HEIGHT = 5f
    private const val NOTE_BORDER_WIDTH = 1f
    private const val NOTE_PAGE_MARGIN = 8f
    private const val NOTE_ANCHOR_GAP = 4f
    private const val NOTE_MIN_BOX_WIDTH = 72f
    private const val NOTE_MAX_BOX_WIDTH = 180f

}
