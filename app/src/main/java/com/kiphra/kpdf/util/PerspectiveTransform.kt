package com.kiphra.kpdf.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 4-Point Perspective Document Warping without OpenCV.
 * Transforms a captured quadrilateral document page into a flat, rectified rectangular image
 * using Android's native hardware-accelerated projective Matrix math.
 */
object PerspectiveTransform {

    data class Corners(
        val topLeft: PointF,
        val topRight: PointF,
        val bottomRight: PointF,
        val bottomLeft: PointF
    )

    /**
     * Warps a quadrilateral region defined by [corners] in [source] into a rectangular bitmap.
     * 
     * @param source The original camera or imported image bitmap.
     * @param corners The 4 corners of the detected document quadrilateral in source coordinates.
     * @return Rectified rectangular document bitmap.
     */
    fun warpPerspective(source: Bitmap, corners: Corners): Bitmap {
        // Calculate destination width as maximum of top and bottom edge lengths
        val topWidth = hypot(
            (corners.topRight.x - corners.topLeft.x).toDouble(),
            (corners.topRight.y - corners.topLeft.y).toDouble()
        )
        val bottomWidth = hypot(
            (corners.bottomRight.x - corners.bottomLeft.x).toDouble(),
            (corners.bottomRight.y - corners.bottomLeft.y).toDouble()
        )
        val targetWidth = max(topWidth, bottomWidth).roundToInt().coerceIn(100, 4096)

        // Calculate destination height as maximum of left and right edge lengths
        val leftHeight = hypot(
            (corners.bottomLeft.x - corners.topLeft.x).toDouble(),
            (corners.bottomLeft.y - corners.topLeft.y).toDouble()
        )
        val rightHeight = hypot(
            (corners.bottomRight.x - corners.topRight.x).toDouble(),
            (corners.bottomRight.y - corners.topRight.y).toDouble()
        )
        val targetHeight = max(leftHeight, rightHeight).roundToInt().coerceIn(100, 4096)

        val srcPoints = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Estimates default document corners with an inset margin from the bitmap bounds.
     */
    fun getDefaultCorners(bitmapWidth: Int, bitmapHeight: Int, insetFraction: Float = 0.08f): Corners {
        val insetX = bitmapWidth * insetFraction
        val insetY = bitmapHeight * insetFraction

        return Corners(
            topLeft = PointF(insetX, insetY),
            topRight = PointF(bitmapWidth - insetX, insetY),
            bottomRight = PointF(bitmapWidth - insetX, bitmapHeight - insetY),
            bottomLeft = PointF(insetX, bitmapHeight - insetY)
        )
    }
}
