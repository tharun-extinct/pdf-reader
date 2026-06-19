package com.pdfreader.app.data.pdfium

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Size
import androidx.compose.ui.geometry.Rect
import com.pdfreader.app.domain.repository.PdfEngine
import com.pdfreader.app.presentation.mvi.PdfTextBox
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
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
class PdfiumEngine(context: Context) : PdfEngine {
    private val pdfiumCore = PdfiumCore(context)
    private var pdfDocument: PdfDocument? = null
    private var textDocument: PDDocument? = null
    private val textBoxCache = mutableMapOf<Int, List<PdfTextBox>>()

    init {
        PDFBoxResourceLoader.init(context)
    }

    override fun openDocument(pfd: ParcelFileDescriptor, pdfBytes: ByteArray) {
        // Closes previous document if exists
        closeDocument()
        pdfDocument = pdfiumCore.newDocument(pfd)
        textDocument = PDDocument.load(ByteArrayInputStream(pdfBytes))
    }

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
            0, 0, width, height, false
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

    override fun closeDocument() {
        pdfDocument?.let { 
            pdfiumCore.closeDocument(it) 
        }
        pdfDocument = null
        textDocument?.close()
        textDocument = null
        textBoxCache.clear()
    }
}

private class PositionedWordStripper(
    private val pageIndex: Int,
    private val pageWidth: Float,
    private val pageHeight: Float
) : PDFTextStripper() {
    val words = mutableListOf<PdfTextBox>()
    private var currentText = StringBuilder()
    private var currentBounds: Rect? = null
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
            words += PdfTextBox(pageIndex = pageIndex, text = text, bounds = bounds)
        }
        currentText = StringBuilder()
        currentBounds = null
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
