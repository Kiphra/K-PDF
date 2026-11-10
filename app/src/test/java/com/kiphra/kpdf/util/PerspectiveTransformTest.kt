package com.kiphra.kpdf.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PerspectiveTransformTest {

    @Test
    fun testDefaultCornersCalculation() {
        val corners = PerspectiveTransform.getDefaultCorners(1000, 2000, 0.1f)
        assertEquals(100f, corners.topLeft.x, 0.01f)
        assertEquals(200f, corners.topLeft.y, 0.01f)
        assertEquals(900f, corners.topRight.x, 0.01f)
        assertEquals(200f, corners.topRight.y, 0.01f)
        assertEquals(900f, corners.bottomRight.x, 0.01f)
        assertEquals(1800f, corners.bottomRight.y, 0.01f)
        assertEquals(100f, corners.bottomLeft.x, 0.01f)
        assertEquals(1800f, corners.bottomLeft.y, 0.01f)
    }

    @Test
    fun testWarpPerspectiveRectifiesDocument() {
        // Create a 500x500 source bitmap
        val source = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)

        // Define a tilted quadrilateral within the bitmap
        val corners = PerspectiveTransform.Corners(
            topLeft = PointF(50f, 60f),
            topRight = PointF(450f, 40f),
            bottomRight = PointF(460f, 440f),
            bottomLeft = PointF(40f, 460f)
        )

        val warped = PerspectiveTransform.warpPerspective(source, corners)
        assertNotNull(warped)
        assertTrue(warped.width > 350)
        assertTrue(warped.height > 350)
        assertEquals(Bitmap.Config.ARGB_8888, warped.config)
    }
}
