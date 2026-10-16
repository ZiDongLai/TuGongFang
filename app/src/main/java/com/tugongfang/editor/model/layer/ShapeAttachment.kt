package com.tugongfang.editor.model.layer

import kotlin.math.cos
import kotlin.math.sin

/**
 * 将图形的位置与变换从旧照片位姿同步到新照片位姿。
 * 图形仍以归一化画布坐标存储；变化使用宽高比修正的等距坐标计算，
 * 避免竖屏中旋转后出现方向或位置漂移。
 */
fun ShapeLayer.followImageTransform(previous: ImageLayer, current: ImageLayer): ShapeLayer {
    if (parentImageId != previous.id || current.id != previous.id) return this
    val aspect = attachmentCanvasAspect.coerceIn(0.1f, 10f)
    val sizeFactor = (current.scale / previous.scale.coerceAtLeast(0.001f)).coerceIn(0.02f, 50f)
    val angleRadians = Math.toRadians((current.rotation - previous.rotation).toDouble())
    val c = cos(angleRadians).toFloat()
    val s = sin(angleRadians).toFloat()
    val localX = (offsetXRatio - previous.offsetXRatio) * aspect
    val localY = offsetYRatio - previous.offsetYRatio
    val nextX = (localX * c - localY * s) * sizeFactor
    val nextY = (localX * s + localY * c) * sizeFactor
    val nextRotation = ((rotation + current.rotation - previous.rotation + 180f) % 360f + 360f) % 360f - 180f
    return copy(
        offsetXRatio = current.offsetXRatio + nextX / aspect,
        offsetYRatio = current.offsetYRatio + nextY,
        scale = (scale * sizeFactor).coerceIn(0.08f, 30f),
        rotation = nextRotation
    )
}
