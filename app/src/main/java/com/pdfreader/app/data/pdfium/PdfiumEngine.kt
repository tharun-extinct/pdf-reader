package com.pdfreader.app.data.pdfium

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.domain.repository.PdfEngine
import com.pdfreader.app.data.pdfbox.PdfCoordinateMapper
import com.pdfreader.app.data.pdfbox.NOX_READER_TEXT_ANNOTATION_ID
import com.pdfreader.app.presentation.mvi.EmbeddedTextHighlight
import com.pdfreader.app.presentation.mvi.EmbeddedInkAnnotation
import com.pdfreader.app.presentation.mvi.EmbeddedTextAnnotation
import com.pdfreader.app.presentation.mvi.PdfTextBox
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayInputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Data-level implementation of the PdfEngine using PDFium-Android.
 * Operates at the C++ level underneath, minimizing JVM garbage collection overhead
 * for reading/rendering files.
 */
class PdfiumEngine(private val context: Context) : PdfEngine {
    /**
     * PDFium loads native (.so) libraries on first touch. Initializing it lazily
     * (instead of in the constructor) keeps app launch crash-free: the native
     * library is only loaded when a PDF is actually opened, so a missing/incompatible
     * ABI surfaces as a recoverable error on the reader screen rather than taking
     * down the whole app at startup (the engine is built during ViewModel creation).
     */
    private val pdfiumCore: PdfiumCore by lazy {
        PDFBoxResourceLoader.init(context)
        PdfiumCore(context)
    }
    private var pdfDocument: PdfDocument? = null
    private var textDocument: PDDocument? = null
    private var rawPdfBytes: ByteArray? = null
    private val textBoxCache = mutableMapOf<Int, List<PdfTextBox>>()
    private val embeddedHighlightCache = mutableMapOf<Int, List<EmbeddedTextHighlight>>()
    private val embeddedInkCache = mutableMapOf<Int, List<EmbeddedInkAnnotation>>()
    private val embeddedTextAnnotationCache = mutableMapOf<Int, List<EmbeddedTextAnnotation>>()

    override fun openDocument(pfd: ParcelFileDescriptor, pdfBytes: ByteArray) {
        // Closes previous document if exists
        closeDocument()
        rawPdfBytes = pdfBytes
        pdfDocument = pdfiumCore.newDocument(pfd)
        textDocument = PDDocument.load(
            ByteArrayInputStream(pdfBytes),
            MemoryUsageSetting.setupMixed(PDFBOX_MEMORY_LIMIT_BYTES)
        )
    }

    override fun getPdfBytes(): ByteArray? = rawPdfBytes

    override fun getPageCount(): Int {
        return pdfDocument?.let { pdfiumCore.getPageCount(it) } ?: 0
    }

    override fun getPageSize(pageIndex: Int): Size {
        val doc = pdfDocument ?: throw IllegalStateException("Document not opened")
        pdfiumCore.openPage(doc, pageIndex)
        val width = pdfiumCore.getPageWidthPoint(doc, pageIndex)
        val height = pdfiumCore.getPageHeightPoint(doc, pageIndex)
        return Size(width, height)
    }

    override fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap {
        val doc = pdfDocument ?: throw IllegalStateException("Document not opened")
        pdfiumCore.openPage(doc, pageIndex)
        
        // We use ARGB_8888 for high quality color rendering
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Render the page onto the bitmap directly mapping it to native memory
        pdfiumCore.renderPageBitmap(
            doc, bitmap, pageIndex, 
            0, 0, width, height, true
        )
        
        return bitmap
    }

    override fun getTextBoxes(pageIndex: Int): List<PdfTextBox> {
        textBoxCache[pageIndex]?.let { return it }

        val doc = textDocument ?: return emptyList()
        val page = doc.getPage(pageIndex)
        val pageWidth = page.mediaBox.width.takeIf { it > 0f } ?: return emptyList()
        val pageHeight = page.mediaBox.height.takeIf { it > 0f } ?: return emptyList()
        val stripper = PositionedWordStripper(pageIndex, pageWidth, pageHeight)

        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.getText(doc)

        val boxes = stripper.words
        textBoxCache[pageIndex] = boxes
        return boxes
    }

    override fun getEmbeddedHighlights(pageIndex: Int): List<EmbeddedTextHighlight> {
        embeddedHighlightCache[pageIndex]?.let { return it }
        val page = textDocument?.getPage(pageIndex) ?: return emptyList()
        val highlights = page.annotations.mapIndexedNotNull { annotationIndex, annotation ->
            val markup = annotation as? PDAnnotationTextMarkup
            if (markup?.subtype != PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT) return@mapIndexedNotNull null

            val quads = markup.quadPoints ?: return@mapIndexedNotNull null
            val rects = quads.asList()
                .chunked(8)
                .mapNotNull { quad -> PdfCoordinateMapper.toNormalizedDisplayRect(page, quad.toFloatArray()) }
            if (rects.isEmpty()) return@mapIndexedNotNull null

            EmbeddedTextHighlight(
                id = "embedded:$pageIndex:$annotationIndex",
                pageIndex = pageIndex,
                color = markup.toArgbColor(),
                rects = rects
            )
        }
        embeddedHighlightCache[pageIndex] = highlights
        return highlights
    }

    override fun getEmbeddedInk(pageIndex: Int): List<EmbeddedInkAnnotation> {
        embeddedInkCache[pageIndex]?.let { return it }
        val page = textDocument?.getPage(pageIndex) ?: return emptyList()
        val ink = page.annotations.mapIndexedNotNull { annotationIndex, annotation ->
            val markup = annotation as? PDAnnotationMarkup
            if (markup?.subtype != PDAnnotationMarkup.SUB_TYPE_INK) return@mapIndexedNotNull null
            val paths = markup.getInkList()
                .map { path ->
                    path.asList().chunked(2).mapNotNull { pair ->
                        if (pair.size != 2) null
                        else PdfCoordinateMapper.toNormalizedDisplayPoint(page, androidx.compose.ui.geometry.Offset(pair[0], pair[1]))
                    }
                }
                .filter { it.isNotEmpty() }
            if (paths.isEmpty()) return@mapIndexedNotNull null
            val borderStyle = markup.cosObject.getDictionaryObject(COSName.BS) as? COSDictionary
            val pdfWidth = borderStyle?.getFloat(COSName.W, DEFAULT_INK_WIDTH) ?: DEFAULT_INK_WIDTH
            EmbeddedInkAnnotation(
                id = "embedded-ink:$pageIndex:$annotationIndex",
                pageIndex = pageIndex,
                color = markup.toArgbColor(),
                normalizedStrokeWidth = PdfCoordinateMapper.toNormalizedStrokeWidth(page, pdfWidth),
                paths = paths
            )
        }
        embeddedInkCache[pageIndex] = ink
        return ink
    }

    override fun getEmbeddedTextAnnotations(pageIndex: Int): List<EmbeddedTextAnnotation> {
        embeddedTextAnnotationCache[pageIndex]?.let { return it }
        val page = textDocument?.getPage(pageIndex) ?: return emptyList()
        val annotations = readEmbeddedTextAnnotations(page, pageIndex)
        embeddedTextAnnotationCache[pageIndex] = annotations
        return annotations
    }

    override fun closeDocument() {
        pdfDocument?.let {
            pdfiumCore.closeDocument(it)
        }
        pdfDocument = null
        textDocument?.close()
        textDocument = null
        rawPdfBytes = null
        textBoxCache.clear()
        embeddedHighlightCache.clear()
        embeddedInkCache.clear()
        embeddedTextAnnotationCache.clear()
    }
}

internal fun readEmbeddedTextAnnotations(
    page: PDPage,
    pageIndex: Int
): List<EmbeddedTextAnnotation> = page.annotations.mapIndexedNotNull { annotationIndex, annotation ->
    val textAnnotation = annotation as? PDAnnotationText ?: return@mapIndexedNotNull null
    val rectangle = textAnnotation.rectangle ?: return@mapIndexedNotNull null
    val iconBounds = PdfCoordinateMapper.toNormalizedDisplayRect(
        page,
        floatArrayOf(
            rectangle.lowerLeftX, rectangle.lowerLeftY,
            rectangle.upperRightX, rectangle.lowerLeftY,
            rectangle.lowerLeftX, rectangle.upperRightY,
            rectangle.upperRightX, rectangle.upperRightY
        )
    ) ?: return@mapIndexedNotNull null
    val anchor = PdfCoordinateMapper.toNormalizedDisplayPoint(
        page,
        Offset(rectangle.lowerLeftX, rectangle.upperRightY)
    )
    EmbeddedTextAnnotation(
        id = embeddedTextAnnotationUiId(pageIndex, annotationIndex),
        embeddedId = "embedded-text:$pageIndex:$annotationIndex",
        pageIndex = pageIndex,
        position = anchor,
        iconBounds = iconBounds,
        color = textAnnotation.toArgbColor(),
        text = textAnnotation.contents.orEmpty(),
        sourceAnnotationId = textAnnotation.cosObject
            .getString(NOX_READER_TEXT_ANNOTATION_ID)
            ?.toLongOrNull()
    )
}

private fun embeddedTextAnnotationUiId(pageIndex: Int, annotationIndex: Int): Long =
    -1L - ((pageIndex.toLong() shl 32) or annotationIndex.toLong())

private const val PDFBOX_MEMORY_LIMIT_BYTES = 50L * 1024L * 1024L

private fun PDAnnotationMarkup.toArgbColor(): Long {
    val components = color?.components ?: floatArrayOf(1f, 1f, 0f)
    val red = ((components.getOrElse(0) { 1f } * 255).toInt()).coerceIn(0, 255)
    val green = ((components.getOrElse(1) { 1f } * 255).toInt()).coerceIn(0, 255)
    val blue = ((components.getOrElse(2) { 0f } * 255).toInt()).coerceIn(0, 255)
    val alpha = (constantOpacity * 255).toInt().coerceIn(0, 255)
    return (alpha.toLong() shl 24) or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
}

private const val DEFAULT_INK_WIDTH = 2f

private class PositionedWordStripper(
    private val pageIndex: Int,
    private val pageWidth: Float,
    private val pageHeight: Float
) : PDFTextStripper() {
    val words = mutableListOf<PdfTextBox>()
    private var currentText = StringBuilder()
    private var currentBounds: Rect? = null
    private val currentCharacterBounds = mutableListOf<Rect>()
    private var previousPosition: TextPosition? = null

    override fun processTextPosition(text: TextPosition) {
        val value = text.unicode
        if (value.isBlank()) {
            flushWord()
            previousPosition = null
            return
        }

        val bounds = text.toNormalizedRect(pageWidth, pageHeight)
        val previous = previousPosition
        val shouldStartNewWord = previous != null && (
            abs(previous.yDirAdj - text.yDirAdj) > max(previous.heightDir, text.heightDir) * 0.6f ||
                text.xDirAdj - (previous.xDirAdj + previous.widthDirAdj) > max(previous.widthOfSpace, text.widthDirAdj) * 0.7f
            )

        if (shouldStartNewWord) {
            flushWord()
        }

        currentText.append(value)
        currentBounds = currentBounds?.union(bounds) ?: bounds
        currentCharacterBounds += bounds
        previousPosition = text
    }

    override fun endPage(page: PDPage) {
        flushWord()
        previousPosition = null
        super.endPage(page)
    }

    private fun flushWord() {
        val text = currentText.toString()
        val bounds = currentBounds
        if (text.isNotBlank() && bounds != null) {
            words += PdfTextBox(
                pageIndex = pageIndex,
                text = text,
                bounds = bounds,
                characterBounds = currentCharacterBounds.toList()
            )
        }
        currentText = StringBuilder()
        currentBounds = null
        currentCharacterBounds.clear()
    }
}

private fun TextPosition.toNormalizedRect(pageWidth: Float, pageHeight: Float): Rect {
    val left = xDirAdj / pageWidth
    val top = (yDirAdj - heightDir) / pageHeight
    val right = (xDirAdj + widthDirAdj) / pageWidth
    val bottom = yDirAdj / pageHeight

    return Rect(
        left = min(left, right).coerceIn(0f, 1f),
        top = min(top, bottom).coerceIn(0f, 1f),
        right = max(left, right).coerceIn(0f, 1f),
        bottom = max(top, bottom).coerceIn(0f, 1f)
    )
}

private fun Rect.union(other: Rect): Rect {
    return Rect(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom)
    )
}
