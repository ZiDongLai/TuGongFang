package com.tugongfang.editor.model.layer

/** Collects photo-local pointer samples at visible-pixel spacing instead of recording
 * every motion event. All coordinates remain normalized to the ORIGINAL photograph.
 * Keeps the first point and pen-up endpoint, including fast strokes and edge releases.
 */
class PhotoStrokePointSampler(
    private val imageWidthPx: Float,
    private val imageHeightPx: Float,
    initialSpacingPx: Float = 1.4f,
    private val maxSamples: Int = 4096
) {
    private val samples = ArrayList<PhotoPoint>()
    private var spacingPx = initialSpacingPx.coerceAtLeast(0.5f)

    init {
        require(imageWidthPx > 0f && imageHeightPx > 0f)
        require(maxSamples >= 4)
    }

    val size: Int get() = samples.size

    fun add(point: PhotoPoint, penUp: Boolean = false): Boolean {
        if (!point.x.isFinite() || !point.y.isFinite()) return false
        val p = PhotoPoint(point.x.coerceIn(0f, 1f), point.y.coerceIn(0f, 1f))
        if (samples.isEmpty()) {
            samples.add(p)
            return true
        }
        val prev = samples.last()
        val dx = (p.x - prev.x) * imageWidthPx
        val dy = (p.y - prev.y) * imageHeightPx
        val movedSq = dx * dx + dy * dy
        // Even at pen-up, do not store coincident samples that create zero-length segments.
        if (movedSq < if (penUp) 0.25f * 0.25f else spacingPx * spacingPx) return false
        if (penUp && movedSq < spacingPx * spacingPx && samples.size > 1) {
            // Preserve the real endpoint, without making a jittery tiny segment.
            samples[samples.lastIndex] = p
        } else {
            samples.add(p)
        }
        if (samples.size > maxSamples) {
            val previous = samples.toList()
            samples.clear()
            samples.add(previous.first())
            for (i in 2 until previous.lastIndex step 2) samples.add(previous[i])
            if (previous.last() != samples.last()) samples.add(previous.last())
            spacingPx *= 1.7f
        }
        return true
    }

    fun snapshot(): List<PhotoPoint> = samples.toList()
}
