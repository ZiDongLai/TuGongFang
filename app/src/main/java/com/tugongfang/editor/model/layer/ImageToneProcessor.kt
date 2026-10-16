package com.tugongfang.editor.model.layer

import android.graphics.Bitmap

/** Applies advanced pixel tone changes without modifying original image content.
 * No-op returns the original bitmap. Callers own/recycle only *new* output.
 */
object ImageToneProcessor {
    fun applyAdvanced(source: Bitmap, layer: ImageLayer): Bitmap = applyAdvanced(
        source, layer.temperature, layer.tint, layer.highlights, layer.shadows)

    fun applyAdvanced(source: Bitmap, temperature: Float, tint: Float,
                      highlights: Float, shadows: Float): Bitmap {
        if (ExtendedToneCurve.isNeutral(temperature, tint, highlights, shadows)) return source
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val rowPixels = IntArray(source.width.coerceAtLeast(1))
        for (y in 0 until source.height) {
            // A single scanline avoids extra full-size IntArray allocation on high-res export.
            source.getPixels(rowPixels, 0, source.width, 0, y, source.width, 1)
            for (x in rowPixels.indices) {
                rowPixels[x] = ExtendedToneCurve.applyPixel(
                    rowPixels[x], temperature, tint, highlights, shadows)
            }
            output.setPixels(rowPixels, 0, source.width, 0, y, source.width, 1)
        }
        return output
    }
}
