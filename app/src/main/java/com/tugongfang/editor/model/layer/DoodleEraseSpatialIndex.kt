package com.tugongfang.editor.model.layer

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Spatial lookup for the gesture eraser path. Build once per gesture, share across
 * every doodle stroke instead of scanning all eraser segments for every pixel
 * sample. maxRadiusPx must be >= any stroke hit radius that uses this index.
 */
class DoodleEraseSpatialIndex(
    private val path: List<DoodleErasePoint>,
    maxRadiusPx: Float
) {
    private val safeRadius = maxRadiusPx.coerceAtLeast(1f)
    private val cell = max(24f, safeRadius)
    private val buckets = HashMap<Long, MutableList<Int>>()
    private val veryLongSegments = mutableListOf<Int>()

    init {
        for (i in 0 until path.lastIndex) {
            val a = path[i]
            val b = path[i + 1]
            val x0 = floor((minOf(a.x, b.x) - safeRadius) / cell).toInt()
            val x1 = floor((maxOf(a.x, b.x) + safeRadius) / cell).toInt()
            val y0 = floor((minOf(a.y, b.y) - safeRadius) / cell).toInt()
            val y1 = floor((maxOf(a.y, b.y) + safeRadius) / cell).toInt()
            val count = (x1.toLong() - x0 + 1) * (y1.toLong() - y0 + 1)
            if (count > 128L) {
                veryLongSegments.add(i)
            } else {
                for (x in x0..x1) for (y in y0..y1) {
                    buckets.getOrPut(key(x, y)) { ArrayList() }.add(i)
                }
            }
        }
    }

    fun touches(point: DoodleErasePoint, hitRadiusPx: Float): Boolean {
        val rSq = hitRadiusPx * hitRadiusPx
        if (path.isEmpty()) return false
        if (path.size == 1) return sq(point, path.first()) <= rSq
        fun near(i: Int): Boolean {
            val a = path[i]
            val b = path[i + 1]
            val vx = b.x - a.x
            val vy = b.y - a.y
            val lenSq = vx * vx + vy * vy
            val t = if (lenSq < .0001f) 0f else
                (((point.x - a.x) * vx + (point.y - a.y) * vy) / lenSq).coerceIn(0f, 1f)
            val dx = point.x - (a.x + vx * t)
            val dy = point.y - (a.y + vy * t)
            return dx * dx + dy * dy <= rSq
        }
        val key = key(floor(point.x / cell).toInt(), floor(point.y / cell).toInt())
        for (i in buckets[key].orEmpty()) if (near(i)) return true
        for (i in veryLongSegments) if (near(i)) return true
        return false
    }

    private fun sq(a: DoodleErasePoint, b: DoodleErasePoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }
    private fun key(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
}
