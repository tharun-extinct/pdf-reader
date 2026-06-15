package com.pdfreader.app.data.pdfium

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Size
import com.pdfreader.app.domain.repository.PdfEngine
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

/**
 * Data-level implementation of the PdfEngine using PDFium-Android.
 * Operates at the C++ level underneath, minimizing JVM garbage collection overhead
 * for reading/rendering files.
 */
class PdfiumEngine(context: Context) : PdfEngine {
    private val pdfiumCore = PdfiumCore(context)
    private var pdfDocument: PdfDocument? = null

    override fun openDocument(pfd: ParcelFileDescriptor) {
        // Closes previous document if exists
        closeDocument()
        pdfDocument = pdfiumCore.newDocument(pfd)
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

    override fun closeDocument() {
        pdfDocument?.let { 
            pdfiumCore.closeDocument(it) 
        }
        pdfDocument = null
    }
}
