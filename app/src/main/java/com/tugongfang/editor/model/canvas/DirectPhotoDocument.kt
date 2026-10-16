package com.tugongfang.editor.model.canvas

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** DEV7: the logical single-photo page follows the actual cropped/rotated photograph.
 * Does not change crop, resolution or stored coordinates. Independent of Android/Compose.
 */
fun directPhotoDocumentAspect(
    sourceWidth: Int, sourceHeight: Int,
    cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float,
    rotationDegrees: Float
): Float? {
    if (sourceWidth <= 0 || sourceHeight <= 0) return null
    val w = sourceWidth * (cropRight - cropLeft).coerceIn(.01f, 1f)
    val h = sourceHeight * (cropBottom - cropTop).coerceIn(.01f, 1f)
    val r = rotationDegrees.toDouble() * (Math.PI / 180.0)
    val c = abs(cos(r)).toFloat()
    val s = abs(sin(r)).toFloat()
    val boundWidth = w*c + h*s
    val boundHeight = w*s + h*c
    return (boundWidth / boundHeight).takeIf { it.isFinite() && it > 0f }
}
