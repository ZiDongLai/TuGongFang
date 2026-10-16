package com.tugongfang.editor.editor.image

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameCorner

/** Draw a stroke inside the given target rectangle; it never shifts a photo's geometry. */
internal object CanvasFrameRenderer {
    fun stroke(canvas: Canvas, rect: RectF, frame: CanvasFrame, shortEdge: Float, radius: Float = 0f) {
        if (!frame.enabled) return
        val width=(shortEdge*frame.normalized().widthRatio).coerceAtLeast(1f)
        val inside=RectF(rect.left+width/2f,rect.top+width/2f,
            rect.right-width/2f,rect.bottom-width/2f)
        if (inside.width() <= 0f || inside.height() <= 0f) return
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=frame.colorArgb
            style=Paint.Style.STROKE
            strokeWidth=width
            strokeJoin=Paint.Join.ROUND
        }
        val outerRadius = if (frame.corner == CanvasFrameCorner.Square) 0f else radius.coerceAtLeast(0f)
        val insideRadius = (outerRadius - width/2f).coerceAtLeast(0f)
        canvas.drawRoundRect(inside,insideRadius,insideRadius,p)
    }
}
