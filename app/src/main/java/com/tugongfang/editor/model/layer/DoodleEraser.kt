package com.tugongfang.editor.model.layer

import java.util.UUID
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Canvas-local position, in pixels. No Android or Compose dependency. */
data class DoodleErasePoint(val x: Float, val y: Float)

/**
 * Non-destructive partial eraser: returns null when untouched, empty list when fully erased,
 * or a list of retained path fragments when partially erased. Fragments preserve the original
 * object's center, transform, brush, opacity, parent photo and session ID, so editor preview,
 * image anchoring and JPG rendering all use the same local path coordinates.
 *
 * One path is split into multiple vector paths; the source bitmap is never changed.
 */
fun eraseDoodlePath(
    layer: ShapeLayer,
    eraserPath: List<DoodleErasePoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    eraserRadiusPx: Float,
    density: Float = 1f,
    spatialIndex: DoodleEraseSpatialIndex? = null
): List<ShapeLayer>? {
    if (layer.doodleSessionId == null || layer.type != ShapeType.Freehand ||
        layer.points.isEmpty() || eraserPath.isEmpty() ||
        canvasWidth <= 0f || canvasHeight <= 0f || layer.locked || !layer.visible) return null

    val width = canvasWidth * layer.widthRatio * layer.scale
    val height = canvasHeight * layer.heightRatio * layer.scale
    val midX = canvasWidth * (.5f + layer.offsetXRatio)
    val midY = canvasHeight * (.5f + layer.offsetYRatio)
    val radians = layer.rotation * (PI / 180.0)
    val cosA = cos(radians).toFloat()
    val sinA = sin(radians).toFloat()
    val hitRadius = max(1f, eraserRadiusPx) + layer.strokeWidth * density.coerceAtLeast(.1f) / 2f
    val hitRadiusSq = hitRadius * hitRadius

    fun world(p: ShapePoint): DoodleErasePoint {
        val x = (p.x - .5f) * width
        val y = (p.y - .5f) * height
        return DoodleErasePoint(midX + x * cosA - y * sinA, midY + x * sinA + y * cosA)
    }

    // Use an indexed eraser path. Build outside the per-layer loop when possible;
    // still supports direct calls without a shared index.
    val lookup = spatialIndex ?: DoodleEraseSpatialIndex(eraserPath, hitRadius)
    fun hit(p: DoodleErasePoint): Boolean = lookup.touches(p, hitRadius)

    // Reject far-away doodles before making hundreds of intermediate samples.
    // Rotated bounding box is conservative (may return extra candidates), never
    // rejects an actual hit including half stroke width.
    val halfW = kotlin.math.abs(width * cosA) / 2f + kotlin.math.abs(height * sinA) / 2f + hitRadius
    val halfH = kotlin.math.abs(width * sinA) / 2f + kotlin.math.abs(height * cosA) / 2f + hitRadius
    if (eraserPath.maxOf { it.x } < midX - halfW || eraserPath.minOf { it.x } > midX + halfW ||
        eraserPath.maxOf { it.y } < midY - halfH || eraserPath.minOf { it.y } > midY + halfH) return null

    val chunks = mutableListOf<MutableList<ShapePoint>>()
    var run = mutableListOf<ShapePoint>()
    var touched = false
    val samplesPerPixel = (max(1.5f, min(8f, hitRadius * .4f)))

    fun sample(p: ShapePoint) {
        if (hit(world(p))) {
            touched = true
            if (run.isNotEmpty()) { chunks += run; run = mutableListOf() }
        } else if (run.lastOrNull() != p) {
            run.add(p)
        }
    }

    if (layer.points.size == 1) {
        sample(layer.points[0])
    } else {
        layer.points.zipWithNext().forEachIndexed { i, (a, b) ->
            val wa = world(a)
            val wb = world(b)
            val distance = sqrt(distanceSquared(wa, wb).toDouble()).toFloat()
            val steps = ceil(distance / samplesPerPixel).toInt().coerceIn(1, 1000)
            for (s in (if (i == 0) 0 else 1)..steps) {
                val t = s.toFloat() / steps
                sample(ShapePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
            }
        }
    }
    if (run.isNotEmpty()) chunks += run
    if (!touched) return null
    return chunks.mapIndexed { i, points ->
        layer.copy(id = if (i == 0) layer.id else UUID.randomUUID().toString(), points = points)
    }
}

private fun distanceSquared(a: DoodleErasePoint, b: DoodleErasePoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}
