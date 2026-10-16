package com.tugongfang.editor.editor.image

import com.tugongfang.editor.model.layer.PhotoPoint

/** Bounds of an offscreen brush-compositing layer in ORIGINAL photo pixels.
 * Clipped to the photo so a short eraser mark never allocates a full-photo layer.
 * Round stroke caps extend half the diameter beyond all recorded points.
 */
internal data class PhotoStrokeLayerBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal fun photoStrokeLayerBounds(
    points: List<PhotoPoint>, photoWidth: Int, photoHeight: Int,
    diameterPx: Float, aaPaddingPx: Float = 2f
): PhotoStrokeLayerBounds? {
    if (points.isEmpty() || photoWidth <= 0 || photoHeight <= 0 ||
        !diameterPx.isFinite() || !aaPaddingPx.isFinite()) return null
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (pt in points) {
        if (!pt.x.isFinite() || !pt.y.isFinite()) return null
        val x = pt.x.coerceIn(0f, 1f) * photoWidth
        val y = pt.y.coerceIn(0f, 1f) * photoHeight
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x)
        maxY = maxOf(maxY, y)
    }
    val extra = diameterPx.coerceAtLeast(1f) / 2f + aaPaddingPx.coerceAtLeast(0f)
    val left = (minX - extra).coerceIn(0f, photoWidth.toFloat())
    val top = (minY - extra).coerceIn(0f, photoHeight.toFloat())
    val right = (maxX + extra).coerceIn(0f, photoWidth.toFloat())
    val bottom = (maxY + extra).coerceIn(0f, photoHeight.toFloat())
    return if (right > left && bottom > top) {
        PhotoStrokeLayerBounds(left, top, right, bottom)
    } else null
}
