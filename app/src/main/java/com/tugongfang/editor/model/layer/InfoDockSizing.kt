package com.tugongfang.editor.model.layer

import kotlin.math.roundToInt

/**
 * Wider external cards for tall portraits. This is an OUTPUT geometry decision,
 * not a change to the original image or its crop. Called by both preview and export.
 */
fun preferredDockWidth(imageWidth: Int, imageHeight: Int, hasExternalInfo: Boolean): Int {
    if (!hasExternalInfo || imageWidth <= 0 || imageHeight <= 0) return imageWidth
    val portrait = imageHeight.toFloat() / imageWidth.toFloat()
    val multiplier = when {
        portrait >= 1.65f -> 1.18f
        portrait >= 1.25f -> 1.10f
        else -> 1f
    }
    return (imageWidth * multiplier).roundToInt().coerceAtLeast(imageWidth)
}
