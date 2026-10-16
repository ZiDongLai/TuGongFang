package com.tugongfang.editor.model.collage


/**
 * v0.8.3: 轻量稳定手势，不再做位置/边缘/45°等磁性吸附。
 *
 * 原始触摸增量连续累积，屏幕上只对旋转/缩放做少量平滑处理；
 * 移动像素原样跟随，不设操作范围或吸附释放阈值，避免“被吸住”的感觉。
 */
data class CollageGestureResult(val slot: CollageSlot)

class CollageGestureSession(
    initial: CollageSlot,
    slotWidthPx: Float,
    slotHeightPx: Float
) {
    private val width = slotWidthPx.coerceAtLeast(1f)
    private val height = slotHeightPx.coerceAtLeast(1f)
    private var panX = initial.panX
    private var panY = initial.panY
    private var zoom = initial.zoom
    private var rotation = normalizedCollageRotation(initial.rotation)
    private var displayedZoom = initial.zoom
    private var displayedRotation = normalizedCollageRotation(initial.rotation)
    private var current = initial

    fun step(dxPx: Float, dyPx: Float, zoomDelta: Float, rotationDelta: Float): CollageGestureResult {
        if (dxPx.isFinite()) panX = (panX + dxPx / width).coerceIn(-2f, 2f)
        if (dyPx.isFinite()) panY = (panY + dyPx / height).coerceIn(-2f, 2f)
        if (zoomDelta.isFinite() && zoomDelta > 0f) zoom = (zoom * zoomDelta).coerceIn(.35f, 6f)
        if (rotationDelta.isFinite()) rotation = normalizedCollageRotation(rotation + rotationDelta)

        // 轻微时间平滑：仍可到达任意比例与角度，绝不强行拉到某个标准位置。
        displayedZoom += (zoom - displayedZoom) * .82f
        val angleDelta = normalizedCollageRotation(rotation - displayedRotation)
        displayedRotation = normalizedCollageRotation(displayedRotation + angleDelta * .82f)

        current = current.copy(
            panX = panX,
            panY = panY,
            zoom = displayedZoom.coerceIn(.35f, 6f),
            rotation = displayedRotation
        )
        return CollageGestureResult(current)
    }

    /** 抬手后让终点精确等于手势累计值，避免插值造成的微小残余误差。 */
    fun finish(): CollageSlot = current.copy(
        panX = panX,
        panY = panY,
        zoom = zoom,
        rotation = rotation
    )
}
