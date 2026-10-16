package com.tugongfang.editor.model.layer

/** DEV23: photo-local, non-destructive strokes in original (uncropped) bitmap coordinates. */
enum class PhotoStrokeKind { Doodle, Mosaic }

data class PhotoPoint(val x: Float, val y: Float)

data class PhotoStroke(
    val kind: PhotoStrokeKind,
    val points: List<PhotoPoint>,
    /** Diameter as a fraction of the shorter photo edge. */
    val sizeRatio: Float = .055f,
    /** Mosaic square size as a fraction of the shorter photo edge. */
    val mosaicBlockRatio: Float = .025f,
    val colorArgb: Int = 0xFF3A78FF.toInt(),
    /** DEV26: remove only previous marks of the same kind, never modify original pixels. */
    val isEraser: Boolean = false
)

/** Clamp persisted inputs; never let an invalid stroke create excessive bitmap allocations. */
fun PhotoStroke.sanitized() = copy(
    points = points.take(4096).map { PhotoPoint(it.x.coerceIn(0f,1f),it.y.coerceIn(0f,1f)) },
    sizeRatio = sizeRatio.coerceIn(.005f,.35f),
    mosaicBlockRatio = mosaicBlockRatio.coerceIn(.004f,.12f)
)
