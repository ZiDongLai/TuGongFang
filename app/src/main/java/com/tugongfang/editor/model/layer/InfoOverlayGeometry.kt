package com.tugongfang.editor.model.layer

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI

/**
 * Non-destructive geometry shared by the editor preview and the bitmap exporter.
 * Coordinates are relative to the actual image/collage rectangle (not the tool viewport).
 * Limits the *rotated* bounding box so it cannot extend beyond that rectangle.
 */
data class ResolvedInfoOverlayGeometry(
    val width: Float,
    val height: Float,
    val centerX: Float,
    val centerY: Float
)

fun resolveInfoOverlayGeometry(
    layer: InfoPanelLayer,
    contentWidth: Float,
    contentHeight: Float,
    measuredMinHeight: Float = 0f
): ResolvedInfoOverlayGeometry {
    val cw = contentWidth.coerceAtLeast(1f)
    val ch = contentHeight.coerceAtLeast(1f)
    val margin = .015f
    var w = (cw * layer.widthRatio.coerceIn(.10f, 1f)).coerceAtMost(cw * (1f-2f*margin))
    var h = maxOf(ch * layer.heightRatio.coerceIn(.08f, 1f), measuredMinHeight)
        .coerceAtMost(ch * (1f-2f*margin))
    val angle = layer.rotation * PI / 180.0
    val cs = abs(cos(angle)).toFloat()
    val sn = abs(sin(angle)).toFloat()
    val bboxW = cs*w + sn*h
    val bboxH = sn*w + cs*h
    val fit = min(1f, min(cw*(1f-2f*margin)/bboxW.coerceAtLeast(1f),
        ch*(1f-2f*margin)/bboxH.coerceAtLeast(1f)))
    w *= fit
    h *= fit
    val halfW = (cs*w+sn*h)/2f
    val halfH = (sn*w+cs*h)/2f
    val cx = (layer.offsetXRatio*cw).coerceIn(-cw/2f+halfW, cw/2f-halfW)
    val cy = (layer.offsetYRatio*ch).coerceIn(-ch/2f+halfH, ch/2f-halfH)
    return ResolvedInfoOverlayGeometry(w,h,cx,cy)
}
