package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.verticalScroll
import com.tugongfang.editor.editor.image.PhotoEffectsPreviewRenderer
import com.tugongfang.editor.editor.image.prepareCollagePreview
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.editor.image.rememberImagePreview
import com.tugongfang.editor.model.collage.*
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.ImageToneMatrix
import com.tugongfang.editor.model.layer.ImageToneProcessor
import com.tugongfang.editor.model.layer.ExtendedToneCurve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.components.TgfIcon
import com.tugongfang.editor.ui.components.TgfIconType
import com.tugongfang.editor.ui.design.TgfTokens
import com.tugongfang.editor.ui.design.tgfToolSurface

/** 一个页面内编辑拼图：使用与导出共用的 CollageProject.rects()。 */
@Composable
internal fun CollageCanvas(
    collage: CollageProject,
    images: List<ImageLayer>,
    frame: CanvasFrame,
    onChange: (CollageProject) -> Unit,
    onAddPhotos: () -> Unit,
    onReplaceSlot: (Int) -> Unit,
    selectedSlotIndex: Int?,
    onSelectSlot: (Int?) -> Unit
) {
    val project=collage.normalized()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
        val width= minOf(maxWidth, maxHeight*project.aspect.ratio)
        val height=width/project.aspect.ratio
        Box(
            Modifier.width(width).height(height)
                .shadow(3.dp, RectangleShape),
            contentAlignment=Alignment.Center
        ) {
            val rectangles=project.rects()
            rectangles.forEachIndexed { index, r ->
                val slot=project.slots[index]
                val layer=images.firstOrNull { it.id==slot.imageId && it.visible }
                CollageTile(
                    modifier=Modifier.offset(x=width*r.left, y=height*r.top)
                        .width(width*r.width).height(height*r.height)
                        .align(Alignment.TopStart),
                    layer=layer, slot=slot, index=index,
                    selected=index==selectedSlotIndex,
                    radius=if(frame.enabled && frame.scope==CanvasFrameScope.Photos &&
                        frame.corner==CanvasFrameCorner.Square) 0.dp else width*project.cornerRatio,
                    onSelect={ onChange(project.copy(activeSlot=index)); onSelectSlot(index) },
                    onUpdate={ changed -> onChange(project.changeSlot(index,changed).copy(activeSlot=index)) },
                    onAdd={onReplaceSlot(index)},  // 空格一点击就打开相册选一张
                    onReplace={onReplaceSlot(index)},
                    onDelete={
                        onChange(project.clearSlot(index).copy(activeSlot=index))
                        onSelectSlot(null)
                    }
                )
            }
            if (frame.enabled && frame.scope == CanvasFrameScope.Photos) {
                Canvas(Modifier.matchParentSize()) {
                    val thickness=(size.minDimension*frame.normalized().widthRatio).coerceAtLeast(1f)
                    rectangles.forEach { r ->
                        val left=r.left*size.width; val top=r.top*size.height
                        val w=r.width*size.width; val h=r.height*size.height
                        val rad=if(frame.corner==CanvasFrameCorner.Square) 0f
                            else (project.cornerRatio*size.width).coerceAtMost(minOf(w,h)/3f)
                        drawRoundRect(Color(frame.colorArgb),
                            topLeft=Offset(left+thickness/2f,top+thickness/2f),
                            size=androidx.compose.ui.geometry.Size((w-thickness).coerceAtLeast(1f),
                                (h-thickness).coerceAtLeast(1f)),
                            cornerRadius=androidx.compose.ui.geometry.CornerRadius(
                                (rad-thickness/2f).coerceAtLeast(0f)),
                            style=Stroke(thickness))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageTile(
    modifier: Modifier,
    layer: ImageLayer?,
    slot: CollageSlot,
    index: Int,
    selected: Boolean,
    radius: Dp,
    onSelect: () -> Unit,
    onUpdate: (CollageSlot) -> Unit,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    val context= LocalContext.current
    val sourceBitmap by rememberImagePreview(context,layer?.uriString,maxDimension=1200)
    val effected by produceState<ImageBitmap?>(sourceBitmap, sourceBitmap, layer?.photoStrokes) {
        value = sourceBitmap
        if (sourceBitmap != null && layer != null && layer.photoStrokes.isNotEmpty()) {
            value = PhotoEffectsPreviewRenderer.render(sourceBitmap!!, layer.photoStrokes)
        }
    }
    val bitmap = remember(effected,layer?.cropLeft,layer?.cropTop,layer?.cropRight,layer?.cropBottom,
        layer?.flipHorizontal,layer?.flipVertical,layer?.rotation) {
        if (effected != null && layer != null) {
            prepareCollagePreview(effected!!.asAndroidBitmap(),layer).asImageBitmap()
        } else null
    }
    val toned by produceState<ImageBitmap?>(
        initialValue = bitmap, bitmap, layer?.temperature, layer?.tint, layer?.highlights, layer?.shadows
    ) {
        val l = layer
        val b = bitmap
        if (l == null || b == null || ExtendedToneCurve.isNeutral(
                l.temperature, l.tint, l.highlights, l.shadows)) {
            value = bitmap
        } else {
            // Do not blink back to unadjusted photo every time a slider moves.
            value = withContext(Dispatchers.Default) {
                ImageToneProcessor.applyAdvanced(b.asAndroidBitmap(),l).asImageBitmap()
            }
        }
    }
    val toneFilter = remember(layer?.brightness,layer?.contrast,layer?.saturation) {
        if (layer == null || ImageToneMatrix.isNeutral(layer.brightness,layer.contrast,layer.saturation)) null
        else ColorFilter.colorMatrix(ColorMatrix(ImageToneMatrix.make(
            layer.brightness,layer.contrast,layer.saturation)))
    }
    val latestSlot by rememberUpdatedState(slot)
    val latestUpdate by rememberUpdatedState(onUpdate)
    // v0.8.3: 不显示对齐辅助线；只对双指旋转/缩放做轻微防抖。
    val shape=RoundedCornerShape(radius)
    BoxWithConstraints(
        modifier.clip(shape)
            .background(Color.Transparent)
            // Old always-on white stroke leaked through colored frames at round corners.
            .then(if (selected) Modifier.border(2.dp,TgfTokens.Blue,shape) else Modifier),
        contentAlignment=Alignment.Center
    ) {
        if (toned != null) {
            // 格位始终裁剪到圆角区域。与正式导出共用 collageImageSize，保证 WYSIWYG。
            Canvas(Modifier.fillMaxSize()) {
                val result = collageImageSize(toned!!.width.toFloat(),toned!!.height.toFloat(),
                    size.width,size.height,slot)
                val centerX = size.width/2f + slot.panX*size.width
                val centerY = size.height/2f + slot.panY*size.height
                rotate(slot.rotation,pivot=Offset(centerX,centerY)) {
                    drawImage(
                        toned!!,
                        srcOffset=IntOffset.Zero,
                        srcSize=IntSize(toned!!.width,toned!!.height),
                        dstOffset=IntOffset((centerX-result.width/2f).toInt(),(centerY-result.height/2f).toInt()),
                        dstSize=IntSize(result.width.toInt().coerceAtLeast(1),result.height.toInt().coerceAtLeast(1)),
                        filterQuality=FilterQuality.High,
                        colorFilter=toneFilter
                    )
                }
            }
            Box(Modifier.fillMaxSize()
                .pointerInput(layer?.id, index) {
                    // 手势按“按下→连续移动→松开”分组。单次手势保留原始值，
                    // 避免被吸附后的数值反复写回造成跳跃或无法离开吸附点。
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val session = CollageGestureSession(
                            initial = latestSlot,
                            slotWidthPx = size.width.toFloat(),
                            slotHeightPx = size.height.toFloat()
                        )
                        var passedSlop = false
                        var panBeforeSlop = Offset.Zero
                        var zoomBeforeSlop = 1f
                        var angleBeforeSlop = 0f
                        val touchSlop = viewConfiguration.touchSlop
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            var justPassedSlop = false
                            if (!passedSlop) {
                                panBeforeSlop += pan
                                zoomBeforeSlop *= zoom
                                angleBeforeSlop += rotation
                                val fingerRadius = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = kotlin.math.abs(zoomBeforeSlop - 1f) * fingerRadius
                                val rotationMotion = kotlin.math.abs(angleBeforeSlop) * (kotlin.math.PI.toFloat() / 180f) * fingerRadius
                                if (panBeforeSlop.getDistance() > touchSlop ||
                                    zoomMotion > touchSlop || rotationMotion > touchSlop) {
                                    passedSlop = true
                                    justPassedSlop = true
                                }
                            }
                            if (passedSlop && event.changes.none { it.isConsumed }) {
                                // 进入拖动的首帧带上触摸斜率内的增量，避免缓慢操作丢失位移。
                                val effectivePan = if (justPassedSlop) panBeforeSlop else pan
                                val effectiveZoom = if (justPassedSlop) zoomBeforeSlop else zoom
                                val effectiveRotation = if (justPassedSlop) angleBeforeSlop else rotation
                                val result = session.step(effectivePan.x, effectivePan.y, effectiveZoom, effectiveRotation)
                                latestUpdate(result.slot)
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (passedSlop) latestUpdate(session.finish())
                    }
                }
                .pointerInput(layer?.id) {
                    detectTapGestures(onTap={onSelect()},onDoubleTap={onReplace()})
                }
            )
        } else {
            Column(Modifier.fillMaxSize().clickable {onSelect();onAdd()},
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center) {
                Text("＋",fontSize=25.sp,color=TgfTokens.Blue)
                Text("照片 ${index+1}",fontSize=10.sp,color=TgfTokens.InkSecondary)
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().border(2.dp,TgfTokens.Blue,shape))
        }
    }
}

/**
 * v0.9.1 FIX3：只重排拼图控制面板，不改变拼图格位、手势、数据或导出。
 * 模板模式保持 150dp，自由模式保持 92dp。
 * 第一行：模式 + 背景 + 退出 + 完成；第二行：添加照片 + 分类；第三行：当前分类。
 */
@Composable
internal fun CollagePanel(
    modifier: Modifier = Modifier,
    project: CollageProject,
    onUpdate: (CollageProject) -> Unit,
    onSelectAspect: (CollageAspect) -> Unit,
    selectedSlotIndex: Int?,
    onClearSlotSelection: () -> Unit,
    onAddPhotos: () -> Unit,
    onReplaceSlot: (Int) -> Unit,
    onCropTemplateSlot: (Int) -> Unit,
    onToneTemplateSlot: (Int) -> Unit,
    onBrushTemplateSlot: (Int, com.tugongfang.editor.model.layer.PhotoStrokeKind) -> Unit,
    requestedSection: String,
    onCropFreeImage: () -> Unit,
    onToneFreeImage: () -> Unit,
    onBrushFreeImage: (com.tugongfang.editor.model.layer.PhotoStrokeKind) -> Unit,
    onUpdateFreeImage: (ImageLayer) -> Unit,
    photoLayers: List<ImageLayer>,
    availablePhotoIds: List<String>,
    background: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
    onPickBackgroundImage: () -> Unit,
    canvasScene: CanvasScene,
    onUpdateScene: (CanvasScene) -> Unit,
    selectedFreeImage: ImageLayer?,
    onReplaceFreeImage: () -> Unit,
    onResetFreeImage: () -> Unit,
    onDeleteFreeImage: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDisable: () -> Unit,
    onClose: () -> Unit
) {
    val current = project.normalized()
    var rotationMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(current.mode, selectedSlotIndex, selectedFreeImage?.id) {
        rotationMenuOpen = false
    }
    val showDecoration = requestedSection in listOf("背景", "边框", "间距") && selectedSlotIndex == null
    var decorationPage by remember(requestedSection) { mutableStateOf(requestedSection) }
    val showBackground = showDecoration && decorationPage == "背景"
    val showAspect = requestedSection == "比例" && selectedSlotIndex == null
    // Panel-only state. A template-choice expansion must never enter saved project/history.
    val selectedPhotoCount = current.slots.count { it.imageId != null }.coerceAtLeast(1)
    var showLayoutGrid by remember(selectedPhotoCount) { mutableStateOf(false) }
    val gridExtra = if (current.mode == CollageMode.Template && showLayoutGrid) 57.dp else 0.dp
    val panelHeight = when {
        showDecoration && decorationPage != "背景" -> if(expanded) 272.dp else 220.dp
        showBackground && background.kind in setOf(CanvasBackgroundKind.Transparent, CanvasBackgroundKind.Solid) ->
            if (expanded) 225.dp else 174.dp
        showBackground -> if (expanded) 305.dp else 235.dp
        selectedSlotIndex != null -> 121.dp
        showAspect -> 103.dp
        expanded -> 201.dp + gridExtra
        else -> 151.dp + gridExtra
    }

    // DEV21: header hit targets require 44dp (was 26dp); preserve the tool area's height.
    GlassCard(
        modifier = modifier.fillMaxWidth().height(panelHeight + 18.dp),
        radius = 22.dp,
        elevation = 8.dp,
        padding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // DEV21: keep Done/Expand pinned; mode and title controls may scroll
            // horizontally instead of clipping on narrower screens.
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (showDecoration) "拼图 · ${decorationPage}" else "拼图",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TgfTokens.Ink,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!showDecoration && selectedSlotIndex == null) {
                        listOf(CollageMode.Template to "模板", CollageMode.Free to "自由")
                            .forEach { (mode, title) ->
                                val active = current.mode == mode
                                Box(
                                    Modifier.width(42.dp).height(27.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised)
                                        .border(
                                            if (active) 1.dp else .6.dp,
                                            if (active) TgfTokens.Blue.copy(alpha = .66f) else TgfTokens.SurfaceOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (mode == CollageMode.Template) {
                                                onUpdate(current.copy(mode = mode)
                                                    .assignNewImages(availablePhotoIds))
                                            } else {
                                                onUpdate(current.copy(mode = mode))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        title, fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) TgfTokens.Blue else TgfTokens.InkSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                    }
                    if (!showDecoration && selectedSlotIndex != null && current.mode == CollageMode.Template) {
                        Text("⌄ 收起照片", fontSize = 11.sp, color = TgfTokens.Blue,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(TgfTokens.SurfaceSelected)
                                .clickable(onClick = onClearSlotSelection)
                                .padding(horizontal = 7.dp, vertical = 7.dp))
                    }
                }
                SecondaryPanelExpandKey(expanded, onToggleExpanded)
                if (!showDecoration && !showAspect && selectedSlotIndex == null) {
                    Box(Modifier.size(width = 38.dp, height = 44.dp)
                        .clickable(onClick = onDisable), contentAlignment = Alignment.Center) {
                        Text("退出", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                    }
                }
                EditorDoneButton(onClick = onClose, compact = true)
            }

            if (showDecoration) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf("背景","边框","间距").forEach { tab ->
                            SmallChoice(tab,tab==decorationPage) { decorationPage=tab }
                        }
                    }
                    when(decorationPage) {
                        "背景" -> CanvasBackgroundControls(background, onBackgroundChange, onPickBackgroundImage)
                        "边框" -> FrameStyleControls(canvasScene.frame) {
                            onUpdateScene(canvasScene.copy(frame=it))
                        }
                        "间距" -> SpacingControls(
                            photoGap=if(current.mode==CollageMode.Template) current.gapRatio else null,
                            outer=if(current.mode==CollageMode.Template) current.outerInsetRatio
                                  else canvasScene.photoInsetRatio,
                            onPhotoGap=if(current.mode==CollageMode.Template) {
                                { next:Float -> onUpdate(current.copy(gapRatio=next)) }
                            } else null,
                            onOuter={ next ->
                                if(current.mode==CollageMode.Template) onUpdate(current.copy(outerInsetRatio=next))
                                else onUpdateScene(canvasScene.copy(photoInsetRatio=next))
                            }
                        )
                    }
                }
            } else if (showAspect) {
                Row(Modifier.fillMaxWidth().height(38.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if (current.mode == CollageMode.Template) "拼图比例" else "画布比例",
                        fontSize=11.sp,color=TgfTokens.InkSecondary)
                    Spacer(Modifier.width(8.dp))
                    CollageAspectOptions(current.aspect,onSelectAspect,Modifier.weight(1f))
                }
            } else if (current.mode == CollageMode.Template) {
                val chosenIndex = selectedSlotIndex?.takeIf { it in current.slots.indices }
                if (chosenIndex != null) {
                    // 格位专属功能：裁切 / 替换 / 调色 / 旋转 / 填充 / 重置。
                    val active = current.slots[chosenIndex]
                    Row(
                        Modifier.fillMaxWidth().height(65.dp).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        val templatePhoto = photoLayers.firstOrNull { it.id == active.imageId }
                        val actions: List<Triple<String,TgfIconType,()->Unit>> = if (rotationMenuOpen) listOf(
                            Triple("返回",TgfIconType.RotateRight,{ rotationMenuOpen = false }),
                            Triple("左转90°",TgfIconType.RotateLeft,{
                                onUpdate(current.changeSlot(chosenIndex,active.copy(
                                    rotation = normalizedCollageRotation(active.rotation - 90f))))
                            }),
                            Triple("右转90°",TgfIconType.RotateRight,{
                                onUpdate(current.changeSlot(chosenIndex,active.copy(
                                    rotation = normalizedCollageRotation(active.rotation + 90f))))
                            }),
                            Triple("水平翻转",TgfIconType.FlipHorizontal,{
                                templatePhoto?.let { onUpdateFreeImage(it.copy(flipHorizontal = !it.flipHorizontal)) }
                            }),
                            Triple("垂直翻转",TgfIconType.FlipVertical,{
                                templatePhoto?.let { onUpdateFreeImage(it.copy(flipVertical = !it.flipVertical)) }
                            })
                        ) else listOf(
                            Triple("裁切",TgfIconType.Crop,{onCropTemplateSlot(chosenIndex)}),
                            Triple("替换",TgfIconType.Replace,{onReplaceSlot(chosenIndex)}),
                            Triple("调色",TgfIconType.Style,{onToneTemplateSlot(chosenIndex)}),
                            Triple("旋转",TgfIconType.RotateRight,{ rotationMenuOpen = true }),
                            Triple(if (active.fitMode == CollageFitMode.Fill) "填充" else "适应",
                                TgfIconType.Photo,{
                                val next = if (active.fitMode == CollageFitMode.Fill)
                                    CollageFitMode.Fit else CollageFitMode.Fill
                                // 切换后重新居中、恢复基础缩放；图片裁切/色调/角度不变。
                                onUpdate(current.changeSlot(chosenIndex,active.copy(
                                    fitMode=next,zoom=1f,panX=0f,panY=0f)))
                            }),
                            Triple("重置",TgfIconType.Reset,{
                                onUpdate(current.changeSlot(chosenIndex,active.copy(
                                    rotation = 0f,zoom = 1f,panX = 0f,panY = 0f,
                                    fitMode = CollageFitMode.Fill)))
                            }),
                            Triple("涂鸦",TgfIconType.Doodle,{
                                onBrushTemplateSlot(chosenIndex,com.tugongfang.editor.model.layer.PhotoStrokeKind.Doodle)
                            }),
                            Triple("马赛克",TgfIconType.Mosaic,{
                                onBrushTemplateSlot(chosenIndex,com.tugongfang.editor.model.layer.PhotoStrokeKind.Mosaic)
                            })
                        )
                        actions.forEach { (name,icon,action) ->
                            Column(Modifier.width(55.dp).fillMaxHeight()
                                .tgfToolSurface(name == "填充" || name == "适应", RoundedCornerShape(9.dp))
                                .clickable(enabled = active.imageId != null, onClick = action),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                TgfIcon(icon,Modifier.size(25.dp),selected = name == "填充" || name == "适应")
                                Text(name,fontSize = 10.sp,
                                    fontWeight=if (name == "填充" || name == "适应") FontWeight.Bold else FontWeight.Normal,
                                    color=if (name == "填充" || name == "适应") TgfTokens.SelectionInk else TgfTokens.Ink)
                            }
                        }
                    }
                } else {
                    // 全局操作直接显示成品画布比例，模板列表紧随其后；省去二次点“比例”。
                    Row(
                        Modifier.fillMaxWidth().height(33.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(Modifier.width(90.dp).fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(TgfTokens.Blue, TgfTokens.Purple)))
                            .clickable(onClick = onAddPhotos), contentAlignment = Alignment.Center) {
                            Text("＋ 添加照片", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 1)
                        }
                        Text("${selectedPhotoCount}张照片 · ${templatesForPhotoCount(selectedPhotoCount).size}款布局",
                            modifier = Modifier.weight(1f), fontSize = 10.sp,
                            maxLines = 1, softWrap = false, color = TgfTokens.InkSecondary)
                        Box(
                            Modifier.width(65.dp).height(29.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showLayoutGrid) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised)
                                .border(.8.dp,
                                    if (showLayoutGrid) TgfTokens.Blue else TgfTokens.SurfaceOutline,
                                    RoundedCornerShape(8.dp))
                                .clickable { showLayoutGrid = !showLayoutGrid },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (showLayoutGrid) "收起模板⌃" else "展开模板⌄",
                                fontSize = 10.sp, maxLines = 1, softWrap = false,
                                color = if (showLayoutGrid) TgfTokens.Blue else TgfTokens.Ink)
                        }
                    }
                    val choices = remember(selectedPhotoCount) {
                        templatesForPhotoCount(selectedPhotoCount)
                    }
                    if (showLayoutGrid) {
                        // All 1..8 choices fit into up to two rows, without hiding the canvas
                        // or requiring an additional scrolling surface on 9:16 phone screens.
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().height(112.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            userScrollEnabled = false
                        ) {
                            items(choices.size) { index ->
                                val template = choices[index]
                                CollageTemplateChoice(
                                    template, template == current.template,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onUpdate(current.changeTemplate(template)
                                            .assignNewImages(availablePhotoIds))
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().height(57.dp)
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            choices.forEach { template ->
                                CollageTemplateChoice(
                                    template, template == current.template,
                                    modifier = Modifier.width(63.dp),
                                    onClick = {
                                        onUpdate(current.changeTemplate(template)
                                            .assignNewImages(availablePhotoIds))
                                    }
                                )
                            }
                        }
                    }
                    if (expanded) {
                        // 只在手动展开时显示低频设置，不抢占“比例、模板”主操作位置。
                        Row(Modifier.fillMaxWidth().height(47.dp).horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 间距/留边统一在独立「间距」页中调整，避免同一设置出现两个入口。
                            Text("照片圆角", fontSize = 11.sp, color = TgfTokens.InkSecondary)
                            CompactControl("−") { onUpdate(current.copy(
                                cornerRatio = (current.cornerRatio - .01f).coerceAtLeast(0f))) }
                            Text("${(current.cornerRatio * 100).toInt()}%", fontSize = 11.sp, color = TgfTokens.Ink)
                            CompactControl("＋") { onUpdate(current.copy(
                                cornerRatio = (current.cornerRatio + .01f).coerceAtMost(.08f))) }
                        }
                    }
                }
            } else {
                val selected = selectedFreeImage?.takeUnless { it.locked }
                val previewContext = LocalContext.current
                val selectedBitmap by rememberImagePreview(previewContext,selected?.uriString,maxDimension=600)
                if (selected == null) {
                    Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.width(116.dp).height(37.dp).clip(RoundedCornerShape(9.dp))
                            .background(TgfTokens.Blue).clickable(onClick=onAddPhotos),
                            contentAlignment=Alignment.Center) {
                            Text("＋ 添加照片",fontSize=11.sp,color=Color.White)
                        }
                        Text("选中照片后进行单张编辑",fontSize=11.sp,color=TgfTokens.InkSecondary)
                    }
                } else {
                    Row(Modifier.fillMaxWidth().height(65.dp).horizontalScroll(rememberScrollState()),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        val actions: List<Triple<String,TgfIconType,()->Unit>> = if (rotationMenuOpen) listOf(
                            Triple("返回",TgfIconType.RotateRight,{ rotationMenuOpen = false }),
                            Triple("左转90°",TgfIconType.RotateLeft,{
                                onUpdateFreeImage(selected.copy(rotation=selected.rotation-90f))
                            }),
                            Triple("右转90°",TgfIconType.RotateRight,{
                                onUpdateFreeImage(selected.copy(rotation=selected.rotation+90f))
                            }),
                            Triple("水平翻转",TgfIconType.FlipHorizontal,{
                                onUpdateFreeImage(selected.copy(flipHorizontal=!selected.flipHorizontal))
                            }),
                            Triple("垂直翻转",TgfIconType.FlipVertical,{
                                onUpdateFreeImage(selected.copy(flipVertical=!selected.flipVertical))
                            })
                        ) else listOf(
                            Triple("裁切",TgfIconType.Crop,onCropFreeImage),
                            Triple("替换",TgfIconType.Replace,onReplaceFreeImage),
                            Triple("调色",TgfIconType.Style,onToneFreeImage),
                            Triple("旋转",TgfIconType.RotateRight,{ rotationMenuOpen = true }),
                            Triple(if (selected.freeFitMode == CollageFitMode.Fill) "填充" else "适应",
                                TgfIconType.Photo,{
                                val bitmap = selectedBitmap
                                if (bitmap != null) {
                                    val cropW = (selected.cropRight-selected.cropLeft).coerceAtLeast(.01f)
                                    val cropH = (selected.cropBottom-selected.cropTop).coerceAtLeast(.01f)
                                    val imageRatio = bitmap.width.toFloat()*cropW /
                                        (bitmap.height.toFloat()*cropH)
                                    val next = if (selected.freeFitMode == CollageFitMode.Fill)
                                        CollageFitMode.Fit else CollageFitMode.Fill
                                    val targetScale = freeCollageFitScale(
                                        imageRatio,current.aspect.ratio,selected.rotation,next)
                                    onUpdateFreeImage(selected.copy(
                                        scale=targetScale, offsetXRatio=0f,offsetYRatio=0f,
                                        freeFitMode=next))
                                }
                            }),
                            Triple("重置",TgfIconType.Reset,onResetFreeImage),
                            Triple("涂鸦",TgfIconType.Doodle,{
                                onBrushFreeImage(com.tugongfang.editor.model.layer.PhotoStrokeKind.Doodle)
                            }),
                            Triple("马赛克",TgfIconType.Mosaic,{
                                onBrushFreeImage(com.tugongfang.editor.model.layer.PhotoStrokeKind.Mosaic)
                            })
                        )
                        actions.forEach { (name,icon,action) ->
                            Column(Modifier.width(55.dp).height(62.dp)
                                .tgfToolSurface(name == "填充" || name == "适应", RoundedCornerShape(9.dp))
                                .clickable(onClick=action),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.Center) {
                                TgfIcon(icon,Modifier.size(25.dp),selected=name == "填充" || name == "适应")
                                Text(name,fontSize=10.sp,
                                    fontWeight=if (name == "填充" || name == "适应") FontWeight.Bold else FontWeight.Normal,
                                    color=if (name == "填充" || name == "适应") TgfTokens.SelectionInk else TgfTokens.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}


/** The compact strip and two-row grid share exactly the same tile thumbnails. */
@Composable
private fun CollageTemplateChoice(
    template: CollageTemplate,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.height(53.dp)
            .tgfToolSurface(active, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Canvas(Modifier.width(43.dp).height(26.dp)) {
            CollageProject(template = template, gapRatio = .035f).rects().forEach { rect ->
                drawRoundRect(
                    if (active) TgfTokens.Blue else TgfTokens.InkSecondary,
                    topLeft = Offset(rect.left*size.width,rect.top*size.height),
                    size = androidx.compose.ui.geometry.Size(rect.width*size.width,rect.height*size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = Stroke(1.6.dp.toPx())
                )
            }
        }
        Text(template.title, fontSize = 9.sp, maxLines = 1, softWrap = false,
            color = TgfTokens.Ink)
    }
}

@Composable
private fun CollageAspectOptions(
    current: CollageAspect,
    onSelect: (CollageAspect) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CollageAspect.entries.forEach { option ->
            val selected = option == current
            Box(Modifier.width(36.dp).height(27.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised)
                .border(if (selected) 1.dp else .5.dp,
                    if (selected) TgfTokens.Blue else TgfTokens.SurfaceOutline,
                    RoundedCornerShape(7.dp))
                .clickable { onSelect(option) },
                contentAlignment = Alignment.Center) {
                Text(option.label, fontSize = 10.sp, color = if (selected) TgfTokens.Blue else TgfTokens.InkSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1)
            }
        }
    }
}

@Composable
private fun CompactControl(text:String,onClick:()->Unit){
    Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
        .background(TgfTokens.SurfaceRaised).clickable(onClick=onClick),contentAlignment=Alignment.Center){
        Text(text,fontSize=13.sp,color=TgfTokens.Blue)
    }
}
