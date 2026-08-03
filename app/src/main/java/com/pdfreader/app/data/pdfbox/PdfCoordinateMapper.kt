package com.pdfreader.app.data.pdfbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tom_roush.pdfbox.pdmodel.PDPage

/**
 * Single coordinate contract shared by PDFBox persistence and embedded-annotation loading.
 * UI coordinates are normalized to the displayed page with a top-left origin; PDF coordinates
 * use the CropBox and a bottom-left origin.
 */
object PdfCoordinateMapper {
    /** Maps a width normalized against the displayed page width into PDF points. */
    fun toPdfStrokeWidth(page: PDPage, normalizedWidth: Float): Float {
        val box = page.cropBox
        val displayedWidth = when (page.normalizedRotation()) {
            90, 270 -> box.height
            else -> box.width
        }
        return normalizedWidth.coerceAtLeast(0f) * displayedWidth
    }

    fun toPdfPoint(page: PDPage, point: Offset): Offset {
        val box = page.cropBox
        val x = point.x.coerceIn(0f, 1f)
        val y = point.y.coerceIn(0f, 1f)
        val rotation = page.normalizedRotation()
        val (relativeX, relativeY) = when (rotation) {
            90 -> y * box.width to x * box.height
            180 -> (1f - x) * box.width to y * box.height
            270 -> (1f - y) * box.width to (1f - x) * box.height
            else -> x * box.width to (1f - y) * box.height
        }
        return Offset(box.lowerLeftX + relativeX, box.lowerLeftY + relativeY)
    }

    fun toNormalizedDisplayPoint(page: PDPage, point: Offset): Offset {
        val box = page.cropBox
        val x = ((point.x - box.lowerLeftX) / box.width).coerceIn(0f, 1f)
        val y = ((point.y - box.lowerLeftY) / box.height).coerceIn(0f, 1f)
        return when (page.normalizedRotation()) {
            90 -> Offset(y, x)
            180 -> Offset(1f - x, y)
            270 -> Offset(1f - y, 1f - x)
            else -> Offset(x, 1f - y)
        }
    }

    fun toNormalizedDisplayRect(page: PDPage, quadPoints: FloatArray): Rect? {
        if (quadPoints.size < 8) return null
        val points = quadPoints.asList()
            .chunked(2)
            .filter { it.size == 2 }
            .map { toNormalizedDisplayPoint(page, Offset(it[0], it[1])) }
        if (points.isEmpty()) return null
        return Rect(
            left = points.minOf { it.x },
            top = points.minOf { it.y },
            right = points.maxOf { it.x },
            bottom = points.maxOf { it.y }
        )
    }

    private fun PDPage.normalizedRotation(): Int = ((rotation % 360) + 360) % 360
}
