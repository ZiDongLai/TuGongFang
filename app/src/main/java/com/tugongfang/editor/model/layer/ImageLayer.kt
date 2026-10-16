package com.tugongfang.editor.model.layer

import java.util.UUID

/**
 * v0.3.0 图片图层数据。
 *
 * - 坐标继续使用相对画布比例，避免绑定到具体屏幕像素。
 * - 裁切使用 0..1 的原图归一化区域，属于非破坏式裁切。
 * - 水平/垂直翻转只保存参数，不改写原图。
 */
data class ImageLayer(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    // v0.9.5 per-photo, non-destructive, -1..1. Old projects default to identity.
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    // v0.9.7 advanced color; -1..1, default neutral for older projects.
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    // DEV10：仅自由拼图使用，保存最近一次“填充/适应”显示模式；旧项目默认适应。
    val freeFitMode: com.tugongfang.editor.model.collage.CollageFitMode =
        com.tugongfang.editor.model.collage.CollageFitMode.Fit,
    /** DEV23: separate, persistent photo-local mosaic and doodle marks. */
    val photoStrokes: List<PhotoStroke> = emptyList()
)
