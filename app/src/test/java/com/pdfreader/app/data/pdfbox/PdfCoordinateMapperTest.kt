package com.pdfreader.app.data.pdfbox

import androidx.compose.ui.geometry.Offset
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCoordinateMapperTest {
    @Test
    fun unrotatedCropBoxMapsTopLeftToPdfTopLeft() {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle(10f, 20f, 200f, 100f))
            document.addPage(page)

            val point = PdfCoordinateMapper.toPdfPoint(page, Offset.Zero)

            assertEquals(10f, point.x, 0.001f)
            assertEquals(120f, point.y, 0.001f)
        }
    }

    @Test
    fun rightAngleRotationRoundTripsDisplayCoordinates() {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle(10f, 20f, 200f, 100f))
            page.rotation = 90
            document.addPage(page)
            val displayPoint = Offset(0.25f, 0.75f)

            val pdfPoint = PdfCoordinateMapper.toPdfPoint(page, displayPoint)
            val roundTrip = PdfCoordinateMapper.toNormalizedDisplayPoint(page, pdfPoint)

            assertEquals(displayPoint.x, roundTrip.x, 0.001f)
            assertEquals(displayPoint.y, roundTrip.y, 0.001f)
        }
    }

    @Test
    fun strokeWidthUsesDisplayedPageWidthForRotation() {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle(200f, 100f))
            document.addPage(page)

            assertEquals(2f, PdfCoordinateMapper.toPdfStrokeWidth(page, 0.01f), 0.001f)

            page.rotation = 90
            assertEquals(1f, PdfCoordinateMapper.toPdfStrokeWidth(page, 0.01f), 0.001f)
        }
    }
}
