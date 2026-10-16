package com.tugongfang.editor.model.layer

/**
 * v0.9.7 extra color controls: temperature, tint, highlight and shadow.
 * Parameters range [-1, 1]; 0 for all parameters is a pixel-exact no-op.
 * Pure Kotlin pixel math, used by both previews and exported image bitmaps.
 */
object ExtendedToneCurve {
    fun isNeutral(temperature: Float, tint: Float, highlights: Float, shadows: Float): Boolean =
        temperature == 0f && tint == 0f && highlights == 0f && shadows == 0f

    fun applyPixel(argb: Int, temperature: Float, tint: Float, highlights: Float, shadows: Float): Int {
        if (isNeutral(temperature, tint, highlights, shadows)) return argb
        val alpha = argb ushr 24
        if (alpha == 0) return argb // preserve fully transparent RGB bytes
        val r = (argb ushr 16) and 255
        val g = (argb ushr 8) and 255
        val b = argb and 255
        val light = (0.2126f*r + 0.7152f*g + 0.0722f*b)/255f
        // Local tonal weighting: shadows affect dark values more, highlights bright values more.
        val shadowWeight = (1f-light)*(1f-light)
        val highlightWeight = light*light
        val exposure = 72f*(shadows.coerceIn(-1f,1f)*shadowWeight +
            highlights.coerceIn(-1f,1f)*highlightWeight)
        val warmth = temperature.coerceIn(-1f,1f)
        val magenta = tint.coerceIn(-1f,1f)
        fun channel(x: Float): Int = x.toInt().coerceIn(0,255)
        val nr = channel(r + exposure + 27f*warmth + 19f*magenta + .5f)
        val ng = channel(g + exposure +  4f*warmth - 30f*magenta + .5f)
        val nb = channel(b + exposure - 32f*warmth + 19f*magenta + .5f)
        return (alpha shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
}
