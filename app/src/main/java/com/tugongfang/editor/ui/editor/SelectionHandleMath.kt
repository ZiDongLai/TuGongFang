package com.tugongfang.editor.ui.editor

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.abs
import kotlin.math.roundToInt

/** Right-top handle, coordinates relative to the unrotated selection center.
 * Pure geometry shared by the drag handler and regression checks.
 */
internal data class TextCornerTransform(val scaleFactor: Float, val rotationDeltaDegrees: Float)

internal fun topRightTextHandleDrag(
    widthPx: Float,
    heightPx: Float,
    dragXPx: Float,
    dragYPx: Float
): TextCornerTransform {
    val x0 = widthPx.coerceAtLeast(2f) / 2f
    val y0 = -heightPx.coerceAtLeast(2f) / 2f
    val initialDistance = hypot(x0, y0).coerceAtLeast(1f)
    val currentX = x0 + dragXPx
    val currentY = y0 + dragYPx
    val factor = (hypot(currentX, currentY) / initialDistance).coerceIn(.20f, 5f)
    val initialAngle = atan2(y0, x0)
    val currentAngle = atan2(currentY, currentX)
    var delta = (currentAngle - initialAngle) * (180f / PI.toFloat())
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return TextCornerTransform(factor, delta)
}


/**
 * DEV39 FIX1: A single top-right handle changes the text frame's two axes
 * independently, while also allowing rotation. The frame is measured when the
 * gesture starts so text reflow cannot restart or jump the drag calculation.
 *
 * The fixed frame padding is accounted for by the editor's layout; unlike
 * uniform scaling, changing width reflows text without changing font size.
 * Height remains a *minimum* so text is never cut off when reflow needs space.
 */
internal data class TextFreeCornerTransform(
    val widthDp: Int,
    val minimumHeightDp: Int,
    val rotationDeltaDegrees: Float
)

internal fun topRightTextFreeTransformDrag(
    startContentWidthDp: Int,
    startMinimumHeightDp: Int,
    measuredFrameWidthPx: Float,
    measuredFrameHeightPx: Float,
    dragXPx: Float,
    dragYPx: Float,
    density: Float
): TextFreeCornerTransform {
    val dpScale = density.coerceAtLeast(.1f)
    // Independent axis resize: the handle moves relative to the frame's fixed
    // opposite edge, not a uniform radius. No 360dp cap: that was why horizontal
    // dragging appeared to stop almost immediately on real devices.
    val widthDp = (startContentWidthDp + (dragXPx / dpScale).roundToInt())
        .coerceIn(88, 640)
    // An unset minimum (0) means content-driven height, and stays unset for a
    // purely horizontal drag so old auto-height text never freezes by accident.
    val minHeightDp = if (abs(dragYPx) < 1f) startMinimumHeightDp else {
        val measuredContentDp = (measuredFrameHeightPx / dpScale - 40f).coerceAtLeast(0f)
        (measuredContentDp - (dragYPx / dpScale)).roundToInt().coerceIn(0, 720)
    }
    val rotation = topRightTextHandleDrag(
        measuredFrameWidthPx, measuredFrameHeightPx, dragXPx, dragYPx
    ).rotationDeltaDegrees
    return TextFreeCornerTransform(widthDp, minHeightDp, rotation)
}
