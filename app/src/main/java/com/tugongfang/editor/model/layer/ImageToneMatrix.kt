package com.tugongfang.editor.model.layer

/**
 * Non-destructive photo adjustments shared by Compose preview and Android Canvas export.
 * All controls are normalized to [-1, 1]. Zero is a mathematically exact identity.
 * Matrix rows operate on 0..255 un-premultiplied RGB and leave alpha unchanged.
 */
object ImageToneMatrix {
    fun make(brightness: Float, contrast: Float, saturation: Float): FloatArray {
        val b = brightness.coerceIn(-1f, 1f) * 82f
        val c = 1f + contrast.coerceIn(-1f, 1f) * .75f
        val s = 1f + saturation.coerceIn(-1f, 1f)
        val neutral = 1f - s
        val red = .2126f * neutral
        val green = .7152f * neutral
        val blue = .0722f * neutral
        val offset = 127.5f * (1f - c) + b
        return floatArrayOf(
            c*(red+s), c*green,   c*blue,      0f, offset,
            c*red,     c*(green+s), c*blue,      0f, offset,
            c*red,     c*green,   c*(blue+s),    0f, offset,
            0f,0f,0f,1f,0f
        )
    }

    fun isNeutral(brightness: Float, contrast: Float, saturation: Float): Boolean =
        brightness == 0f && contrast == 0f && saturation == 0f
}
