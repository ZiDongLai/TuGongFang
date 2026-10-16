package com.tugongfang.editor.editor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrixColorFilter
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.tugongfang.editor.model.collage.CollageProject
import com.tugongfang.editor.model.collage.CollageMode
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.editor.image.CanvasFrameRenderer
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.editor.image.CanvasBackgroundRenderer
import android.graphics.LinearGradient
import android.graphics.Shader
import com.tugongfang.editor.model.collage.collageImageSize
import com.tugongfang.editor.editor.image.PhotoEffectsRenderer
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.ImageToneMatrix
import com.tugongfang.editor.model.layer.ImageToneProcessor
import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.preferredDockWidth
import com.tugongfang.editor.model.layer.InfoTemplateType
import com.tugongfang.editor.model.layer.resolveInfoOverlayGeometry
import com.tugongfang.editor.model.layer.TextLayer
import com.tugongfang.editor.model.layer.ShapeLayer
import com.tugongfang.editor.model.layer.ShapeType
import com.tugongfang.editor.model.layer.SymbolGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * v0.4.3 导出基础引擎。
 *
 * 当前先把“最终排版边界”建立正确：
 * 1. 单图项目以照片本身作为输出主体，不导出编辑器背景。
 * 2. 文本模板统一位于照片外部底部，按内容自动增加最终图片高度。
 * 3. 自由文字覆盖在照片区域，不额外制造无意义画布。
 * 4. 读取原始 Uri，导出长边最高 4096px，后续再升级原尺寸/分块渲染。
 */
object ProjectExporter {

    /**
     * 生成与正式导出使用同一套排版规则的预览图。
     * 预览只降低解码分辨率，不改变裁切、旋转、自由文字和信息栏的最终布局关系。
     */
    suspend fun renderPreview(
        context: Context,
        imageLayers: List<ImageLayer>,
        textLayers: List<TextLayer>,
        infoLayers: List<InfoPanelLayer>,
        shapeLayers: List<ShapeLayer>,
        collage: CollageProject? = null,
        canvasScene: CanvasScene = CanvasScene(),
        maxSourceLongEdge: Int = 1400
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        ExportResultGuard.run {
            renderProjectBitmap(
                context = context,
                imageLayers = imageLayers,
                textLayers = textLayers,
                infoLayers = infoLayers,
                shapeLayers = shapeLayers,
                collage = collage,
                canvasScene = canvasScene,
                maxSourceLongEdge = maxSourceLongEdge.coerceIn(320, 1400)
            )
        }
    }

    /** 始终先使用原有合成规则渲染，再等比压缩成品尺寸，不触碰项目画布与图层几何。 */
    suspend fun exportImage(
        context: Context,
        imageLayers: List<ImageLayer>,
        textLayers: List<TextLayer>,
        infoLayers: List<InfoPanelLayer>,
        shapeLayers: List<ShapeLayer>,
        collage: CollageProject? = null,
        canvasScene: CanvasScene = CanvasScene(),
        options: ExportOptions = ExportOptions()
    ): Result<String> = withContext(Dispatchers.IO) {
        ExportResultGuard.run {
            val rendered = renderProjectBitmap(context, imageLayers, textLayers, infoLayers,
                shapeLayers, collage, canvasScene, 4096)
            try {
                val (outW, outH) = ExportFileRules.boundedDimensions(
                    rendered.width, rendered.height, options.maxOutputLongEdge)
                val sized = if (outW == rendered.width && outH == rendered.height) rendered
                    else Bitmap.createScaledBitmap(rendered, outW, outH, true)
                try {
                    // Include milliseconds and a per-export nonce: two fast exports must never
                    // rely on the gallery provider to de-duplicate an identical display name.
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                    val fileName = ExportFileRules.uniqueOutputName(
                        options.namePrefix, options.format, stamp, java.util.UUID.randomUUID().toString().take(8))
                    saveToGallery(context, sized, fileName,
                        options.format, ExportFileRules.boundedQuality(options.jpgQuality)).toString()
                } finally {
                    if (sized !== rendered && !sized.isRecycled) sized.recycle()
                }
            } finally {
                if (!rendered.isRecycled) rendered.recycle()
            }
        }
    }

    /** Shared effect matrix: preview, JPEG, PNG and project thumbnails read identical tone values. */
    private fun photoPaint(layer: ImageLayer): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        if (!ImageToneMatrix.isNeutral(layer.brightness,layer.contrast,layer.saturation)) {
            colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(ImageToneMatrix.make(
                layer.brightness,layer.contrast,layer.saturation)))
        }
    }

    /** Uses same advanced pixel adjustment as Compose preview; paint keeps v0.9.5 matrix. */
    private fun advancedBitmap(bitmap: Bitmap, layer: ImageLayer): Bitmap =
        ImageToneProcessor.applyAdvanced(bitmap, layer)

    private fun renderProjectBitmap(
        context: Context,
        imageLayers: List<ImageLayer>,
        textLayers: List<TextLayer>,
        infoLayers: List<InfoPanelLayer>,
        shapeLayers: List<ShapeLayer>,
        collage: CollageProject?,
        canvasScene: CanvasScene,
        maxSourceLongEdge: Int
    ): Bitmap {
        if (collage?.mode == CollageMode.Template) {
            return renderCollageBitmap(context,imageLayers,collage,textLayers,shapeLayers,infoLayers,
                canvasScene,maxSourceLongEdge)
        }
        if (collage?.mode == CollageMode.Free ||
            (!canvasScene.singlePhotoCanvas && canvasScene.background.kind != CanvasBackgroundKind.Transparent) ||
            imageLayers.count { it.visible } > 1) {
            return renderFreeCanvas(context,imageLayers,textLayers,shapeLayers,infoLayers,canvasScene,maxSourceLongEdge)
        }
        val visibleImages = imageLayers.filter { it.visible }
        require(visibleImages.isNotEmpty()) { "请先导入照片" }
        require(visibleImages.size == 1) { "当前导出基础版先支持单图项目，多图导出将在拼图阶段接入" }

        val imageLayer = visibleImages.first()
        val source = decodeBitmap(context, Uri.parse(imageLayer.uriString), maxSourceLongEdge)
            ?: error("无法读取原始照片")

        val effected = PhotoEffectsRenderer.apply(source,imageLayer.photoStrokes)
        if (effected !== source && !source.isRecycled) source.recycle()
        val photo = preparePhoto(effected, imageLayer)
        if (photo !== effected && !effected.isRecycled) effected.recycle()

        try {
            // v0.5.0：文本模板统一作为照片外部的底部信息区。
            // 这样编辑界面与最终导出保持一致，避免模板覆盖照片主体。
            val topInfo = infoLayers.filter { it.visible && !it.overlayMode && it.positionTop }
            val bottomInfo = infoLayers.filter { it.visible && !it.overlayMode && !it.positionTop }
            val innerWidth = preferredDockWidth(photo.width, photo.height,
                topInfo.isNotEmpty() || bottomInfo.isNotEmpty())
            val margin = if (canvasScene.singlePhotoCanvas) canvasScene.photoInsetRatio.coerceIn(0f,.20f) else 0f
            // Insets are relative to both axes. Using photo.width for top/bottom
            // would under-pad portrait photos and break preview/export matching.
            val insetX = (photo.width * margin / (1f - 2f*margin)).roundToInt().coerceAtLeast(0)
            val insetY = (photo.height * margin / (1f - 2f*margin)).roundToInt().coerceAtLeast(0)
            val outputWidth = innerWidth + insetX * 2
            val photoPanelHeight = photo.height + insetY * 2
            val imageOffsetX = (outputWidth - photo.width) / 2f
            val topHeight = measureInfoPanels(outputWidth, topInfo)
            val bottomHeight = measureInfoPanels(outputWidth, bottomInfo)

            val output = Bitmap.createBitmap(
                outputWidth,
                photoPanelHeight + topHeight + bottomHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            // Legacy direct-photo output stays white. Explicit DEV42 transparent margins
            // preserve alpha for PNG; JPEG export already composites against white.
            if (!canvasScene.singlePhotoCanvas) canvas.drawColor(Color.WHITE)
            // Background is behind only the photograph area, not the docked info bars.
            if (canvasScene.singlePhotoCanvas) {
                canvas.save()
                canvas.translate(0f,topHeight.toFloat())
                CanvasBackgroundRenderer.draw(context,canvas,canvasScene.background,outputWidth,photoPanelHeight)
                canvas.restore()
            }
            if (topInfo.isNotEmpty()) drawInfoPanels(canvas,outputWidth,0,topInfo)

            val photoTop = topHeight + insetY
            canvas.save()
            canvas.translate(imageOffsetX, 0f)
            val adjusted = advancedBitmap(photo,imageLayer)
            try {
                if (canvasScene.frame.enabled && canvasScene.frame.scope==CanvasFrameScope.Photos &&
                    canvasScene.frame.corner==CanvasFrameCorner.Rounded) {
                    val photoRadius=minOf(photo.width,photo.height)*.028f
                    canvas.save()
                    val photoClip=android.graphics.Path().apply { addRoundRect(
                        RectF(0f,photoTop.toFloat(),photo.width.toFloat(),
                            (photoTop+photo.height).toFloat()),photoRadius,photoRadius,
                        android.graphics.Path.Direction.CW) }
                    canvas.clipPath(photoClip)
                    canvas.drawBitmap(adjusted,0f,photoTop.toFloat(),photoPaint(imageLayer))
                    canvas.restore()
                } else canvas.drawBitmap(adjusted, 0f, photoTop.toFloat(), photoPaint(imageLayer))
            } finally {
                if(adjusted !== photo && !adjusted.isRecycled) adjusted.recycle()
            }
            if (canvasScene.frame.scope == CanvasFrameScope.Photos) {
                CanvasFrameRenderer.stroke(canvas,RectF(0f,photoTop.toFloat(),
                    photo.width.toFloat(),(photoTop+photo.height).toFloat()),canvasScene.frame,
                    minOf(photo.width,photo.height).toFloat(),
                    if(canvasScene.frame.corner==CanvasFrameCorner.Rounded)
                        minOf(photo.width,photo.height)*.028f else 0f)
            }
            drawShapeLayers(canvas, photo.width, photo.height, photoTop, shapeLayers.filter { it.visible })
            drawTextLayers(canvas, photo.width, photo.height, photoTop, textLayers.filter { it.visible })
            drawOverlayInfoPanels(canvas, photo.width, photo.height, photoTop,
                infoLayers.filter { it.visible && it.overlayMode })
            canvas.restore()
            if (canvasScene.frame.scope == CanvasFrameScope.Canvas) {
                CanvasFrameRenderer.stroke(canvas, RectF(0f,topHeight.toFloat(),
                    outputWidth.toFloat(),(topHeight+photoPanelHeight).toFloat()),canvasScene.frame,
                    minOf(outputWidth,photoPanelHeight).toFloat(),
                    minOf(outputWidth,photoPanelHeight)*.028f)
            }
            if (bottomInfo.isNotEmpty()) {
                drawInfoPanels(canvas, outputWidth, topHeight + photoPanelHeight, bottomInfo)
            }

            return output
        } finally {
            if (!photo.isRecycled) photo.recycle()
        }
    }

    /** v0.8.0 模板拼图输出，不携带编辑器背景。
     * 阶段限制：模板拼图模式仅渲染照片格位；普通图形/文字图层仍原样保存在项目数据里。
     */
    private fun renderCollageBitmap(
        context: Context,
        images: List<ImageLayer>,
        raw: CollageProject,
        textLayers: List<TextLayer>,
        shapeLayers: List<ShapeLayer>,
        infoLayers: List<InfoPanelLayer>,
        scene: CanvasScene,
        maxSourceLongEdge: Int
    ): Bitmap {
        val project=raw.normalized()
        val idToLayer=images.associateBy { it.id }
        val placed=project.slots.mapNotNull { slot -> slot.imageId?.let(idToLayer::get)?.takeIf { it.visible } }
        require(placed.isNotEmpty()) { "请先为拼图添加照片" }
        val longEdge=if (maxSourceLongEdge < 2000) 1200 else 3000
        val ratio=project.aspect.ratio
        val outputWidth=if (ratio>=1f) longEdge else (longEdge*ratio).roundToInt().coerceAtLeast(1)
        val outputHeight=if (ratio>=1f) (longEdge/ratio).roundToInt().coerceAtLeast(1) else longEdge
        val visibleInfo = infoLayers.filter { it.visible && !it.overlayMode }
        val output=Bitmap.createBitmap(outputWidth,outputHeight,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(output)
            CanvasBackgroundRenderer.draw(context,canvas,scene.background,outputWidth,outputHeight)
            val dests=project.rects()
            project.slots.forEachIndexed { index, slot ->
                val layer=slot.imageId?.let(idToLayer::get)?.takeIf { it.visible } ?: return@forEachIndexed
                val source=decodeBitmap(context,Uri.parse(layer.uriString),if (maxSourceLongEdge<2000) 1400 else 2600)
                    ?: error("无法读取第${index+1}张照片")
                val effected=PhotoEffectsRenderer.apply(source,layer.photoStrokes)
                if (effected!==source && !source.isRecycled) source.recycle()
                val prepared=preparePhoto(effected,layer)
                if (prepared!==effected && !effected.isRecycled) effected.recycle()
                try {
                    val r=dests[index]
                    val left=r.left*outputWidth
                    val top=r.top*outputHeight
                    val tileW=r.width*outputWidth
                    val tileH=r.height*outputHeight
                    val radius=if(scene.frame.enabled && scene.frame.scope==CanvasFrameScope.Photos &&
                        scene.frame.corner==CanvasFrameCorner.Square) 0f
                        else (project.cornerRatio*outputWidth).coerceAtMost(minOf(tileW,tileH)/3f)
                    val clipPath=android.graphics.Path().apply {
                        addRoundRect(RectF(left,top,left+tileW,top+tileH),radius,radius,android.graphics.Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(clipPath)
                    val result=collageImageSize(prepared.width.toFloat(),prepared.height.toFloat(),tileW,tileH,slot)
                    val centerX=left+tileW/2f+slot.panX*tileW
                    val centerY=top+tileH/2f+slot.panY*tileH
                    canvas.rotate(slot.rotation,centerX,centerY)
                    val adjusted = advancedBitmap(prepared,layer)
                    try {
                        canvas.drawBitmap(adjusted,null,RectF(centerX-result.width/2,centerY-result.height/2,
                            centerX+result.width/2,centerY+result.height/2),photoPaint(layer))
                    } finally {
                        if(adjusted !== prepared && !adjusted.isRecycled) adjusted.recycle()
                    }
                    canvas.restore()
                    if (scene.frame.scope == CanvasFrameScope.Photos) {
                        CanvasFrameRenderer.stroke(canvas,RectF(left,top,left+tileW,top+tileH),
                            scene.frame,minOf(outputWidth,outputHeight).toFloat(),radius)
                    }
                } finally {
                    if (!prepared.isRecycled) prepared.recycle()
                }
            }
            if (scene.frame.scope == CanvasFrameScope.Canvas) {
                CanvasFrameRenderer.stroke(canvas,RectF(0f,0f,outputWidth.toFloat(),outputHeight.toFloat()),
                    scene.frame,minOf(outputWidth,outputHeight).toFloat(),
                    minOf(outputWidth,outputHeight)*.028f)
            }
            drawShapeLayers(canvas,outputWidth,outputHeight,0,shapeLayers.filter { it.visible })
            drawTextLayers(canvas,outputWidth,outputHeight,0,textLayers.filter { it.visible })
            drawOverlayInfoPanels(canvas,outputWidth,outputHeight,0,
                infoLayers.filter { it.visible && it.overlayMode })
            if (visibleInfo.isEmpty()) return output
            val completed=appendInfoPanelsToCanvas(output,visibleInfo,compact=false)
            if (!output.isRecycled) output.recycle()
            return completed
        } catch (t:Throwable) {
            if (!output.isRecycled) output.recycle()
            throw t
        }
    }

    /** 普通多图与自由拼图共用；归一化位置和单图图层控件一一对应。 */
    private fun renderFreeCanvas(
        context: Context, images: List<ImageLayer>, texts: List<TextLayer>,
        shapes: List<ShapeLayer>, infos: List<InfoPanelLayer>, scene: CanvasScene,
        maxSourceLongEdge: Int
    ): Bitmap {
        val visible = images.filter { it.visible }
        require(visible.isNotEmpty()) { "请先添加照片" }
        val ratio = scene.validAspect()
        val edge = if (maxSourceLongEdge < 2000) 1200 else 3000
        val width = if (ratio >= 1f) edge else (edge*ratio).roundToInt().coerceAtLeast(1)
        val height = if (ratio >= 1f) (edge/ratio).roundToInt().coerceAtLeast(1) else edge
        val visibleInfo = infos.filter { it.visible && !it.overlayMode }
        // 图片和所有覆盖图层先绘制到逻辑画布；信息模板另行追加。
        // 这样无背景时才可以裁掉图片与信息区之间未使用的垂直画布空间。
        val result = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(result)
            CanvasBackgroundRenderer.draw(context,canvas,scene.background,width,height)
            val edgeInset=scene.photoInsetRatio.coerceIn(0f,.20f)
            canvas.save()
            canvas.translate(width*edgeInset,height*edgeInset)
            canvas.scale(1f-2f*edgeInset,1f-2f*edgeInset)
            visible.forEach { layer ->
                val original = decodeBitmap(context,Uri.parse(layer.uriString),if(maxSourceLongEdge<2000) 1400 else 3000)
                    ?: error("无法读取照片")
                // 裁切与翻转一次处理，旋转留给图层矩阵，防止旋转两次。
                val effected = PhotoEffectsRenderer.apply(original,layer.photoStrokes)
                if (effected !== original && !original.isRecycled) original.recycle()
                val bitmap = preparePhoto(effected,layer.copy(rotation=0f))
                if (bitmap !== effected && !effected.isRecycled) effected.recycle()
                try {
                    val wLimit = width*.90f
                    // 信息模板不再改变照片的布局尺寸；与编辑页的固定图层几何一致。
                    val hLimit = height * .83f
                    val fit = minOf(wLimit/bitmap.width,hLimit/bitmap.height)
                    val renderWidth = bitmap.width*fit*layer.scale
                    val renderHeight = bitmap.height*fit*layer.scale
                    val cx = width*(.5f+layer.offsetXRatio)
                    // 不依据模板有无重新排布照片；仅用户的 offsetYRatio 可以改变位置。
                    val cy = height*.5f + layer.offsetYRatio*height
                    canvas.save()
                    canvas.rotate(layer.rotation,cx,cy)
                    val adjusted = advancedBitmap(bitmap,layer)
                    try {
                        val photoRect=RectF(cx-renderWidth/2,cy-renderHeight/2,
                            cx+renderWidth/2,cy+renderHeight/2)
                        if (scene.frame.enabled && scene.frame.scope==CanvasFrameScope.Photos &&
                            scene.frame.corner==CanvasFrameCorner.Rounded) {
                            val radius=minOf(renderWidth,renderHeight)*.028f
                            canvas.save()
                            canvas.clipPath(android.graphics.Path().apply {
                                addRoundRect(photoRect,radius,radius,android.graphics.Path.Direction.CW)
                            })
                            canvas.drawBitmap(adjusted,null,photoRect,photoPaint(layer))
                            canvas.restore()
                        } else canvas.drawBitmap(adjusted,null,photoRect,photoPaint(layer))
                    } finally {
                        if(adjusted !== bitmap && !adjusted.isRecycled) adjusted.recycle()
                    }
                    if (scene.frame.scope == CanvasFrameScope.Photos) {
                        CanvasFrameRenderer.stroke(canvas,RectF(cx-renderWidth/2,cy-renderHeight/2,
                            cx+renderWidth/2,cy+renderHeight/2),scene.frame,
                            minOf(width,height).toFloat(),
                            if(scene.frame.corner==CanvasFrameCorner.Rounded)
                                minOf(renderWidth,renderHeight)*.028f else 0f)
                    }
                    canvas.restore()
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
            drawShapeLayers(canvas,width,height,0,shapes.filter { it.visible })
            drawTextLayers(canvas,width,height,0,texts.filter { it.visible })
            drawOverlayInfoPanels(canvas,width,height,0,
                infos.filter { it.visible && it.overlayMode })
            canvas.restore()
            if (scene.frame.scope == CanvasFrameScope.Canvas) {
                CanvasFrameRenderer.stroke(canvas,RectF(0f,0f,width.toFloat(),height.toFloat()),
                    scene.frame,minOf(width,height).toFloat(),
                    minOf(width,height)*.028f)
            }
            if (visibleInfo.isEmpty()) return result
            // User-configured outer padding or a frame must not be trimmed by the
            // legacy transparent-background information-panel compacting rule.
            val compact = scene.background.kind == CanvasBackgroundKind.Transparent &&
                scene.photoInsetRatio <= 0f && !scene.frame.enabled
            val finalOutput = appendInfoPanelsToCanvas(result, visibleInfo, compact)
            if (!result.isRecycled) result.recycle()
            return finalOutput
        } catch (t: Throwable) {
            if (!result.isRecycled) result.recycle()
            throw t
        }
    }

    /**
     * FIX1: 不改变照片/图形在画布中的相对位置，仅收紧「透明背景 + 信息模板」
     * 上下的无效透明行。彩色/渐变/图案背景沿用完整画布，不做裁剪。
     * 预览和正式导出均调用本函数。
     */
    private fun appendInfoPanelsToCanvas(
        base: Bitmap,
        visibleInfo: List<InfoPanelLayer>,
        compact: Boolean
    ): Bitmap {
        val w = base.width
        val h = base.height
        val padding = if (compact) (w * .018f).roundToInt().coerceAtLeast(2) else 0
        val content = if (compact) visibleVerticalBounds(base) else 0 until h
        // 内容有一定画布留边；两张照片之间的空白不会被修改。
        val firstY = (content.first - padding).coerceAtLeast(0)
        val lastExclusive = ((content.last + 1) + padding).coerceAtMost(h).coerceAtLeast(firstY + 1)
        val photoHeight = lastExclusive - firstY
        val topPanels = visibleInfo.filter { it.positionTop }
        val bottomPanels = visibleInfo.filterNot { it.positionTop }
        val outputWidth = preferredDockWidth(w, photoHeight, visibleInfo.isNotEmpty())
        val imageOffsetX = (outputWidth - w) / 2f
        val topHeight = measureInfoPanels(outputWidth,topPanels)
        val bottomHeight = measureInfoPanels(outputWidth,bottomPanels)
        val output = Bitmap.createBitmap(outputWidth, topHeight+photoHeight+bottomHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        if(topPanels.isNotEmpty()) drawInfoPanels(canvas,outputWidth,0,topPanels)
        canvas.drawBitmap(base, android.graphics.Rect(0,firstY,w,lastExclusive),
            RectF(imageOffsetX,topHeight.toFloat(),imageOffsetX+w,(topHeight+photoHeight).toFloat()), null)
        if(bottomPanels.isNotEmpty()) drawInfoPanels(canvas,outputWidth,topHeight+photoHeight,bottomPanels)
        return output
    }

    /** 只查找最上/最下非透明像素行；横向布局和所有图层坐标不变。 */
    private fun visibleVerticalBounds(bitmap: Bitmap): IntRange {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width)
        fun containsVisiblePixel(y: Int): Boolean {
            bitmap.getPixels(pixels,0,width,0,y,width,1)
            return pixels.any { (it ushr 24) != 0 }
        }
        var first = 0
        while (first < height && !containsVisiblePixel(first)) first++
        if (first == height) return 0 until height
        var last = height - 1
        while (last > first && !containsVisiblePixel(last)) last--
        return first..last
    }

    private fun preparePhoto(source: Bitmap, layer: ImageLayer): Bitmap {
        val left = (layer.cropLeft.coerceIn(0f, 1f) * source.width).roundToInt().coerceIn(0, source.width - 1)
        val top = (layer.cropTop.coerceIn(0f, 1f) * source.height).roundToInt().coerceIn(0, source.height - 1)
        val right = (layer.cropRight.coerceIn(0f, 1f) * source.width).roundToInt().coerceIn(left + 1, source.width)
        val bottom = (layer.cropBottom.coerceIn(0f, 1f) * source.height).roundToInt().coerceIn(top + 1, source.height)

        val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        val matrix = Matrix().apply {
            postScale(
                if (layer.flipHorizontal) -1f else 1f,
                if (layer.flipVertical) -1f else 1f,
                cropped.width / 2f,
                cropped.height / 2f
            )
            postRotate(layer.rotation, cropped.width / 2f, cropped.height / 2f)
        }
        if (!layer.flipHorizontal && !layer.flipVertical && kotlin.math.abs(layer.rotation) < .01f) {
            return cropped
        }
        val transformed = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
        if (transformed !== cropped && !cropped.isRecycled) cropped.recycle()
        return transformed
    }

    private fun drawShapeLayers(
        canvas: Canvas, photoWidth: Int, photoHeight: Int, photoTop: Int, layers: List<ShapeLayer>
    ) {
        layers.forEach { layer ->
            val centerX = photoWidth * (.5f + layer.offsetXRatio)
            val centerY = photoTop + photoHeight * (.5f + layer.offsetYRatio)
            val rawWidth = photoWidth * layer.widthRatio * layer.scale
            val rawHeight = photoHeight * layer.heightRatio * layer.scale
            val width = if (layer.type == ShapeType.Ellipse && layer.perfectCircle) minOf(rawWidth, rawHeight) else rawWidth
            val height = if (layer.type == ShapeType.Ellipse && layer.perfectCircle) minOf(rawWidth, rawHeight) else rawHeight
            val stroke = (layer.strokeWidth * photoWidth / 360f).coerceAtLeast(1f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.colorArgb
                if (layer.doodleSessionId != null) alpha = (layer.doodleOpacity.coerceIn(0f, 1f) * 255f).roundToInt()
                style = if (layer.filled && layer.type != ShapeType.Line && layer.type != ShapeType.Arrow)
                    Paint.Style.FILL else Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.save()
            canvas.rotate(layer.rotation, centerX, centerY)
            val r = RectF(centerX - width/2f, centerY - height/2f,
                centerX + width/2f, centerY + height/2f)
            when (layer.type) {
                ShapeType.Rectangle -> {
                    if (layer.roundedCorners) {
                        val radius = minOf(width, height) * .12f
                        canvas.drawRoundRect(r, radius, radius, paint)
                    } else canvas.drawRect(r, paint)
                }
                ShapeType.Ellipse -> canvas.drawOval(r, paint)
                ShapeType.Symbol -> {
                    val side = minOf(width,height).coerceAtLeast(1f)
                    val ox = centerX - side/2f
                    val oy = centerY - side/2f
                    paint.style = Paint.Style.STROKE
                    val glyph = SymbolGeometry.glyph(layer.symbolKind)
                    glyph.strokes.forEach { poly ->
                        if (poly.vertices.isNotEmpty()) {
                            val p = android.graphics.Path().apply {
                                val first=poly.vertices.first()
                                moveTo(ox+first.x*side,oy+first.y*side)
                                poly.vertices.drop(1).forEach { lineTo(ox+it.x*side,oy+it.y*side) }
                                if (poly.closed) close()
                            }
                            canvas.drawPath(p,paint)
                        }
                    }
                    glyph.rings.forEach { ring ->
                        canvas.drawCircle(ox+ring.cx*side,oy+ring.cy*side,ring.radius*side,paint)
                    }
                }
                ShapeType.Freehand -> {
                    val pts = layer.points
                    if (pts.isNotEmpty()) {
                        val path = android.graphics.Path().apply {
                            moveTo(r.left + pts[0].x * width, r.top + pts[0].y * height)
                            pts.drop(1).forEach { lineTo(r.left + it.x * width, r.top + it.y * height) }
                        }
                        paint.style = Paint.Style.STROKE
                        if (pts.size == 1) {
                            // DEV19: 单点笔迹是实心点，不可复用普通路径的 STROKE 样式画成空心环。
                            paint.style = Paint.Style.FILL
                            canvas.drawCircle(r.left + pts[0].x * width,
                                r.top + pts[0].y * height, stroke / 2f, paint)
                        } else canvas.drawPath(path, paint)
                    }
                }
                ShapeType.Line, ShapeType.Arrow -> {
                    canvas.drawLine(r.left, centerY, r.right, centerY, paint)
                    if (layer.type == ShapeType.Arrow) {
                        val head = kotlin.math.min(width * .18f, photoWidth * .065f)
                        canvas.drawLine(r.right, centerY, r.right-head, centerY-head*.6f, paint)
                        canvas.drawLine(r.right, centerY, r.right-head, centerY+head*.6f, paint)
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawTextLayers(
        canvas: Canvas,
        photoWidth: Int,
        photoHeight: Int,
        photoTop: Int,
        layers: List<TextLayer>
    ) {
        layers.forEach { layer ->
            val text = layer.text.ifBlank { return@forEach }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.colorArgb
                textSize = (layer.fontSizeSp * (photoWidth / 360f) * layer.scale).coerceAtLeast(10f)
                val family = when (layer.fontFamilyId) {
                    1 -> "serif"
                    2 -> "monospace"
                    else -> "sans-serif"
                }
                typeface = android.graphics.Typeface.create(
                    family,
                    (if (layer.bold) android.graphics.Typeface.BOLD else 0) or
                        (if (layer.italic) android.graphics.Typeface.ITALIC else 0)
                )
                isUnderlineText = layer.underline
                textAlign = when (layer.textAlign) {
                    0 -> Paint.Align.LEFT
                    2 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
            }

            val x = photoWidth / 2f + layer.offsetXRatio * photoWidth
            val y = photoTop + photoHeight / 2f + layer.offsetYRatio * photoHeight
            // DEV14: 长文字使用Android原生自动断行，避免仅按 \n 分行导致导出溢出。
            // 宽度与编辑器内容宽度共用：基准画布宽360、原默认框宽268dp。
            val textFrameWidth = (layer.boxWidthDp.coerceIn(88, 640) *
                (photoWidth / 360f) * layer.scale).roundToInt().coerceAtLeast(1)
            val wrappedPaint = TextPaint(paint).apply { textAlign = Paint.Align.LEFT }
            val alignment = when (layer.textAlign) {
                0 -> Layout.Alignment.ALIGN_NORMAL
                2 -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, wrappedPaint, textFrameWidth)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setLineSpacing(0f, layer.lineSpacing.coerceIn(1f, 2f))
                .build()

            canvas.save()
            canvas.rotate(layer.rotation, x, y)
            // Frame may be taller than its text; reflow always depends on width,
            // extra height is empty frame space, never stretched glyphs.
            val minimumHeight = (layer.boxHeightDp.coerceIn(0,720) *
                (photoWidth / 360f) * layer.scale).roundToInt()
            val frameHeight = maxOf(layout.height, minimumHeight)
            canvas.translate(x - textFrameWidth / 2f, y - frameHeight / 2f)
            if ((layer.backgroundColorArgb ushr 24) != 0) {
                // Same content-frame footprint as Compose: 9dp horizontal padding,
                // 6dp vertical padding on each side, scaled to the export canvas.
                val unit = (photoWidth / 360f) * layer.scale
                val padX = 9f * unit
                val padY = 6f * unit
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.backgroundColorArgb
                }
                canvas.drawRoundRect(
                    RectF(-padX, -padY,
                        textFrameWidth + padX, frameHeight + padY),
                    8f * unit, 8f * unit, fill
                )
            }
            layout.draw(canvas)
            canvas.restore()
        }
    }

    /** Draw overlays relative to the original photo/collage area, never the extra text strips. */
    private fun drawOverlayInfoPanels(
        canvas: Canvas, imageWidth: Int, imageHeight: Int, imageTop: Int,
        panels: List<InfoPanelLayer>
    ) {
        panels.forEach { panel ->
            val tentativeW = (imageWidth * panel.widthRatio.coerceIn(.28f,.98f)).roundToInt().coerceAtLeast(1)
            val contentMinHeight = measureInfoPanel(tentativeW, panel).toFloat()
            // Same rotated bounds and position constraints as the preview.
            val bounded = resolveInfoOverlayGeometry(panel, imageWidth.toFloat(), imageHeight.toFloat(), contentMinHeight)
            val panelWidth = bounded.width.roundToInt().coerceAtLeast(1)
            val panelHeight = bounded.height.roundToInt().coerceAtLeast(1)
            val scratch=Bitmap.createBitmap(panelWidth,panelHeight,Bitmap.Config.ARGB_8888)
            try {
                val inner=Canvas(scratch)
                drawInfoPanel(inner,panelWidth,0,panelHeight,panel)
                val centerX=imageWidth*.5f+bounded.centerX
                val centerY=imageTop+imageHeight*.5f+bounded.centerY
                canvas.save()
                canvas.rotate(panel.rotation,centerX,centerY)
                canvas.drawBitmap(scratch,centerX-panelWidth/2f,centerY-panelHeight/2f,null)
                canvas.restore()
            } finally {
                scratch.recycle()
            }
        }
    }

    private fun measureInfoPanels(width: Int, panels: List<InfoPanelLayer>): Int {
        if (panels.isEmpty()) return 0
        return panels.sumOf { measureInfoPanel(width, it) }
    }

    // DEV6: one compact dock layout shared by preview export and final export.
    // All values contribute to measured height; there is no four-line truncation.
    private fun dockedFieldRows(panel: InfoPanelLayer): List<String> {
        val labels = infoTemplateFieldLabels(panel.templateType)
        val first = if (panel.primary.isBlank()) "" else "${labels.first}：${panel.primary}"
        val second = if (panel.secondary.isBlank()) "" else "${labels.second}：${panel.secondary}"
        val note = if (panel.note.isBlank()) "" else "${labels.third}：${panel.note}"
        return listOf(first,second,note).filter { it.isNotBlank() }
    }

    private fun infoTypeface(panel: InfoPanelLayer, bold: Boolean = false): android.graphics.Typeface {
        val base = when (panel.fontFamilyId) {
            1 -> android.graphics.Typeface.SERIF
            2 -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.DEFAULT
        }
        return android.graphics.Typeface.create(base,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun dockedTypography(width: Int, panel: InfoPanelLayer): Triple<Paint, Paint, Float> {
        // DEV43 FIX1: exported information panels should stay readable and match the
        // visibly larger preview size chosen by the user.
        val base = (width / 360f * 15f).coerceIn(13f, 64f) * panel.textScale.coerceIn(.90f,1.20f)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = base * 1.16f
            typeface = infoTypeface(panel, bold=true)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize=base
            typeface=infoTypeface(panel)
        }
        return Triple(titlePaint,textPaint,base*1.55f)
    }

    private fun dockedLines(width: Int, panel: InfoPanelLayer, paint: Paint): List<String> {
        val padding = (width * .032f).coerceAtLeast(12f)
        val available = (width - padding*2).roundToInt().coerceAtLeast(1)
        val raw = dockedFieldRows(panel)
        val joined = if(raw.size>=2 && panel.primary.isNotBlank() && panel.secondary.isNotBlank() &&
            raw[0].length+raw[1].length <= 26 &&
            paint.measureText(raw[0]+"    "+raw[1])<=available) {
            listOf(raw[0]+"    "+raw[1])+raw.drop(2)
        } else raw
        return joined.flatMap { wrapText(it,paint,available) }
    }

    /** Do not truncate dates or titles when an information card is narrower. */
    private fun dockedHeader(width: Int, panel: InfoPanelLayer, titlePaint: Paint, datePaint: Paint): Pair<List<String>, Boolean> {
        val padding=(width*.032f).coerceAtLeast(12f)
        val available=(width-padding*2).roundToInt().coerceAtLeast(1)
        val title=infoTitle(panel)
        val dateLabel = exportInfoDate(panel)
        val stacked=titlePaint.measureText(title)+datePaint.measureText(dateLabel)+padding>available
        return Pair(if (stacked) wrapText(title,titlePaint,available) else listOf(title),stacked)
    }

    /** All compact header and field lines are measured with the same paints used by export. */
    private data class CompactInfoPlan(
        val padding: Float,
        val titleLines: List<String>,
        val stackedDate: Boolean,
        val bodyLines: List<String>
    )

    private fun compactInfoPlan(
        width: Int, panel: InfoPanelLayer, titlePaint: Paint, textPaint: Paint
    ): CompactInfoPlan {
        val padding = (width * .045f).coerceAtLeast(9f)
        val available = (width - padding * 2).roundToInt().coerceAtLeast(1)
        val title = infoTitle(panel)
        val dateLabel = exportInfoDate(panel)
        val stacked = title.length > 10 ||
            titlePaint.measureText(title) + textPaint.measureText(dateLabel) + padding > available
        val titleLines = if (stacked) wrapText(title, titlePaint, available) else listOf(title)
        val bodyLines = dockedFieldRows(panel).flatMap { wrapText(it, textPaint, available) }
        return CompactInfoPlan(padding, titleLines, stacked, bodyLines)
    }

    private fun measureInfoPanel(width: Int, panel: InfoPanelLayer): Int {
        if (panel.overlayMode) {
            val font = (width / 360f * 15f).coerceIn(18f, 92f) * panel.textScale.coerceIn(.90f,1.20f)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = font * 1.08f
                typeface = infoTypeface(panel, bold=true)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = font
                typeface = infoTypeface(panel)
            }
            val plan = compactInfoPlan(width, panel, titlePaint, bodyPaint)
            val headerHeight = plan.titleLines.size * font * 1.35f +
                (if (plan.stackedDate) font * 1.3f else 0f)
            return (plan.padding * 2 + (-titlePaint.fontMetrics.top) + headerHeight +
                plan.bodyLines.size * font * 1.4f + font * .4f).roundToInt().coerceAtLeast(1)
        }
        val (titlePaint, textPaint, lineHeight) = dockedTypography(width,panel)
        val padding = (width*.032f).coerceAtLeast(12f)
        val datePaint=Paint(textPaint)
        val (titles,stacked)=dockedHeader(width,panel,titlePaint,datePaint)
        val lineCount = dockedLines(width,panel,textPaint).size
        val headerHeight=titles.size*titlePaint.textSize*1.45f +
            if(stacked) textPaint.textSize*1.4f else 0f
        return (padding*2 + headerHeight + lineCount*lineHeight).roundToInt()
            .coerceAtLeast(1)
    }

    private fun drawInfoPanels(
        canvas: Canvas,
        width: Int,
        startY: Int,
        panels: List<InfoPanelLayer>
    ) {
        var y = startY
        panels.forEach { panel ->
            val height = measureInfoPanel(width, panel)
            drawInfoPanel(canvas, width, y, height, panel)
            y += height
        }
    }

    private fun drawInfoPanel(
        canvas: Canvas,
        width: Int,
        top: Int,
        height: Int,
        panel: InfoPanelLayer
    ) {
        if(panel.overlayMode) {
            drawCompactOverlayInfo(canvas,width,top,height,panel)
            return
        }
        val dark = panel.darkBackground
        val foreground = if(dark) Color.rgb(247,249,252) else Color.rgb(32,37,54)
        val secondary = if(dark) Color.rgb(210,223,241) else Color.rgb(98,112,132)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if(dark) Color.rgb(35,47,64) else Color.rgb(244,247,252)
        }
        canvas.drawRect(0f,top.toFloat(),width.toFloat(),(top+height).toFloat(),fill)
        val (titlePaint,textPaint,lineHeight) = dockedTypography(width,panel)
        titlePaint.color=foreground
        textPaint.color=foreground
        val padding=(width*.032f).coerceAtLeast(12f)
        val title=infoTitle(panel)
        val date=exportInfoDate(panel)
        val datePaint=Paint(textPaint).apply {color=secondary;textAlign=Paint.Align.RIGHT}
        val (titles,stacked)=dockedHeader(width,panel,titlePaint,datePaint)
        var baseline=top+padding-titlePaint.fontMetrics.top
        titles.forEachIndexed { index,line ->
            canvas.drawText(line,padding,baseline,titlePaint)
            if (!stacked && index==0) canvas.drawText(date,width-padding,baseline,datePaint)
            baseline+=titlePaint.textSize*1.45f
        }
        if(stacked) {
            canvas.drawText(date,width-padding,baseline,datePaint)
            baseline+=textPaint.textSize*1.4f
        }
        dockedLines(width,panel,textPaint).forEach { line ->
            canvas.drawText(line,padding,baseline,textPaint)
            baseline+=lineHeight
        }
    }

    private fun drawCompactOverlayInfo(
        canvas: Canvas, width: Int, top: Int, height: Int, panel: InfoPanelLayer
    ) {
        val padding=(width*.045f).coerceAtLeast(9f)
        val font=(width/360f*15f).coerceIn(18f,92f) * panel.textScale.coerceIn(.90f,1.20f)
        val bg=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=if(panel.darkBackground) Color.rgb(37,48,69) else Color.rgb(247,250,253)
        }
        canvas.drawRoundRect(RectF(0f,top.toFloat(),width.toFloat(),(top+height).toFloat()),
            width*.027f,width*.027f,bg)
        val title=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=if(panel.darkBackground) Color.WHITE else Color.rgb(33,44,59)
            typeface=infoTypeface(panel, bold=true)
            textSize=font*1.08f
        }
        val sub=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=if(panel.darkBackground) Color.rgb(229,240,254) else Color.rgb(66,79,94)
            textSize=font
            typeface=infoTypeface(panel)
        }
        val plan = compactInfoPlan(width, panel, title, sub)
        var baseline = top + padding - title.fontMetrics.top
        val rightDatePaint = Paint(sub).apply { textAlign=Paint.Align.RIGHT }
        plan.titleLines.forEachIndexed { index, line ->
            canvas.drawText(line, padding, baseline, title)
            if (!plan.stackedDate && index == 0) {
                canvas.drawText(exportInfoDate(panel), width-padding, baseline, rightDatePaint)
            }
            baseline += font * 1.35f
        }
        if (plan.stackedDate) {
            canvas.drawText(exportInfoDate(panel), width-padding, baseline, rightDatePaint)
            baseline += font * 1.3f
        }
        plan.bodyLines.forEach { line ->
            canvas.drawText(line, padding, baseline, sub)
            baseline += font * 1.4f
        }
    }

    private fun infoRows(panel: InfoPanelLayer): List<Pair<String, String>> {
        return when (panel.templateType) {
            InfoTemplateType.MovieContinuity -> listOf(
                "日期" to panel.date,
                "角色" to panel.primary,
                "场次" to panel.secondary,
                "备注" to panel.note
            )
            InfoTemplateType.PhotoNote -> listOf(
                "日期" to panel.date,
                "标题" to panel.primary,
                "地点" to panel.secondary,
                "备注" to panel.note
            )
            InfoTemplateType.WorkRecord -> listOf(
                "日期" to panel.date,
                "项目" to panel.primary,
                "事项" to panel.secondary,
                "备注" to panel.note
            )
            InfoTemplateType.SiteRecord -> listOf(
                "日期" to panel.date,
                "项目" to panel.primary,
                "位置" to panel.secondary,
                "说明" to panel.note
            )
            InfoTemplateType.SimpleTitle -> listOf(
                "标题" to panel.primary,
                "说明" to panel.note
            )
            InfoTemplateType.Custom -> listOf(
                "日期" to panel.date,
                "标题" to panel.primary,
                "信息" to panel.secondary,
                "备注" to panel.note
            )
        }
    }

    private fun exportInfoDate(panel: InfoPanelLayer): String = "日期：${panel.date}"

    /** DEV3 FIX1: exporter cannot access EditorScreen.kt private helper; keep field labels consistent. */
    private fun infoTemplateFieldLabels(type: InfoTemplateType): Triple<String, String, String> = when (type) {
        InfoTemplateType.MovieContinuity -> Triple("角色", "场次", "描述/备注")
        InfoTemplateType.PhotoNote -> Triple("标题", "地点", "备注")
        InfoTemplateType.WorkRecord -> Triple("项目", "事项", "备注")
        InfoTemplateType.SiteRecord -> Triple("项目", "位置", "说明")
        InfoTemplateType.SimpleTitle -> Triple("标题", "副标题", "说明")
        InfoTemplateType.Custom -> Triple("标题", "信息", "备注")
    }

    private fun infoTitle(panel: InfoPanelLayer): String = when (panel.templateType) {
        InfoTemplateType.MovieContinuity -> "影视接戏照"
        InfoTemplateType.PhotoNote -> "照片说明"
        InfoTemplateType.WorkRecord -> "工作记录"
        InfoTemplateType.SiteRecord -> "工程 / 现场"
        InfoTemplateType.SimpleTitle -> "简洁标题"
        InfoTemplateType.Custom -> panel.customTemplateName.ifBlank { "自定义模板" }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isBlank()) return listOf("")
        val out = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            if (paragraph.isEmpty()) {
                out += ""
            } else {
                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxWidth.toFloat(), null).coerceAtLeast(1)
                    out += remaining.substring(0, count)
                    remaining = remaining.substring(count)
                }
            }
        }
        return out
    }

    private fun decodeBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val longest = max(width, height)
                if (longest > maxDimension) {
                    val ratio = maxDimension.toFloat() / longest.toFloat()
                    decoder.setTargetSize(
                        (width * ratio).roundToInt().coerceAtLeast(1),
                        (height * ratio).roundToInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String, format: ExportImageFormat, jpgQuality: Int): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, if(format==ExportImageFormat.PNG) "image/png" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/图工坊")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: error("无法创建导出文件")
        try {
            (resolver.openOutputStream(uri) ?: error("无法写入导出文件")).use { stream ->
                if (format == ExportImageFormat.PNG) {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) error("PNG导出失败")
                } else {
                    val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    try {
                        val outCanvas = Canvas(opaque)
                        outCanvas.drawColor(Color.WHITE)
                        outCanvas.drawBitmap(bitmap,0f,0f,null)
                        if (!opaque.compress(Bitmap.CompressFormat.JPEG, jpgQuality, stream)) error("JPG导出失败")
                    } finally {
                        opaque.recycle()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (t: Throwable) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) { }
            throw t
        }
    }
}
