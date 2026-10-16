package com.tugongfang.editor.model.collage

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 图片格位与导出共用的尺寸计算；单位由调用方决定（像素）。 */
data class CollageImageSize(val width: Float, val height: Float)

fun normalizedCollageRotation(value: Float): Float {
    var angle = value % 360f
    if (angle > 180f) angle -= 360f
    if (angle < -180f) angle += 360f
    return angle
}

/**
 * Fill：保证旋转后的图片覆盖格位的全部四角。
 * Fit：保证旋转后的整张图片全部可见，可能留白。
 * 两者都允许用户后续自由缩放与平移。
 */
fun collageImageSize(
    imageWidth: Float,
    imageHeight: Float,
    slotWidth: Float,
    slotHeight: Float,
    slot: CollageSlot
): CollageImageSize {
    val iw = imageWidth.coerceAtLeast(1f)
    val ih = imageHeight.coerceAtLeast(1f)
    val tw = slotWidth.coerceAtLeast(1f)
    val th = slotHeight.coerceAtLeast(1f)
    val angle = Math.toRadians(slot.rotation.toDouble())
    val c = abs(cos(angle)).toFloat()
    val s = abs(sin(angle)).toFloat()
    val baseScale = when (slot.fitMode) {
        CollageFitMode.Fill -> max((c * tw + s * th) / iw, (s * tw + c * th) / ih)
        CollageFitMode.Fit -> min(tw / (c * iw + s * ih), th / (s * iw + c * ih))
    }
    val factor = baseScale * slot.zoom.coerceIn(.35f, 6f)
    return CollageImageSize(iw * factor, ih * factor)
}

/**
 * 自由拼图：给“填充/适应”按钮提供可复现的目标缩放倍数。
 * 参数都是相对画布的尺寸，和编辑层/导出器现有的 90% × 83% 基础放置范围一致。
 * Fill 为旋转后覆盖整幅画布，Fit 为完整显示在画布的 90% 内。
 * 不更改照片真实裁切范围，也不改写底层原图。
 */
fun freeCollageFitScale(
    imageAspect: Float,
    canvasAspect: Float,
    rotationDegrees: Float,
    mode: CollageFitMode
): Float {
    val ratio = imageAspect.coerceIn(.01f,100f)
    val pageRatio = canvasAspect.coerceIn(.01f,100f)
    val baseWidth = min(.90f, .83f*ratio/pageRatio)
    val baseHeight = baseWidth*pageRatio/ratio
    // 模板拼图使用同一套旋转几何函数，确保0/90/任意角度都遵循相同规则。
    val goalWidth = if (mode == CollageFitMode.Fit) .90f else 1f
    val goalHeight = if (mode == CollageFitMode.Fit) .90f/pageRatio else 1f/pageRatio
    val dims = collageImageSize(
        // collageImageSize uses pixel-sized inputs (>=1); convert normalized units
        // to a safe virtual pixel scale before calling it.
        imageWidth = baseWidth*10000f,
        imageHeight = baseHeight*10000f,
        slotWidth = goalWidth*10000f,
        slotHeight = goalHeight*10000f,
        slot = CollageSlot(rotation=rotationDegrees,fitMode=mode)
    )
    return (dims.width/(baseWidth*10000f)).coerceIn(.25f,24f)
}
