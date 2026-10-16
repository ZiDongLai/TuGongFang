package com.tugongfang.editor.editor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.tugongfang.editor.model.layer.PhotoStroke
import com.tugongfang.editor.model.layer.PhotoStrokeKind
import com.tugongfang.editor.model.layer.sanitized
import kotlin.math.roundToInt

/** Applies brush strokes in original-photo coordinates BEFORE crop, flip and rotation.
 * Both Compose preview and export call this exact renderer. Never mutates [source]. */
object PhotoEffectsRenderer {
    fun apply(source: Bitmap, strokes: List<PhotoStroke>): Bitmap {
        if (strokes.isEmpty()) return source
        // Preserve the exact DEV25 fast path for legacy projects without erasing.
        if (strokes.any { it.isEraser }) return applyWithErasing(source, strokes)
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("无法创建照片局部编辑图层")
        val canvas = Canvas(output)
        val minEdge = minOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        // A drag may contain hundreds of strokes with different pixel settings. Keeping
        // every reduced bitmap until export finishes can exhaust memory on large photos.
        // Access-order LRU retains only the three most recently used mosaic sizes.
        val pixelCache = LinkedHashMap<Int, Bitmap>(4, .75f, true)
        try {
            for (raw in strokes.take(1200)) {
                val stroke = raw.sanitized()
                if (stroke.points.isEmpty()) continue
                val path = Path().apply {
                    stroke.points.forEachIndexed { index, p ->
                        val x = p.x * source.width
                        val y = p.y * source.height
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                val width = (stroke.sizeRatio * minEdge).coerceAtLeast(1f)
                val coverage = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = width
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    style = Paint.Style.STROKE
                    color = stroke.colorArgb
                }
                when (stroke.kind) {
                    PhotoStrokeKind.Doodle -> {
                        if (stroke.points.size == 1) {
                            val pt = stroke.points[0]
                            coverage.style = Paint.Style.FILL
                            canvas.drawCircle(pt.x * source.width,pt.y * source.height,width/2f,coverage)
                        } else canvas.drawPath(path, coverage)
                    }
                    PhotoStrokeKind.Mosaic -> {
                        // The mask is a thick round-capped path; mosaics use nearest-neighbour
                        // enlarged SOURCE pixels so prior doodles never contaminate pixel blocks.
                        val mask = Path()
                        val first = stroke.points.first()
                        if (stroke.points.size == 1) {
                            mask.addCircle(first.x*source.width,first.y*source.height,width/2f,Path.Direction.CW)
                        } else {
                            coverage.getFillPath(path,mask)
                        }
                        val block = (stroke.mosaicBlockRatio*minEdge).roundToInt().coerceAtLeast(2)
                        val small = pixelCache[block] ?: run {
                            if (pixelCache.size >= 3) {
                                val iterator = pixelCache.entries.iterator()
                                if (iterator.hasNext()) {
                                    val eldest = iterator.next().value
                                    iterator.remove()
                                    if (!eldest.isRecycled) eldest.recycle()
                                }
                            }
                            Bitmap.createScaledBitmap(source,
                                (source.width / block).coerceAtLeast(1),
                                (source.height / block).coerceAtLeast(1), false
                            ).also { pixelCache[block] = it }
                        }
                        canvas.save()
                        canvas.clipPath(mask)
                        canvas.drawBitmap(small,Rect(0,0,small.width,small.height),
                            RectF(0f,0f,source.width.toFloat(),source.height.toFloat()),
                            Paint().apply { isFilterBitmap = false })
                        canvas.restore()
                    }
                }
            }
            return output
        } catch (e: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw e
        } finally {
            pixelCache.values.forEach { if (it !== source && !it.isRecycled) it.recycle() }
        }
    }

    /**
     * Render erasable photo marks newest-first so every eraser applies only to older
     * strokes of the SAME tool. A bounded offscreen layer produces antialiased
     * coverage and eraser edges without rasterizing a full-photo mask per stroke.
     * The layer is composited DST_OVER, preserving photo, doodle and mosaic order.
     * No-eraser projects retain the original DEV25 rendering implementation.
     */
    private fun applyWithErasing(source: Bitmap, strokes: List<PhotoStroke>): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val minEdge = minOf(source.width, source.height).toFloat().coerceAtLeast(1f)
        val doodleErased = Path()
        val mosaicErased = Path()
        var hasDoodleErasures = false
        var hasMosaicErasures = false
        val behind = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
        val layerRestorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = behind }
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        val keepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        val pixelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }
        val pixelCache = LinkedHashMap<Int, Bitmap>(4, .75f, true)
        try {
            for (raw in strokes.take(1200).asReversed()) {
                val stroke = raw.sanitized()
                if (stroke.points.isEmpty()) continue
                val path = Path().apply {
                    stroke.points.forEachIndexed { index, point ->
                        val x = point.x * source.width
                        val y = point.y * source.height
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                val diameter = (stroke.sizeRatio * minEdge).coerceAtLeast(1f)
                val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = diameter
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    style = Paint.Style.STROKE
                    color = stroke.colorArgb
                }
                val footprint = Path().apply {
                    if (stroke.points.size == 1) {
                        val point = stroke.points[0]
                        addCircle(point.x * source.width, point.y * source.height,
                            diameter / 2f, Path.Direction.CW)
                    } else {
                        brush.getFillPath(path, this)
                    }
                }
                if (stroke.isEraser) {
                    if (stroke.kind == PhotoStrokeKind.Mosaic) {
                        mosaicErased.addPath(footprint)
                        hasMosaicErasures = true
                    } else {
                        doodleErased.addPath(footprint)
                        hasDoodleErasures = true
                    }
                    continue
                }

                val isMosaic = stroke.kind == PhotoStrokeKind.Mosaic
                val hasLaterErasure = if (isMosaic) hasMosaicErasures else hasDoodleErasures
                // Doodles without relevant later erasers can still be drawn straight
                // behind later content; this avoids an extra temporary bitmap layer.
                if (!isMosaic && !hasLaterErasure) {
                    brush.xfermode = behind
                    if (stroke.points.size == 1) {
                        val pt = stroke.points[0]
                        brush.style = Paint.Style.FILL
                        canvas.drawCircle(pt.x * source.width, pt.y * source.height,
                            diameter / 2f, brush)
                    } else canvas.drawPath(path, brush)
                    continue
                }

                // Bounds cover the full round-capped footprint plus two AA pixels.
                // An offscreen layer limited to this rectangle avoids full-image
                // allocation for each short mark on a high-resolution photograph.
                val layerBounds = photoStrokeLayerBounds(
                    stroke.points, source.width, source.height, diameter
                ) ?: continue
                val bounds = RectF(layerBounds.left, layerBounds.top,
                    layerBounds.right, layerBounds.bottom)
                val saved = canvas.saveLayer(bounds, layerRestorePaint)
                try {
                    if (!isMosaic) {
                        if (stroke.points.size == 1) {
                            val pt = stroke.points[0]
                            brush.style = Paint.Style.FILL
                            canvas.drawCircle(pt.x * source.width, pt.y * source.height,
                                diameter / 2f, brush)
                        } else canvas.drawPath(path, brush)
                    } else {
                        val block = (stroke.mosaicBlockRatio * minEdge).roundToInt()
                            .coerceAtLeast(2)
                        val small = pixelCache[block] ?: run {
                            if (pixelCache.size >= 3) {
                                val iterator = pixelCache.entries.iterator()
                                if (iterator.hasNext()) {
                                    val oldest = iterator.next().value
                                    iterator.remove()
                                    if (!oldest.isRecycled) oldest.recycle()
                                }
                            }
                            Bitmap.createScaledBitmap(source,
                                (source.width / block).coerceAtLeast(1),
                                (source.height / block).coerceAtLeast(1), false
                            ).also { pixelCache[block] = it }
                        }
                        // DST_IN with an antialiased footprint avoids the hard
                        // pixel-stepped boundary of the old clipPath implementation.
                        canvas.drawBitmap(small, Rect(0, 0, small.width, small.height),
                            RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
                            pixelPaint)
                        canvas.drawPath(footprint, keepPaint)
                    }
                    if (hasLaterErasure) {
                        canvas.drawPath(if (isMosaic) mosaicErased else doodleErased, cutPaint)
                    }
                } finally {
                    canvas.restoreToCount(saved)
                }
            }
            canvas.drawBitmap(source, 0f, 0f, Paint().apply { xfermode = behind })
            return output
        } catch (e: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw e
        } finally {
            pixelCache.values.forEach { if (it !== source && !it.isRecycled) it.recycle() }
        }
    }
}
