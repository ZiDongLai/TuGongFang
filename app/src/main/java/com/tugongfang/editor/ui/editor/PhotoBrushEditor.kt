package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.tugongfang.editor.editor.image.PhotoEffectsPreviewRenderer
import com.tugongfang.editor.editor.image.rememberImagePreview
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.PhotoPoint
import com.tugongfang.editor.model.layer.PhotoStroke
import com.tugongfang.editor.model.layer.PhotoStrokeKind
import com.tugongfang.editor.model.layer.PhotoStrokePointSampler
import com.tugongfang.editor.model.layer.RemovedPhotoStroke
import com.tugongfang.editor.model.layer.undoLatestOf
import com.tugongfang.editor.model.layer.redoRemoved
import com.tugongfang.editor.ui.design.TgfTokens
import com.tugongfang.editor.ui.design.tgfToolSurface
import kotlin.math.roundToInt

/** Focused photo-local brush, deliberately separated from the collage drag gestures.
 * Points are normalized in the ORIGINAL photograph, so crop, pan and rotation can change
 * later without the marks sliding to a different cell or affecting another photo. */
@Composable
internal fun PhotoBrushEditor(
    layer: ImageLayer,
    kind: PhotoStrokeKind,
    onUpdate: (ImageLayer) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val original by rememberImagePreview(context, layer.uriString, maxDimension = 1400)
    val effected by produceState<ImageBitmap?>(original, original, layer.photoStrokes) {
        // Keep the previous rendered frame while the next brush stroke is composited:
        // briefly showing the untouched photo caused an avoidable flash after each stroke.
        val sourcePhoto = original // Stable local value: delegated state cannot be smart-cast.
        if (sourcePhoto == null || layer.photoStrokes.isEmpty()) value = sourcePhoto
        else value = PhotoEffectsPreviewRenderer.render(sourcePhoto, layer.photoStrokes)
    }
    var isEraser by remember(layer.id, kind) { mutableStateOf(false) }
    var brushSize by remember(kind) { mutableFloatStateOf(if (kind == PhotoStrokeKind.Mosaic) .08f else .027f) }
    var blockSize by remember { mutableFloatStateOf(.025f) }
    var color by remember { mutableIntStateOf(0xFF3A78FF.toInt()) }
    var draft by remember { mutableStateOf<List<PhotoPoint>>(emptyList()) }
    var brushCursor by remember { mutableStateOf<PhotoPoint?>(null) }
    val sampleStepPx = with(LocalDensity.current) { 1.4.dp.toPx() }
    // Scoped to this photo/tool session. A new stroke clears the redo chain.
    var redoStack by remember(layer.id, kind) {
        mutableStateOf<List<RemovedPhotoStroke>>(emptyList())
    }
    val latestLayer by rememberUpdatedState(layer)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val currentPhoto = effected ?: original
    Dialog(onDismissRequest=onDone) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.91f)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(TgfTokens.GlassTop, TgfTokens.GlassBottom)))
                .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(20.dp))
                .padding(horizontal=12.dp, vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().height(49.dp), verticalAlignment=Alignment.CenterVertically) {
                Text(if(kind==PhotoStrokeKind.Mosaic) "照片 · 马赛克" else "照片 · 涂鸦",
                    fontWeight=FontWeight.Bold, fontSize=16.sp, color=TgfTokens.Ink,
                    modifier=Modifier.weight(1f))
                EditorDoneButton(onDone,compact=true)
            }
            Text("在当前照片上涂抹；效果只属于这张照片",fontSize=11.sp,
                color=TgfTokens.InkSecondary)
            Spacer(Modifier.height(8.dp))
            // Toggle draw/erase inside the existing photo tool. Each eraser gesture
            // is a non-destructive history action for this photo and brush kind.
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(false to "绘制", true to "橡皮擦").forEach { (eraseMode, label) ->
                    val selected = isEraser == eraseMode
                    Box(Modifier.weight(1f).heightIn(min=42.dp)
                        .tgfToolSurface(selected, RoundedCornerShape(10.dp))
                        .clickable { isEraser = eraseMode },
                        contentAlignment=Alignment.Center) {
                        Text(label, fontSize=13.sp,
                            fontWeight=if(selected) FontWeight.Bold else FontWeight.Medium,
                            color=if(selected) TgfTokens.SelectionInk else TgfTokens.InkSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            var viewport by remember { mutableStateOf(IntSize.Zero) }
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))
                .background(if (TgfTokens.isDark) Color(0xFF12192B) else Color(0xFFE1E7F0))) {
                if (currentPhoto == null) {
                    Text("正在读取照片…",Modifier.align(Alignment.Center),fontSize=12.sp, color=TgfTokens.InkSecondary)
                } else {
                    val img = currentPhoto!!
                    val scaled = remember(viewport,img.width,img.height) {
                        val vw=viewport.width.toFloat().coerceAtLeast(1f)
                        val vh=viewport.height.toFloat().coerceAtLeast(1f)
                        val scale=minOf(vw/img.width, vh/img.height)
                        val w=(img.width*scale).coerceAtLeast(1f)
                        val h=(img.height*scale).coerceAtLeast(1f)
                        floatArrayOf((vw-w)/2f,(vh-h)/2f,w,h)
                    }
                    fun positionToPoint(position: Offset): PhotoPoint? {
                        if (position.x < scaled[0] || position.x > scaled[0] + scaled[2] ||
                            position.y < scaled[1] || position.y > scaled[1] + scaled[3]) return null
                        return clippedPoint(position, scaled[0], scaled[1], scaled[2], scaled[3])
                    }
                    Canvas(Modifier.fillMaxSize().onSizeChanged { viewport=it }
                        .pointerInput(layer.id, img.width, img.height, kind, brushSize, blockSize, color, isEraser, viewport) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPoint = positionToPoint(down.position)
                                val collector = PhotoStrokePointSampler(
                                    scaled[2], scaled[3], sampleStepPx
                                )
                                if (startPoint != null) collector.add(startPoint)
                                draft = collector.snapshot()
                                brushCursor = startPoint
                                var cancelled = false
                                do {
                                    val event = awaitPointerEvent()
                                    // A second finger cancels this stroke; no accidental bridge.
                                    if (event.changes.count { it.pressed } > 1) {
                                        cancelled = true
                                        break
                                    }
                                    val touch = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (startPoint != null) {
                                        val next = clippedPoint(touch.position,
                                            scaled[0], scaled[1], scaled[2], scaled[3])
                                        brushCursor = next
                                        if (collector.add(next, penUp = !touch.pressed)) {
                                            draft = collector.snapshot()
                                        }
                                    }
                                    touch.consume()
                                } while (touch.pressed)
                                if (!cancelled && collector.size > 0) {
                                    val stroke = PhotoStroke(kind, collector.snapshot(), brushSize, blockSize, color, isEraser)
                                    val current = latestLayer
                                    redoStack = emptyList()
                                    latestUpdate(current.copy(photoStrokes = (current.photoStrokes + stroke).takeLast(1200)))
                                }
                                brushCursor = null
                                draft = emptyList()
                            }
                        }) {
                        drawImage(img,dstOffset=IntOffset(scaled[0].roundToInt(),scaled[1].roundToInt()),
                            dstSize=IntSize(scaled[2].roundToInt().coerceAtLeast(1),scaled[3].roundToInt().coerceAtLeast(1)),
                            filterQuality=FilterQuality.High)
                        if(draft.isNotEmpty()) {
                            val line=Path().apply { draft.forEachIndexed { i,pt ->
                                val x=scaled[0]+pt.x*scaled[2]
                                val y=scaled[1]+pt.y*scaled[3]
                                if(i==0) moveTo(x,y) else lineTo(x,y)
                            } }
                            val minEdge=minOf(scaled[2],scaled[3]);val rad=brushSize*minEdge/2
                            val mark=if(isEraser) Color(0xFFF19B32).copy(alpha=.50f)
                                else if(kind==PhotoStrokeKind.Mosaic) Color.White.copy(alpha=.58f) else Color(color)
                            if(draft.size==1) drawCircle(mark,rad,Offset(scaled[0]+draft[0].x*scaled[2],
                                scaled[1]+draft[0].y*scaled[3]))
                            else drawPath(line,mark,style=Stroke(width=rad*2,cap=StrokeCap.Round))
                        }
                        // Actual brush footprint, independent of the sampled stroke path.
                        // A light/dark double ring stays visible over photos of either tone.
                        brushCursor?.let { pt ->
                            val center = Offset(scaled[0] + pt.x * scaled[2],
                                scaled[1] + pt.y * scaled[3])
                            val radius = (brushSize * minOf(scaled[2], scaled[3]) / 2f).coerceAtLeast(2f)
                            drawCircle(Color.Black.copy(alpha = .65f), radius = radius,
                                center = center, style = Stroke(width = 2.7.dp.toPx()))
                            drawCircle(if(isEraser) Color(0xFFFFC44A) else Color.White.copy(alpha = .94f), radius = radius,
                                center = center, style = Stroke(width = 1.1.dp.toPx()))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("笔刷",fontSize=12.sp,color=TgfTokens.Ink)
                Slider(value=brushSize,onValueChange={brushSize=it},valueRange=.012f.. .24f,
                    modifier=Modifier.weight(1f))
                Text("${(brushSize*100).roundToInt()}%",fontSize=11.sp,color=TgfTokens.Ink)
            }
            if(kind==PhotoStrokeKind.Mosaic && !isEraser) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("像素",fontSize=12.sp,color=TgfTokens.Ink)
                    Slider(value=blockSize,onValueChange={blockSize=it},valueRange=.006f.. .075f,
                        modifier=Modifier.weight(1f))
                    Text("${(blockSize*100).roundToInt()}%",fontSize=11.sp,color=TgfTokens.Ink)
                }
            } else if (!isEraser) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    listOf(0xFF3A78FF.toInt(),0xFFE94159.toInt(),0xFFFFC52E.toInt(),
                        0xFF151B29.toInt(),0xFFFFFFFF.toInt()).forEach { argb ->
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(argb))
                            .border(if(color==argb) 3.dp else 1.dp,
                                if(color==argb) TgfTokens.Blue else Color.Gray,CircleShape)
                            .clickable { color=argb })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                val hasCurrentStroke = layer.photoStrokes.any { it.kind == kind }
                Text("绘制 ${layer.photoStrokes.count { it.kind == kind && !it.isEraser }} · 擦除 ${layer.photoStrokes.count { it.kind == kind && it.isEraser }}",
                    fontSize = 11.sp, color = TgfTokens.InkSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("撤销", modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = hasCurrentStroke) {
                            val current = latestLayer
                            val undo = current.photoStrokes.undoLatestOf(kind)
                            undo.removed?.let { removed ->
                                redoStack = redoStack + removed
                                latestUpdate(current.copy(photoStrokes = undo.remaining))
                            }
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        color = if (hasCurrentStroke) TgfTokens.Blue else TgfTokens.InkSecondary)
                    val canRedo = redoStack.isNotEmpty()
                    Text("重做", modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canRedo) {
                            val current = latestLayer
                            val removed = redoStack.lastOrNull()
                            if (removed != null) {
                                redoStack = redoStack.dropLast(1)
                                latestUpdate(current.copy(photoStrokes = current.photoStrokes.redoRemoved(removed)))
                            }
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        color = if (canRedo) TgfTokens.Blue else TgfTokens.InkSecondary)
                }
            }
        }
    }
}

/** Clamp an in-progress stroke to the PHOTO rectangle, not the surrounding dialog. */
private fun clippedPoint(position: Offset, left: Float, top: Float, width: Float, height: Float): PhotoPoint =
    PhotoPoint(((position.x - left) / width.coerceAtLeast(1f)).coerceIn(0f, 1f),
        ((position.y - top) / height.coerceAtLeast(1f)).coerceIn(0f, 1f))
