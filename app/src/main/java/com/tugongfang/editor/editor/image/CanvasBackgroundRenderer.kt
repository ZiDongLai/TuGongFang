package com.tugongfang.editor.editor.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.ImageDecoder
import android.os.Build
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.tugongfang.editor.model.canvas.BackgroundGradientDirection
import com.tugongfang.editor.model.canvas.BackgroundImageFit
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Preview and exported JPG/PNG call this same renderer. No foreground layer is mutated. */
object CanvasBackgroundRenderer {
    fun draw(context: Context, canvas: Canvas, bg: CanvasBackground, width: Int, height: Int) {
        require(width > 0 && height > 0)
        val w = width.toFloat(); val h = height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        when (bg.kind) {
            CanvasBackgroundKind.Transparent -> Unit
            CanvasBackgroundKind.Solid -> {
                paint.color = bg.firstColor
                canvas.drawColor(bg.firstColor)
            }
            CanvasBackgroundKind.Gradient -> {
                val (endX, endY) = when (bg.gradientDirection) {
                    BackgroundGradientDirection.Diagonal -> w to h
                    BackgroundGradientDirection.Horizontal -> w to 0f
                    BackgroundGradientDirection.Vertical -> 0f to h
                }
                paint.shader = LinearGradient(0f, 0f, endX, endY,
                    bg.firstColor, bg.secondColor, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null
            }
            CanvasBackgroundKind.Dots, CanvasBackgroundKind.Grid, CanvasBackgroundKind.Paper -> {
                canvas.drawColor(bg.firstColor)
                paint.color = bg.secondColor
                paint.alpha = when (bg.kind) {
                    CanvasBackgroundKind.Paper -> 28
                    else -> 90
                }
                val step = (w / 18f).coerceAtLeast(2f)
                when (bg.kind) {
                    CanvasBackgroundKind.Dots -> {
                        var y = step / 2f
                        while (y < h) { var x = step / 2f
                            while (x < w) { canvas.drawCircle(x, y, step*.08f, paint); x += step }; y += step }
                    }
                    CanvasBackgroundKind.Grid -> {
                        var x = 0f; while (x < w) { canvas.drawLine(x,0f,x,h,paint); x+=step }
                        var y = 0f; while (y < h) { canvas.drawLine(0f,y,w,y,paint); y+=step }
                    }
                    CanvasBackgroundKind.Paper -> {
                        // Very light deterministic paper grain, no bulky texture assets.
                        val grain = (step/4f).coerceAtLeast(2f)
                        var y=0f; var row=0
                        while(y<h) { var x=0f; var col=0
                            while(x<w) { if ((row*37 + col*19)%7==0)
                                canvas.drawCircle(x,y,grain*.14f,paint)
                                x+=grain; col++ }
                            y+=grain; row++ }
                    }
                    else -> Unit
                }
            }
            CanvasBackgroundKind.Image -> drawImage(context, canvas, bg, width, height)
        }
    }

    private fun drawImage(context: Context, canvas: Canvas, bg: CanvasBackground, width: Int, height: Int) {
        val uri = bg.imageUri?.takeIf { it.isNotBlank() } ?: return
        val target = max(width,height).coerceAtMost(2600)
        val bitmap = readSampled(context,Uri.parse(uri),target) ?: return
        try {
            val bw = bitmap.width.toFloat(); val bh = bitmap.height.toFloat()
            val w = width.toFloat(); val h = height.toFloat()
            val fit = if(bg.imageFit == BackgroundImageFit.Fill) max(w/bw,h/bh) else min(w/bw,h/bh)
            val drawW = bw*fit; val drawH = bh*fit
            val x=(w-drawW)/2f; val y=(h-drawH)/2f
            val strength=bg.blur.coerceIn(0,100)
            // A bounded low-frequency soft blur, rendered identically by preview and export.
            val reduced = if(strength==0) bitmap else {
                val divisor = 1 + strength/5
                Bitmap.createScaledBitmap(bitmap,
                    (bitmap.width/divisor).coerceAtLeast(2),
                    (bitmap.height/divisor).coerceAtLeast(2),true)
            }
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (bg.opacity.coerceIn(0f,1f)*255f).roundToInt()
                }
                canvas.save()
                canvas.clipRect(0f,0f,w,h)
                canvas.drawBitmap(reduced,null,RectF(x,y,x+drawW,y+drawH),paint)
                canvas.restore()
            } finally { if (reduced !== bitmap && !reduced.isRecycled) reduced.recycle() }
        } finally { if(!bitmap.isRecycled) bitmap.recycle() }
    }

    private fun readSampled(context: Context, uri: Uri, longest: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder,info,_ ->
                val oldW=info.size.width; val oldH=info.size.height
                val edge=max(oldW,oldH)
                if(edge>longest) {
                    val ratio=longest.toFloat()/edge
                    decoder.setTargetSize((oldW*ratio).roundToInt().coerceAtLeast(1),
                        (oldH*ratio).roundToInt().coerceAtLeast(1))
                }
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val b=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,b) }
        if(b.outWidth<=0 || b.outHeight<=0) return null
        var sample=1
        while(max(b.outWidth/sample,b.outHeight/sample)>longest) sample*=2
        val options=BitmapFactory.Options().apply {
            inSampleSize=sample; inPreferredConfig=Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,options) }
    }
}
