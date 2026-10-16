package com.tugongfang.editor.model.canvas

/**
 * 固定逻辑文档与屏幕视口的适配工具。
 * 工具面板/键盘改变的是视口，不应改变照片在文档里的归一化位置和比例。
 * 纯 Kotlin，可独立测试。
 */
internal data class CanvasDisplaySize(
    val widthPx: Float,
    val heightPx: Float,
    val scale: Float
)

internal object CanvasViewportScale {
    /** 等比 contain：保持固定宽高比，不允许因工具面板展开把内容沿单轴挤压。 */
    fun fit(
        referenceWidth: Float,
        referenceHeight: Float,
        currentWidth: Float,
        currentHeight: Float
    ): CanvasDisplaySize {
        if (referenceWidth <= 0f || referenceHeight <= 0f || currentWidth <= 0f || currentHeight <= 0f) {
            return CanvasDisplaySize(currentWidth.coerceAtLeast(1f), currentHeight.coerceAtLeast(1f), 1f)
        }
        // 基准是进入编辑器时的画布显示尺寸：屏幕更大也不重设文档尺寸。
        val scale = minOf(1f, currentWidth / referenceWidth, currentHeight / referenceHeight)
            .coerceAtLeast(.01f)
        return CanvasDisplaySize(referenceWidth * scale, referenceHeight * scale, scale)
    }

    /** 保留旧接口：文字、图形和图片的视觉尺寸使用同一缩放比例。 */
    fun uniform(referenceWidth: Float, referenceHeight: Float, currentWidth: Float, currentHeight: Float): Float {
        if (referenceWidth <= 0f || referenceHeight <= 0f || currentWidth <= 0f || currentHeight <= 0f) return 1f
        return minOf(currentWidth / referenceWidth, currentHeight / referenceHeight).coerceIn(.10f, 4f)
    }
}
