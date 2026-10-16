package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.editor.image.PhotoEffectsPreviewRenderer
import com.tugongfang.editor.model.layer.PhotoStrokeKind
import com.tugongfang.editor.editor.image.rememberImagePreview
import com.tugongfang.editor.model.collage.CollageProject
import com.tugongfang.editor.model.collage.CollageMode
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
import com.tugongfang.editor.model.collage.CollageTemplate
import com.tugongfang.editor.model.collage.CollageAspect
import com.tugongfang.editor.model.collage.collageForInitialPhotos
import com.tugongfang.editor.model.canvas.CanvasViewportScale
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.ImageToneMatrix
import com.tugongfang.editor.model.layer.ImageToneProcessor
import com.tugongfang.editor.model.layer.ExtendedToneCurve
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tugongfang.editor.model.layer.TextLayer
import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.preferredDockWidth
import com.tugongfang.editor.model.canvas.directPhotoDocumentAspect
import com.tugongfang.editor.model.layer.resolveInfoOverlayGeometry
import com.tugongfang.editor.model.layer.InfoTemplateType
import com.tugongfang.editor.model.layer.ShapeLayer
import com.tugongfang.editor.model.layer.ShapeType
import com.tugongfang.editor.model.layer.ShapePoint
import com.tugongfang.editor.model.layer.SymbolKind
import com.tugongfang.editor.model.layer.SymbolGeometry
import com.tugongfang.editor.model.layer.DoodleBrush
import com.tugongfang.editor.model.layer.DoodleErasePoint
import com.tugongfang.editor.model.layer.eraseDoodlePath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.components.TgfIcon
import com.tugongfang.editor.ui.components.TgfIconType
import com.tugongfang.editor.ui.design.TgfTokens
import com.tugongfang.editor.ui.design.tgfToolSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import java.time.LocalDate
import android.app.DatePickerDialog

private fun fontFamilyForText(id: Int): FontFamily = when (id) {
    1 -> FontFamily.Serif
    2 -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}


/** DEV21: text-only Done; visual badge stays compact while the hit target is at least 44dp. */
@Composable
internal fun EditorDoneButton(onClick: () -> Unit, compact: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(if (compact) 66.dp else 74.dp)
            .height(if (compact) 44.dp else 48.dp)
            .semantics { contentDescription = "完成" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(if (compact) 34.dp else 38.dp)
                .shadow(3.dp, shape)
                .background(Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)), shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            Text("完成", color = Color.White,
                fontSize = if (compact) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

private const val SNAP_ROTATION_MAJOR_DEG = 6f
private const val SNAP_ROTATION_MINOR_DEG = 2.4f
private const val MIN_CROP_SPAN = .08f
private val EDIT_CANVAS_EDGE_RED = Color(0xFFE45D65)

private data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun sanitize(): CropRect {
        var l = left.coerceIn(0f, 1f)
        var t = top.coerceIn(0f, 1f)
        var r = right.coerceIn(0f, 1f)
        var b = bottom.coerceIn(0f, 1f)
        if (r - l < MIN_CROP_SPAN) {
            val c = (l + r) / 2f
            l = (c - MIN_CROP_SPAN / 2f).coerceAtLeast(0f)
            r = (l + MIN_CROP_SPAN).coerceAtMost(1f)
            l = (r - MIN_CROP_SPAN).coerceAtLeast(0f)
        }
        if (b - t < MIN_CROP_SPAN) {
            val c = (t + b) / 2f
            t = (c - MIN_CROP_SPAN / 2f).coerceAtLeast(0f)
            b = (t + MIN_CROP_SPAN).coerceAtMost(1f)
            t = (b - MIN_CROP_SPAN).coerceAtLeast(0f)
        }
        return CropRect(l, t, r, b)
    }
}

private data class SnapFeedback(
    val verticalGuideRatio: Float? = null,
    val horizontalGuideRatio: Float? = null,
    val rotationAngle: Float? = null
)

private data class AxisSnap(
    val value: Float,
    val guideRatio: Float? = null
)

private enum class CropPanelPage { Crop, Rotate }
private enum class ShapePanelPage { Choose, Edit }
private enum class ShapeToolMode { Shapes, Doodle, Symbols }
private enum class DoodleTool { Pencil, Marker, Highlighter, Eraser }
private enum class DoodleEraseMode { Partial, Whole }
private enum class CustomColorMode { Wheel, RGB }

@Composable
fun EditorScreen(
    projectKey: String,
    layers: List<ImageLayer>,
    textLayers: List<TextLayer>,
    infoLayers: List<InfoPanelLayer>,
    shapeLayers: List<ShapeLayer>,
    collage: CollageProject?,
    canvasScene: CanvasScene,
    onUpdateScene: (CanvasScene) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onUpdateCollage: (CollageProject?)->Unit,
    onAddCollagePhotos: ()->Unit,
    onReselectCollagePhotos: ()->Unit,
    onReplaceCollageSlot: (Int)->Unit,
    selectedLayerId: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayer: (ImageLayer) -> Unit,
    onAddTextLayer: (TextLayer) -> Unit,
    onUpdateTextLayer: (TextLayer) -> Unit,
    onDuplicateTextLayer: (String) -> Unit,
    onDeleteTextLayer: (String) -> Unit,
    onToggleTextLock: (String) -> Unit,
    onToggleTextVisibility: (String) -> Unit,
    onAddInfoLayer: (InfoPanelLayer) -> Unit,
    onUpdateInfoLayer: (InfoPanelLayer) -> Unit,
    onDeleteInfoLayer: (String) -> Unit,
    onToggleInfoVisibility: (String) -> Unit,
    onAddShapeLayer: (ShapeLayer) -> Unit,
    onEraseDoodleStrokes: (Map<String, List<ShapeLayer>>) -> Unit,
    onUpdateShapeLayer: (ShapeLayer) -> Unit,
    onDuplicateShapeLayer: (String) -> Unit,
    onDeleteShapeLayer: (String) -> Unit,
    onToggleShapeLock: (String) -> Unit,
    onToggleShapeVisibility: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onReplacePhoto: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    // DEV23: focus editor for the selected photo, never a global drawing layer.
    var photoBrushTarget by remember(projectKey) { mutableStateOf<String?>(null) }
    var photoBrushKind by remember { mutableStateOf(PhotoStrokeKind.Mosaic) }
    fun openPhotoBrush(id: String?, kind: PhotoStrokeKind) {
        if (id != null && layers.any { it.id == id && it.visible && !it.locked }) {
            photoBrushTarget=id
            photoBrushKind=kind
        }
    }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showImagePanel by remember { mutableStateOf(false) }
    // The single-photo toolbar can open background or frame directly.
    var singlePhotoPanelPage by remember(projectKey) { mutableStateOf("调色") }
    var showCollagePanel by remember { mutableStateOf(false) }
    var collageRequestedSection by remember { mutableStateOf("拼图") }
    var returnToCollageAfterTone by remember { mutableStateOf(false) }
    // UI-only selection. It must not be stored in CollageProject or in project history.
    var selectedCollageSlotIndex by remember(projectKey) { mutableStateOf<Int?>(null) }
    var showCollageEntryChoice by remember { mutableStateOf(false) }
    // 仅等待首次相册回传。取消选择时不创建空格位，且不改变项目数据。
    var pendingInitialCollageSelection by remember { mutableStateOf(false) }
    var showShapePanel by remember { mutableStateOf(false) }
    // v0.9.8 FIX1: UI-only expansion state. Never saved in the project/canvas model.
    var secondaryPanelExpanded by remember { mutableStateOf(false) }
    // DEV12: 工具参数面板临时展开时仍保留二级菜单的左右滚动位置。
    val editorToolbarScroll = rememberScrollState()
    var drawingFreehand by remember { mutableStateOf(false) }
    var shapeToolMode by remember { mutableStateOf(ShapeToolMode.Shapes) }
    var doodleTool by remember { mutableStateOf(DoodleTool.Pencil) }
    var doodleEraseMode by remember { mutableStateOf(DoodleEraseMode.Partial) }
    var doodleEraserSize by remember { mutableFloatStateOf(16f) }
    var doodleColor by remember { mutableIntStateOf(0xFF3A78FF.toInt()) }
    var doodleStrokeWidth by remember { mutableFloatStateOf(8f) }
    var doodleOpacity by remember { mutableFloatStateOf(1f) }
    var doodleSessionId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var shapePanelPage by remember { mutableStateOf(ShapePanelPage.Choose) }
    var shapeTargetImageId by remember { mutableStateOf<String?>(null) }
    // 同一个项目整个编辑会话只初始化一次逻辑画布；重开不同项目才重新捕获视口。
    // 不能使用 collage、键盘或工具面板状态作为 remember 的 key。
    var referenceCanvasSize by remember(projectKey) { mutableStateOf(IntSize.Zero) }
    fun linkedShape(shape: ShapeLayer): ShapeLayer {
        val parent = layers.firstOrNull { it.id == shapeTargetImageId && it.visible }
            ?: layers.lastOrNull { it.visible }
            return shape.copy(parentImageId = parent?.id, attachmentCanvasAspect = canvasScene.aspectRatio.takeIf { it in .25f..4f }
            ?: referenceCanvasSize.takeIf { it.height > 0 }?.let { it.width.toFloat() / it.height } ?: .75f)
    }
    var showTextModePanel by remember { mutableStateOf(false) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var styleTextId by remember { mutableStateOf<String?>(null) }
    var showInfoTemplatePanel by remember { mutableStateOf(false) }
    var editingInfoId by remember { mutableStateOf<String?>(null) }
    // Only visible while a template is selected. No persistent menu-state in the project.
    var infoQuickSection by remember { mutableStateOf<String?>(null) }
    var infoEditScale by remember { mutableFloatStateOf(1f) }
    var cropLayerId by remember { mutableStateOf<String?>(null) }
    var cropDraft by remember { mutableStateOf<CropRect?>(null) }
    var cropPanelPage by remember { mutableStateOf(CropPanelPage.Crop) }
    // 裁切作为“选中照片”的功能。只记录返回哪个操作面板，不改变图层数据。
    var returnToImageAfterCrop by remember { mutableStateOf(false) }
    var returnToCollageAfterCrop by remember { mutableStateOf(false) }

    val cropLayer = layers.firstOrNull { it.id == cropLayerId }
    val editingText = textLayers.firstOrNull { it.id == editingTextId }
    val styleText = textLayers.firstOrNull { it.id == styleTextId }
    val selectedImage = layers.firstOrNull { it.id == selectedLayerId }
    // DEV8: 单图照片是文档主体，不再充当可删除/复制/变换的装饰图层。
    val singlePhotoMode = collage == null
    // 不同项目/单图和拼图切换时回到各自菜单起点，不在同一模式的面板切换中重置。
    LaunchedEffect(projectKey, singlePhotoMode) { editorToolbarScroll.scrollTo(0) }
    val singlePhoto = if (singlePhotoMode) layers.firstOrNull { it.visible } else null
    val selectedText = textLayers.firstOrNull { it.id == selectedLayerId }
    val selectedInfo = infoLayers.firstOrNull { it.id == selectedLayerId }
    val selectedShape = shapeLayers.firstOrNull { it.id == selectedLayerId }
    val editingInfo = infoLayers.firstOrNull { it.id == editingInfoId }
    val windowDensity = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(windowDensity) > 0

    // DEV7: A plain single-photo project uses the cropped photo as the document,
    // rather than treating the first editor viewport or its background as a canvas.
    // Historical projects with an explicit background remain on the legacy canvas path.
    val directPhoto = if (collage == null &&
        (canvasScene.background.kind == CanvasBackgroundKind.Transparent || canvasScene.singlePhotoCanvas)) {
        layers.filter { it.visible }.singleOrNull()
    } else null
    val previewContext = LocalContext.current
    val directPhotoPreview by rememberImagePreview(previewContext, directPhoto?.uriString, maxDimension = 900)
    val directPhotoAspect = if (directPhoto != null && directPhotoPreview != null) {
        directPhotoDocumentAspect(
            directPhotoPreview!!.width, directPhotoPreview!!.height,
            directPhoto.cropLeft, directPhoto.cropTop, directPhoto.cropRight, directPhoto.cropBottom,
            directPhoto.rotation
        )
    } else null
    LaunchedEffect(referenceCanvasSize, directPhotoAspect, canvasScene.aspectRatio) {
        val requested = directPhotoAspect ?: if (referenceCanvasSize.width > 0 &&
            referenceCanvasSize.height > 0 && canvasScene.aspectRatio == 0f) {
            referenceCanvasSize.width.toFloat() / referenceCanvasSize.height
        } else null
        if (requested != null && requested in .25f..4f &&
            kotlin.math.abs(canvasScene.aspectRatio - requested) > .002f) {
            // Debounce rotation/gesture updates; the rendered document already uses
            // directPhotoAspect immediately, so no screen lag is introduced.
            if (directPhotoAspect != null) delay(300)
            onUpdateScene(canvasScene.copy(aspectRatio = requested))
        }
    }
    val keyboardSafeEdit = imeVisible && (editingInfoId != null || editingTextId != null)
    // v0.9.6: 一级/二级工具栏互斥显示。只读取当前 UI 状态，
    // 不会改写任何画布、照片、文字、图形、涂鸦或拼图的几何数据。
    val secondaryToolActive =
        (cropLayer != null && cropDraft != null) || styleText != null ||
        editingText != null || editingInfo != null || selectedInfo != null ||
        (showCollagePanel && collage != null) || showShapePanel ||
        showImagePanel || showTextModePanel || showInfoTemplatePanel

    // 初次点拼图必须先选相册；仅当回传产生非空拼图后才进入二级面板。
    // 这一效果不写入历史/项目，取消相册不出现空的四宫格。
    val collageHasPhotos = collage?.slots?.any { it.imageId != null } == true
    LaunchedEffect(collage?.mode, collage?.template) {
        selectedCollageSlotIndex = null
    }
    LaunchedEffect(collageHasPhotos) {
        if (collageHasPhotos && pendingInitialCollageSelection) {
            pendingInitialCollageSelection = false
            showCollagePanel = true
        }
    }

    // Opening/closing a secondary tool starts in the compact state; edits and picker
    // callbacks do not reset the open panel's expansion state.
    LaunchedEffect(showImagePanel, showCollagePanel, showShapePanel,
        showTextModePanel, showInfoTemplatePanel, cropLayerId, editingTextId, styleTextId) {
        secondaryPanelExpanded = false
    }

    LaunchedEffect(layers, cropLayerId) {
        if (cropLayerId != null && cropLayer == null) {
            cropLayerId = null
            cropDraft = null
        }
    }
    LaunchedEffect(textLayers, editingTextId) {
        if (editingTextId != null && editingText == null) editingTextId = null
    }
    LaunchedEffect(textLayers, styleTextId) {
        if (styleTextId != null && styleText == null) styleTextId = null
    }
    LaunchedEffect(selectedLayerId) { infoQuickSection = null }
    LaunchedEffect(infoLayers, editingInfoId) {
        if (editingInfoId != null && editingInfo == null) editingInfoId = null
    }
    LaunchedEffect(selectedLayerId, editingInfoId) {
        if (editingInfoId != null && selectedLayerId != null && selectedLayerId != editingInfoId) {
            editingInfoId = null
        }
    }

    fun closeTransientPanels() {
        pendingInitialCollageSelection = false
        showLayerPanel = false
        showImagePanel = false
        showCollagePanel = false
        showShapePanel = false
        drawingFreehand = false
        showTextModePanel = false
        showInfoTemplatePanel = false
        editingTextId = null
        styleTextId = null
        editingInfoId = null
        cropLayerId = null
        cropDraft = null
        cropPanelPage = CropPanelPage.Crop
        returnToImageAfterCrop = false
        returnToCollageAfterCrop = false
    }

    fun openCrop(targetId: String? = null) {
        drawingFreehand = false
        showShapePanel = false
        val target = (
            layers.firstOrNull { it.id == (targetId ?: selectedLayerId) }
                ?.takeIf { it.visible && !it.locked }
                ?: (if (targetId == null) layers.lastOrNull { it.visible && !it.locked } else null)
        ) ?: return
        returnToImageAfterCrop = showImagePanel
        returnToCollageAfterCrop = showCollagePanel
        onSelectLayer(target.id)
        showLayerPanel = false
        showImagePanel = false
        showCollagePanel = false
        showTextModePanel = false
        showInfoTemplatePanel = false
        editingTextId = null
        styleTextId = null
        editingInfoId = null
        cropLayerId = target.id
        cropDraft = CropRect(target.cropLeft, target.cropTop, target.cropRight, target.cropBottom)
        cropPanelPage = CropPanelPage.Crop
    }

    fun addTextBox() {
        drawingFreehand = false
        showShapePanel = false
        val layer = TextLayer(text = "")
        onAddTextLayer(layer)
        showTextModePanel = false
        showInfoTemplatePanel = false
        showLayerPanel = false
        showImagePanel = false
        cropLayerId = null
        cropDraft = null
        styleTextId = null
        editingTextId = layer.id
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // DEV33: background-only glow. This is outside the project artwork and
        // is never captured by the project's photo/collage export renderer.
        EditorAmbientBackdrop()
        if (showCollageEntryChoice) {
            AlertDialog(
                onDismissRequest = { showCollageEntryChoice = false },
                title = { Text("开始拼图") },
                text = { Text("画布已有照片。使用现有照片会保留当前位置；也可以继续从相册添加照片。") },
                confirmButton = {
                    TextButton(onClick = {
                        val existing = layers.filter { it.visible }.map { it.id }
                        onUpdateCollage(CollageProject(mode = CollageMode.Free).assignNewImages(existing))
                        showCollageEntryChoice = false
                        showCollagePanel = true
                    }) { Text("使用现有照片") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val existing = layers.filter { it.visible }.map { it.id }
                        onUpdateCollage(CollageProject(mode = CollageMode.Free).assignNewImages(existing))
                        showCollageEntryChoice = false
                        showCollagePanel = true
                        onAddCollagePhotos()
                    }) { Text("相册添加") }
                }
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (keyboardSafeEdit) Modifier.imePadding() else Modifier)
        ) {
            EditorTopBar(
                onBack = onBack,
                onReselect = if (collage != null) onReselectCollagePhotos else null,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onExport = onExport
            )

            // 外层只是可见视口：底部面板展开时视口变矮，项目画布的逻辑尺寸不变。
            // 内层按首次画布比例整体 contain 缩放，不允许高度单独挤压图层布局。
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)
                    .onSizeChanged { size ->
                        if (referenceCanvasSize == IntSize.Zero && size.width > 0 && size.height > 0) {
                            // 如果是重新打开的项目，优先使用已经持久化的画布比例，
                            // 绝不因键盘/功能栏改变可用高度重新定义文档。
                            val fixedAspect = canvasScene.aspectRatio.takeIf { it in .25f..4f }
                            val refHeight = fixedAspect?.let { (size.width / it).roundToInt().coerceAtLeast(1) }
                                ?: size.height
                            referenceCanvasSize = IntSize(size.width, refHeight)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val availableWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val availableHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                // 文档的固定宽高比来自 CanvasScene，不来自底部面板的可用高度。
                // 只要项目画布比例发生过用户明确的初始化/调整，就重新推导参考高度；
                // 绝不更新照片/文字/图形/涂鸦的变换参数。
                val documentReferenceSize = referenceCanvasSize.takeIf { it.width > 0 && it.height > 0 }
                    ?.let { original ->
                        val savedRatio = (directPhotoAspect ?: collage?.aspect?.ratio ?: canvasScene.aspectRatio)
                            .takeIf { it in .25f..4f }
                        if (savedRatio == null) original else IntSize(
                            original.width,
                            (original.width / savedRatio).roundToInt().coerceAtLeast(1)
                        )
                    } ?: referenceCanvasSize
                val referenceWidthPx = documentReferenceSize.width.takeIf { it > 0 }?.toFloat() ?: availableWidthPx
                val referenceHeightPx = documentReferenceSize.height.takeIf { it > 0 }?.toFloat()
                    ?: availableHeightPx
                // DEV6: fit the PHOTO without deducting the template height. The complete
                // photo + external template remains one scrollable artwork when it is tall.
                // Width can grow for a narrow portrait so the external template can use space
                // beside the photograph. Neither viewport scroll nor template changes alter
                // the saved image-layer coordinates.
                val dockCount = infoLayers.count { it.visible && !it.overlayMode }
                var measuredTopPx by remember(dockCount) { mutableIntStateOf(0) }
                var measuredBottomPx by remember(dockCount) { mutableIntStateOf(0) }
                val estimatedStripPx = with(density) { 110.dp.toPx() } * dockCount
                val totalMeasured = measuredTopPx + measuredBottomPx
                val dockGapPx = if (dockCount > 0) with(density) { 2.dp.toPx() } *
                    ((if (infoLayers.any { it.visible && !it.overlayMode && it.positionTop }) 1 else 0) +
                        (if (infoLayers.any { it.visible && !it.overlayMode && !it.positionTop }) 1 else 0)) else 0f
                val stripBudget = if (dockCount == 0) 0f else if (totalMeasured > 0) totalMeasured.toFloat() else estimatedStripPx
                val compositeReferenceWidthPx = preferredDockWidth(
                    referenceWidthPx.roundToInt().coerceAtLeast(1),
                    referenceHeightPx.roundToInt().coerceAtLeast(1),
                    dockCount > 0
                ).toFloat()
                val scaleFactor = if (dockCount == 0) {
                    CanvasViewportScale.fit(referenceWidthPx, referenceHeightPx,
                        availableWidthPx, availableHeightPx).scale
                } else {
                    minOf(availableWidthPx / compositeReferenceWidthPx,
                        availableHeightPx / referenceHeightPx).coerceAtLeast(.01f)
                }
                val photoWidthPx = referenceWidthPx * scaleFactor
                val photoHeightPx = referenceHeightPx * scaleFactor
                val artworkWidthPx = compositeReferenceWidthPx * scaleFactor
                val artworkHeightPx = photoHeightPx + stripBudget + dockGapPx
                val artworkScroll = rememberScrollState()
                // DEV17: 文字输入启用视口滚动，不改项目的照片/图层坐标。
                // 底部额外滚动空间只在键盘显示时存在，不进入导出画布。
                val scrollingTextInput = keyboardSafeEdit && editingTextId != null
                val scrollArtwork = dockCount > 0 || scrollingTextInput
                val keyboardScrollReserve = if (scrollingTextInput) availableHeightPx * .55f else 0f
                val selectedBottomDock = infoLayers.any {
                    it.visible && !it.overlayMode && !it.positionTop && it.id == selectedLayerId
                }
                LaunchedEffect(selectedBottomDock, artworkScroll.maxValue) {
                    if (selectedBottomDock && dockCount > 0) {
                        artworkScroll.animateScrollTo(artworkScroll.maxValue)
                    }
                }
                val topPaddingPx = if (dockCount > 0) (availableHeightPx - artworkHeightPx).coerceAtLeast(0f) / 2f else 0f
                Box(
                    modifier = Modifier.fillMaxSize().then(
                        if (scrollArtwork) Modifier.verticalScroll(artworkScroll) else Modifier
                    ),
                    contentAlignment = if (scrollArtwork) Alignment.TopCenter else Alignment.Center
                ) {
                Box(
                    modifier = Modifier
                        .padding(
                            top = with(density) { topPaddingPx.toDp() },
                            bottom = with(density) { keyboardScrollReserve.toDp() }
                        )
                        .size(
                            with(density) { artworkWidthPx.toDp() },
                            with(density) { artworkHeightPx.toDp() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                // In external-layout mode the canvas background belongs behind the photograph,
                // not behind the independent top/bottom text strips.
                if (dockCount == 0 &&
                    (collage != null || canvasScene.singlePhotoCanvas ||
                        (directPhoto == null && collage == null))) {
                    CanvasBackgroundSurface(canvasScene.background,
                        showTransparencyGrid = collage?.mode == CollageMode.Free ||
                            canvasScene.background.kind != CanvasBackgroundKind.Transparent ||
                            (canvasScene.singlePhotoCanvas && canvasScene.photoInsetRatio > 0f),
                        modifier = Modifier.matchParentSize())
                }
                EditorCanvas(
                    photoBackground = if (dockCount > 0 &&
                        (collage != null || canvasScene.singlePhotoCanvas || (directPhoto == null && collage == null)))
                        canvasScene.background else null,
                    showPhotoTransparencyGrid = collage?.mode == CollageMode.Free ||
                        (canvasScene.singlePhotoCanvas && canvasScene.photoInsetRatio > 0f) ||
                        (directPhoto == null && collage == null &&
                            canvasScene.background.kind != CanvasBackgroundKind.Transparent),
                    directPhotoIsDocument = directPhoto != null,
                    singlePhotoMode = singlePhotoMode,
                    frame = canvasScene.frame,
                    foregroundInset = if (canvasScene.singlePhotoCanvas && collage == null ||
                        collage?.mode == CollageMode.Free) canvasScene.photoInsetRatio.coerceIn(0f,.20f) else 0f,
                    onDockedHeightsMeasured = { top, bottom ->
                        if (kotlin.math.abs(measuredTopPx - top) > 2) measuredTopPx = top
                        if (kotlin.math.abs(measuredBottomPx - bottom) > 2) measuredBottomPx = bottom
                    },
                    referenceCanvasSize = documentReferenceSize,
                    dockPhotoWidth = if (dockCount > 0) with(density) { photoWidthPx.toDp() } else null,
                    layers = layers,
                    textLayers = textLayers,
                    infoLayers = infoLayers,
                    shapeLayers = shapeLayers,
                    collage = collage,
                    selectedCollageSlotIndex = selectedCollageSlotIndex,
                    onSelectCollageSlot = { index ->
                        selectedCollageSlotIndex = index
                        if (index != null) showCollagePanel = true
                    },
                    onUpdateCollage = onUpdateCollage,
                    onAddCollagePhotos = onAddCollagePhotos,
                    onReplaceCollageSlot = onReplaceCollageSlot,
                    drawingFreehand = drawingFreehand && showShapePanel && shapeToolMode == ShapeToolMode.Doodle,
                    doodleTool = doodleTool,
                    doodleEraseMode = doodleEraseMode,
                    doodleEraserSize = doodleEraserSize,
                    doodleColor = doodleColor,
                    doodleStrokeWidth = doodleStrokeWidth,
                    doodleOpacity = doodleOpacity,
                    doodleSessionId = doodleSessionId,
                    onDrawFreehand = { shape -> onAddShapeLayer(linkedShape(shape)) },
                    onEraseDoodleStrokes = onEraseDoodleStrokes,
                    onEditShape = { id ->
                        onSelectLayer(id)
                        shapeTargetImageId = shapeLayers.firstOrNull { it.id == id }?.parentImageId
                        showShapePanel = true
                        drawingFreehand = false
                        shapePanelPage = ShapePanelPage.Edit
                        shapeToolMode = if (shapeLayers.firstOrNull { it.id == id }?.type == ShapeType.Symbol) ShapeToolMode.Symbols else ShapeToolMode.Shapes
                        showLayerPanel = false
                        showImagePanel = false
                    },
                    selectedLayerId = selectedLayerId,
                    activeShapeId = if (showShapePanel && shapePanelPage == ShapePanelPage.Edit) selectedShape?.id else null,
                    cropLayerId = cropLayerId,
                    cropDraft = cropDraft,
                    cropPanelPage = cropPanelPage,
                    editingTextId = editingTextId,
                    editingInfoId = editingInfoId,
                    infoKeyboardSafe = imeVisible && editingInfoId != null,
                    infoEditScale = infoEditScale,
                    onInfoEditScaleChange = { infoEditScale = it.coerceIn(.85f, 1.60f) },
                    onCropDraftChange = { cropDraft = it },
                    onSelectLayer = onSelectLayer,
                    // DEV8: 单图点照片不弹出重复的图片菜单，使用下方横向工具栏。
                    onTapImageLayer = { id ->
                        onSelectLayer(id)
                        if (!singlePhotoMode && cropLayerId == null && !showShapePanel &&
                            !drawingFreehand && editingTextId == null && editingInfoId == null &&
                            !showInfoTemplatePanel) {
                            if (collage?.mode == CollageMode.Free) {
                                collageRequestedSection = "拼图"
                                showCollagePanel = true
                                showImagePanel = false
                            } else showImagePanel = true
                            showLayerPanel = false
                            showTextModePanel = false
                            styleTextId = null
                        }
                    },
                    onUpdateLayer = onUpdateLayer,
                    onUpdateTextLayer = onUpdateTextLayer,
                    onUpdateInfoLayer = onUpdateInfoLayer,
                    onEditText = { id ->
                        onSelectLayer(id)
                        showTextModePanel = false
                        showInfoTemplatePanel = false
                        editingInfoId = null
                        showLayerPanel = false
                        showImagePanel = false
                        cropLayerId = null
                        cropDraft = null
                        styleTextId = null
                        editingTextId = id
                    },
                    onDoneTextEdit = { id ->
                        editingTextId = null
                        showTextModePanel = false
                        showInfoTemplatePanel = false
                        styleTextId = id
                        onSelectLayer(id)
                    },
                    onAddPhoto = onAddPhoto,
                    onDeleteImageLayer = onDeleteLayer,
                    onDuplicateImageLayer = onDuplicateLayer,
                    onDuplicateTextLayer = onDuplicateTextLayer,
                    onDeleteTextLayer = onDeleteTextLayer,
                    onEditInfo = { id ->
                        onSelectLayer(id)
                        showTextModePanel = false
                        showInfoTemplatePanel = false
                        showLayerPanel = false
                        showImagePanel = false
                        editingTextId = null
                        styleTextId = null
                        infoEditScale = 1f
                        editingInfoId = id
                    },
                    onDoneInfoEdit = { id ->
                        editingInfoId = null
                        onSelectLayer(id)
                    },
                    onDeleteInfo = onDeleteInfoLayer,
                    onDuplicateInfo = { id ->
                        infoLayers.firstOrNull { it.id == id }?.let { source ->
                            onAddInfoLayer(source.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                offsetXRatio = (source.offsetXRatio + .035f).coerceAtMost(.48f),
                                offsetYRatio = (source.offsetYRatio + .035f).coerceAtMost(.48f)
                            ))
                        }
                    },
                    onUpdateShapeLayer = onUpdateShapeLayer,
                    onDeleteShapeLayer = onDeleteShapeLayer,
                    onDuplicateShapeLayer = onDuplicateShapeLayer
                )
                if (dockCount == 0 && canvasScene.frame.enabled &&
                    canvasScene.frame.scope == CanvasFrameScope.Canvas) {
                    Canvas(Modifier.matchParentSize()) {
                        val stroke=(size.minDimension*canvasScene.frame.normalized().widthRatio).coerceAtLeast(1f)
                        drawRoundRect(Color(canvasScene.frame.colorArgb),topLeft=Offset(stroke/2f,stroke/2f),
                            size=Size((size.width-stroke).coerceAtLeast(1f),
                                (size.height-stroke).coerceAtLeast(1f)),
                            cornerRadius=CornerRadius(if(canvasScene.frame.corner==CanvasFrameCorner.Rounded) 10.dp.toPx() else 0f),
                            style=Stroke(stroke))
                    }
                }
                // 红色边缘提示已由 SnapGuides 按触边状态动态显示。
                // 此处禁止绘制常驻边框，避免干扰多图排版。
                }
                } // scrollable artwork viewport
            }

            when {
                cropLayer != null && cropDraft != null -> {
                    CropPanel(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        layer = cropLayer,
                        draft = cropDraft!!,
                        page = cropPanelPage,
                        allowRotateControls = singlePhotoMode,
                        onPageChange = { cropPanelPage = it },
                        onDraftChange = { cropDraft = it.sanitize() },
                        onUpdateLayer = onUpdateLayer,
                        onCancel = {
                            cropLayerId = null
                            cropDraft = null
                            cropPanelPage = CropPanelPage.Crop
                            showImagePanel = returnToImageAfterCrop
                            showCollagePanel = returnToCollageAfterCrop
                            returnToImageAfterCrop = false
                            returnToCollageAfterCrop = false
                        },
                        onApply = {
                            val d = cropDraft!!.sanitize()
                            onUpdateLayer(
                                cropLayer.copy(
                                    cropLeft = d.left,
                                    cropTop = d.top,
                                    cropRight = d.right,
                                    cropBottom = d.bottom
                                )
                            )
                            cropLayerId = null
                            cropDraft = null
                            cropPanelPage = CropPanelPage.Crop
                            showImagePanel = returnToImageAfterCrop
                            showCollagePanel = returnToCollageAfterCrop
                            returnToImageAfterCrop = false
                            returnToCollageAfterCrop = false
                        }
                    )
                }

                styleText != null -> {
                    TextEditorPanel(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        layer = styleText,
                        onUpdate = onUpdateTextLayer,
                        onDone = { styleTextId = null }
                    )
                }

                editingText != null -> {
                    // 输入模式只保留画布中的文本输入框，不显示字号/颜色/对齐等属性。
                    // 点击文本框右上角“完成”后，才进入普通选中状态调整样式与变换。
                    Spacer(Modifier.height(0.dp))
                }

                showCollagePanel && collage != null -> {
                    CollagePanel(
                        modifier=Modifier.padding(start=14.dp,end=14.dp,bottom=6.dp),
                        project=collage,
                        onUpdate={ onUpdateCollage(it) },
                        onSelectAspect={ aspect ->
                            // 用户点击“画布比例”才同步调整项目页面尺寸。
                            // 两个模型参数必须一致，图片/文字/图形的图层变换值不写回。
                            onUpdateCollage(collage.copy(aspect = aspect))
                            onUpdateScene(canvasScene.copy(aspectRatio = aspect.ratio))
                        },
                        selectedSlotIndex = selectedCollageSlotIndex,
                        onClearSlotSelection = { selectedCollageSlotIndex = null },
                        onAddPhotos=onAddCollagePhotos,
                        onReplaceSlot=onReplaceCollageSlot,
                        onCropTemplateSlot={ index ->
                            collage.slots.getOrNull(index)?.imageId?.let { openCrop(it) }
                        },
                        onBrushTemplateSlot={ index, kind ->
                            openPhotoBrush(collage.slots.getOrNull(index)?.imageId,kind)
                        },
                        onToneTemplateSlot={ index ->
                            collage.slots.getOrNull(index)?.imageId?.let { imageId ->
                                onSelectLayer(imageId)
                                returnToCollageAfterTone = true
                                showCollagePanel = false
                                showImagePanel = true
                            }
                        },
                        requestedSection = collageRequestedSection,
                        onCropFreeImage={ selectedImage?.let { openCrop(it.id) } },
                        onBrushFreeImage={ kind -> openPhotoBrush(selectedImage?.id,kind) },
                        onToneFreeImage={ selectedImage?.let { image ->
                            onSelectLayer(image.id)
                            returnToCollageAfterTone = true
                            showCollagePanel = false
                            showImagePanel = true
                        } },
                        onUpdateFreeImage=onUpdateLayer,
                        photoLayers=layers,
                        availablePhotoIds=layers.filter { it.visible }.map { it.id },
                        selectedFreeImage=selectedImage,
                        onReplaceFreeImage={ selectedImage?.let { onReplacePhoto(it.id) } },
                        onResetFreeImage={ selectedImage?.let { image ->
                            if (!image.locked) onUpdateLayer(image.copy(
                                offsetXRatio=0f,offsetYRatio=0f,scale=1f,rotation=0f,
                                freeFitMode=com.tugongfang.editor.model.collage.CollageFitMode.Fit
                            ))
                        } },
                        onDeleteFreeImage={ selectedImage?.let { image ->
                            if (!image.locked) onDeleteLayer(image.id)
                        } },
                        background=canvasScene.background,
                        onBackgroundChange={ onUpdateScene(canvasScene.copy(background=it)) },
                        canvasScene=canvasScene,
                        onUpdateScene=onUpdateScene,
                        onPickBackgroundImage=onPickBackgroundImage,
                        expanded = secondaryPanelExpanded,
                        onToggleExpanded = { secondaryPanelExpanded = !secondaryPanelExpanded },
                        onDisable={onUpdateCollage(null);selectedCollageSlotIndex=null;showCollagePanel=false},
                        onClose={selectedCollageSlotIndex=null;showCollagePanel=false}
                    )
                }

                showShapePanel -> {
                    ShapeToolPanel(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        selectedShape = selectedShape,
                        page = shapePanelPage,
                        onAdd = { shape ->
                            onAddShapeLayer(linkedShape(shape))
                            drawingFreehand = false
                            shapePanelPage = ShapePanelPage.Edit
                        },
                        onChange = onUpdateShapeLayer,
                        mode = shapeToolMode,
                        onModeChange = { mode ->
                            shapeToolMode = mode
                            shapePanelPage = ShapePanelPage.Choose
                            drawingFreehand = mode == ShapeToolMode.Doodle
                            if (drawingFreehand) {
                                doodleSessionId = java.util.UUID.randomUUID().toString()
                                onSelectLayer(null)
                            }
                        },
                        doodleTool = doodleTool,
                        doodleEraseMode = doodleEraseMode,
                        onDoodleEraseModeChange = { doodleEraseMode = it },
                        doodleEraserSize = doodleEraserSize,
                        onDoodleEraserSizeChange = { doodleEraserSize = it },
                        onDoodleToolChange = {
                            doodleTool = it
                            when (it) {
                                DoodleTool.Pencil -> { doodleStrokeWidth = 5f; doodleOpacity = 1f }
                                DoodleTool.Marker -> { doodleStrokeWidth = 16f; doodleOpacity = 1f }
                                DoodleTool.Highlighter -> { doodleStrokeWidth = 26f; doodleOpacity = .8f }
                                DoodleTool.Eraser -> Unit
                            }
                        },
                        doodleColor = doodleColor,
                        onDoodleColorChange = { doodleColor = it },
                        doodleStrokeWidth = doodleStrokeWidth,
                        onDoodleStrokeWidthChange = { doodleStrokeWidth = it },
                        doodleOpacity = doodleOpacity,
                        onDoodleOpacityChange = { doodleOpacity = it },
                        expanded = secondaryPanelExpanded,
                        onToggleExpanded = { secondaryPanelExpanded = !secondaryPanelExpanded },
                        onDone = {
                            drawingFreehand = false
                            // 所有图形/符号/涂鸦“完成”均返回一级工具栏，
                            // 保留新增或修改后的图层内容，不改变任何图层坐标。
                            showShapePanel = false
                            shapeToolMode = ShapeToolMode.Shapes
                            shapePanelPage = ShapePanelPage.Choose
                            onSelectLayer(null)
                        },
                        onClose = {
                            showShapePanel = false
                            drawingFreehand = false
                            shapeToolMode = ShapeToolMode.Shapes
                            shapePanelPage = ShapePanelPage.Choose
                        }
                    )
                }

                showImagePanel -> {
                    ImageToolPanel(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        selectedLayer = if (singlePhotoMode) singlePhoto else selectedImage,
                        canvasAspectRatio = canvasScene.aspectRatio,
                        directPhotoIsDocument = singlePhotoMode || returnToCollageAfterTone,
                        onSelectCanvasAspect = { aspect ->
                            // 只有显式点击比例才修改文档尺寸；照片的变换/吸附状态不写回。
                            if (collage == null) onUpdateScene(canvasScene.copy(aspectRatio = aspect.ratio))
                        },
                        onCropSelected = { selectedImage?.let { openCrop(it.id) } },
                        onDuplicateSelected = { selectedImage?.let { onDuplicateLayer(it.id) } },
                        onUpdateLayer = onUpdateLayer,
                        initialPage = singlePhotoPanelPage,
                        background = canvasScene.background,
                        onBackgroundChange = { bg ->
                            val firstEnabled = singlePhotoMode && !canvasScene.singlePhotoCanvas &&
                                bg.kind != CanvasBackgroundKind.Transparent
                            onUpdateScene(canvasScene.copy(background=bg,
                                singlePhotoCanvas=canvasScene.singlePhotoCanvas || firstEnabled,
                                photoInsetRatio=if(firstEnabled && canvasScene.photoInsetRatio==0f) .04f
                                    else canvasScene.photoInsetRatio))
                        },
                        canvasScene = canvasScene,
                        onUpdateScene = onUpdateScene,
                        onPickBackgroundImage = onPickBackgroundImage,
                        onAddPhoto = onAddPhoto,
                        onReplacePhoto = onReplacePhoto,
                        expanded = secondaryPanelExpanded,
                        onToggleExpanded = { secondaryPanelExpanded = !secondaryPanelExpanded },
                        onResetLayer = { layer ->
                            onUpdateLayer(
                                layer.copy(
                                    offsetXRatio = 0f,
                                    offsetYRatio = 0f,
                                    scale = 1f,
                                    rotation = 0f
                                )
                            )
                        },
                        onClose = {
                            showImagePanel = false
                            if (returnToCollageAfterTone) {
                                returnToCollageAfterTone = false
                                showCollagePanel = true
                            }
                        }
                    )
                }

                showTextModePanel -> {
                    TextModePanel(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                        onAddTextBox = ::addTextBox,
                        onInfoText = {
                            showTextModePanel = false
                            showInfoTemplatePanel = true
                            editingInfoId = null
                        },
                        onClose = { showTextModePanel = false }
                    )
                }

                showInfoTemplatePanel -> {
                    TextTemplatePickerPanel(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        onAdd = { info ->
                            onAddInfoLayer(info)
                            infoEditScale = 1f
                            editingInfoId = info.id
                            showInfoTemplatePanel = false
                        },
                        onClose = { showInfoTemplatePanel = false }
                    )
                }

                editingInfo != null -> {
                    // In-canvas focused editing; no second huge bottom form.
                    Spacer(Modifier.height(0.dp))
                }

                selectedInfo != null -> {
                    CompactInfoTools(
                        modifier = Modifier.padding(horizontal=8.dp, vertical=3.dp),
                        layer = selectedInfo,
                        section = infoQuickSection,
                        onSectionChange = { infoQuickSection = if(infoQuickSection == it) null else it },
                        onEdit = { editingInfoId = selectedInfo.id; infoQuickSection = null },
                        onUpdate = onUpdateInfoLayer,
                        onDone = { onSelectLayer(null); infoQuickSection = null }
                    )
                }

            }

            // DEV8：单图主工具直接列出照片操作；拼图保留图片/拼图入口。
            // 二者统一为水平滚动工具栏，工具参数面板打开时暂时让出空间。
            if (!secondaryToolActive) {
                EditorToolBar(
                    scrollState = editorToolbarScroll,
                    singlePhotoMode = singlePhotoMode,
                    imageSelected = showImagePanel,
                    shapeSelected = showShapePanel && shapeToolMode != ShapeToolMode.Doodle,
                    doodleSelected = showShapePanel && shapeToolMode == ShapeToolMode.Doodle,
                    mosaicSelected = photoBrushTarget != null && photoBrushKind == PhotoStrokeKind.Mosaic,
                    textSelected = showTextModePanel || showInfoTemplatePanel || editingTextId != null || editingInfoId != null || selectedText != null || selectedInfo != null,
                    onSingleCrop = { singlePhoto?.takeUnless { it.locked }?.let { openCrop(it.id) } },
                    onSingleTone = {
                        val target = singlePhoto?.takeUnless { it.locked }
                        if (target != null) {
                            closeTransientPanels()
                            singlePhotoPanelPage = "调色"
                            onSelectLayer(target.id)
                            showImagePanel = true
                        }
                    },
                    onSingleDecoration = { page ->
                        if (singlePhoto != null) {
                            closeTransientPanels()
                            singlePhotoPanelPage = page
                            onSelectLayer(singlePhoto.id)
                            showImagePanel = true
                        }
                    },
                    onSingleReplace = { singlePhoto?.let { onReplacePhoto(it.id) } },
                    collageSelected = showCollagePanel || collage!=null,
                    onCollage = {
                        collageRequestedSection = "拼图"
                        closeTransientPanels()
                        if (collage == null) {
                            val existing = layers.filter { it.visible }.map { it.id }
                            if (existing.isEmpty()) {
                                // 不提前创建空模板：选图前画布、图层和历史均保持原状。
                                pendingInitialCollageSelection = true
                                onAddCollagePhotos()
                            } else {
                                // 已有自由摆放的照片：先询问用户，绝不在进入拼图时自动重排。
                                showCollageEntryChoice = true
                            }
                        } else if (collage.slots.none { it.imageId != null } && layers.none { it.visible }) {
                            // 兼容旧版本留下的空模板记录：也先选照片，不显示空占位。
                            pendingInitialCollageSelection = true
                            onAddCollagePhotos()
                        } else {
                            showCollagePanel = true
                        }
                        onSelectLayer(null)
                    },
                    onCollageBackground = {
                        if (collage != null) {
                            closeTransientPanels()
                            selectedCollageSlotIndex = null
                            collageRequestedSection = "背景"
                            showCollagePanel = true
                        }
                    },
                    onCollageAspect = {
                        if (collage != null) {
                            closeTransientPanels()
                            selectedCollageSlotIndex = null
                            collageRequestedSection = "比例"
                            showCollagePanel = true
                        }
                    },
                    onShape = {
                        cropLayerId = null
                        cropDraft = null
                        showLayerPanel = false
                        showImagePanel = false
                        showTextModePanel = false
                        showInfoTemplatePanel = false
                        editingInfoId = null
                        editingTextId = null
                        styleTextId = null
                        shapeTargetImageId = selectedImage?.id ?: layers.lastOrNull { it.visible }?.id
                        showShapePanel = !showShapePanel
                        drawingFreehand = false
                        shapeToolMode = ShapeToolMode.Shapes
                        shapePanelPage = ShapePanelPage.Choose
                        if (showShapePanel) onSelectLayer(null)
                    },
                    onText = {
                        showShapePanel = false
                        cropLayerId = null
                        cropDraft = null
                        cropPanelPage = CropPanelPage.Crop
                        showLayerPanel = false
                        showImagePanel = false
                        showInfoTemplatePanel = false
                        editingInfoId = null
                        when {
                            selectedText != null -> {
                                showTextModePanel = false
                                editingTextId = null
                                styleTextId = selectedText.id
                            }
                            selectedInfo != null -> {
                                showTextModePanel = false
                                styleTextId = null
                                infoEditScale = 1f
                                editingInfoId = selectedInfo.id
                            }
                            else -> {
                                styleTextId = null
                                editingTextId = null
                                showTextModePanel = !showTextModePanel
                            }
                        }
                    },
                    onMosaic = {
                        val targetId = if (collage?.mode == CollageMode.Template)
                            selectedCollageSlotIndex?.let { collage.slots.getOrNull(it)?.imageId }
                        else selectedImage?.id ?: singlePhoto?.id ?: layers.firstOrNull { it.visible }?.id
                        openPhotoBrush(targetId,PhotoStrokeKind.Mosaic)
                    },
                    onDoodle = {
                        val currentlyDoodling = showShapePanel && shapeToolMode == ShapeToolMode.Doodle
                        closeTransientPanels()
                        if (!currentlyDoodling) {
                            showShapePanel = true
                            shapeToolMode = ShapeToolMode.Doodle
                            shapePanelPage = ShapePanelPage.Choose
                            drawingFreehand = true
                            shapeTargetImageId = selectedImage?.id ?: layers.lastOrNull { it.visible }?.id
                            doodleSessionId = java.util.UUID.randomUUID().toString()
                            onSelectLayer(null)
                        }
                    }
                )
            }
        }
        // v0.9.3 FIX1：信息模板与画布视口彻底解耦；键盘只改变浮层可用高度。
        // 浮层内可垂直滚动，标题/输入框宽度不会被底部面板或画布缩放压缩。
        if (editingInfo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(120f)
                    .background(Color(0x91E7EFF9))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        InfoPanelItem(
                            layer = editingInfo,
                            selected = true,
                            editing = true,
                            keyboardSafe = imeVisible,
                            editScale = infoEditScale,
                            onEditScaleChange = { infoEditScale = it.coerceIn(.85f, 1.60f) },
                            onSelect = onSelectLayer,
                            onEdit = { editingInfoId = it },
                            onUpdate = onUpdateInfoLayer,
                            onDone = {
                                editingInfoId = null
                                onSelectLayer(editingInfo.id)
                            },
                            onDelete = { id ->
                                onDeleteInfoLayer(id)
                                editingInfoId = null
                            }
                        )
                    }
                }
            }
        }
        // The drawer is overlay-only; it does not participate in viewport measurement.
        if (!keyboardSafeEdit) {
            LayerDrawer(
                modifier = Modifier.align(Alignment.CenterStart).zIndex(100f),
                expanded = showLayerPanel,
                onToggle = {
                    showLayerPanel = !showLayerPanel
                },
                layers = layers, textLayers = textLayers, infoLayers = infoLayers,
                shapeLayers = shapeLayers, selectedLayerId = selectedLayerId,
                onSelectLayer = onSelectLayer,
                onToggleVisibility = onToggleVisibility, onToggleLock = onToggleLock,
                onDeleteLayer = onDeleteLayer, onDuplicateLayer = onDuplicateLayer,
                onReplacePhoto = onReplacePhoto,
                onToggleTextVisibility = onToggleTextVisibility,
                onToggleTextLock = onToggleTextLock,
                onDeleteTextLayer = onDeleteTextLayer,
                onDuplicateTextLayer = onDuplicateTextLayer,
                onEditText = { id ->
                    showLayerPanel = false
                    showImagePanel = false
                    styleTextId = null
                    editingTextId = id
                    onSelectLayer(id)
                },
                onEditInfo = { id ->
                    showLayerPanel = false
                    showImagePanel = false
                    showInfoTemplatePanel = false
                    editingTextId = null
                    styleTextId = null
                    infoEditScale = 1f
                    editingInfoId = id
                    onSelectLayer(id)
                },
                onToggleInfoVisibility = onToggleInfoVisibility,
                onDeleteInfoLayer = onDeleteInfoLayer,
                onDeleteShapeLayer = onDeleteShapeLayer,
                onDuplicateShapeLayer = onDuplicateShapeLayer,
                onToggleShapeLock = onToggleShapeLock,
                onToggleShapeVisibility = onToggleShapeVisibility
            )
        }
    }
    val activePhotoBrush = layers.firstOrNull { it.id==photoBrushTarget && it.visible }
    if (activePhotoBrush != null) {
        PhotoBrushEditor(activePhotoBrush,photoBrushKind,
            onUpdate=onUpdateLayer,
            onDone={photoBrushTarget=null})
    } else if (photoBrushTarget != null) photoBrushTarget=null
}

@Composable
private fun EditorCanvas(
    singlePhotoMode: Boolean,
    photoBackground: com.tugongfang.editor.model.canvas.CanvasBackground?,
    showPhotoTransparencyGrid: Boolean,
    directPhotoIsDocument: Boolean,
    frame: CanvasFrame,
    foregroundInset: Float,
    onDockedHeightsMeasured: (Int, Int) -> Unit,
    referenceCanvasSize: IntSize,
    dockPhotoWidth: Dp?,
    layers: List<ImageLayer>,
    textLayers: List<TextLayer>,
    infoLayers: List<InfoPanelLayer>,
    shapeLayers: List<ShapeLayer>,
    collage: CollageProject?,
    selectedCollageSlotIndex: Int?,
    onSelectCollageSlot: (Int?) -> Unit,
    onUpdateCollage: (CollageProject?)->Unit,
    onAddCollagePhotos: ()->Unit,
    onReplaceCollageSlot: (Int)->Unit,
    drawingFreehand: Boolean,
    doodleTool: DoodleTool,
    doodleEraseMode: DoodleEraseMode,
    doodleEraserSize: Float,
    doodleColor: Int,
    doodleStrokeWidth: Float,
    doodleOpacity: Float,
    doodleSessionId: String,
    onDrawFreehand: (ShapeLayer) -> Unit,
    onEraseDoodleStrokes: (Map<String, List<ShapeLayer>>) -> Unit,
    onEditShape: (String) -> Unit,
    selectedLayerId: String?,
    activeShapeId: String?,
    cropLayerId: String?,
    cropDraft: CropRect?,
    cropPanelPage: CropPanelPage,
    editingTextId: String?,
    editingInfoId: String?,
    infoKeyboardSafe: Boolean,
    infoEditScale: Float,
    onInfoEditScaleChange: (Float) -> Unit,
    onCropDraftChange: (CropRect) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onTapImageLayer: (String) -> Unit,
    onUpdateLayer: (ImageLayer) -> Unit,
    onUpdateTextLayer: (TextLayer) -> Unit,
    onUpdateInfoLayer: (InfoPanelLayer) -> Unit,
    onEditText: (String) -> Unit,
    onDoneTextEdit: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onDeleteImageLayer: (String) -> Unit,
    onDuplicateImageLayer: (String) -> Unit,
    onDuplicateTextLayer: (String) -> Unit,
    onDeleteTextLayer: (String) -> Unit,
    onEditInfo: (String) -> Unit,
    onDoneInfoEdit: (String) -> Unit,
    onDeleteInfo: (String) -> Unit,
    onDuplicateInfo: (String) -> Unit,
    onUpdateShapeLayer: (ShapeLayer) -> Unit,
    onDeleteShapeLayer: (String) -> Unit,
    onDuplicateShapeLayer: (String) -> Unit
) {
    if (layers.isEmpty() && textLayers.isEmpty() && infoLayers.isEmpty() && shapeLayers.isEmpty() && collage == null) {
        EmptyCanvas(onAddPhoto)
        return
    }

    var viewportScale by remember { mutableFloatStateOf(1f) }
    var viewportPan by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(drawingFreehand) {
        if (!drawingFreehand) { viewportScale = 1f; viewportPan = Offset.Zero }
    }

    val dockedTopInfo = infoLayers.filter { it.visible && !it.overlayMode && it.positionTop }
    val dockedBottomInfo = infoLayers.filter { it.visible && !it.overlayMode && !it.positionTop }
    val isolatedTextInput = editingTextId != null
    val isolatedInfoInput = editingInfoId != null
    val isolatedInput = isolatedTextInput || isolatedInfoInput

    var topMeasured by remember { mutableIntStateOf(0) }
    var bottomMeasured by remember { mutableIntStateOf(0) }
    LaunchedEffect(dockedTopInfo.size, dockedBottomInfo.size, topMeasured, bottomMeasured) {
        onDockedHeightsMeasured(if (dockedTopInfo.isEmpty()) 0 else topMeasured,
            if (dockedBottomInfo.isEmpty()) 0 else bottomMeasured)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (dockedTopInfo.isNotEmpty()) {
            DockedInfoPanelSection(
                modifier = Modifier.onSizeChanged { topMeasured = it.height },
                layers = dockedTopInfo,
                selectedLayerId = selectedLayerId,
                editingInfoId = editingInfoId,
                isolatedInput = isolatedInput,
                infoKeyboardSafe = infoKeyboardSafe,
                infoEditScale = infoEditScale,
                onInfoEditScaleChange = onInfoEditScaleChange,
                onSelect = onSelectLayer,
                onEdit = onEditInfo,
                onUpdate = onUpdateInfoLayer,
                onDone = onDoneInfoEdit,
                onDelete = onDeleteInfo
            )
            Spacer(Modifier.height(2.dp))
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .then(if (dockPhotoWidth == null) Modifier.fillMaxWidth() else Modifier.width(dockPhotoWidth))
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    val innerScale=1f-2f*foregroundInset
                    scaleX = innerScale * (if (drawingFreehand) viewportScale else 1f)
                    scaleY = (1f-2f*foregroundInset) * (if (drawingFreehand) viewportScale else 1f)
                    translationX = if (drawingFreehand) viewportPan.x else 0f
                    translationY = if (drawingFreehand) viewportPan.y else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val referenceWidthPx = referenceCanvasSize.width.takeIf { it > 0 }?.toFloat() ?: canvasWidthPx
            val referenceHeightPx = referenceCanvasSize.height.takeIf { it > 0 }?.toFloat() ?: canvasHeightPx
            val layoutContentScale = CanvasViewportScale.uniform(
                referenceWidthPx, referenceHeightPx, canvasWidthPx, canvasHeightPx
            )
            val referenceWidthDp = with(density) { referenceWidthPx.toDp() }
            val referenceHeightDp = with(density) { referenceHeightPx.toDp() }
            val snapThresholdPx = with(density) { 9.dp.toPx() }
            var snapFeedback by remember { mutableStateOf<SnapFeedback?>(null) }

            val editingTextLayer = textLayers.firstOrNull { it.id == editingTextId && it.visible }
            // 信息模板只参与排版/导出，不得改变已有照片的基准尺寸与对齐方式。
            if (photoBackground != null && cropLayerId == null) {
                CanvasBackgroundSurface(photoBackground, showPhotoTransparencyGrid,
                    modifier = Modifier.matchParentSize().graphicsLayer {
                        // Parent scales all foreground edits, not the background underneath.
                        val factor=(1f-2f*foregroundInset).coerceAtLeast(.60f)
                        scaleX=1f/factor; scaleY=1f/factor
                    })
            }

        if (collage != null && collage.mode == CollageMode.Template && cropLayerId == null) {
            // 模板使用固定格位；自由拼图使用原来的图片层操作。
            // 其它图层不会被删除；退出拼图可继续原有编辑。
            CollageCanvas(
                collage=collage,images=layers,
                frame=frame,
                onChange={onUpdateCollage(it)},
                onAddPhotos=onAddCollagePhotos,
                onReplaceSlot=onReplaceCollageSlot,
                selectedSlotIndex=selectedCollageSlotIndex,
                onSelectSlot=onSelectCollageSlot
            )
        } else {
        // 背景图片层。文本/模板输入模式时仍展示，但所有手势都由上层遮罩拦截。
        layers.forEach { layer ->
            if (layer.visible && (cropLayerId == null || layer.id == cropLayerId)) {
                ImageLayerItem(
                    layer = layer,
                    selected = !isolatedInput && layer.id == selectedLayerId,
                    frame = frame,
                    directPhotoIsDocument = directPhotoIsDocument,
                    singlePhotoMode = singlePhotoMode,
                    cropEditing = layer.id == cropLayerId,
                    showCropOverlay = layer.id == cropLayerId && cropPanelPage == CropPanelPage.Crop,
                    cropDraft = if (layer.id == cropLayerId) cropDraft else null,
                    otherLayers = layers.filter { it.id != layer.id && it.visible },
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    snapThresholdPx = snapThresholdPx,
                    maxCanvasWidth = referenceWidthDp,
                    maxCanvasHeight = referenceHeightDp,
                    layoutContentScale = layoutContentScale,
                    onSelectLayer = onSelectLayer,
                    onTapImageLayer = onTapImageLayer,
                    onUpdateLayer = onUpdateLayer,
                    onDeleteLayer = onDeleteImageLayer,
                    onDuplicateLayer = onDuplicateImageLayer,
                    onCropDraftChange = onCropDraftChange,
                    onSnapFeedback = { feedback ->
                        if (!isolatedInput && layer.id == selectedLayerId) snapFeedback = feedback
                    }
                )
            }
        }

        } // 普通图片与自由拼图层结束；下面图形文字在两种拼图中都可编辑。

        // Docked information bars sit outside this picture canvas. Stroke only the picture area.
        if (dockPhotoWidth != null && frame.enabled && frame.scope == CanvasFrameScope.Canvas) {
            Canvas(Modifier.matchParentSize()) {
                val stroke=(size.minDimension*frame.normalized().widthRatio).coerceAtLeast(1f)
                drawRoundRect(Color(frame.colorArgb),topLeft=Offset(stroke/2f,stroke/2f),
                    size=Size((size.width-stroke).coerceAtLeast(1f),
                        (size.height-stroke).coerceAtLeast(1f)),
                    cornerRadius=CornerRadius(if(frame.corner==CanvasFrameCorner.Rounded) 10.dp.toPx() else 0f),
                    style=Stroke(stroke))
            }
        }

        // 向量图形与图片一样位于同一编辑画布；文字及模板仍按原有层级显示。
        if (cropLayerId == null) {
            shapeLayers.forEach { shape ->
                val parentVisible = shape.parentImageId == null ||
                    layers.any { it.id == shape.parentImageId && it.visible }
                if (shape.visible && parentVisible) {
                    ShapeLayerItem(
                        layer = shape,
                        selected = !isolatedInput && shape.id == selectedLayerId,
                        editable = !isolatedInput && shape.id == activeShapeId,
                        enabled = !isolatedInput,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        referenceCanvasWidthPx = referenceWidthPx,
                        referenceCanvasHeightPx = referenceHeightPx,
                        layoutContentScale = layoutContentScale,
                        onSelect = onSelectLayer,
                        onEdit = onEditShape,
                        onUpdate = onUpdateShapeLayer,
                        onDelete = onDeleteShapeLayer,
                        onDuplicate = onDuplicateShapeLayer,
                        onSnapFeedback = { if (!isolatedInput) snapFeedback = it },
                        otherCenterX = shapeLayers.filter { it.id != shape.id && it.visible }
                            .map { it.offsetXRatio } + textLayers.filter { it.visible }
                            .map { it.offsetXRatio },
                        otherCenterY = shapeLayers.filter { it.id != shape.id && it.visible }
                            .map { it.offsetYRatio } + textLayers.filter { it.visible }
                            .map { it.offsetYRatio }
                    )
                }
            }
        }

        // 非当前输入目标的自由文本先画在背景层。
        textLayers.forEach { layer ->
            if (layer.visible && cropLayerId == null && layer.id != editingTextId) {
                TextLayerItem(
                    layer = layer,
                    selected = !isolatedInput && layer.id == selectedLayerId,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    snapThresholdPx = snapThresholdPx,
                    layoutContentScale = layoutContentScale,
                    otherImageCentersX = layers.filter { it.visible }.map { it.offsetXRatio },
                    otherImageCentersY = layers.filter { it.visible }.map { it.offsetYRatio },
                    otherTextCentersX = textLayers.filter { it.id != layer.id && it.visible }.map { it.offsetXRatio },
                    otherTextCentersY = textLayers.filter { it.id != layer.id && it.visible }.map { it.offsetYRatio },
                    editing = false,
                    onSelectLayer = onSelectLayer,
                    onUpdateLayer = onUpdateTextLayer,
                    onEditText = onEditText,
                    onDoneTextEdit = onDoneTextEdit,
                    onDuplicate = onDuplicateTextLayer,
                    onDelete = onDeleteTextLayer,
                    onSnapFeedback = { feedback ->
                        if (!isolatedInput && layer.id == selectedLayerId) snapFeedback = feedback
                    }
                )
            }
        }

        // Match the actual visible photograph/montage rectangle, not the larger editor canvas.
        // In a free collage the logical canvas itself is the export target.
        val singlePhoto = if (collage == null) layers.filter { it.visible }.singleOrNull() else null
        val contextForInfo = LocalContext.current
        val singlePreview by rememberImagePreview(contextForInfo, singlePhoto?.uriString, maxDimension = 900)
        val photoAnchor = if (singlePhoto != null && directPhotoIsDocument) {
            androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidthPx, canvasHeightPx)
        } else if (singlePhoto != null && singlePreview != null) {
            val cropW = (singlePhoto.cropRight - singlePhoto.cropLeft).coerceIn(.01f, 1f)
            val cropH = (singlePhoto.cropBottom - singlePhoto.cropTop).coerceIn(.01f, 1f)
            val ratio = (singlePreview!!.width * cropW) / (singlePreview!!.height * cropH)
            val photoW = minOf(canvasWidthPx * .90f, canvasHeightPx * .83f * ratio) * singlePhoto.scale
            val photoH = photoW / ratio
            val cx = canvasWidthPx * (.5f + singlePhoto.offsetXRatio)
            val cy = canvasHeightPx * (.5f + singlePhoto.offsetYRatio)
            androidx.compose.ui.geometry.Rect(cx-photoW/2f, cy-photoH/2f,
                cx+photoW/2f, cy+photoH/2f)
        } else if (collage != null && collage.mode == CollageMode.Template) {
            val collageWidth = minOf(canvasWidthPx, canvasHeightPx * collage.aspect.ratio)
            val collageHeight = collageWidth / collage.aspect.ratio
            androidx.compose.ui.geometry.Rect((canvasWidthPx-collageWidth)/2f,
                (canvasHeightPx-collageHeight)/2f,
                (canvasWidthPx+collageWidth)/2f, (canvasHeightPx+collageHeight)/2f)
        } else androidx.compose.ui.geometry.Rect(0f,0f,canvasWidthPx,canvasHeightPx)

        if (cropLayerId == null) {
            infoLayers.forEach { info ->
                if (info.visible && info.overlayMode && info.id != editingInfoId) {
                    InfoOverlayItem(
                        layer = info,
                        selected = !isolatedInput && info.id == selectedLayerId,
                        canvasWidthPx=canvasWidthPx, canvasHeightPx=canvasHeightPx,
                        photoBounds = photoAnchor,
                        onSelect = onSelectLayer,
                        onEdit = onEditInfo,
                        onUpdate = onUpdateInfoLayer,
                        onDelete = onDeleteInfo,
                        onDuplicate = onDuplicateInfo,
                        onSnapFeedback = { if (!isolatedInput) snapFeedback = it }
                    )
                }
            }
        }

        // 专注输入遮罩：进入自由文字或文本模板输入后，底下所有图层都不可点击。
        // 遮罩只隔离交互，不改变实际项目内容；完成输入后立即恢复。
        if (isolatedInput && cropLayerId == null) {
            Box(
                Modifier
                    .matchParentSize()
                    .zIndex(45f)
                    .background(if (isolatedTextInput) Color.Transparent else Color(0x7AEEF2F8))
                    .pointerInput(editingTextId, editingInfoId) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            )
        }

        // 当前自由文本输入框永远位于专注遮罩之上。
        if (cropLayerId == null && editingTextLayer != null) {
            TextLayerItem(
                layer = editingTextLayer,
                selected = true,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                snapThresholdPx = snapThresholdPx,
                layoutContentScale = layoutContentScale,
                otherImageCentersX = layers.filter { it.visible }.map { it.offsetXRatio },
                otherImageCentersY = layers.filter { it.visible }.map { it.offsetYRatio },
                otherTextCentersX = textLayers.filter { it.id != editingTextLayer.id && it.visible }.map { it.offsetXRatio },
                otherTextCentersY = textLayers.filter { it.id != editingTextLayer.id && it.visible }.map { it.offsetYRatio },
                editing = true,
                onSelectLayer = onSelectLayer,
                onUpdateLayer = onUpdateTextLayer,
                onEditText = onEditText,
                onDoneTextEdit = onDoneTextEdit,
                onDuplicate = onDuplicateTextLayer,
                onDelete = onDeleteTextLayer,
                onSnapFeedback = { snapFeedback = it }
            )
        }

        // FIX1: 文本模板输入框移至 EditorScreen 最外层独立浮层。
        // 键盘弹出不再把编辑卡放在随画布收缩的容器中（此处仅保留交互遮罩）。

        if (drawingFreehand && cropLayerId == null && !isolatedInput) {
            DoodleDrawingOverlay(
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                tool = doodleTool,
                eraseMode = doodleEraseMode,
                eraserSize = doodleEraserSize,
                color = doodleColor,
                strokeWidth = doodleStrokeWidth,
                opacity = doodleOpacity,
                sessionId = doodleSessionId,
                doodles = shapeLayers.filter { it.doodleSessionId != null && it.visible && !it.locked },
                onComplete = onDrawFreehand,
                onErase = onEraseDoodleStrokes,
                onViewportTransform = { pan, zoom ->
                    viewportScale = (viewportScale * zoom).coerceIn(1f, 3f)
                    viewportPan = Offset(
                        (viewportPan.x + pan.x * viewportScale).coerceIn(-canvasWidthPx, canvasWidthPx),
                        (viewportPan.y + pan.y * viewportScale).coerceIn(-canvasHeightPx, canvasHeightPx)
                    )
                }
            )
        }

        if (cropLayerId == null && !isolatedInput) {
            SnapGuides(
                feedback = snapFeedback,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx
            )
        }
    }
        if (dockedBottomInfo.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            DockedInfoPanelSection(
                modifier = Modifier.onSizeChanged { bottomMeasured = it.height },
                layers = dockedBottomInfo,
                selectedLayerId = selectedLayerId,
                editingInfoId = editingInfoId,
                isolatedInput = isolatedInput,
                infoKeyboardSafe = infoKeyboardSafe,
                infoEditScale = infoEditScale,
                onInfoEditScaleChange = onInfoEditScaleChange,
                onSelect = onSelectLayer,
                onEdit = onEditInfo,
                onUpdate = onUpdateInfoLayer,
                onDone = onDoneInfoEdit,
                onDelete = onDeleteInfo
            )
        }
    }
}

@Composable
private fun BoxScope.ImageLayerItem(
    layer: ImageLayer,
    selected: Boolean,
    frame: CanvasFrame,
    directPhotoIsDocument: Boolean,
    singlePhotoMode: Boolean,
    cropEditing: Boolean,
    showCropOverlay: Boolean,
    cropDraft: CropRect?,
    otherLayers: List<ImageLayer>,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    snapThresholdPx: Float,
    layoutContentScale: Float,
    maxCanvasWidth: Dp,
    maxCanvasHeight: Dp,
    onSelectLayer: (String?) -> Unit,
    onTapImageLayer: (String) -> Unit,
    onUpdateLayer: (ImageLayer) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onCropDraftChange: (CropRect) -> Unit,
    onSnapFeedback: (SnapFeedback?) -> Unit
) {
    val context = LocalContext.current
    val preview by rememberImagePreview(context, layer.uriString)

    if (preview == null) {
        if (selected) {
            GlassCard(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(.72f).height(130.dp),
                radius = 22.dp,
                elevation = 10.dp,
                padding = PaddingValues(16.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TgfIcon(TgfIconType.Photo, modifier = Modifier.size(42.dp), selected = true)
                    Spacer(Modifier.height(8.dp))
                    Text("正在载入照片…", fontSize = 13.sp, color = TgfTokens.InkSecondary)
                }
            }
        }
        return
    }

    val effected by produceState<ImageBitmap?>(preview, preview, layer.photoStrokes) {
        value = preview
        if (layer.photoStrokes.isNotEmpty()) {
            value = PhotoEffectsPreviewRenderer.render(preview!!, layer.photoStrokes)
        }
    }
    val bitmap = effected ?: preview!!
    val sourceCrop = when {
        cropEditing && showCropOverlay && cropDraft != null -> CropRect(0f, 0f, 1f, 1f)
        cropEditing && cropDraft != null -> cropDraft.sanitize()
        else -> CropRect(layer.cropLeft, layer.cropTop, layer.cropRight, layer.cropBottom).sanitize()
    }
    val cropWidth = (sourceCrop.right - sourceCrop.left).coerceAtLeast(.01f)
    val cropHeight = (sourceCrop.bottom - sourceCrop.top).coerceAtLeast(.01f)
    val imageRatio = (bitmap.width * cropWidth) / (bitmap.height * cropHeight)

    // 先依据首次完整画布确定照片基准尺寸，再随着可见画布等比缩放。
    // 避免底部面板展开后只改变高度、照片仍由宽度决定而保持原大小。
    // During crop editing the full source must remain visible and undistorted so
    // the crop handles can reach outside the previously committed crop rectangle.
    val fillDocument = directPhotoIsDocument && !cropEditing
    val availableWidth = maxCanvasWidth * when {
        cropEditing -> .98f
        fillDocument -> 1f
        else -> .90f
    } * layoutContentScale
    val availableHeight = maxCanvasHeight * when {
        cropEditing -> .92f
        fillDocument -> 1f
        else -> .83f
    } * layoutContentScale
    val availableRatio = availableWidth.value / availableHeight.value

    val baseWidth: Dp
    val baseHeight: Dp
    if (fillDocument) {
        // The source crop rotates INSIDE the document. For a 90-degree rotation the
        // unrotated rectangle is portrait while the surrounding document is landscape.
        // Make the rotated bounding box fill the document without stretching pixels.
        val angle = layer.rotation * (kotlin.math.PI / 180.0)
        val c = kotlin.math.abs(kotlin.math.cos(angle)).toFloat()
        val t = kotlin.math.abs(kotlin.math.sin(angle)).toFloat()
        val boundW = (bitmap.width * cropWidth * c + bitmap.height * cropHeight * t).coerceAtLeast(.001f)
        val boundH = (bitmap.width * cropWidth * t + bitmap.height * cropHeight * c).coerceAtLeast(.001f)
        baseWidth = availableWidth * ((bitmap.width * cropWidth) / boundW)
        baseHeight = availableHeight * ((bitmap.height * cropHeight) / boundH)
    } else if (imageRatio >= availableRatio) {
        baseWidth = availableWidth
        baseHeight = availableWidth / imageRatio
    } else {
        baseHeight = availableHeight
        baseWidth = availableHeight * imageRatio
    }

    val density = LocalDensity.current
    val baseWidthPx = with(density) { baseWidth.toPx() }
    val baseHeightPx = with(density) { baseHeight.toPx() }

    var rawOffsetXRatio by remember(layer.id, layer.uriString) { mutableStateOf(layer.offsetXRatio) }
    var rawOffsetYRatio by remember(layer.id, layer.uriString) { mutableStateOf(layer.offsetYRatio) }
    var rawRotation by remember(layer.id, layer.uriString) { mutableStateOf(layer.rotation) }
    var gestureOffsetXRatio by remember(layer.id, layer.uriString) { mutableStateOf(layer.offsetXRatio) }
    var gestureOffsetYRatio by remember(layer.id, layer.uriString) { mutableStateOf(layer.offsetYRatio) }
    var gestureScale by remember(layer.id, layer.uriString) { mutableStateOf(layer.scale) }
    var gestureRotation by remember(layer.id, layer.uriString) { mutableStateOf(layer.rotation) }
    var gesturePulse by remember(layer.id) { mutableStateOf(0) }
    var gestureActive by remember(layer.id) { mutableStateOf(false) }
    val latestLayer by rememberUpdatedState(layer)

    LaunchedEffect(
        gestureActive,
        layer.uriString,
        layer.offsetXRatio,
        layer.offsetYRatio,
        layer.scale,
        layer.rotation
    ) {
        if (!gestureActive) {
            rawOffsetXRatio = layer.offsetXRatio
            rawOffsetYRatio = layer.offsetYRatio
            rawRotation = layer.rotation
            gestureOffsetXRatio = layer.offsetXRatio
            gestureOffsetYRatio = layer.offsetYRatio
            gestureScale = layer.scale
            gestureRotation = layer.rotation
        }
    }

    LaunchedEffect(gesturePulse) {
        if (gesturePulse > 0) {
            delay(180)
            gestureActive = false
            onSnapFeedback(null)
        }
    }

    var modifier = Modifier
        .align(Alignment.Center)
        .size(baseWidth, baseHeight)
        .graphicsLayer {
            translationX = if (cropEditing) 0f else gestureOffsetXRatio * canvasWidthPx
            translationY = if (cropEditing) 0f else gestureOffsetYRatio * canvasHeightPx
            scaleX = if (cropEditing) 1f else gestureScale
            scaleY = if (cropEditing) 1f else gestureScale
            rotationZ = gestureRotation
        }
        .pointerInput(layer.id) {
            detectTapGestures(onTap = { onTapImageLayer(layer.id) })
        }

    if (!singlePhotoMode && !layer.locked && !cropEditing) {
        modifier = modifier.pointerInput(
            layer.id,
            layer.uriString,
            canvasWidthPx,
            canvasHeightPx,
            baseWidthPx,
            baseHeightPx,
            otherLayers
        ) {
            detectTransformGestures { _, pan, zoom, rotation ->
                onSelectLayer(layer.id)
                gestureActive = true
                gesturePulse += 1

                // FIX4: Pan is delivered in the unscaled, unrotated layer coordinate system.
                // Convert it to visible canvas pixels before storing normalized offsets.
                // Use the transform that produced this pointer delta (before this frame's zoom).
                val previousScale = gestureScale
                val previousAngle = gestureRotation * PI.toFloat() / 180f
                val screenPanX = (pan.x * cos(previousAngle) - pan.y * sin(previousAngle)) * previousScale
                val screenPanY = (pan.x * sin(previousAngle) + pan.y * cos(previousAngle)) * previousScale
                rawOffsetXRatio = (rawOffsetXRatio + screenPanX / canvasWidthPx).coerceIn(-1.5f, 1.5f)
                rawOffsetYRatio = (rawOffsetYRatio + screenPanY / canvasHeightPx).coerceIn(-1.5f, 1.5f)
                gestureScale = (gestureScale * zoom).coerceIn(.25f, 24f)
                rawRotation = normalizeRotation(rawRotation + rotation)

                val rotationSnap = snapRotation(rawRotation)
                gestureRotation = rotationSnap.first

                val bound = rotatedBounds(
                    width = baseWidthPx * gestureScale,
                    height = baseHeightPx * gestureScale,
                    degrees = gestureRotation
                )
                val xSnap = snapAxis(
                    rawRatio = rawOffsetXRatio,
                    boundPx = bound.first,
                    canvasPx = canvasWidthPx,
                    thresholdPx = snapThresholdPx,
                    otherCenterRatios = otherLayers.map { it.offsetXRatio }
                )
                val ySnap = snapAxis(
                    rawRatio = rawOffsetYRatio,
                    boundPx = bound.second,
                    canvasPx = canvasHeightPx,
                    thresholdPx = snapThresholdPx,
                    otherCenterRatios = otherLayers.map { it.offsetYRatio }
                )

                gestureOffsetXRatio = xSnap.value
                gestureOffsetYRatio = ySnap.value

                val feedback = SnapFeedback(
                    verticalGuideRatio = xSnap.guideRatio,
                    horizontalGuideRatio = ySnap.guideRatio,
                    rotationAngle = if (rotationSnap.second) gestureRotation else null
                )
                onSnapFeedback(
                    if (feedback.verticalGuideRatio != null || feedback.horizontalGuideRatio != null || feedback.rotationAngle != null) {
                        feedback
                    } else null
                )

                onUpdateLayer(
                    latestLayer.copy(
                        offsetXRatio = gestureOffsetXRatio,
                        offsetYRatio = gestureOffsetYRatio,
                        scale = gestureScale,
                        rotation = gestureRotation
                    )
                )
            }
        }
    }

    // Compute frame inset once in composable scope. Do not call LocalDensity.current
    // inside the non-composable modifier builder lambdas.
    val photoFrameInset = ((minOf(canvasWidthPx, canvasHeightPx) * frame.normalized().widthRatio)
        .coerceAtLeast(1f) / density.density).dp
    val hasPhotoFrame = frame.enabled && frame.scope == CanvasFrameScope.Photos && !cropEditing
    val photoFrameShape = if (hasPhotoFrame && frame.corner == CanvasFrameCorner.Rounded)
        RoundedCornerShape(10.dp) else RoundedCornerShape(0.dp)
    Box(modifier = if (hasPhotoFrame) modifier.clip(photoFrameShape)
        .background(Color(frame.colorArgb)) else modifier) {
        CroppedImage(
            bitmap = bitmap,
            crop = sourceCrop,
            flipHorizontal = layer.flipHorizontal,
            flipVertical = layer.flipVertical,
            toneBrightness = layer.brightness,
            toneContrast = layer.contrast,
            toneSaturation = layer.saturation,
            toneTemperature = layer.temperature,
            toneTint = layer.tint,
            toneHighlights = layer.highlights,
            toneShadows = layer.shadows,
            modifier = if (hasPhotoFrame) Modifier.fillMaxSize()
                .padding(photoFrameInset)
                .clip(if (frame.corner == CanvasFrameCorner.Rounded)
                    RoundedCornerShape((10f - photoFrameInset.value).coerceAtLeast(0f).dp)
                    else RoundedCornerShape(0.dp))
                else Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
        )

        if (selected && (cropEditing || !singlePhotoMode)) {
            if (cropEditing && showCropOverlay && cropDraft != null) {
                val visualDraft = cropDraft.toVisual(layer.flipHorizontal, layer.flipVertical)
                CropOverlay(
                    crop = visualDraft,
                    onChange = { visual ->
                        onCropDraftChange(visual.toSource(layer.flipHorizontal, layer.flipVertical).sanitize())
                    }
                )
            } else {
                Box(
                    Modifier.matchParentSize().border(
                        1.dp,
                        TgfTokens.Blue.copy(alpha = .88f),
                        RoundedCornerShape(4.dp)
                    )
                )
                if (singlePhotoMode) {
                    CornerDeleteButton(
                        modifier = Modifier.align(Alignment.TopStart)
                            .offset(x = (-13).dp, y = (-13).dp).zIndex(24f),
                        onClick = { onDeleteLayer(layer.id) }
                    )
                } else {
                    Box(
                        Modifier.align(Alignment.TopStart)
                            .offset(x = (-16).dp, y = (-16).dp)
                            .size(40.dp)
                            .zIndex(24f)
                            .clickable { onDeleteLayer(layer.id) }
                            .semantics { contentDescription = "删除图片" },
                        contentAlignment = Alignment.Center
                    ) {
                        LightCornerIconSurface(iconSize = 22.dp, accent = Color(0xFFE48794)) {
                            TgfIcon(TgfIconType.Delete, Modifier.size(18.dp), bare = true)
                        }
                    }
                }
                // 角部操作按钮已经承担主要视觉提示，隐藏角点避免与图标重叠。
                ImageCornerCopyButton(
                    insidePlacement = !singlePhotoMode,
                    onClick = { onDuplicateLayer(layer.id) }
                )
                if (!layer.locked && !cropEditing) {
                    var totalDrag by remember(layer.id) { mutableStateOf(Offset.Zero) }
                    var startScale by remember(layer.id) { mutableFloatStateOf(layer.scale) }
                    var startRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }
                    val initVector = Offset(baseWidthPx/2f,-baseHeightPx/2f)
                    ImageCornerTransformButton(
                        insidePlacement = !singlePhotoMode,
                        onStart = {
                            gestureActive=true
                            totalDrag=Offset.Zero
                            startScale=gestureScale
                            startRotation=rawRotation
                        },
                        onMove = { delta ->
                            gestureActive=true
                            gesturePulse+=1
                            totalDrag+=delta
                            val newVector=initVector+totalDrag
                            gestureScale=(startScale*(newVector.getDistance()/initVector.getDistance().coerceAtLeast(1f))).coerceIn(.25f,24f)
                            val startAngle=atan2(initVector.y,initVector.x)
                            val newAngle=atan2(newVector.y,newVector.x)
                            rawRotation=normalizeRotation(startRotation+(newAngle-startAngle)*180f/PI.toFloat())
                            val snap=snapRotation(rawRotation)
                            gestureRotation=snap.first
                            onSnapFeedback(if(snap.second) SnapFeedback(rotationAngle=gestureRotation) else null)
                            onUpdateLayer(latestLayer.copy(scale=gestureScale,rotation=gestureRotation))
                        },
                        onStop={ gestureActive=false;onSnapFeedback(null) }
                    )
                }
                if (layer.locked) {
                    GlassCard(
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-18).dp),
                        radius = 12.dp,
                        elevation = 4.dp,
                        padding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("已锁定", fontSize = 9.sp, color = TgfTokens.Blue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TextLayerItem(
    layer: TextLayer,
    selected: Boolean,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    snapThresholdPx: Float,
    layoutContentScale: Float,
    otherImageCentersX: List<Float>,
    otherImageCentersY: List<Float>,
    otherTextCentersX: List<Float>,
    otherTextCentersY: List<Float>,
    editing: Boolean,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayer: (TextLayer) -> Unit,
    onEditText: (String) -> Unit,
    onDoneTextEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSnapFeedback: (SnapFeedback?) -> Unit
) {
    var rawOffsetXRatio by remember(layer.id) { mutableStateOf(layer.offsetXRatio) }
    var rawOffsetYRatio by remember(layer.id) { mutableStateOf(layer.offsetYRatio) }
    var rawRotation by remember(layer.id) { mutableStateOf(layer.rotation) }
    var gestureOffsetXRatio by remember(layer.id) { mutableStateOf(layer.offsetXRatio) }
    var gestureOffsetYRatio by remember(layer.id) { mutableStateOf(layer.offsetYRatio) }
    var gestureScale by remember(layer.id) { mutableStateOf(layer.scale) }
    var gestureRotation by remember(layer.id) { mutableStateOf(layer.rotation) }
    var gesturePulse by remember(layer.id) { mutableStateOf(0) }
    var gestureActive by remember(layer.id) { mutableStateOf(false) }
    var boxSize by remember(layer.id) { mutableStateOf(IntSize.Zero) }
    val latestLayer by rememberUpdatedState(layer)
    val focusRequester = remember(layer.id) { FocusRequester() }
    val textBringIntoView = remember(layer.id) { BringIntoViewRequester() }
    val keyboardInsetPx = WindowInsets.ime.getBottom(LocalDensity.current)
    // DEV14: 保持编辑态/展示态相同的自动换行宽度，老项目默认268dp。
    val textContentWidth = layer.boxWidthDp.coerceIn(88, 640).dp
    val frameMinHeight = layer.boxHeightDp.coerceIn(0, 720).dp
    val textHandleDensity = LocalDensity.current.density

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    // 输入时（尤其位于图片下半部的文本）随软键盘显示自动滚动到可见区域。
    // request 不改变文字层的位置/缩放，也不写入撤销或项目存档。
    LaunchedEffect(editing, keyboardInsetPx) {
        if (editing) {
            delay(if (keyboardInsetPx > 0) 100L else 180L)
            textBringIntoView.bringIntoView()
        }
    }

    LaunchedEffect(
        gestureActive,
        layer.offsetXRatio,
        layer.offsetYRatio,
        layer.scale,
        layer.rotation
    ) {
        if (!gestureActive) {
            rawOffsetXRatio = layer.offsetXRatio
            rawOffsetYRatio = layer.offsetYRatio
            rawRotation = layer.rotation
            gestureOffsetXRatio = layer.offsetXRatio
            gestureOffsetYRatio = layer.offsetYRatio
            gestureScale = layer.scale
            gestureRotation = layer.rotation
        }
    }

    LaunchedEffect(gesturePulse) {
        if (gesturePulse > 0) {
            delay(180)
            gestureActive = false
            onSnapFeedback(null)
        }
    }

    val outerModifier = Modifier
        .align(Alignment.Center)
        .zIndex(if (editing) 60f else 0f)
        .graphicsLayer {
            translationX = gestureOffsetXRatio * canvasWidthPx
            translationY = gestureOffsetYRatio * canvasHeightPx
            // 编辑和预览共用完全一致的画布比例；状态切换不改变文字几何变换。
            scaleX = gestureScale * layoutContentScale
            scaleY = gestureScale * layoutContentScale
            rotationZ = gestureRotation
        }
        .onGloballyPositioned { boxSize = it.size }

    Box(
        // requiredWidth preserves the actual user-selected width even when the
        // parent canvas is narrower. Regular width() silently clamped it, so a
        // horizontal handle drag changed saved data but not the visible text box.
        modifier = outerModifier.requiredWidth(textContentWidth + 46.dp)
            .heightIn(min = frameMinHeight + 40.dp)
    ) {
        var contentModifier = Modifier
            .padding(14.dp)
            .fillMaxWidth()
            .heightIn(min = frameMinHeight + 12.dp)
            .then(
                if ((layer.backgroundColorArgb ushr 24) != 0) Modifier.background(
                    Color(layer.backgroundColorArgb), RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .then(
                if (editing) Modifier
                    .border(1.dp, TgfTokens.Blue.copy(alpha = .90f), RoundedCornerShape(10.dp))
                else if (selected) Modifier
                    .border(1.dp, TgfTokens.Blue.copy(alpha = .90f), RoundedCornerShape(10.dp))
                else Modifier
            )
            // 普通状态：点击选中、双击进入输入。输入状态由专注遮罩隔离背景，
            // 文本框自身只负责文字输入，不再承担移动/删除/复制/样式等其它操作。
            .pointerInput(layer.id, editing) {
                if (!editing) {
                    detectTapGestures(
                        onTap = { onSelectLayer(layer.id) },
                        onDoubleTap = {
                            onSelectLayer(layer.id)
                            onEditText(layer.id)
                        }
                    )
                }
            }

        // 手势只绑定在“文字内容框”本身，不覆盖三个角控制点。
        // 这样右上角旋转缩放手柄不会再与文本移动手势互相抢事件。
        if (!layer.locked && !editing) {
            contentModifier = contentModifier.pointerInput(
                layer.id,
                canvasWidthPx,
                canvasHeightPx,
                otherImageCentersX,
                otherImageCentersY,
                otherTextCentersX,
                otherTextCentersY
            ) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelectLayer(layer.id)
                    gestureActive = true
                    gesturePulse += 1

                    // FIX4: The text's graphicsLayer scales by gestureScale * layoutContentScale.
                    // Pointer pan is local, so compensate for both scaling and rotation.
                    val previousScale = gestureScale * layoutContentScale
                    val previousAngle = gestureRotation * PI.toFloat() / 180f
                    val screenPanX = (pan.x * cos(previousAngle) - pan.y * sin(previousAngle)) * previousScale
                    val screenPanY = (pan.x * sin(previousAngle) + pan.y * cos(previousAngle)) * previousScale
                    rawOffsetXRatio = (rawOffsetXRatio + screenPanX / canvasWidthPx).coerceIn(-1.5f, 1.5f)
                    rawOffsetYRatio = (rawOffsetYRatio + screenPanY / canvasHeightPx).coerceIn(-1.5f, 1.5f)
                    gestureScale = (gestureScale * zoom).coerceIn(.35f, 5f)
                    rawRotation = normalizeRotation(rawRotation + rotation)

                    val rotationSnap = snapRotation(rawRotation)
                    gestureRotation = rotationSnap.first

                    // DEV11: Snap actual rotated text edges as well as centers.
                    // Uses screen pixels after current viewport/scale, not the unscaled text width.
                    val (snappedBoundW, snappedBoundH) = rotatedBounds(
                        boxSize.width.toFloat().coerceAtLeast(1f) * gestureScale * layoutContentScale,
                        boxSize.height.toFloat().coerceAtLeast(1f) * gestureScale * layoutContentScale,
                        gestureRotation
                    )
                    val xSnap = snapAxis(
                        rawRatio = rawOffsetXRatio,
                        boundPx = snappedBoundW,
                        canvasPx = canvasWidthPx,
                        thresholdPx = snapThresholdPx,
                        otherCenterRatios = otherImageCentersX + otherTextCentersX
                    )
                    val ySnap = snapAxis(
                        rawRatio = rawOffsetYRatio,
                        boundPx = snappedBoundH,
                        canvasPx = canvasHeightPx,
                        thresholdPx = snapThresholdPx,
                        otherCenterRatios = otherImageCentersY + otherTextCentersY
                    )
                    gestureOffsetXRatio = xSnap.value
                    gestureOffsetYRatio = ySnap.value

                    val feedback = SnapFeedback(
                        verticalGuideRatio = xSnap.guideRatio,
                        horizontalGuideRatio = ySnap.guideRatio,
                        rotationAngle = if (rotationSnap.second) gestureRotation else null
                    )
                    onSnapFeedback(
                        if (feedback.verticalGuideRatio != null ||
                            feedback.horizontalGuideRatio != null ||
                            feedback.rotationAngle != null
                        ) feedback else null
                    )

                    onUpdateLayer(
                        latestLayer.copy(
                            offsetXRatio = gestureOffsetXRatio,
                            offsetYRatio = gestureOffsetYRatio,
                            scale = gestureScale,
                            rotation = gestureRotation
                        )
                    )
                }
            }
        }

        // v0.4.8：文本正在输入时禁止移动文本框。
        // BasicTextField 只负责光标、选择与文字输入，避免“输入”和“拖动”抢手势导致位置抖动。

        Box(
            modifier = contentModifier.padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            val align = when (layer.textAlign) {
                0 -> TextAlign.Left
                2 -> TextAlign.Right
                else -> TextAlign.Center
            }

            if (editing) {
                BasicTextField(
                    value = layer.text,
                    onValueChange = { onUpdateLayer(layer.copy(text = it.take(300))) },
                    textStyle = TextStyle(
                        color = Color(layer.colorArgb),
                        fontSize = layer.fontSizeSp.sp,
                        fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (layer.underline) TextDecoration.Underline else TextDecoration.None,
                        fontFamily = fontFamilyForText(layer.fontFamilyId),
                        textAlign = align,
                        lineHeight = (layer.fontSizeSp * layer.lineSpacing.coerceIn(1f, 2f)).sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(textBringIntoView)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box {
                            if (layer.text.isEmpty()) {
                                Text(
                                    "请输入文字",
                                    fontSize = layer.fontSizeSp.sp,
                                    color = TgfTokens.InkSecondary.copy(alpha = .55f),
                                    textAlign = align
                                )
                            }
                            inner()
                        }
                    }
                )
            } else {
                Text(
                    text = layer.text.ifEmpty { "文字" },
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true,
                    fontSize = layer.fontSizeSp.sp,
                    fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (layer.underline) TextDecoration.Underline else TextDecoration.None,
                    fontFamily = fontFamilyForText(layer.fontFamilyId),
                    color = Color(layer.colorArgb),
                    textAlign = align,
                    lineHeight = (layer.fontSizeSp * layer.lineSpacing.coerceIn(1f, 2f)).sp
                )
            }
        }

        if (selected || editing) {
            CornerDeleteButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .zIndex(35f),
                onClick = { onDelete(layer.id) }
            )
        }

        // DEV10：文本框左下角快捷复制，与既有历史记录/撤销共用同一个回调。
        // 输入时隐藏，避免和系统键盘、文字选择手势冲突。
        if (selected && !editing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-16).dp, y = 16.dp)
                    .zIndex(36f)
                    .size(40.dp)
                    .semantics { contentDescription = "复制文字框" }
                    .clickable { onDuplicate(layer.id) },
                contentAlignment = Alignment.Center
            ) {
                LightCornerIconSurface(iconSize = 22.dp, accent = TgfTokens.Blue) {
                    TgfIcon(TgfIconType.Duplicate, modifier = Modifier.size(19.dp), bare = true)
                }
            }
        }

        if (editing) {
            // 输入模式只允许输入文字，右上角提供唯一且明显的“完成”出口。
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .zIndex(30f)
                    .height(34.dp)
                    .shadow(7.dp, RoundedCornerShape(13.dp), clip = false)
                    .background(
                        Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)),
                        RoundedCornerShape(13.dp)
                    )
                    .clickable { onDoneTextEdit(layer.id) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "完成",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else if (selected && !layer.locked) {
            // DEV39 FIX1: 右上角一个手柄完成旋转与自由宽高缩放；不再显示独立宽高手柄。
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .zIndex(36f)
                    .size(42.dp)
                    .semantics { contentDescription = "右上角拖动旋转并自由调整文字框宽高" }
                    .pointerInput(layer.id, textHandleDensity) {
                        awaitEachGesture {
                            val first = awaitFirstDown(requireUnconsumed = false)
                            val startLayer = latestLayer
                            val frame = boxSize
                            var accumulated = Offset.Zero
                            var ended = false
                            gestureActive = true
                            onSelectLayer(layer.id)
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == first.id } ?: break
                                val delta = pointer.position - pointer.previousPosition
                                if (delta != Offset.Zero) {
                                    accumulated += delta
                                    pointer.consume()
                                    if (frame.width > 0 && frame.height > 0) {
                                        val rotation = topRightTextHandleDrag(
                                            frame.width.toFloat(),
                                            frame.height.toFloat(),
                                            accumulated.x,
                                            accumulated.y
                                        )
                                        val snapped = snapRotation(normalizeRotation(
                                            startLayer.rotation + rotation.rotationDeltaDegrees
                                        ))
                                        gestureRotation = snapped.first
                                        val transform = topRightTextFreeTransformDrag(
                                            startLayer.boxWidthDp, startLayer.boxHeightDp,
                                            frame.width.toFloat(), frame.height.toFloat(),
                                            accumulated.x, accumulated.y, textHandleDensity
                                        )
                                        onUpdateLayer(startLayer.copy(
                                            boxWidthDp = transform.widthDp,
                                            boxHeightDp = transform.minimumHeightDp,
                                            rotation = snapped.first
                                        ))
                                        onSnapFeedback(if (snapped.second) SnapFeedback(rotationAngle = snapped.first) else null)
                                    }
                                }
                                ended = !pointer.pressed
                            } while (!ended)
                            gestureActive = false
                            onSnapFeedback(null)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                LightCornerIconSurface(iconSize = 23.dp, accent = TgfTokens.Blue) {
                    TransformHandleGlyph(modifier = Modifier.size(18.dp), color = TgfTokens.Blue)
                }
            }
        }

        if (!editing && selected && layer.locked) {
            GlassCard(
                modifier = Modifier.align(Alignment.TopStart).zIndex(10f),
                radius = 10.dp,
                elevation = 3.dp,
                padding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("锁定", fontSize = 8.sp, color = TgfTokens.Blue)
            }
        }
    }
}

@Composable
private fun TextModePanel(
    modifier: Modifier,
    onAddTextBox: () -> Unit,
    onInfoText: () -> Unit,
    onClose: () -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 23.dp,
        elevation = 12.dp,
        padding = PaddingValues(13.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("文字", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Spacer(Modifier.weight(1f))
                Text("关闭", fontSize = 11.sp, color = TgfTokens.Blue, modifier = Modifier.clickable(onClick = onClose))
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier.weight(1f)
                        .tgfToolSurface(selected = true, shape = RoundedCornerShape(17.dp))
                        .clickable(onClick = onAddTextBox)
                        .padding(vertical = 10.dp, horizontal = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TgfIcon(TgfIconType.Text, modifier = Modifier.size(36.dp), selected = true)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("＋ 文本框", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                            Text("添加后直接在画布输入", fontSize = 9.sp, color = TgfTokens.InkSecondary)
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f)
                        .tgfToolSurface(selected = false, shape = RoundedCornerShape(17.dp))
                        .clickable(onClick = onInfoText)
                        .padding(vertical = 10.dp, horizontal = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TgfIcon(TgfIconType.Template, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("＋ 文本模板", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                            Text("影视接戏照 · 自定义模板等", fontSize = 9.sp, color = TgfTokens.InkSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextEditorPanel(
    modifier: Modifier,
    layer: TextLayer,
    onUpdate: (TextLayer) -> Unit,
    onDone: () -> Unit
) {
    val colors = listOf(
        0xFF202536.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF3578E5.toInt(),
        0xFFE04D5A.toInt(),
        0xFFF2B84B.toInt()
    )
    var textSettingsTab by remember(layer.id) { mutableStateOf(0) }
    // One palette edits either glyph foreground or entire text-frame background.
    var colorTarget by remember(layer.id) { mutableIntStateOf(0) } // 0 text, 1 frame
    var showCustomColor by remember(layer.id) { mutableStateOf(false) }
    val pickedColor = if (colorTarget == 0) layer.colorArgb else
        if ((layer.backgroundColorArgb ushr 24) == 0) 0xFFFFFFFF.toInt() else layer.backgroundColorArgb
    val swatchSelectionColor = if (colorTarget == 0) layer.colorArgb else layer.backgroundColorArgb
    fun updatedColor(argb: Int): TextLayer = if (colorTarget == 0) {
        layer.copy(colorArgb = argb)
    } else layer.copy(backgroundColorArgb = argb)

    var red by remember(layer.id) { mutableFloatStateOf(android.graphics.Color.red(layer.colorArgb).toFloat()) }
    var green by remember(layer.id) { mutableFloatStateOf(android.graphics.Color.green(layer.colorArgb).toFloat()) }
    var blue by remember(layer.id) { mutableFloatStateOf(android.graphics.Color.blue(layer.colorArgb).toFloat()) }
    val initialHsv = remember(layer.id) { FloatArray(3).also { android.graphics.Color.colorToHSV(layer.colorArgb, it) } }
    var hue by remember(layer.id) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(layer.id) { mutableFloatStateOf(initialHsv[1].coerceIn(0f, 1f)) }
    var brightness by remember(layer.id) { mutableFloatStateOf(initialHsv[2].coerceIn(0f, 1f)) }
    var customColorMode by remember(layer.id) { mutableStateOf(CustomColorMode.Wheel) }

    fun syncHsvFromColor(argb: Int) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hue = hsv[0]
        saturation = hsv[1].coerceIn(0f, 1f)
        brightness = hsv[2].coerceIn(0f, 1f)
    }

    fun applyCustomColor(r: Float = red, g: Float = green, b: Float = blue) {
        val argb = android.graphics.Color.rgb(r.roundToInt(), g.roundToInt(), b.roundToInt())
        syncHsvFromColor(argb)
        onUpdate(updatedColor(argb))
    }

    fun applyHsvColor(h: Float = hue, sValue: Float = saturation, vValue: Float = brightness) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, sValue, vValue))
        red = android.graphics.Color.red(argb).toFloat()
        green = android.graphics.Color.green(argb).toFloat()
        blue = android.graphics.Color.blue(argb).toFloat()
        onUpdate(updatedColor(argb))
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 23.dp,
        elevation = 12.dp,
        padding = PaddingValues(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("文字样式", fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Spacer(Modifier.weight(1f))
                EditorDoneButton(onClick = onDone)
            }
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("字体", "排版", "颜色").forEachIndexed { index, label ->
                    Box(Modifier.weight(1f)) {
                        TextSettingsTabChip(label, selected = textSettingsTab == index) {
                            textSettingsTab = index
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (textSettingsTab) {
                0 -> {
                    // 使用系统内置字体，不依赖外部字体文件；三种类型均可即时预览。
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("默认字体", "衬线字体", "等宽字体").forEachIndexed { index, name ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (layer.fontFamilyId == index) TgfTokens.Blue.copy(alpha = .16f)
                                        else TgfTokens.SurfaceRaised.copy(alpha = .78f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (layer.fontFamilyId == index) TgfTokens.SelectionInk else TgfTokens.GlassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onUpdate(layer.copy(fontFamilyId = index)) }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Aa 文", fontSize = 15.sp,
                                        fontFamily = fontFamilyForText(index), color = TgfTokens.Ink)
                                    Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                        color = TgfTokens.Ink)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("字号", fontSize = 11.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(7.dp))
                        TextPropertyButton("A−") {
                            onUpdate(layer.copy(fontSizeSp = (layer.fontSizeSp - 1f).coerceIn(10f, 120f)))
                        }
                        Spacer(Modifier.width(7.dp))
                        Slider(
                            value = layer.fontSizeSp.coerceIn(10f, 120f),
                            onValueChange = { onUpdate(layer.copy(fontSizeSp = it.coerceIn(10f, 120f))) },
                            valueRange = 10f..120f,
                            modifier = Modifier.weight(1f).height(36.dp)
                        )
                        Text(layer.fontSizeSp.roundToInt().toString(),
                            fontSize = 11.sp, color = TgfTokens.Ink,
                            modifier = Modifier.width(30.dp))
                        TextPropertyButton("A+") {
                            onUpdate(layer.copy(fontSizeSp = (layer.fontSizeSp + 1f).coerceIn(10f, 120f)))
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("字形", fontSize = 11.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(9.dp))
                        TextPropertyButton(if (layer.bold) "B 粗体✓" else "B 粗体",
                            selected = layer.bold) {
                            onUpdate(layer.copy(bold = !layer.bold))
                        }
                        Spacer(Modifier.width(6.dp))
                        TextPropertyButton("I 倾斜", selected = layer.italic) {
                            onUpdate(layer.copy(italic = !layer.italic))
                        }
                        Spacer(Modifier.width(6.dp))
                        TextPropertyButton("U̲ 下划线", selected = layer.underline) {
                            onUpdate(layer.copy(underline = !layer.underline))
                        }
                    }
                }
                1 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("对齐", fontSize = 11.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(10.dp))
                        listOf("≡ 左对齐", "≡ 居中", "≡ 右对齐").forEachIndexed { index, label ->
                            TextPropertyButton(label, selected = layer.textAlign == index) {
                                onUpdate(layer.copy(textAlign = index))
                            }
                            if (index < 2) Spacer(Modifier.width(5.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("行距", fontSize = 11.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = layer.lineSpacing.coerceIn(1f, 2f),
                            onValueChange = { onUpdate(layer.copy(lineSpacing = it.coerceIn(1f, 2f))) },
                            valueRange = 1f..2f,
                            modifier = Modifier.weight(1f).height(36.dp)
                        )
                        Text("%.2f×".format(java.util.Locale.US, layer.lineSpacing),
                            fontSize = 11.sp, color = TgfTokens.Ink,
                            modifier = Modifier.width(54.dp))
                        TextPropertyButton("重置") {
                            onUpdate(layer.copy(lineSpacing = 1.22f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("框宽", fontSize = 11.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(10.dp))
                        listOf(190 to "窄", 268 to "标准", 320 to "宽").forEachIndexed { index, (value, label) ->
                            TextPropertyButton(label, selected = layer.boxWidthDp == value) {
                                onUpdate(layer.copy(boxWidthDp = value))
                            }
                            if (index < 2) Spacer(Modifier.width(7.dp))
                        }
                    }
                }
                else -> {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically) {
                TextPropertyButton("字体颜色", selected = colorTarget == 0) {
                    colorTarget = 0
                    showCustomColor = false
                    val target = layer.colorArgb
                    red = android.graphics.Color.red(target).toFloat()
                    green = android.graphics.Color.green(target).toFloat()
                    blue = android.graphics.Color.blue(target).toFloat()
                    syncHsvFromColor(target)
                }
                Spacer(Modifier.width(7.dp))
                TextPropertyButton("背景颜色", selected = colorTarget == 1) {
                    colorTarget = 1
                    showCustomColor = false
                    val target = if ((layer.backgroundColorArgb ushr 24) == 0) 0xFFFFFFFF.toInt()
                        else layer.backgroundColorArgb
                    red = android.graphics.Color.red(target).toFloat()
                    green = android.graphics.Color.green(target).toFloat()
                    blue = android.graphics.Color.blue(target).toFloat()
                    syncHsvFromColor(target)
                }
                if (colorTarget == 1) {
                    Spacer(Modifier.width(7.dp))
                    TextPropertyButton("透明背景", selected = layer.backgroundColorArgb == 0) {
                        onUpdate(layer.copy(backgroundColorArgb = 0))
                        showCustomColor = false
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically) {
                Text("颜色", fontSize = 10.5.sp, color = TgfTokens.InkSecondary)
                Spacer(Modifier.width(9.dp))
                colors.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(if (swatchSelectionColor == argb && !showCustomColor) 26.dp else 23.dp)
                            .shadow(2.dp, CircleShape, clip = false)
                            .background(Color(argb), CircleShape)
                            .border(
                                if (swatchSelectionColor == argb && !showCustomColor) 2.dp else 1.dp,
                                if (swatchSelectionColor == argb && !showCustomColor) TgfTokens.SelectionInk else TgfTokens.SurfaceOutline,
                                CircleShape
                            )
                            .clickable {
                                showCustomColor = false
                                red = android.graphics.Color.red(argb).toFloat()
                                green = android.graphics.Color.green(argb).toFloat()
                                blue = android.graphics.Color.blue(argb).toFloat()
                                syncHsvFromColor(argb)
                                onUpdate(updatedColor(argb))
                            }
                    )
                }
                TextPropertyButton("自定义", selected = showCustomColor) {
                    showCustomColor = !showCustomColor
                    red = android.graphics.Color.red(pickedColor).toFloat()
                    green = android.graphics.Color.green(pickedColor).toFloat()
                    blue = android.graphics.Color.blue(pickedColor).toFloat()
                    syncHsvFromColor(pickedColor)
                }
            }

            if (showCustomColor) {
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(TgfTokens.SurfaceRaised.copy(alpha = .78f), RoundedCornerShape(13.dp))
                        .border(1.dp, TgfTokens.SurfaceOutline, RoundedCornerShape(13.dp))
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(26.dp)
                                    .background(Color(pickedColor), CircleShape)
                                    .border(1.dp, TgfTokens.SurfaceOutline, CircleShape)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text("自定义颜色", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TgfTokens.Ink)
                            Spacer(Modifier.weight(1f))
                            TextPropertyButton("色环", selected = customColorMode == CustomColorMode.Wheel) {
                                customColorMode = CustomColorMode.Wheel
                            }
                            Spacer(Modifier.width(5.dp))
                            TextPropertyButton("RGB", selected = customColorMode == CustomColorMode.RGB) {
                                customColorMode = CustomColorMode.RGB
                            }
                        }

                        Spacer(Modifier.height(7.dp))
                        if (customColorMode == CustomColorMode.Wheel) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ColorWheelPicker(
                                    hue = hue,
                                    saturation = saturation,
                                    brightness = brightness,
                                    onHueChange = {
                                        hue = it
                                        applyHsvColor(h = it)
                                    }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("饱和度", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                                    Slider(
                                        value = saturation,
                                        onValueChange = {
                                            saturation = it
                                            applyHsvColor(sValue = it)
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.height(32.dp)
                                    )
                                    Text("明度", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                                    Slider(
                                        value = brightness,
                                        onValueChange = {
                                            brightness = it
                                            applyHsvColor(vValue = it)
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.height(32.dp)
                                    )
                                    Text(
                                        "#%02X%02X%02X".format(red.roundToInt(), green.roundToInt(), blue.roundToInt()),
                                        fontSize = 9.sp,
                                        color = TgfTokens.InkSecondary
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("RGB 精确调整", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "#%02X%02X%02X".format(red.roundToInt(), green.roundToInt(), blue.roundToInt()),
                                    fontSize = 9.5.sp,
                                    color = TgfTokens.InkSecondary
                                )
                            }
                            ColorChannelSlider("R", red) {
                                red = it
                                applyCustomColor(r = it)
                            }
                            ColorChannelSlider("G", green) {
                                green = it
                                applyCustomColor(g = it)
                            }
                            ColorChannelSlider("B", blue) {
                                blue = it
                                applyCustomColor(b = it)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
}

@Composable
internal fun ColorWheelPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit
) {
    val latestOnHueChange = rememberUpdatedState(onHueChange)
    val ringColors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )
    Canvas(
        modifier = Modifier
            .size(118.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { position ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = atan2(position.y - center.y, position.x - center.x) * 180f / PI.toFloat()
                    latestOnHueChange.value((angle + 360f) % 360f)
                })
            }
            .pointerInput(Unit) {
                fun updateHue(position: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = atan2(position.y - center.y, position.x - center.x) * 180f / PI.toFloat()
                    latestOnHueChange.value((angle + 360f) % 360f)
                }
                detectDragGestures(
                    onDragStart = { updateHue(it) },
                    onDrag = { change, _ ->
                        updateHue(change.position)
                        change.consume()
                    }
                )
            }
    ) {
        val radius = size.minDimension * .46f
        val ringWidth = size.minDimension * .17f
        drawCircle(
            brush = Brush.sweepGradient(ringColors),
            radius = radius,
            style = Stroke(width = ringWidth)
        )
        drawCircle(
            color = TgfTokens.SurfaceRaised.copy(alpha = .82f),
            radius = size.minDimension * .29f
        )
        drawCircle(
            color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))),
            radius = size.minDimension * .22f
        )

        val angleRad = hue / 180f * PI.toFloat()
        val indicatorRadius = radius
        val indicator = Offset(
            center.x + cos(angleRad) * indicatorRadius,
            center.y + sin(angleRad) * indicatorRadius
        )
        drawCircle(Color.White, radius = size.minDimension * .055f, center = indicator)
        drawCircle(TgfTokens.Blue, radius = size.minDimension * .055f, center = indicator, style = Stroke(width = 1.4.dp.toPx()))
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(16.dp), fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f).height(30.dp)
        )
        Text(value.roundToInt().toString(), modifier = Modifier.width(28.dp), fontSize = 9.sp, color = TgfTokens.InkSecondary, textAlign = TextAlign.End)
    }
}

@Composable
private fun TextSettingsTabChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(40.dp)
            .background(
                if (selected) TgfTokens.Blue.copy(alpha = .16f)
                else TgfTokens.SurfaceRaised.copy(alpha = .75f),
                RoundedCornerShape(12.dp)
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) TgfTokens.SelectionInk else TgfTokens.GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) TgfTokens.SelectionInk else TgfTokens.Ink)
    }
}

@Composable
private fun TextPropertyButton(
    title: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.height(32.dp)
            .tgfToolSurface(selected, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 9.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) TgfTokens.Blue else TgfTokens.Ink)
    }
}

// DEV3: Overlay card has three clear controls and two direct edge-resize zones.
// Its geometry is stored as normalized values and reused by the exporter.
@Composable
private fun BoxScope.InfoOverlayItem(
    layer: InfoPanelLayer,
    selected: Boolean,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    photoBounds: androidx.compose.ui.geometry.Rect,
    onSelect: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onUpdate: (InfoPanelLayer) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onSnapFeedback: (SnapFeedback?) -> Unit
) {
    val density = LocalDensity.current
    val latest by rememberUpdatedState(layer)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val latestSelect by rememberUpdatedState(onSelect)
    val latestEdit by rememberUpdatedState(onEdit)
    // DEV5: All overlay dimensions and offsets use the visible picture rectangle.
    // This matches the exporter, which uses the rendered image/collage as its anchor.
    val anchorW = photoBounds.width.coerceAtLeast(1f)
    val anchorH = photoBounds.height.coerceAtLeast(1f)
    val provisionalW = anchorW * layer.widthRatio.coerceIn(.28f, .98f)
    val scale = layer.textScale.coerceIn(.90f, 1.15f)
    val estimatedCharsPerLine = (provisionalW / with(density) { (11f * scale).dp.toPx() })
        .roundToInt().coerceAtLeast(6)
    val labels = infoTemplateFieldLabels(layer.templateType)
    val fieldRows = listOf(
        labels.first to layer.primary, labels.second to layer.secondary, labels.third to layer.note
    ).filter { it.second.isNotBlank() }
    val bodyLines = fieldRows.sumOf { (label, value) ->
        (label.length + value.length + 1 + estimatedCharsPerLine - 1) / estimatedCharsPerLine
    }
    val narrowHeader = provisionalW < with(density) { 235.dp.toPx() } ||
        infoTemplateDisplayName(layer).length > 10
    // Count each visible field and the optional stacked date before checking rotated photo bounds.
    // Keep the frame responsive to newlines and text scale; the image boundary is still authoritative.
    val explicitLines = fieldRows.sumOf { (_, value) -> value.count { it == '\n' } }
    val estimatedHeightDp = 19f + (if (narrowHeader) 31f else 18f) * scale +
        (bodyLines + explicitLines) * 15f * scale
    val minHeight = with(density) { estimatedHeightDp.dp.toPx() }
    val geometry = resolveInfoOverlayGeometry(
        layer, anchorW, anchorH, minHeight
    )
    val w = geometry.width
    val actualHeight = geometry.height
    val visualWidth = with(density) { w.toDp() }
    val visualHeight = with(density) { actualHeight.toDp() }
    // Document colors match ProjectExporter.drawCompactOverlayInfo(),
    // independently of the App's light/dark UI theme.
    val fg = if(layer.darkBackground) Color.White else Color(0xFF212C3B)
    val subtitle = if(layer.darkBackground) Color(0xFFE5F0FE) else Color(0xFF42505E)
    val back = if(layer.darkBackground) Color(0xFF253045) else Color(0xFFF7FAFD)
    var moveX by remember(layer.id) { mutableFloatStateOf(layer.offsetXRatio) }
    var moveY by remember(layer.id) { mutableFloatStateOf(layer.offsetYRatio) }
    var dragging by remember(layer.id) { mutableStateOf(false) }
    var startW by remember(layer.id) { mutableFloatStateOf(layer.widthRatio) }
    var startH by remember(layer.id) { mutableFloatStateOf(layer.heightRatio) }
    var startRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }
    var total by remember(layer.id) { mutableStateOf(Offset.Zero) }
    // Local screen offset follows the finger; final coordinates enter project history once.
    val offsetX = (photoBounds.left + anchorW/2f - canvasWidthPx/2f) +
        if (dragging) moveX * anchorW else geometry.centerX
    val offsetY = (photoBounds.top + anchorH/2f - canvasHeightPx/2f) +
        if (dragging) moveY * anchorH else geometry.centerY
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset { IntOffset(offsetX.roundToInt(),offsetY.roundToInt()) }
            .size(visualWidth,visualHeight)
            .graphicsLayer { rotationZ=layer.rotation }
            .background(back,RoundedCornerShape(9.dp))
            .then(if(selected) Modifier.border(1.dp,TgfTokens.Blue,RoundedCornerShape(9.dp)) else Modifier)
            .pointerInput(layer.id) {
                detectTapGestures(
                    onTap = { latestSelect(layer.id) },
                    onDoubleTap = { latestSelect(layer.id);latestEdit(layer.id) }
                )
            }
            .pointerInput(layer.id, anchorW, anchorH) {
                detectDragGestures(
                    onDragStart = {
                        moveX = latest.offsetXRatio
                        moveY = latest.offsetYRatio
                        dragging = true
                        latestSelect(layer.id)
                    },
                    onDragEnd = {
                        latestUpdate(latest.copy(offsetXRatio = moveX, offsetYRatio = moveY))
                        dragging = false
                        onSnapFeedback(null)
                    },
                    onDragCancel = {
                        dragging = false
                        onSnapFeedback(null)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        // GraphicsLayer rotates the touch coordinate space; convert the
                        // drag displacement back to actual image X/Y before persisting.
                        val theta = latest.rotation * PI.toFloat() / 180f
                        val dx = cos(theta) * delta.x - sin(theta) * delta.y
                        val dy = sin(theta) * delta.x + cos(theta) * delta.y
                        moveX += dx / anchorW
                        moveY += dy / anchorH
                        // No mid-gesture center snap: otherwise the panel sticks/jumps.
                        // Bound only to the visible photograph and keep the cursor smooth.
                        val candidate = latest.copy(offsetXRatio = moveX, offsetYRatio = moveY)
                        val bounded = resolveInfoOverlayGeometry(
                            candidate, anchorW, anchorH, minHeight
                        )
                        moveX = bounded.centerX / anchorW
                        moveY = bounded.centerY / anchorH
                        // Avoid writing project state for every touch sample.
                        onSnapFeedback(null)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal=10.dp,vertical=8.dp),
            verticalArrangement=Arrangement.spacedBy(3.dp)) {
            val scale = layer.textScale.coerceIn(.90f, 1.15f)
            val family = fontFamilyForText(layer.fontFamilyId)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val title = infoTemplateDisplayName(layer)
                val stackHeader = maxWidth < 215.dp || title.length > 10
                if (stackHeader) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(1.dp)) {
                        Text(title, fontSize=(11f*scale).sp, fontWeight=FontWeight.SemiBold,
                            fontFamily=family, color=fg, softWrap=true)
                        Text("日期：${layer.date}", fontSize=(9f*scale).sp, fontFamily=family,
                            color=subtitle, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.End)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Text(title, fontSize=(11f*scale).sp, fontWeight=FontWeight.SemiBold,
                            fontFamily=family, color=fg, softWrap=true, modifier=Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        Text("日期：${layer.date}", fontSize=(9f*scale).sp, fontFamily=family,
                            color=subtitle, maxLines=1)
                    }
                }
            }
            val labels=infoTemplateFieldLabels(layer.templateType)
            if(layer.primary.isNotBlank()) Text("${labels.first}：${layer.primary}",fontSize=(10.5f*scale).sp,
                fontFamily=family, lineHeight=(13f*scale).sp,color=fg)
            if(layer.secondary.isNotBlank()) Text("${labels.second}：${layer.secondary}",fontSize=(10.5f*scale).sp,
                fontFamily=family, lineHeight=(13f*scale).sp,color=fg)
            if(layer.note.isNotBlank()) Text("${labels.third}：${layer.note}",fontSize=(10f*scale).sp,
                fontFamily=family, lineHeight=(13f*scale).sp,color=subtitle)
        }
        if(selected) {
            // Small visible circles with 46dp touch targets, high contrast on any photo.
            InfoOverlayAction(Alignment.TopStart,TgfIconType.Delete,"删除信息框",onClick={onDelete(layer.id)})
            InfoOverlayAction(Alignment.BottomStart,TgfIconType.Duplicate,"复制信息框",onClick={onDuplicate(layer.id)})
            Box(Modifier.align(Alignment.TopEnd).offset(x=16.dp,y=(-16).dp).size(40.dp)
                .zIndex(24f).semantics {contentDescription="拖动旋转缩放信息框"}
                .pointerInput(layer.id) {
                    detectDragGestures(onDragStart={
                        startW=latest.widthRatio;startH=latest.heightRatio
                        startRotation=latest.rotation;total=Offset.Zero
                    },onDragEnd={onSnapFeedback(null)},onDragCancel={onSnapFeedback(null)},
                        onDrag={change,delta ->
                            change.consume();total+=delta
                            val vector=Offset(w/2f,-actualHeight/2f)+total
                            val origin=Offset(w/2f,-actualHeight/2f)
                            val factor=vector.getDistance()/origin.getDistance().coerceAtLeast(1f)
                            val angle=atan2(vector.y,vector.x)-atan2(origin.y,origin.x)
                            val raw=normalizeRotation(startRotation+angle*180f/PI.toFloat())
                            val snapped=snapRotation(raw)
                            val candidate = latest.copy(
                                widthRatio=(startW*factor).coerceIn(.28f,.98f),
                                heightRatio=(startH*factor).coerceIn(.16f,.88f),
                                rotation=snapped.first
                            )
                            val bounded = resolveInfoOverlayGeometry(candidate, anchorW, anchorH, minHeight)
                            latestUpdate(candidate.copy(
                                widthRatio=(bounded.width/anchorW).coerceIn(.01f,.98f),
                                heightRatio=(bounded.height/anchorH).coerceIn(.01f,.98f),
                                offsetXRatio=bounded.centerX/anchorW,
                                offsetYRatio=bounded.centerY/anchorH
                            ))
                            onSnapFeedback(if(snapped.second) SnapFeedback(rotationAngle=snapped.first) else null)
                        })
                },contentAlignment=Alignment.Center) {
                LightCornerIconSurface(iconSize = 23.dp, accent = TgfTokens.Blue) {
                    TransformHandleGlyph(modifier = Modifier.size(18.dp), color = TgfTokens.Blue)
                }
            }
            // Resize zones are present only while selected; no width/height sliders or numeric forms.
            Box(Modifier.align(Alignment.CenterEnd).offset(x=9.dp).width(26.dp).fillMaxHeight(.45f)
                .zIndex(20f).semantics {contentDescription="拖动右边缘调整信息框宽度"}
                .pointerInput(layer.id) {detectDragGestures {change,delta ->
                    change.consume()
                    latestUpdate(latest.copy(widthRatio=
                        (latest.widthRatio+delta.x/anchorW).coerceIn(.28f,.98f)))
                }},contentAlignment=Alignment.Center) {
                Box(Modifier.width(3.dp).height(25.dp).background(TgfTokens.Blue,RoundedCornerShape(3.dp)))
            }
            Box(Modifier.align(Alignment.BottomCenter).offset(y=9.dp).height(26.dp).fillMaxWidth(.45f)
                .zIndex(20f).semantics {contentDescription="拖动下边缘调整信息框高度"}
                .pointerInput(layer.id) {detectDragGestures {change,delta ->
                    change.consume()
                    latestUpdate(latest.copy(heightRatio=
                        (latest.heightRatio+delta.y/anchorH).coerceIn(.16f,.88f)))
                }},contentAlignment=Alignment.Center) {
                Box(Modifier.height(3.dp).width(27.dp).background(TgfTokens.Blue,RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
private fun BoxScope.InfoOverlayAction(
    alignment: Alignment,
    type: TgfIconType,
    label: String,
    onClick: () -> Unit
) {
    Box(Modifier.align(alignment).offset(
        x=if(alignment==Alignment.TopStart||alignment==Alignment.BottomStart) (-16).dp else 16.dp,
        y=if(alignment==Alignment.TopStart||alignment==Alignment.TopEnd) (-16).dp else 16.dp)
        .size(40.dp).zIndex(22f).semantics {contentDescription=label}.clickable(onClick=onClick),
        contentAlignment=Alignment.Center) {
        LightCornerIconSurface(iconSize = 22.dp,
            accent = if (type==TgfIconType.Delete) Color(0xFFE48794) else TgfTokens.Blue) {
            TgfIcon(type, modifier = Modifier.size(19.dp), bare = true)
        }
    }
}

@Composable
private fun CompactInfoTools(
    modifier: Modifier,
    layer: InfoPanelLayer,
    section: String?,
    onSectionChange: (String) -> Unit,
    onEdit: () -> Unit,
    onUpdate: (InfoPanelLayer) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier.fillMaxWidth().background(TgfTokens.SurfaceRaised,RoundedCornerShape(15.dp))
        .padding(horizontal=9.dp,vertical=5.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,
            verticalAlignment=Alignment.CenterVertically) {
            InfoCompactButton("内容",TgfIconType.Text,onEdit)
            InfoCompactButton("排版",TgfIconType.Template,{onSectionChange("layout")})
            InfoCompactButton("样式",TgfIconType.Style,{onSectionChange("style")})
            InfoCompactButton("完成",TgfIconType.Reset,onDone)
        }
        when(section) {
            "layout" -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                InfoTemplateChip("叠加",layer.overlayMode) {onUpdate(layer.copy(overlayMode=true))}
                InfoTemplateChip("底部",!layer.overlayMode&&!layer.positionTop) {
                    onUpdate(layer.copy(overlayMode=false,positionTop=false,rotation=0f))
                }
                InfoTemplateChip("顶部",!layer.overlayMode&&layer.positionTop) {
                    onUpdate(layer.copy(overlayMode=false,positionTop=true,rotation=0f))
                }
            }
            "style" -> Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text("字体", fontSize=10.sp, color=TgfTokens.InkSecondary)
                    listOf("默认", "衬线", "等宽").forEachIndexed { i, label ->
                        InfoTemplateChip(label, layer.fontFamilyId == i) {
                            onUpdate(layer.copy(fontFamilyId=i))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text("字号", fontSize=10.sp, color=TgfTokens.InkSecondary)
                    listOf("紧凑" to .90f, "标准" to 1f, "大字" to 1.15f).forEach { (label, scale) ->
                        InfoTemplateChip(label, kotlin.math.abs(layer.textScale - scale) < .02f) {
                            onUpdate(layer.copy(textScale=scale))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    InfoTemplateChip("浅色",!layer.darkBackground) {onUpdate(layer.copy(darkBackground=false))}
                    InfoTemplateChip("深色",layer.darkBackground) {onUpdate(layer.copy(darkBackground=true))}
                }
            }
        }
    }
}

@Composable
private fun InfoCompactButton(title:String,type:TgfIconType,onClick:()->Unit) {
    if (title == "完成") {
        EditorDoneButton(onClick=onClick, compact=true)
    } else {
        Column(Modifier.clickable(onClick=onClick).padding(horizontal=8.dp,vertical=3.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            TgfIcon(type,modifier=Modifier.size(29.dp),selected=true)
            Text(title,fontSize=10.sp,fontWeight=FontWeight.Medium,color=TgfTokens.Ink)
        }
    }
}

@Composable
private fun DockedInfoPanelSection(
    modifier: Modifier = Modifier,
    layers: List<InfoPanelLayer>,
    selectedLayerId: String?,
    editingInfoId: String?,
    isolatedInput: Boolean,
    infoKeyboardSafe: Boolean,
    infoEditScale: Float,
    onInfoEditScaleChange: (Float) -> Unit,
    onSelect: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onUpdate: (InfoPanelLayer) -> Unit,
    onDone: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        layers.forEach { info ->
            Box(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                InfoPanelItem(
                    layer = info,
                    selected = !isolatedInput && info.id == selectedLayerId,
                    // Editing is handled by the existing full-screen focus overlay.
                    // Rendering the docked card as editing too would duplicate its input form.
                    editing = false,
                    keyboardSafe = false,
                    editScale = 1f,
                    onEditScaleChange = onInfoEditScaleChange,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onUpdate = onUpdate,
                    onDone = { onDone(info.id) },
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun BoxScope.InfoPanelItem(
    layer: InfoPanelLayer,
    selected: Boolean,
    editing: Boolean,
    keyboardSafe: Boolean,
    editScale: Float,
    onEditScaleChange: (Float) -> Unit,
    onSelect: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onUpdate: (InfoPanelLayer) -> Unit,
    onDone: () -> Unit,
    onDelete: (String) -> Unit
) {
    // The chosen card style belongs to the document, not the application theme.
    // In particular, a dark info card must look dark even in the light App theme.
    val infoDarkPreview = layer.darkBackground && !editing
    val infoInk = if (infoDarkPreview) Color(0xFFF7F9FC) else TgfTokens.Ink
    val infoSecondary = if (infoDarkPreview) Color(0xFFD2DFF1) else TgfTokens.InkSecondary
    // 顶部/底部模板始终作为独立信息区存在，不与图片重叠；
    // 叠加模式才在图片区域内自由移动。
    val alignment = Alignment.Center
    val cardModifier = Modifier
        .align(alignment)
        .padding(
            horizontal = if (keyboardSafe) 5.dp else 4.dp,
            vertical = if (keyboardSafe) 3.dp else 2.dp
        )
        .fillMaxWidth(if (editing) .98f else .985f)
        .zIndex(if (editing) 60f else 6f)
        .graphicsLayer {
            if (editing) {
                scaleX = editScale
                scaleY = editScale
            }
        }
        .then(
            if (selected || editing) Modifier.border(
                1.4.dp,
                TgfTokens.Blue.copy(alpha = .60f),
                RoundedCornerShape(18.dp)
            ) else Modifier
        )
        .then(
            if (!editing) Modifier.pointerInput(layer.id) {
                detectTapGestures(
                    onTap = { onSelect(layer.id) },
                    onDoubleTap = {
                        onSelect(layer.id)
                        onEdit(layer.id)
                    }
                )
            } else Modifier
        )

    Box(modifier = cardModifier) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            radius = 14.dp,
            elevation = 5.dp,
            backgroundOverride = if (infoDarkPreview) Color(0xFF232F40) else null,
            padding = PaddingValues(
                horizontal = if (keyboardSafe) 11.dp else 12.dp,
                vertical = if (keyboardSafe) 7.dp else 8.dp
            )
        ) {
            Column {
            // Only display typography changes; inline editing stays legible and keyboard-safe.
            val templateScale = layer.textScale.coerceIn(.90f, 1.15f)
            val templateFamily = fontFamilyForText(layer.fontFamilyId)
            // 模板名称单独占一行。缩放和完成按钮另起一行，不再让标题变成竖排。
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactHeader = maxWidth < 320.dp
                if (compactHeader && !editing) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TgfIcon(TgfIconType.Template, modifier = Modifier.size(27.dp), selected = selected)
                            Spacer(Modifier.width(7.dp))
                            Text(infoTemplateDisplayName(layer), fontSize = (14f*templateScale).sp,
                                fontFamily = templateFamily, fontWeight = FontWeight.SemiBold, color = infoInk,
                                softWrap = true, modifier = Modifier.weight(1f))
                        }
                        Text("日期：${layer.date}", fontSize = (11f*templateScale).sp, fontFamily = templateFamily, color = infoSecondary,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TgfIcon(
                            TgfIconType.Template,
                            modifier = Modifier.size(27.dp),
                            selected = selected || editing
                        )
                        Spacer(Modifier.width(7.dp))
                        if (editing && layer.templateType == InfoTemplateType.Custom) {
                            InlineTemplateField(
                                value = layer.customTemplateName,
                                placeholder = "自定义模板名称",
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                emphasize = true,
                                onValueChange = { onUpdate(layer.copy(customTemplateName = it.take(24))) }
                            )
                        } else {
                            Text(infoTemplateDisplayName(layer), fontSize = (14f*templateScale).sp,
                                fontFamily = templateFamily, fontWeight = FontWeight.SemiBold, color = infoInk,
                                softWrap = true, modifier = Modifier.weight(1f))
                        }
                        if (!editing) {
                            Spacer(Modifier.width(6.dp))
                            Text("日期：${layer.date}", fontSize = 11.sp, color = infoSecondary)
                        }
                    }
                }
            }
            if (editing) {
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .background(
                                Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onDone()
                            }
                            .padding(horizontal = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("完成", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(if (keyboardSafe) 4.dp else 7.dp))

            if (editing) {
                // v0.4.6：模板本体就是编辑区，也是展示区。
                // 键盘弹出后不会再有第二套底部字段表单遮挡画布。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日期", fontSize = 11.5.sp, color = infoSecondary, modifier = Modifier.width(42.dp))
                    // 日期直接点击系统日历选择。默认今天；选中日期只改文本字段，
                    // 不修改画布/照片/涂鸦/拼图图层的任何几何参数。
                    val calendarContext = LocalContext.current
                    val shownDate = layer.date.ifBlank { LocalDate.now().toString() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TgfTokens.Blue.copy(alpha = .11f))
                            .border(1.dp, TgfTokens.Blue.copy(alpha = .32f), RoundedCornerShape(12.dp))
                            .clickable {
                                val initial = runCatching { LocalDate.parse(shownDate) }
                                    .getOrDefault(LocalDate.now())
                                DatePickerDialog(
                                    calendarContext,
                                    { _, year, monthZeroBased, day ->
                                        val picked = LocalDate.of(year, monthZeroBased + 1, day)
                                        onUpdate(layer.copy(date = picked.toString(), autoDate = false))
                                    },
                                    initial.year, initial.monthValue - 1, initial.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shownDate, color = infoInk, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("日历 ▾", fontSize = 10.sp, color = TgfTokens.Blue)
                        }
                    }
                }

                Spacer(Modifier.height(if (keyboardSafe) 4.dp else 7.dp))
                val labels = infoTemplateFieldLabels(layer.templateType)
                InlineTemplateLabeledField(
                    label = labels.first,
                    value = layer.primary,
                    placeholder = "请输入${labels.first}",
                    onValueChange = { onUpdate(layer.copy(primary = it.take(40))) }
                )
                Spacer(Modifier.height(if (keyboardSafe) 4.dp else 6.dp))
                InlineTemplateLabeledField(
                    label = labels.second,
                    value = layer.secondary,
                    placeholder = "请输入${labels.second}",
                    onValueChange = { onUpdate(layer.copy(secondary = it.take(40))) }
                )
                Spacer(Modifier.height(if (keyboardSafe) 4.dp else 6.dp))
                InlineTemplateLabeledField(
                    label = labels.third,
                    value = layer.note,
                    placeholder = "请输入${labels.third}",
                    multiLine = true,
                    onValueChange = { onUpdate(layer.copy(note = it.take(240))) }
                )

                Spacer(Modifier.height(if (keyboardSafe) 3.dp else 5.dp))
                Text(
                    "完成后可点“排版”切换叠加 / 顶部 / 底部",
                    fontSize = 9.5.sp,
                    color = infoSecondary.copy(alpha = .78f)
                )
            } else {
                val labels = infoTemplateFieldLabels(layer.templateType)
                val firstText = if (layer.primary.isNotBlank()) "${labels.first}：${layer.primary}" else ""
                val secondText = if (layer.secondary.isNotBlank()) "${labels.second}：${layer.secondary}" else ""
                if (firstText.isNotBlank() || secondText.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val oneLine = maxWidth >= 300.dp &&
                            firstText.length + secondText.length <= 26 &&
                            firstText.isNotBlank() && secondText.isNotBlank()
                        if (oneLine) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(firstText, fontSize = (11.8f*templateScale).sp, fontFamily = templateFamily, color = infoInk,
                                    modifier = Modifier.weight(1f), softWrap = true)
                                Text(secondText, fontSize = (11.8f*templateScale).sp, fontFamily = templateFamily, color = infoInk,
                                    modifier = Modifier.weight(1f), softWrap = true)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (firstText.isNotBlank()) Text(firstText, fontSize = (11.8f * templateScale).sp,
                                    fontFamily = templateFamily, color = infoInk, softWrap = true)
                                if (secondText.isNotBlank()) Text(secondText, fontSize = (11.8f * templateScale).sp,
                                    fontFamily = templateFamily, color = infoInk, softWrap = true)
                            }
                        }
                    }
                }
                if (layer.note.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text("${labels.third}：${layer.note}", fontSize = (11.2f*templateScale).sp, fontFamily = templateFamily,
                        color = infoSecondary, softWrap = true)
                }
            }
        }
        }

        if (selected || editing) {
            CornerDeleteButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .zIndex(30f),
                onClick = { onDelete(layer.id) }
            )
        }
    }
}

@Composable
private fun InlineTemplateLabeledField(
    label: String,
    value: String,
    placeholder: String,
    multiLine: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(verticalAlignment = if (multiLine) Alignment.Top else Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 11.5.sp,
            color = TgfTokens.InkSecondary,
            modifier = Modifier.width(60.dp).padding(top = if (multiLine) 7.dp else 0.dp)
        )
        InlineTemplateField(
            value = value,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            singleLine = !multiLine,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun InlineTemplateField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    emphasize: Boolean = false,
    onValueChange: (String) -> Unit
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val keyboardInsetPx = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, keyboardInsetPx) {
        if (focused) {
            delay(if (keyboardInsetPx > 0) 110L else 170L)
            bringIntoView.bringIntoView()
        }
    }
    Box(
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            .background(
                TgfTokens.SurfaceRaised.copy(alpha = if (enabled) .92f else .50f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (emphasize) TgfTokens.SelectionInk.copy(alpha = .60f) else TgfTokens.SurfaceOutline,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = if (singleLine) 5.dp else 7.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            enabled = enabled,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
            textStyle = TextStyle(
                fontSize = if (emphasize) 12.2.sp else 11.8.sp,
                lineHeight = if (emphasize) 15.sp else 14.5.sp,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) TgfTokens.Ink else TgfTokens.InkSecondary
            ),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = if (emphasize) 11.5.sp else 10.8.sp,
                            color = TgfTokens.InkSecondary.copy(alpha = .55f)
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun TextTemplatePickerPanel(
    modifier: Modifier,
    onAdd: (InfoPanelLayer) -> Unit,
    onClose: () -> Unit
) {
    var template by remember { mutableStateOf(InfoTemplateType.MovieContinuity) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 24.dp,
        elevation = 14.dp,
        padding = PaddingValues(13.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("文本模板", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Text(" · 选择后直接在画布模板内编辑", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                Spacer(Modifier.weight(1f))
                Text("关闭", fontSize = 11.sp, color = TgfTokens.Blue, modifier = Modifier.clickable(onClick = onClose))
            }

            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                InfoTemplateType.values().forEach { type ->
                    TemplatePreviewCard(
                        type = type,
                        selected = type == template,
                        onClick = { template = type }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "添加后可自由调整排版",
                    fontSize = 10.sp,
                    color = TgfTokens.InkSecondary
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onAdd(
                                InfoPanelLayer(
                                    templateType = template,
                                    customTemplateName = if (template == InfoTemplateType.Custom) "自定义模板" else "",
                                    positionTop = false,
                                    overlayMode = true
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("添加模板", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TemplatePreviewCard(
    type: InfoTemplateType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val labels = infoTemplateFieldLabels(type)
    val sample1: String
    val sample2: String
    when (type) {
        InfoTemplateType.MovieContinuity -> { sample1 = "日期 · 角色 · 场次"; sample2 = "描述 / 备注" }
        InfoTemplateType.PhotoNote -> { sample1 = "标题 · 地点"; sample2 = "照片说明 / 备注" }
        InfoTemplateType.WorkRecord -> { sample1 = "项目 · 事项"; sample2 = "工作记录 / 备注" }
        InfoTemplateType.SiteRecord -> { sample1 = "项目 · 位置"; sample2 = "现场说明" }
        InfoTemplateType.SimpleTitle -> { sample1 = "标题"; sample2 = "副标题 · 说明" }
        InfoTemplateType.Custom -> { sample1 = "自定义模板名"; sample2 = "${labels.first} · ${labels.second} · ${labels.third}" }
    }

    Column(
        modifier = Modifier
            .width(138.dp)
            .height(92.dp)
            .tgfToolSurface(selected, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TgfIcon(TgfIconType.Template, modifier = Modifier.size(25.dp), selected = selected)
            Spacer(Modifier.width(6.dp))
            Text(
                infoTemplateTypeName(type),
                fontSize = 10.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) TgfTokens.SelectionInk else TgfTokens.Ink,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(sample1, fontSize = 8.5.sp, color = TgfTokens.InkSecondary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(sample2, fontSize = 8.2.sp, color = TgfTokens.InkSecondary.copy(alpha = .78f), maxLines = 1)
    }
}

@Composable
private fun CornerDeleteButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .semantics { contentDescription = "删除选中对象" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LightCornerIconSurface(iconSize = 22.dp, accent = Color(0xFFE48794)) {
            TgfIcon(TgfIconType.Delete, modifier = Modifier.size(18.dp), bare = true)
        }
    }
}

@Composable
private fun LightCornerIconSurface(
    iconSize: Dp,
    accent: Color = TgfTokens.Blue,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(iconSize + 4.dp)
            .shadow(3.dp, CircleShape, clip = false)
            .background(Color.White.copy(alpha = .88f), CircleShape)
            .border(.8.dp, accent.copy(alpha = .22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@Composable
private fun TransformHandleGlyph(
    modifier: Modifier = Modifier,
    color: Color = TgfTokens.Blue
) {
    Canvas(modifier) {
        val sw = 1.8.dp.toPx()
        // 右上外扩箭头表示缩放，左下弧线表示旋转。
        drawLine(color, Offset(size.width*.33f,size.height*.68f),
            Offset(size.width*.76f,size.height*.25f),sw,StrokeCap.Round)
        drawLine(color, Offset(size.width*.52f,size.height*.25f),
            Offset(size.width*.76f,size.height*.25f),sw,StrokeCap.Round)
        drawLine(color, Offset(size.width*.76f,size.height*.25f),
            Offset(size.width*.76f,size.height*.49f),sw,StrokeCap.Round)
        drawArc(color, 55f, 155f, false,
            topLeft=Offset(size.width*.11f,size.height*.35f),
            size=Size(size.width*.65f,size.height*.59f),
            style=Stroke(sw,cap=StrokeCap.Round))
    }
}

@Composable
private fun InfoTemplateChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .tgfToolSurface(selected, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(title, fontSize = 10.sp, color = if (selected) TgfTokens.SelectionInk else TgfTokens.Ink)
    }
}

@Composable
private fun InfoSmallChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .tgfToolSurface(selected, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(title, fontSize = 9.5.sp, color = if (selected) TgfTokens.SelectionInk else TgfTokens.InkSecondary)
    }
}

@Composable
private fun InfoLabeledField(
    label: String,
    value: String,
    placeholder: String,
    multiLine: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Row(verticalAlignment = if (multiLine) Alignment.Top else Alignment.CenterVertically) {
        Text(label, fontSize = 11.5.sp, color = TgfTokens.Ink, modifier = Modifier.width(42.dp).padding(top = if (multiLine) 8.dp else 0.dp))
        InfoField(
            value = value,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            multiLine = multiLine,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun InfoField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiLine: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = modifier
            .background(TgfTokens.SurfaceRaised.copy(alpha = if (enabled) .92f else .50f), RoundedCornerShape(12.dp))
            .border(1.dp, TgfTokens.SurfaceOutline, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = if (multiLine) 8.dp else 6.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            textStyle = TextStyle(
                color = TgfTokens.Ink,
                fontSize = 11.8.sp,
                lineHeight = 14.5.sp
            ),
            enabled = enabled,
            singleLine = !multiLine,
            maxLines = if (multiLine) 3 else 1,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, fontSize = 10.sp, color = TgfTokens.InkSecondary.copy(alpha = .58f))
                    inner()
                }
            }
        )
    }
}


private fun infoTemplateTypeName(type: InfoTemplateType): String = when (type) {
    InfoTemplateType.MovieContinuity -> "影视接戏照"
    InfoTemplateType.PhotoNote -> "照片说明"
    InfoTemplateType.WorkRecord -> "工作记录"
    InfoTemplateType.SiteRecord -> "工程/现场"
    InfoTemplateType.SimpleTitle -> "简洁标题"
    InfoTemplateType.Custom -> "自定义"
}

private fun infoTemplateDisplayName(layer: InfoPanelLayer): String = when (layer.templateType) {
    InfoTemplateType.Custom -> layer.customTemplateName.ifBlank { "自定义模板" }
    else -> infoTemplateTypeName(layer.templateType)
}

private fun infoTemplateFieldLabels(type: InfoTemplateType): Triple<String, String, String> = when (type) {
    InfoTemplateType.MovieContinuity -> Triple("角色", "场次", "描述/备注")
    InfoTemplateType.PhotoNote -> Triple("标题", "地点", "备注")
    InfoTemplateType.WorkRecord -> Triple("项目", "事项", "备注")
    InfoTemplateType.SiteRecord -> Triple("项目", "位置", "说明")
    InfoTemplateType.SimpleTitle -> Triple("标题", "副标题", "说明")
    InfoTemplateType.Custom -> Triple("标题", "信息", "备注")
}


@Composable
private fun CroppedImage(
    bitmap: ImageBitmap,
    crop: CropRect,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    toneBrightness: Float = 0f,
    toneContrast: Float = 0f,
    toneSaturation: Float = 0f,
    toneTemperature: Float = 0f,
    toneTint: Float = 0f,
    toneHighlights: Float = 0f,
    toneShadows: Float = 0f,
    modifier: Modifier = Modifier
) {
    val safe = crop.sanitize()
    // Pixel curve is rendered off the UI thread, never changes geometry or source bitmap.
    val tintedBitmap by produceState<ImageBitmap>(
        initialValue = bitmap, bitmap, toneTemperature, toneTint, toneHighlights, toneShadows
    ) {
        if (ExtendedToneCurve.isNeutral(toneTemperature, toneTint, toneHighlights, toneShadows)) {
            value = bitmap
        } else {
            // Keep the previous adjusted frame visible until the next result is ready.
            // Resetting to original on every pointer sample caused visible flicker/latency.
            value = withContext(Dispatchers.Default) {
                ImageToneProcessor.applyAdvanced(bitmap.asAndroidBitmap(),
                    toneTemperature, toneTint, toneHighlights, toneShadows).asImageBitmap()
            }
        }
    }
    val toneFilter = remember(toneBrightness,toneContrast,toneSaturation) {
        if (ImageToneMatrix.isNeutral(toneBrightness,toneContrast,toneSaturation)) null
        else ColorFilter.colorMatrix(ColorMatrix(ImageToneMatrix.make(
            toneBrightness,toneContrast,toneSaturation)))
    }
    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = if (flipHorizontal) -1f else 1f
            scaleY = if (flipVertical) -1f else 1f
        }
    ) {
        val srcX = (safe.left * tintedBitmap.width).roundToInt().coerceIn(0, tintedBitmap.width - 1)
        val srcY = (safe.top * tintedBitmap.height).roundToInt().coerceIn(0, tintedBitmap.height - 1)
        val srcRight = (safe.right * tintedBitmap.width).roundToInt().coerceIn(srcX + 1, tintedBitmap.width)
        val srcBottom = (safe.bottom * tintedBitmap.height).roundToInt().coerceIn(srcY + 1, tintedBitmap.height)
        drawImage(
            image = tintedBitmap,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcRight - srcX, srcBottom - srcY),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            colorFilter = toneFilter
        )
    }
}

@Composable
private fun BoxScope.CropOverlay(
    crop: CropRect,
    onChange: (CropRect) -> Unit
) {
    BoxWithConstraints(Modifier.matchParentSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val lineWidth = with(density) { 1.3.dp.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            val l = crop.left * size.width
            val t = crop.top * size.height
            val r = crop.right * size.width
            val b = crop.bottom * size.height
            val shade = Color.Black.copy(alpha = .34f)

            drawRect(shade, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, t))
            drawRect(shade, Offset(0f, b), androidx.compose.ui.geometry.Size(size.width, size.height - b))
            drawRect(shade, Offset(0f, t), androidx.compose.ui.geometry.Size(l, b - t))
            drawRect(shade, Offset(r, t), androidx.compose.ui.geometry.Size(size.width - r, b - t))

            drawRect(
                color = Color.White.copy(alpha = .98f),
                topLeft = Offset(l, t),
                size = androidx.compose.ui.geometry.Size(r - l, b - t),
                style = Stroke(width = lineWidth)
            )
            val gridColor = Color.White.copy(alpha = .46f)
            val oneThirdX = (r - l) / 3f
            val oneThirdY = (b - t) / 3f
            drawLine(gridColor, Offset(l + oneThirdX, t), Offset(l + oneThirdX, b), lineWidth * .6f)
            drawLine(gridColor, Offset(l + oneThirdX * 2f, t), Offset(l + oneThirdX * 2f, b), lineWidth * .6f)
            drawLine(gridColor, Offset(l, t + oneThirdY), Offset(r, t + oneThirdY), lineWidth * .6f)
            drawLine(gridColor, Offset(l, t + oneThirdY * 2f), Offset(r, t + oneThirdY * 2f), lineWidth * .6f)
        }

        CropHandle(CropCorner.TopLeft, crop, widthPx, heightPx, onChange)
        CropHandle(CropCorner.TopRight, crop, widthPx, heightPx, onChange)
        CropHandle(CropCorner.BottomLeft, crop, widthPx, heightPx, onChange)
        CropHandle(CropCorner.BottomRight, crop, widthPx, heightPx, onChange)
        CropEdgeHandle(CropEdge.Top, crop, widthPx, heightPx, onChange)
        CropEdgeHandle(CropEdge.Bottom, crop, widthPx, heightPx, onChange)
        CropEdgeHandle(CropEdge.Left, crop, widthPx, heightPx, onChange)
        CropEdgeHandle(CropEdge.Right, crop, widthPx, heightPx, onChange)
    }
}

private enum class CropCorner { TopLeft, TopRight, BottomLeft, BottomRight }
private enum class CropEdge { Top, Bottom, Left, Right }

@Composable
private fun BoxScope.CropHandle(
    corner: CropCorner,
    crop: CropRect,
    widthPx: Float,
    heightPx: Float,
    onChange: (CropRect) -> Unit
) {
    val density = LocalDensity.current
    val touchSize = 38.dp
    val touchSizePx = with(density) { touchSize.toPx() }
    val latestCrop by rememberUpdatedState(crop)

    val xRatio = if (corner == CropCorner.TopLeft || corner == CropCorner.BottomLeft) crop.left else crop.right
    val yRatio = if (corner == CropCorner.TopLeft || corner == CropCorner.TopRight) crop.top else crop.bottom

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (xRatio * widthPx - touchSizePx / 2f).roundToInt(),
                    (yRatio * heightPx - touchSizePx / 2f).roundToInt()
                )
            }
            .size(touchSize)
            .pointerInput(corner, widthPx, heightPx) {
                var dragStart = latestCrop
                var totalX = 0f
                var totalY = 0f

                detectDragGestures(
                    onDragStart = {
                        dragStart = latestCrop
                        totalX = 0f
                        totalY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalX += dragAmount.x
                        totalY += dragAmount.y
                        val dx = totalX / widthPx
                        val dy = totalY / heightPx

                        val next = when (corner) {
                            CropCorner.TopLeft -> dragStart.copy(
                                left = (dragStart.left + dx).coerceIn(0f, dragStart.right - MIN_CROP_SPAN),
                                top = (dragStart.top + dy).coerceIn(0f, dragStart.bottom - MIN_CROP_SPAN)
                            )
                            CropCorner.TopRight -> dragStart.copy(
                                right = (dragStart.right + dx).coerceIn(dragStart.left + MIN_CROP_SPAN, 1f),
                                top = (dragStart.top + dy).coerceIn(0f, dragStart.bottom - MIN_CROP_SPAN)
                            )
                            CropCorner.BottomLeft -> dragStart.copy(
                                left = (dragStart.left + dx).coerceIn(0f, dragStart.right - MIN_CROP_SPAN),
                                bottom = (dragStart.bottom + dy).coerceIn(dragStart.top + MIN_CROP_SPAN, 1f)
                            )
                            CropCorner.BottomRight -> dragStart.copy(
                                right = (dragStart.right + dx).coerceIn(dragStart.left + MIN_CROP_SPAN, 1f),
                                bottom = (dragStart.bottom + dy).coerceIn(dragStart.top + MIN_CROP_SPAN, 1f)
                            )
                        }
                        onChange(next.sanitize())
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(18.dp)
                .shadow(4.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
                .border(2.dp, TgfTokens.Blue, CircleShape)
        )
    }
}


@Composable
private fun BoxScope.CropEdgeHandle(
    edge: CropEdge,
    crop: CropRect,
    widthPx: Float,
    heightPx: Float,
    onChange: (CropRect) -> Unit
) {
    val density = LocalDensity.current
    val horizontal = edge == CropEdge.Top || edge == CropEdge.Bottom
    val touchWidth = if (horizontal) 52.dp else 30.dp
    val touchHeight = if (horizontal) 30.dp else 52.dp
    val touchWidthPx = with(density) { touchWidth.toPx() }
    val touchHeightPx = with(density) { touchHeight.toPx() }
    val latestCrop by rememberUpdatedState(crop)

    val xRatio = when (edge) {
        CropEdge.Left -> crop.left
        CropEdge.Right -> crop.right
        else -> (crop.left + crop.right) / 2f
    }
    val yRatio = when (edge) {
        CropEdge.Top -> crop.top
        CropEdge.Bottom -> crop.bottom
        else -> (crop.top + crop.bottom) / 2f
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (xRatio * widthPx - touchWidthPx / 2f).roundToInt(),
                    (yRatio * heightPx - touchHeightPx / 2f).roundToInt()
                )
            }
            .size(touchWidth, touchHeight)
            .pointerInput(edge, widthPx, heightPx) {
                var dragStart = latestCrop
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragStart = latestCrop
                        totalX = 0f
                        totalY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalX += dragAmount.x
                        totalY += dragAmount.y
                        val dx = totalX / widthPx
                        val dy = totalY / heightPx
                        val next = when (edge) {
                            CropEdge.Top -> dragStart.copy(
                                top = (dragStart.top + dy).coerceIn(0f, dragStart.bottom - MIN_CROP_SPAN)
                            )
                            CropEdge.Bottom -> dragStart.copy(
                                bottom = (dragStart.bottom + dy).coerceIn(dragStart.top + MIN_CROP_SPAN, 1f)
                            )
                            CropEdge.Left -> dragStart.copy(
                                left = (dragStart.left + dx).coerceIn(0f, dragStart.right - MIN_CROP_SPAN)
                            )
                            CropEdge.Right -> dragStart.copy(
                                right = (dragStart.right + dx).coerceIn(dragStart.left + MIN_CROP_SPAN, 1f)
                            )
                        }
                        onChange(next.sanitize())
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(if (horizontal) 20.dp else 4.dp, if (horizontal) 4.dp else 20.dp)
                .shadow(3.dp, RoundedCornerShape(4.dp), clip = false)
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, TgfTokens.Blue.copy(alpha = .72f), RoundedCornerShape(4.dp))
        )
    }
}

private fun CropRect.toVisual(flipH: Boolean, flipV: Boolean): CropRect {
    val l = if (flipH) 1f - right else left
    val r = if (flipH) 1f - left else right
    val t = if (flipV) 1f - bottom else top
    val b = if (flipV) 1f - top else bottom
    return CropRect(l, t, r, b).sanitize()
}

private fun CropRect.toSource(flipH: Boolean, flipV: Boolean): CropRect = toVisual(flipH, flipV)

@Composable
private fun CropPanel(
    modifier: Modifier,
    layer: ImageLayer,
    draft: CropRect,
    page: CropPanelPage,
    allowRotateControls: Boolean,
    onPageChange: (CropPanelPage) -> Unit,
    onDraftChange: (CropRect) -> Unit,
    onUpdateLayer: (ImageLayer) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current
    val preview by rememberImagePreview(context, layer.uriString)
    val sourceRatio = preview?.let { it.width.toFloat() / it.height.toFloat() }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 23.dp,
        elevation = 12.dp,
        padding = PaddingValues(12.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                if (allowRotateControls) {
                    // 单图模式保留裁切/旋转双页签。
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically) {
                        CropPanelTab(
                            type = TgfIconType.Crop,
                            title = "裁切",
                            selected = page == CropPanelPage.Crop
                        ) { onPageChange(CropPanelPage.Crop) }
                        Spacer(Modifier.width(8.dp))
                        CropPanelTab(
                            type = TgfIconType.RotateRight,
                            title = "旋转",
                            selected = page == CropPanelPage.Rotate
                        ) { onPageChange(CropPanelPage.Rotate) }
                    }
                } else {
                    Text(
                        "裁切",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TgfTokens.Ink
                    )
                }
                Box(Modifier.size(width = 48.dp, height = 44.dp)
                    .clickable(onClick = onCancel)
                    .semantics { contentDescription = "取消裁切" },
                    contentAlignment = Alignment.Center) {
                    Text("取消", fontSize = 11.sp, color = TgfTokens.InkSecondary)
                }
                Spacer(Modifier.width(3.dp))
                EditorDoneButton(onClick = onApply, compact = true)
            }

            Spacer(Modifier.height(9.dp))

            when (if (allowRotateControls) page else CropPanelPage.Crop) {
                CropPanelPage.Crop -> {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CropChoice("原图") { onDraftChange(CropRect(0f, 0f, 1f, 1f)) }
                        CropChoice("自由") { }
                        CropChoice("1:1") { sourceRatio?.let { onDraftChange(centerCropForAspect(it, 1f)) } }
                        CropChoice("4:3") { sourceRatio?.let { onDraftChange(centerCropForAspect(it, 4f / 3f)) } }
                        CropChoice("3:4") { sourceRatio?.let { onDraftChange(centerCropForAspect(it, 3f / 4f)) } }
                        CropChoice("16:9") { sourceRatio?.let { onDraftChange(centerCropForAspect(it, 16f / 9f)) } }
                        CropChoice("9:16") { sourceRatio?.let { onDraftChange(centerCropForAspect(it, 9f / 16f)) } }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "拖动四角或边线精准调整裁切区域",
                        fontSize = 9.5.sp,
                        color = TgfTokens.InkSecondary
                    )
                }

                CropPanelPage.Rotate -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TransformIconChoice(TgfIconType.FlipHorizontal, "水平翻转") {
                            onUpdateLayer(layer.copy(flipHorizontal = !layer.flipHorizontal))
                        }
                        TransformIconChoice(TgfIconType.FlipVertical, "垂直翻转") {
                            onUpdateLayer(layer.copy(flipVertical = !layer.flipVertical))
                        }
                        TransformIconChoice(TgfIconType.RotateLeft, "左转90°") {
                            onUpdateLayer(layer.copy(rotation = normalizeRotation(layer.rotation - 90f)))
                        }
                        TransformIconChoice(TgfIconType.RotateRight, "右转90°") {
                            onUpdateLayer(layer.copy(rotation = normalizeRotation(layer.rotation + 90f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropPanelTab(
    type: TgfIconType,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .tgfToolSurface(selected, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TgfIcon(type, modifier = Modifier.size(31.dp), selected = selected)
        Spacer(Modifier.width(5.dp))
        Text(
            title,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) TgfTokens.Blue else TgfTokens.InkSecondary
        )
    }
}

@Composable
private fun CropChoice(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.padding(end = 7.dp)
            .background(Color.White.copy(alpha = .56f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = .84f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(title, fontSize = 10.5.sp, color = TgfTokens.Ink)
    }
}

@Composable
private fun TransformIconChoice(type: TgfIconType, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TgfIcon(type, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 8.5.sp, fontWeight = FontWeight.Medium, color = TgfTokens.InkSecondary)
    }
}

private fun centerCropForAspect(sourceAspect: Float, targetAspect: Float): CropRect {
    if (sourceAspect <= 0f || targetAspect <= 0f) return CropRect(0f, 0f, 1f, 1f)
    return if (sourceAspect > targetAspect) {
        val widthFraction = (targetAspect / sourceAspect).coerceIn(MIN_CROP_SPAN, 1f)
        val margin = (1f - widthFraction) / 2f
        CropRect(margin, 0f, 1f - margin, 1f)
    } else {
        val heightFraction = (sourceAspect / targetAspect).coerceIn(MIN_CROP_SPAN, 1f)
        val margin = (1f - heightFraction) / 2f
        CropRect(0f, margin, 1f, 1f - margin)
    }
}

@Composable
private fun BoxScope.SnapGuides(
    feedback: SnapFeedback?,
    canvasWidthPx: Float,
    canvasHeightPx: Float
) {
    if (feedback == null) return
    // 右侧/底部的边界线向画布内收 2dp，避免被直角画布裁剪掉。
    val guideInsetPx = with(LocalDensity.current) { 2.dp.toPx() }.roundToInt()
    feedback.verticalGuideRatio?.let { ratio ->
        val nearCanvasEdge = abs(abs(ratio) - .5f) < .001f
        val guideColor = if (nearCanvasEdge) EDIT_CANVAS_EDGE_RED else TgfTokens.Blue
        val line = Brush.verticalGradient(listOf(Color.Transparent, guideColor.copy(alpha = .92f), Color.Transparent))
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        (canvasWidthPx * (.5f + ratio)).roundToInt()
                            .coerceIn(0, (canvasWidthPx.roundToInt() - guideInsetPx).coerceAtLeast(0)),
                        0
                    )
                }
                .width(if (nearCanvasEdge) 2.dp else 1.dp)
                .fillMaxHeight()
                .background(line)
        )
    }
    feedback.horizontalGuideRatio?.let { ratio ->
        val nearCanvasEdge = abs(abs(ratio) - .5f) < .001f
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        0,
                        (canvasHeightPx * (.5f + ratio)).roundToInt()
                            .coerceIn(0, (canvasHeightPx.roundToInt() - guideInsetPx).coerceAtLeast(0))
                    )
                }
                .height(if (nearCanvasEdge) 2.dp else 1.dp)
                .fillMaxWidth()
                .background(if (nearCanvasEdge) EDIT_CANVAS_EDGE_RED.copy(alpha = .92f) else TgfTokens.Blue.copy(alpha = .52f))
        )
    }
    feedback.rotationAngle?.let { angle ->
        GlassCard(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            radius = 12.dp,
            elevation = 5.dp,
            padding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("角度吸附 ${formatAngle(angle)}", fontSize = 10.sp, color = TgfTokens.Blue, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun snapAxis(
    rawRatio: Float,
    boundPx: Float,
    canvasPx: Float,
    thresholdPx: Float,
    otherCenterRatios: List<Float>
): AxisSnap {
    val thresholdRatio = thresholdPx / canvasPx
    val candidates = mutableListOf<Pair<Float, Float>>()
    candidates += 0f to 0f

    if (boundPx <= canvasPx * 1.05f) {
        val halfBoundRatio = boundPx / (2f * canvasPx)
        val startCenter = -.5f + halfBoundRatio
        val endCenter = .5f - halfBoundRatio
        candidates += startCenter to -.5f
        candidates += endCenter to .5f
    }
    otherCenterRatios.forEach { candidates += it to it }

    val nearest = candidates.minByOrNull { abs(rawRatio - it.first) } ?: return AxisSnap(rawRatio)
    return if (abs(rawRatio - nearest.first) <= thresholdRatio) {
        AxisSnap(nearest.first, nearest.second)
    } else {
        AxisSnap(rawRatio, null)
    }
}

private fun snapRotation(raw: Float): Pair<Float, Boolean> {
    val r = normalizeRotation(raw)
    val major = round(r / 90f) * 90f
    if (angleDistance(r, major) <= SNAP_ROTATION_MAJOR_DEG) {
        return normalizeRotation(major) to true
    }
    val minor = round(r / 45f) * 45f
    if (angleDistance(r, minor) <= SNAP_ROTATION_MINOR_DEG) {
        return normalizeRotation(minor) to true
    }
    return r to false
}

private fun angleDistance(a: Float, b: Float): Float = abs(normalizeRotation(a - b))

private fun rotatedBounds(width: Float, height: Float, degrees: Float): Pair<Float, Float> {
    val rad = degrees / 180f * PI.toFloat()
    val c = abs(cos(rad))
    val s = abs(sin(rad))
    return (width * c + height * s) to (width * s + height * c)
}

private fun formatAngle(value: Float): String {
    val rounded = value.roundToInt()
    val normalized = if (rounded == -180) 180 else rounded
    return "${normalized}°"
}



@Composable
private fun ImageToolPanel(
    modifier: Modifier,
    selectedLayer: ImageLayer?,
    canvasAspectRatio: Float,
    directPhotoIsDocument: Boolean,
    onSelectCanvasAspect: (CollageAspect) -> Unit,
    onCropSelected: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onUpdateLayer: (ImageLayer) -> Unit,
    initialPage: String,
    background: com.tugongfang.editor.model.canvas.CanvasBackground,
    onBackgroundChange: (com.tugongfang.editor.model.canvas.CanvasBackground) -> Unit,
    canvasScene: CanvasScene,
    onUpdateScene: (CanvasScene) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onAddPhoto: () -> Unit,
    onReplacePhoto: (String) -> Unit,
    onResetLayer: (ImageLayer) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit
) {
    // 紧凑态为173dp，为“画布比例＋选中照片快捷键”保留完整显示空间；展开仍为322dp。
    var currentPage by remember { mutableStateOf(if(directPhotoIsDocument) initialPage else "图片") }
    LaunchedEffect(initialPage) {
        if (directPhotoIsDocument) currentPage=initialPage
    }
    var toneKind by remember { mutableStateOf("亮度") }
    LaunchedEffect(directPhotoIsDocument) {
        if (directPhotoIsDocument && currentPage == "图片") currentPage = "调色"
        else if (!directPhotoIsDocument && currentPage == "间距") currentPage = "图片"
    }
    GlassCard(
        modifier = modifier.fillMaxWidth().height(if (expanded) 322.dp
            else if (currentPage in setOf("背景","边框","间距")) 252.dp
            else if(directPhotoIsDocument) 171.dp else 196.dp),
        radius = 22.dp,
        elevation = 10.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 单行紧凑图标模式键：固定上栏高度、内容区独立显示，
            // 小屏不压缩“图片/调色/背景”标签；工具区单独预留高度防遮挡。
            Row(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    if (currentPage in setOf("背景","边框","间距")) currentPage
                        else if (directPhotoIsDocument) "调色" else "图片",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = TgfTokens.Ink, maxLines = 1, softWrap = false
                )
                // DEV42: single-photo secondary menus also expose background/frame/spacing.
                    // DEV21: only the low-priority mode choices scroll on narrow screens.
                    // Expand and Done stay pinned at the right edge at all widths.
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (if(directPhotoIsDocument) listOf(
                            Triple("调色",TgfIconType.Style,"调色"),
                            Triple("背景",TgfIconType.Background,"背景"),
                            Triple("边框",TgfIconType.Shapes,"边框"),
                            Triple("间距",TgfIconType.Aspect,"间距")
                        ) else listOf(
                            Triple("图片", TgfIconType.Photo, "图片操作"),
                            Triple("调色", TgfIconType.Style, "图片调色"),
                            Triple("背景", TgfIconType.Background, "画布背景"),
                            Triple("边框", TgfIconType.Shapes, "边框"),
                            Triple("间距", TgfIconType.Aspect, "间距")
                        )).forEach { (page, icon, desc) ->
                            ImageModeKey(
                                title = page,
                                icon = icon,
                                selected = currentPage == page,
                                description = desc,
                                onClick = { currentPage = page }
                            )
                        }
                    }
                    SecondaryPanelExpandKey(expanded, onToggleExpanded)
                EditorDoneButton(onClick=onClose, compact=true)
            }
            Spacer(Modifier.height(2.dp))
            when (currentPage) {
                "边框" -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    FrameStyleControls(canvasScene.frame) { onUpdateScene(canvasScene.copy(frame=it)) }
                }
                "间距" -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    SpacingControls(photoGap=null,outer=canvasScene.photoInsetRatio,
                        onOuter={ next -> onUpdateScene(canvasScene.copy(photoInsetRatio=next,
                            singlePhotoCanvas=canvasScene.singlePhotoCanvas || directPhotoIsDocument)) })
                }
                "背景" -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    CanvasBackgroundControls(background,onBackgroundChange,onPickBackgroundImage)
                }
                "调色" -> {
                    val target=selectedLayer?.takeUnless { it.locked }
                    if(target==null) {
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                            Text("请先选择一张未锁定的照片",fontSize=12.sp,color=TgfTokens.InkSecondary)
                        }
                    } else {
                        val value=when(toneKind) {
                            "对比度" -> target.contrast
                            "饱和度" -> target.saturation
                            "色温" -> target.temperature
                            "色调" -> target.tint
                            "高光" -> target.highlights
                            "阴影" -> target.shadows
                            else -> target.brightness
                        }.coerceIn(-1f,1f)
                        // Direct update: delaying layer state by 24ms was visible on Huawei.
                        // Brightness/contrast/saturation are cheap GPU ColorMatrix filters;
                        // advanced tone curves are calculated asynchronously by CroppedImage.
                        val shownValue = value
                        val latestTarget by rememberUpdatedState(target)
                        fun updateTone(next: Float) {
                            val safe=next.coerceIn(-1f,1f)
                            val source=latestTarget
                            onUpdateLayer(when(toneKind) {
                                "对比度" -> source.copy(contrast=safe)
                                "饱和度" -> source.copy(saturation=safe)
                                "色温" -> source.copy(temperature=safe)
                                "色调" -> source.copy(tint=safe)
                                "高光" -> source.copy(highlights=safe)
                                "阴影" -> source.copy(shadows=safe)
                                else -> source.copy(brightness=safe)
                            })
                        }
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text("-100",fontSize=9.sp,color=TgfTokens.InkSecondary)
                            Slider(value=shownValue,onValueChange={ next ->
                                updateTone(next)
                            },valueRange=-1f..1f,modifier=Modifier.weight(1f).height(38.dp))
                            Text("${(shownValue*100f).roundToInt()}",fontSize=10.sp,
                                color=TgfTokens.Blue,modifier=Modifier.width(29.dp),textAlign=TextAlign.Center)
                        }
                        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                val toneIcons = listOf(
                                    "亮度" to "☀", "对比度" to "◐", "饱和度" to "◉",
                                    "色温" to "♨", "色调" to "◈", "高光" to "✧", "阴影" to "☾"
                                )
                                toneIcons.forEach { (kind, glyph) ->
                                    val chosen = toneKind == kind
                                    Column(
                                        Modifier.width(55.dp).height(46.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if(chosen) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised)
                                            .border(if (chosen) 1.3.dp else .6.dp,
                                                if (chosen) TgfTokens.SelectionInk else TgfTokens.InkSecondary.copy(alpha = .19f),
                                                RoundedCornerShape(9.dp))
                                            .semantics { contentDescription = "调色：$kind" }
                                            .clickable { toneKind = kind },
                                        horizontalAlignment=Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.Center
                                    ) {
                                        Text(glyph,fontSize=19.sp,color=if(chosen)TgfTokens.SelectionInk else TgfTokens.Ink)
                                        Text(kind,fontSize=9.5.sp,color=if(chosen)TgfTokens.SelectionInk else TgfTokens.InkSecondary)
                                    }
                                }
                            }
                            Column(
                                Modifier.width(52.dp).height(46.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(TgfTokens.SurfaceRaised, RoundedCornerShape(9.dp))
                                    .border(.7.dp, TgfTokens.Blue.copy(alpha = .36f), RoundedCornerShape(9.dp))
                                    .clickable {
                                        onUpdateLayer(target.copy(brightness=0f,contrast=0f,saturation=0f,
                                            temperature=0f,tint=0f,highlights=0f,shadows=0f))
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TgfIcon(TgfIconType.Reset, Modifier.size(23.dp), bare = true)
                                Text("还原",fontSize=10.sp,color=TgfTokens.SelectionInk,fontWeight=FontWeight.SemiBold)
                            }
                        }
                    }
                }
                else -> {
                    // “画布比例”与“裁切单张照片”彻底分离；改画布不改图层变换值。
                    if (!directPhotoIsDocument) Row(Modifier.fillMaxWidth().height(34.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("画布比例",fontSize=10.sp,color=TgfTokens.InkSecondary,maxLines=1)
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            CollageAspect.entries.forEach { aspect ->
                                val chosen = kotlin.math.abs(canvasAspectRatio - aspect.ratio) < .008f
                                Box(Modifier.width(49.dp).height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if(chosen) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised)
                                    .border(if(chosen) 1.dp else .6.dp,
                                        if(chosen) TgfTokens.Blue else TgfTokens.SurfaceOutline,
                                        RoundedCornerShape(8.dp))
                                    .clickable { onSelectCanvasAspect(aspect) },
                                    contentAlignment=Alignment.Center) {
                                    Text(aspect.label,fontSize=10.sp,maxLines=1,
                                        color=if(chosen) TgfTokens.Blue else TgfTokens.InkSecondary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        if (!directPhotoIsDocument) ImagePanelButton(icon=TgfIconType.AddPhoto,title="添加照片",
                            modifier=Modifier.weight(1f),onClick=onAddPhoto)
                        ImagePanelButton(icon=TgfIconType.Replace,title="替换",
                            modifier=Modifier.weight(1f),enabled=selectedLayer!=null,
                            onClick={selectedLayer?.let { onReplacePhoto(it.id) }})
                        ImagePanelButton(icon=TgfIconType.Crop,title="裁切",
                            modifier=Modifier.weight(1f),enabled=selectedLayer!=null && !selectedLayer.locked,
                            onClick=onCropSelected)
                        if (!directPhotoIsDocument) ImagePanelButton(icon=TgfIconType.Duplicate,title="复制",
                            modifier=Modifier.weight(1f),enabled=selectedLayer!=null && !selectedLayer.locked,
                            onClick=onDuplicateSelected)
                        ImagePanelButton(icon=TgfIconType.Reset,title="重置",
                            modifier=Modifier.weight(1f),enabled=selectedLayer!=null && !selectedLayer.locked,
                            onClick={selectedLayer?.let(onResetLayer)})
                    }
                }
            }
        }
    }
}

/**
 * v0.9.8 FIX1: reversible UI-only second-level-panel expander.
 * It never touches canvasScene, layers, collage, or their transforms.
 */
@Composable
internal fun SecondaryPanelExpandKey(expanded: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier.size(42.dp)
            .semantics { contentDescription = if (expanded) "收起二级功能区" else "展开二级功能区" }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(27.dp)
                .clip(shape)
                .background(TgfTokens.SurfaceSelected, shape)
                .border(1.dp, TgfTokens.Blue.copy(alpha = .52f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (expanded) "⌄" else "⌃", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = TgfTokens.Blue, maxLines = 1)
        }
    }
}

/** v0.9.6: 专业微型玻璃功能键，图标与文字均有独立高度，避免手机上文字被裁切。 */
@Composable
private fun ImageModeKey(
    title: String,
    icon: TgfIconType,
    selected: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier.width(43.dp).height(42.dp)
            .tgfToolSurface(selected, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TgfIcon(icon, modifier = Modifier.size(18.dp), selected = selected, bare = true)
        Spacer(Modifier.height(2.dp))
        Text(title, fontSize = 9.sp, maxLines = 1, softWrap = false,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TgfTokens.Blue else TgfTokens.InkSecondary)
    }
}

@Composable
private fun ImagePanelButton(
    icon: TgfIconType,
    title: String,
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier = modifier
            .tgfToolSurface(selected = false, shape = shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .height(53.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TgfIcon(icon, modifier = Modifier.size(26.dp), selected = enabled)
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            fontSize = 9.5.sp,
            maxLines = 1,
            softWrap = false,
            color = if (enabled) TgfTokens.Ink else TgfTokens.InkSecondary.copy(alpha = .55f)
        )
    }
}

@Composable
private fun ActionIconButton(type: TgfIconType, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(42.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TgfIcon(type, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            title,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            color = TgfTokens.InkSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyCanvas(onImportPhoto: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(.88f).height(350.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
            .background(TgfTokens.CanvasInner, RoundedCornerShape(24.dp))
            .clickable(onClick = onImportPhoto),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TgfIcon(TgfIconType.Photo, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(13.dp))
            Text("导入照片", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
            Text("选择一张照片开始编辑", fontSize = 12.sp, color = TgfTokens.InkSecondary)
        }
    }
}

@Composable
private fun BoxScope.ImageCornerCopyButton(
    insidePlacement: Boolean = false,
    onClick:()->Unit
) {
    Box(Modifier.align(Alignment.BottomStart)
        .offset(
            x = if (insidePlacement) (-16).dp else (-19).dp,
            y = if (insidePlacement) 16.dp else 19.dp
        ).size(40.dp)
        .zIndex(28f).semantics {contentDescription="复制图片"}
        .clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        if (insidePlacement) {
            LightCornerIconSurface(iconSize = 22.dp, accent = TgfTokens.Blue) {
                TgfIcon(TgfIconType.Duplicate,modifier=Modifier.size(19.dp),bare=true)
            }
        } else {
            LightCornerIconSurface(iconSize = 22.dp, accent = TgfTokens.Blue) {
                TgfIcon(TgfIconType.Duplicate,modifier=Modifier.size(19.dp),bare=true)
            }
        }
    }
}

@Composable
private fun BoxScope.ImageCornerTransformButton(
    insidePlacement: Boolean = false,
    onStart:()->Unit,onMove:(Offset)->Unit,onStop:()->Unit
) {
    val latestStart by rememberUpdatedState(onStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestStop by rememberUpdatedState(onStop)
    Box(Modifier.align(Alignment.TopEnd)
        .offset(
            x = if (insidePlacement) 16.dp else 19.dp,
            y = if (insidePlacement) (-16).dp else (-19).dp
        ).size(40.dp)
        .zIndex(30f).semantics {contentDescription="拖动缩放并旋转图片"}
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart={latestStart()},onDragEnd={latestStop()},onDragCancel={latestStop()},
                onDrag={change,amount->change.consume();latestMove(amount)}
            )
        },contentAlignment=Alignment.Center) {
        if (insidePlacement) {
            LightCornerIconSurface(iconSize = 23.dp, accent = TgfTokens.Blue) {
                TransformHandleGlyph(modifier = Modifier.size(18.dp), color = TgfTokens.Blue)
            }
        } else {
            LightCornerIconSurface(iconSize = 23.dp, accent = TgfTokens.Blue) {
                TransformHandleGlyph(modifier = Modifier.size(18.dp), color = TgfTokens.Blue)
            }
        }
    }
}

@Composable
private fun BoxScope.SelectionHandles() {
    val points = listOf(
        Alignment.TopStart,
        Alignment.TopEnd,
        Alignment.BottomStart,
        Alignment.BottomEnd
    )
    points.forEach { alignment ->
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(
                    x = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) (-5).dp else 5.dp,
                    y = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) (-5).dp else 5.dp
                )
                .size(10.dp)
                .shadow(3.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
                .border(1.5.dp, TgfTokens.Blue, CircleShape)
        )
    }
}

private fun normalizeRotation(value: Float): Float {
    var r = value
    while (r > 180f) r -= 360f
    while (r < -180f) r += 360f
    return if (abs(r) < .05f) 0f else r
}

/** Calm, static editor atmosphere; no bitmap allocations or animation per frame. */
@Composable
private fun EditorAmbientBackdrop() {
    val dark = TgfTokens.isDark
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(
            TgfTokens.BackgroundTop, TgfTokens.BackgroundMid, TgfTokens.BackgroundBottom
        )))
        val width = size.width
        val height = size.height
        if (width > 0f && height > 0f) {
            val right = Offset(width * .94f, height * .09f)
            drawCircle(
                Brush.radialGradient(
                    if (dark) listOf(Color(0x334C3CB4), Color.Transparent)
                    else listOf(Color(0x335B96ED), Color.Transparent),
                    center = right, radius = width * .82f
                ), center = right, radius = width * .82f
            )
            val left = Offset(width * .04f, height * .87f)
            drawCircle(
                Brush.radialGradient(
                    if (dark) listOf(Color(0x282A429C), Color.Transparent)
                    else listOf(Color(0x246E61CC), Color.Transparent),
                    center = left, radius = width * .82f
                ), center = left, radius = width * .82f
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    onBack: () -> Unit,
    onReselect: (() -> Unit)?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCard(
            modifier = Modifier.size(39.dp).clickable(onClick = onBack),
            radius = 13.dp,
            elevation = 4.dp,
            padding = PaddingValues(0.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 30.sp, color = TgfTokens.Ink)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("未命名项目", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
            Text("自动保存", fontSize = 10.sp, color = TgfTokens.InkSecondary)
        }
        Spacer(Modifier.weight(1f))
        if (onReselect != null) {
            Box(
                Modifier.background(TgfTokens.SurfaceRaised, RoundedCornerShape(10.dp))
                    .clickable(onClick = onReselect)
                    .padding(horizontal = 8.dp, vertical = 9.dp)
            ) {
                Text("↺ 重选", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = TgfTokens.Ink)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            "↶",
            fontSize = 24.sp,
            color = if (canUndo) TgfTokens.InkSecondary else TgfTokens.InkSecondary.copy(alpha = .35f),
            modifier = if (canUndo) Modifier.clickable(onClick = onUndo) else Modifier
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "↷",
            fontSize = 24.sp,
            color = if (canRedo) TgfTokens.InkSecondary else TgfTokens.InkSecondary.copy(alpha = .35f),
            modifier = if (canRedo) Modifier.clickable(onClick = onRedo) else Modifier
        )
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier.height(37.dp)
                .background(Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)), RoundedCornerShape(12.dp))
                .clickable(onClick = onExport)
                .padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("导出", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun BoxScope.ShapeLayerItem(
    layer: ShapeLayer,
    selected: Boolean,
    editable: Boolean,
    enabled: Boolean,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    referenceCanvasWidthPx: Float,
    referenceCanvasHeightPx: Float,
    layoutContentScale: Float,
    onSelect: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onUpdate: (ShapeLayer) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onSnapFeedback: (SnapFeedback?) -> Unit,
    otherCenterX: List<Float>,
    otherCenterY: List<Float>
) {
    val density = LocalDensity.current
    var x by remember(layer.id) { mutableFloatStateOf(layer.offsetXRatio) }
    var y by remember(layer.id) { mutableFloatStateOf(layer.offsetYRatio) }
    var rawX by remember(layer.id) { mutableFloatStateOf(layer.offsetXRatio) }
    var rawY by remember(layer.id) { mutableFloatStateOf(layer.offsetYRatio) }
    var zoom by remember(layer.id) { mutableFloatStateOf(layer.scale) }
    var rawRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }
    var rotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }
    var active by remember(layer.id) { mutableStateOf(false) }
    var pulse by remember(layer.id) { mutableIntStateOf(0) }
    val latest by rememberUpdatedState(layer)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val latestEdit by rememberUpdatedState(onEdit)
    val latestDuplicate by rememberUpdatedState(onDuplicate)
    val isCircle = layer.type == ShapeType.Ellipse && layer.perfectCircle
    val baseW = referenceCanvasWidthPx * layer.widthRatio * layoutContentScale
    val baseH = referenceCanvasHeightPx * layer.heightRatio * layoutContentScale
    // 将缩放纳入布局尺寸而非 graphicsLayer.scale，以保证边框外控制点始终保持 42dp 触控区域。
    val displayW = (if (isCircle) minOf(baseW, baseH) else baseW) * zoom
    val displayH = (if (isCircle) minOf(baseW, baseH) else baseH) * zoom
    val drawW = with(density) { displayW.coerceAtLeast(20f).toDp() }
    val drawH = with(density) { displayH.coerceAtLeast(20f).toDp() }
    val outerGap = 52.dp

    LaunchedEffect(active, layer.offsetXRatio, layer.offsetYRatio, layer.scale, layer.rotation) {
        if (!active) {
            x = layer.offsetXRatio; y = layer.offsetYRatio
            rawX = layer.offsetXRatio; rawY = layer.offsetYRatio
            zoom = layer.scale; rawRotation = layer.rotation; rotation = layer.rotation
        }
    }
    LaunchedEffect(pulse) {
        if (pulse > 0) {
            delay(260)
            active = false
            onSnapFeedback(null)
        }
    }
    fun pushTransform() {
        latestUpdate(latest.copy(offsetXRatio=x, offsetYRatio=y, scale=zoom, rotation=rotation))
    }
    var handleOrigin by remember(layer.id) { mutableStateOf(Offset.Zero) }
    var handleTotal by remember(layer.id) { mutableStateOf(Offset.Zero) }
    var startScale by remember(layer.id) { mutableFloatStateOf(layer.scale) }
    var startRotation by remember(layer.id) { mutableFloatStateOf(layer.rotation) }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            // 额外的控制层包围实际图形；小图形也不会发生角标堆叠。
            .size(drawW + outerGap * 2, drawH + outerGap * 2)
            .graphicsLayer {
                translationX = x * canvasWidthPx
                translationY = y * canvasHeightPx
                rotationZ = rotation
            }
    ) {
        var bodyMod = Modifier.align(Alignment.Center).size(drawW, drawH)
            .pointerInput(layer.id) {
                detectTapGestures(
                    onTap = { if (enabled) onSelect(layer.id) },
                    onDoubleTap = { if (enabled) { onSelect(layer.id); latestEdit(layer.id) } }
                )
            }
        if (enabled && editable && !layer.locked) {
            bodyMod = bodyMod.pointerInput(layer.id, canvasWidthPx, canvasHeightPx) {
                detectTransformGestures { _, pan, magnify, turn ->
                    onSelect(layer.id)
                    active = true; pulse += 1
                    // pan 是图形的局部坐标；反旋转手势坐标后换算到画布，避免旋转后的移动方向漂移。
                    val angle = rotation * PI.toFloat() / 180f
                    val worldX = pan.x * cos(angle) - pan.y * sin(angle)
                    val worldY = pan.x * sin(angle) + pan.y * cos(angle)
                    rawX = (rawX + worldX / canvasWidthPx).coerceIn(-1.2f, 1.2f)
                    rawY = (rawY + worldY / canvasHeightPx).coerceIn(-1.2f, 1.2f)
                    zoom = (zoom * magnify).coerceIn(.35f, 6f)
                    rawRotation = normalizeRotation(rawRotation + turn)
                    val (snapped, wasSnapped) = snapRotation(rawRotation)
                    rotation = snapped
                    val threshold = 9.dp.toPx()
                    // 每帧使用实时缩放尺寸计算吸附，避免上一帧 displayW / displayH 延迟。
                    val currentWidth = (if (isCircle) minOf(baseW, baseH) else baseW) * zoom
                    val currentHeight = (if (isCircle) minOf(baseW, baseH) else baseH) * zoom
                    val bounds = rotatedBounds(currentWidth, currentHeight, rotation)
                    val xm = snapAxis(rawX, bounds.first, canvasWidthPx, threshold, otherCenterX)
                    val ym = snapAxis(rawY, bounds.second, canvasHeightPx, threshold, otherCenterY)
                    x = xm.value; y = ym.value
                    onSnapFeedback(SnapFeedback(xm.guideRatio, ym.guideRatio, if (wasSnapped) rotation else null))
                    pushTransform()
                }
            }
        }
        Box(bodyMod) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Color(layer.colorArgb).copy(alpha = if (layer.doodleSessionId != null) layer.doodleOpacity.coerceIn(0f, 1f) else 1f)
                val strokePx = with(density) { layer.strokeWidth.dp.toPx() }.coerceAtLeast(1f)
                val pad = strokePx * 1.3f
                val w = (size.width - 2f * pad).coerceAtLeast(1f)
                val h = (size.height - 2f * pad).coerceAtLeast(1f)
                val style: DrawStyle = if (layer.filled && layer.type in listOf(ShapeType.Rectangle, ShapeType.Ellipse)) Fill else Stroke(width=strokePx,cap=StrokeCap.Round)
                when (layer.type) {
                    ShapeType.Rectangle -> {
                        if (layer.roundedCorners) drawRoundRect(c, topLeft=Offset(pad,pad),size=Size(w,h),cornerRadius=CornerRadius(minOf(w,h)*.12f),style=style)
                        else drawRect(c,topLeft=Offset(pad,pad),size=Size(w,h),style=style)
                    }
                    ShapeType.Ellipse -> drawOval(c,topLeft=Offset(pad,pad),size=Size(w,h),style=style)
                    ShapeType.Line, ShapeType.Arrow -> {
                        val from=Offset(pad,size.height/2f); val to=Offset(size.width-pad,size.height/2f)
                        drawLine(c,from,to,strokeWidth=strokePx,cap=StrokeCap.Round)
                        if (layer.type==ShapeType.Arrow) {
                            val head=minOf(w*.18f,36.dp.toPx())
                            drawLine(c,Offset(to.x-head,to.y-head*.6f),to,strokeWidth=strokePx,cap=StrokeCap.Round)
                            drawLine(c,Offset(to.x-head,to.y+head*.6f),to,strokeWidth=strokePx,cap=StrokeCap.Round)
                        }
                    }
                    ShapeType.Symbol -> drawSymbolGlyph(layer.symbolKind, c, strokePx)
                    ShapeType.Freehand -> {
                        val pts = layer.points
                        if (pts.isNotEmpty()) {
                            // DEV19: 小笔迹需要至少20px的选择触控框，但绘图必须以真实路径宽高为准。
                            // 否则单点落笔/竖线会被绘制在选择框左上角，和高清导出的位置不一致。
                            val inkW = displayW.coerceIn(1f, size.width)
                            val inkH = displayH.coerceIn(1f, size.height)
                            val insetX = (size.width - inkW) / 2f
                            val insetY = (size.height - inkH) / 2f
                            fun pointAt(point: ShapePoint) = Offset(
                                insetX + point.x * inkW, insetY + point.y * inkH
                            )
                            if (pts.size == 1) {
                                drawCircle(c, strokePx / 2f, pointAt(pts.first()))
                            } else {
                                val path = Path().apply {
                                    val start = pointAt(pts.first())
                                    moveTo(start.x, start.y)
                                    pts.drop(1).forEach { point ->
                                        val next = pointAt(point)
                                        lineTo(next.x, next.y)
                                    }
                                }
                                drawPath(path, c, style = Stroke(width = strokePx,
                                    cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                    }
                }
            }
            if (selected && editable && enabled) {
                Box(Modifier.matchParentSize().border(1.dp,TgfTokens.Blue.copy(alpha=.90f),RoundedCornerShape(5.dp)))
            }
        }

        if (selected && editable && enabled) {
            // 三个操作角标都在实际图形边框之外，图形尺寸很小时也不会挤在一起。
            ShapeOuterAction(Alignment.TopStart, "删除图形", ShapeQuickAction.Delete) { onDelete(layer.id) }
            ShapeOuterAction(Alignment.BottomStart, "复制图形", ShapeQuickAction.Copy) { latestDuplicate(layer.id) }
            if (!layer.locked) {
                ShapeOuterTransformHandle(
                    label = "拖动同时缩放与旋转图形",
                    onStart = {
                        active = true
                        handleTotal = Offset.Zero
                        val width = with(density) { drawW.toPx() }
                        val height = with(density) { drawH.toPx() }
                        val controlGap = with(density) { outerGap.toPx() }
                        handleOrigin = Offset(width/2f+controlGap,-height/2f-controlGap)
                        startScale = zoom
                        startRotation = rawRotation
                    },
                    onMove = { delta ->
                        active = true; pulse += 1
                        handleTotal += delta
                        val vector = handleOrigin + handleTotal
                        val initialLength = handleOrigin.getDistance().coerceAtLeast(1f)
                        val zoomFactor = vector.getDistance()/initialLength
                        zoom = (startScale*zoomFactor).coerceIn(.35f,6f)
                        val startAngle = atan2(handleOrigin.y,handleOrigin.x)
                        val currentAngle = atan2(vector.y,vector.x)
                        rawRotation = normalizeRotation(startRotation + (currentAngle-startAngle)*180f/PI.toFloat())
                        val (snapped,wasSnapped) = snapRotation(rawRotation)
                        rotation=snapped
                        onSnapFeedback(if(wasSnapped) SnapFeedback(rotationAngle=rotation) else null)
                        pushTransform()
                    },
                    onStop = { active = false; onSnapFeedback(null) }
                )
            }
            if (layer.locked) {
                Text("已锁定",color=TgfTokens.Blue,fontSize=10.sp,
                    modifier=Modifier.align(Alignment.BottomEnd).padding(end=46.dp))
            }
        }
    }
}

/** 图形 / 符号快捷操作统一使用矢量图标，不使用依赖字体渲染的特殊字符。 */
private enum class ShapeQuickAction { Delete, Copy }

@Composable
private fun BoxScope.ShapeOuterAction(
    alignment: Alignment,
    label: String,
    action: ShapeQuickAction,
    onClick: () -> Unit
) {
    val accent = if (action == ShapeQuickAction.Delete) Color(0xFFE48794) else TgfTokens.Blue
    Box(
        Modifier.align(alignment).size(42.dp).zIndex(15f)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LightCornerIconSurface(iconSize = 21.dp, accent = accent) {
            Canvas(Modifier.size(17.dp)) {
                val sw = 1.7.dp.toPx()
                val c = accent
                if (action == ShapeQuickAction.Delete) {
                    // 垃圾桶图标：盖、桶口、桶身和两条竖线。
                    drawLine(c, Offset(size.width*.19f, size.height*.30f),
                        Offset(size.width*.81f,size.height*.30f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.39f,size.height*.21f),
                        Offset(size.width*.61f,size.height*.21f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.26f,size.height*.38f),
                        Offset(size.width*.33f,size.height*.81f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.74f,size.height*.38f),
                        Offset(size.width*.67f,size.height*.81f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.33f,size.height*.81f),
                        Offset(size.width*.67f,size.height*.81f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.44f,size.height*.43f),
                        Offset(size.width*.44f,size.height*.69f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.56f,size.height*.43f),
                        Offset(size.width*.56f,size.height*.69f), sw, StrokeCap.Round)
                } else {
                    // 两个叠放的矩形；上下边距和笔宽随图标尺寸自适应。
                    drawRoundRect(c, topLeft=Offset(size.width*.18f,size.height*.30f),
                        size=Size(size.width*.49f,size.height*.51f),
                        cornerRadius=CornerRadius(2.dp.toPx()), style=Stroke(sw))
                    drawLine(c, Offset(size.width*.42f,size.height*.18f),
                        Offset(size.width*.79f,size.height*.18f), sw, StrokeCap.Round)
                    drawLine(c, Offset(size.width*.79f,size.height*.18f),
                        Offset(size.width*.79f,size.height*.64f), sw, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ShapeOuterTransformHandle(
    label: String,
    onStart: () -> Unit,
    onMove: (Offset) -> Unit,
    onStop: () -> Unit
) {
    val latestStart by rememberUpdatedState(onStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestStop by rememberUpdatedState(onStop)
    Box(
        Modifier.align(Alignment.TopEnd).size(42.dp).zIndex(17f)
            .semantics { contentDescription=label }
            .pointerInput(label) {
                detectDragGestures(
                    onDragStart={ latestStart() },
                    onDragEnd={ latestStop() },
                    onDragCancel={ latestStop() },
                    onDrag={ change,dragAmount -> change.consume();latestMove(dragAmount) }
                )
            },contentAlignment=Alignment.Center
    ) {
        LightCornerIconSurface(iconSize = 23.dp, accent = TgfTokens.Blue) {
            TransformHandleGlyph(modifier = Modifier.size(18.dp), color = TgfTokens.Blue)
        }
    }
}

/** 同一会话为一个逻辑图层，每笔是可撤销的路径。
 * 双指仅缩放/平移查看画布，不会产生笔迹；橡皮擦仅删除涂鸦路径。 */
@Composable
private fun BoxScope.DoodleDrawingOverlay(
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    tool: DoodleTool,
    eraseMode: DoodleEraseMode,
    eraserSize: Float,
    color: Int,
    strokeWidth: Float,
    opacity: Float,
    sessionId: String,
    doodles: List<ShapeLayer>,
    onComplete: (ShapeLayer) -> Unit,
    onErase: (Map<String, List<ShapeLayer>>) -> Unit,
    onViewportTransform: (Offset, Float) -> Unit
) {
    var trail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val currentTool by rememberUpdatedState(tool)
    val currentEraseMode by rememberUpdatedState(eraseMode)
    val currentEraserSize by rememberUpdatedState(eraserSize)
    val currentColor by rememberUpdatedState(color)
    val currentWidth by rememberUpdatedState(strokeWidth)
    val currentOpacity by rememberUpdatedState(opacity)
    val currentSession by rememberUpdatedState(sessionId)
    val currentDoodles by rememberUpdatedState(doodles)
    val complete by rememberUpdatedState(onComplete)
    val erase by rememberUpdatedState(onErase)
    val viewport by rememberUpdatedState(onViewportTransform)
    val density = LocalDensity.current
    val eraserScope = rememberCoroutineScope()
    val eraseMutex = remember { kotlinx.coroutines.sync.Mutex() }
    Box(Modifier.matchParentSize().zIndex(40f)
        .pointerInput(canvasWidthPx, canvasHeightPx) {
            awaitEachGesture {
                val first = awaitFirstDown(requireUnconsumed = false)
                val points = mutableListOf(first.position)
                var multiTouch = false
                // Eraser only needs a live cursor, not a full new immutable Compose
                // list on each frame. The actual path is buffered for the final erase.
                trail = points.takeLast(96)
                val minTravelPx = with(density) { 1.2.dp.toPx() }
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size > 1) {
                        multiTouch = true
                        points.clear()
                        trail = emptyList()
                        viewport(event.calculatePan(), event.calculateZoom())
                    } else if (!multiTouch) {
                        // DEV19: 以最初落笔的指针为准，并记录 UP 事件中的最终坐标。
                        // 过去仅在 pressed == 1 时写入，会漏掉快速抬笔前的最后一段。
                        val primary = event.changes.firstOrNull { it.id == first.id }
                        if (primary != null) {
                            val point = primary.position
                            val last = points.lastOrNull()
                            val dx = point.x - (last?.x ?: point.x)
                            val dy = point.y - (last?.y ?: point.y)
                            // 过滤重复/超细抖动，但最后一个释放点无论距离都保留。
                            val addPoint = last == null ||
                                dx * dx + dy * dy >= minTravelPx * minTravelPx || !primary.pressed
                            if (point.x.isFinite() && point.y.isFinite() && addPoint && point != last) {
                                if (points.size < 4096 || !primary.pressed) {
                                    points.add(point)
                                } else if (points.size == 4096) {
                                    // Keep endpoint fresh without unbounded path growth.
                                    points[points.lastIndex] = point
                                }
                                trail = points.takeLast(96)
                            }
                        }
                    }
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
                if (!multiTouch) {
                    if (currentTool == DoodleTool.Eraser) {
                        val path = points.map { DoodleErasePoint(it.x, it.y) }
                        val radius = with(density) { (currentEraserSize / 2f).dp.toPx() }
                        val displayDensity = density.density
                        // 明确映射类型，避免 Kotlin 对 mapNotNull + toMap 的泛型推断失败。
                        // null = 本笔迹未命中，空列表 = 整笔删除，非空列表 = 保留片段。
                        val sourceDoodles = currentDoodles.toList()
                        val eraseModeAtRelease = currentEraseMode
                        eraserScope.launch {
                            eraseMutex.lock()
                            try {
                                val modifications = withContext(Dispatchers.Default) {
                                    val result = mutableMapOf<String, List<ShapeLayer>>()
                                    val maxRadius = radius + (sourceDoodles.maxOfOrNull { it.strokeWidth }
                                        ?: 0f) * displayDensity / 2f
                                    val spatialIndex = com.tugongfang.editor.model.layer.DoodleEraseSpatialIndex(path, maxRadius)
                                    for (stroke in sourceDoodles) {
                                        val chunks = eraseDoodlePath(
                                            layer = stroke,
                                            eraserPath = path,
                                            canvasWidth = canvasWidthPx,
                                            canvasHeight = canvasHeightPx,
                                            eraserRadiusPx = radius,
                                            density = displayDensity,
                                            spatialIndex = spatialIndex
                                        )
                                        if (chunks != null) {
                                            result[stroke.id] = if (eraseModeAtRelease == DoodleEraseMode.Whole)
                                                emptyList() else chunks
                                        }
                                    }
                                    result
                                }
                                if (modifications.isNotEmpty()) erase(modifications)
                            } finally {
                                eraseMutex.unlock()
                            }
                        }
                    } else if (points.isNotEmpty()) {
                        // 坐标先裁入画布再归一化，防止在边缘快速抬笔时出现负尺寸/越界点。
                        val safePoints = points.filter { it.x.isFinite() && it.y.isFinite() }
                            .map { Offset(it.x.coerceIn(0f, canvasWidthPx),
                                it.y.coerceIn(0f, canvasHeightPx)) }
                        if (safePoints.isNotEmpty()) {
                            val left = safePoints.minOf { it.x }
                            val top = safePoints.minOf { it.y }
                            val right = safePoints.maxOf { it.x }
                            val bottom = safePoints.maxOf { it.y }
                            val w = (right - left).coerceAtLeast(1f)
                            val h = (bottom - top).coerceAtLeast(1f)
                            complete(ShapeLayer(
                                type = ShapeType.Freehand,
                                doodleSessionId = currentSession,
                                doodleBrush = when(currentTool) {
                                    DoodleTool.Marker -> DoodleBrush.Marker
                                    DoodleTool.Highlighter -> DoodleBrush.Highlighter
                                    else -> DoodleBrush.Pencil
                                },
                                doodleOpacity = if (currentTool == DoodleTool.Highlighter) (currentOpacity * .38f).coerceIn(.08f, .48f) else currentOpacity,
                                offsetXRatio = ((left+right)/2f-canvasWidthPx/2f)/canvasWidthPx,
                                offsetYRatio = ((top+bottom)/2f-canvasHeightPx/2f)/canvasHeightPx,
                                widthRatio = w / canvasWidthPx,
                                heightRatio = h / canvasHeightPx,
                                colorArgb = currentColor,
                                strokeWidth = currentWidth,
                                points = safePoints.map { ShapePoint(((it.x-left)/w).coerceIn(0f,1f), ((it.y-top)/h).coerceIn(0f,1f)) }
                            ))
                        }
                    }
                }
                trail = emptyList()
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            if (trail.isNotEmpty() && tool == DoodleTool.Eraser) {
                val radius = (eraserSize / 2f).dp.toPx()
                // Immediate full gesture feedback, even while the final vector
                // erase operation is running on Dispatchers.Default.
                if (trail.size > 1) {
                    drawPath(Path().apply {
                        moveTo(trail.first().x, trail.first().y)
                        trail.drop(1).forEach { lineTo(it.x, it.y) }
                    }, TgfTokens.Blue.copy(alpha = .14f),
                        style = Stroke(width = radius * 2f, cap = StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
                drawCircle(TgfTokens.Blue.copy(alpha = .12f), radius = radius, center = trail.last())
                drawCircle(TgfTokens.Blue.copy(alpha = .85f), radius = radius,
                    center = trail.last(), style = Stroke(width = 1.5.dp.toPx()))
            }
            if (trail.isNotEmpty() && tool != DoodleTool.Eraser) {
                val paintColor = Color(color).copy(alpha = if (tool == DoodleTool.Highlighter) (opacity*.38f).coerceIn(.08f,.48f) else opacity)
                val stroke = strokeWidth.dp.toPx()
                if (trail.size == 1) drawCircle(paintColor, stroke/2f, trail.first())
                else drawPath(Path().apply {
                    moveTo(trail.first().x,trail.first().y)
                    trail.drop(1).forEach { lineTo(it.x,it.y) }
                }, paintColor, style = Stroke(width=stroke, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

private fun symbolKindTitle(kind: SymbolKind): String = when (kind) {
    SymbolKind.Check -> "对勾"
    SymbolKind.Cross -> "叉号"
    SymbolKind.Star -> "星标"
    SymbolKind.Heart -> "爱心"
    SymbolKind.Alert -> "提醒"
    SymbolKind.Pin -> "定位"
    SymbolKind.Flag -> "旗标"
    SymbolKind.Info -> "信息"
}

/** All symbols use one normalized geometry for both Compose and JPG rendering. */
private fun DrawScope.drawSymbolGlyph(kind: SymbolKind, color: Color, strokePx: Float) {
    val side = minOf(size.width, size.height).coerceAtLeast(1f)
    val originX = (size.width - side) / 2f
    val originY = (size.height - side) / 2f
    val glyph = SymbolGeometry.glyph(kind)
    val pen = Stroke(width=strokePx.coerceAtLeast(1f),cap=StrokeCap.Round,
        join=androidx.compose.ui.graphics.StrokeJoin.Round)
    glyph.strokes.forEach { poly ->
        if (poly.vertices.isNotEmpty()) {
            val path = Path().apply {
                val start = poly.vertices.first()
                moveTo(originX + start.x*side, originY + start.y*side)
                poly.vertices.drop(1).forEach { lineTo(originX + it.x*side,originY + it.y*side) }
                if (poly.closed) close()
            }
            drawPath(path, color, style=pen)
        }
    }
    glyph.rings.forEach { ring ->
        drawCircle(color, radius=ring.radius*side, center=Offset(originX+ring.cx*side,
            originY+ring.cy*side), style=pen)
    }
}

@Composable
private fun SymbolTypePreview(kind: SymbolKind) {
    Canvas(Modifier.size(25.dp)) {
        drawSymbolGlyph(kind,TgfTokens.Blue,2.3.dp.toPx())
    }
}

private fun shapeTypeTitle(type: ShapeType): String = when(type) {
    ShapeType.Rectangle -> "矩形"
    ShapeType.Ellipse -> "圆形 / 椭圆"
    ShapeType.Line -> "直线"
    ShapeType.Arrow -> "箭头"
    ShapeType.Freehand -> "手绘"
    ShapeType.Symbol -> "符号"
}

@Composable
private fun ShapeTypePreview(kind: ShapeType) {
    Canvas(Modifier.size(26.dp)) {
        val c=TgfTokens.Blue
        val sw=2.6.dp.toPx()
        val m=4.dp.toPx()
        val inner=Size(size.width-2*m,size.height-2*m)
        when(kind) {
            ShapeType.Rectangle -> drawRoundRect(c,Offset(m,m),inner,CornerRadius(4.dp.toPx()),style=Stroke(sw))
            ShapeType.Ellipse -> drawOval(c,Offset(m,m),inner,style=Stroke(sw))
            ShapeType.Line -> drawLine(c,Offset(m,size.height/2f),Offset(size.width-m,size.height/2f),sw,StrokeCap.Round)
            ShapeType.Arrow -> {
                val end=Offset(size.width-m,size.height/2f)
                drawLine(c,Offset(m,size.height/2f),end,sw,StrokeCap.Round)
                drawLine(c,Offset(end.x-7.dp.toPx(),end.y-5.dp.toPx()),end,sw,StrokeCap.Round)
                drawLine(c,Offset(end.x-7.dp.toPx(),end.y+5.dp.toPx()),end,sw,StrokeCap.Round)
            }
            ShapeType.Symbol -> drawSymbolGlyph(SymbolKind.Check, c, sw)
            ShapeType.Freehand -> {
                val p=Path().apply { moveTo(m,size.height*.60f); cubicTo(size.width*.36f,0f,size.width*.65f,size.height*.95f,size.width-m,size.height*.30f) }
                drawPath(p,c,style=Stroke(sw,cap=StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ShapeToolPanel(
    modifier: Modifier,
    selectedShape: ShapeLayer?,
    page: ShapePanelPage,
    onAdd: (ShapeLayer) -> Unit,
    onChange: (ShapeLayer) -> Unit,
    mode: ShapeToolMode,
    onModeChange: (ShapeToolMode) -> Unit,
    doodleTool: DoodleTool,
    onDoodleToolChange: (DoodleTool) -> Unit,
    doodleEraseMode: DoodleEraseMode,
    onDoodleEraseModeChange: (DoodleEraseMode) -> Unit,
    doodleEraserSize: Float,
    onDoodleEraserSizeChange: (Float) -> Unit,
    doodleColor: Int,
    onDoodleColorChange: (Int) -> Unit,
    doodleStrokeWidth: Float,
    onDoodleStrokeWidthChange: (Float) -> Unit,
    doodleOpacity: Float,
    onDoodleOpacityChange: (Float) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit
) {
    var customColorOpen by remember { mutableStateOf(false) }
    val edit = if (page == ShapePanelPage.Edit) selectedShape else null
    LaunchedEffect(page, edit?.id, mode) { customColorOpen = false }
    GlassCard(
        modifier = modifier.fillMaxWidth(), radius = 22.dp, elevation = 10.dp,
        padding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(Modifier.heightIn(max = if (expanded) 310.dp else if (edit != null && customColorOpen) 245.dp else 176.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (edit != null) "编辑 · ${if (edit.type == ShapeType.Symbol) symbolKindTitle(edit.symbolKind) else shapeTypeTitle(edit.type)}"
                    else if (mode == ShapeToolMode.Doodle) "涂鸦" else "图形 · 符号",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink,
                    modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
                SecondaryPanelExpandKey(expanded, onToggleExpanded)
                Spacer(Modifier.width(5.dp))
                if (edit == null && mode != ShapeToolMode.Doodle) {
                    Text("关闭", fontSize=11.sp, color=TgfTokens.Blue,
                        modifier=Modifier.clickable(onClick=onClose).padding(horizontal=10.dp,vertical=9.dp))
                } else {
                    EditorDoneButton(onClick=onDone, compact=true)
                }
            }
            if (edit == null) {
                if (mode != ShapeToolMode.Doodle) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ShapeToolMode.Shapes to "图形", ShapeToolMode.Symbols to "符号").forEach { (kind, label) ->
                        Box(Modifier.weight(1f).height(25.dp)
                            .background(if (mode == kind) TgfTokens.SurfaceSelected else TgfTokens.SurfaceBase, RoundedCornerShape(10.dp))
                            .border(1.dp, if (mode == kind) TgfTokens.Blue else TgfTokens.SurfaceOutline, RoundedCornerShape(10.dp))
                            .clickable { onModeChange(kind) }, contentAlignment = Alignment.Center) {
                            Text(label, fontSize=11.sp, color=if (mode == kind) TgfTokens.Blue else TgfTokens.Ink)
                        }
                    }
                }
                if (mode == ShapeToolMode.Shapes) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ShapeType.Rectangle, ShapeType.Ellipse, ShapeType.Line, ShapeType.Arrow).forEach { kind ->
                            Row(Modifier.width(96.dp).height(42.dp)
                                .background(TgfTokens.SurfaceRaised, RoundedCornerShape(12.dp))
                                .border(1.dp,TgfTokens.SurfaceOutline,RoundedCornerShape(12.dp))
                                .clickable { onAdd(ShapeLayer(type = kind)) }
                                .padding(horizontal=6.dp), verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                ShapeTypePreview(kind)
                                Text(if (kind == ShapeType.Ellipse) "圆形" else shapeTypeTitle(kind), fontSize=11.sp,
                                    maxLines=1, color=TgfTokens.Ink)
                            }
                        }
                    }
                    Text("单击选中 · 双击图形重新编辑", fontSize=10.sp, color=TgfTokens.InkSecondary, maxLines=1)
                } else if (mode == ShapeToolMode.Symbols) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SymbolKind.values().forEach { kind ->
                            Row(Modifier.width(89.dp).height(42.dp)
                                .background(TgfTokens.SurfaceRaised, RoundedCornerShape(12.dp))
                                .border(1.dp,TgfTokens.SurfaceOutline,RoundedCornerShape(12.dp))
                                .clickable {
                                    onAdd(ShapeLayer(type = ShapeType.Symbol, symbolKind = kind,
                                        widthRatio = .26f, heightRatio = .16f, strokeWidth = 5f))
                                }.padding(horizontal=6.dp),
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                SymbolTypePreview(kind)
                                Text(symbolKindTitle(kind), fontSize=11.sp, maxLines=1, color=TgfTokens.Ink)
                            }
                        }
                    }
                    Text("矢量符号 · 点击添加 · 双击重新编辑",fontSize=10.sp,
                        color=TgfTokens.InkSecondary,maxLines=1)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(DoodleTool.Pencil to "画笔", DoodleTool.Marker to "马克笔",
                            DoodleTool.Highlighter to "荧光笔", DoodleTool.Eraser to "橡皮擦").forEach { (kind,label) ->
                            Box(Modifier.weight(1f).height(31.dp)
                                .background(if (doodleTool == kind) TgfTokens.SurfaceSelected else TgfTokens.SurfaceBase,RoundedCornerShape(9.dp))
                                .border(1.dp,if (doodleTool == kind) TgfTokens.Blue else TgfTokens.SurfaceOutline,RoundedCornerShape(9.dp))
                                .clickable { onDoodleToolChange(kind) },contentAlignment=Alignment.Center) {
                                Text(label,fontSize=10.sp,maxLines=1,color=if (doodleTool==kind) TgfTokens.Blue else TgfTokens.Ink)
                            }
                        }
                    }
                    if (doodleTool == DoodleTool.Eraser) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("擦除", fontSize=10.sp, color=TgfTokens.InkSecondary)
                            listOf(DoodleEraseMode.Partial to "局部", DoodleEraseMode.Whole to "整笔").forEach { (kind, label) ->
                                Box(Modifier.background(if (doodleEraseMode == kind) TgfTokens.SurfaceSelected else TgfTokens.SurfaceBase,
                                    RoundedCornerShape(9.dp))
                                    .border(1.dp, if (doodleEraseMode == kind) TgfTokens.Blue else TgfTokens.SurfaceOutline,
                                        RoundedCornerShape(9.dp))
                                    .clickable { onDoodleEraseModeChange(kind) }
                                    .padding(horizontal=9.dp, vertical=3.dp)) {
                                    Text(label, fontSize=10.sp, color=TgfTokens.Ink)
                                }
                            }
                            Text("大小", fontSize=10.sp, color=TgfTokens.InkSecondary)
                            Slider(value=doodleEraserSize.coerceIn(6f,48f), onValueChange=onDoodleEraserSizeChange,
                                valueRange=6f..48f, modifier=Modifier.weight(1f).height(25.dp))
                            Text("${doodleEraserSize.roundToInt()}", fontSize=9.sp, color=TgfTokens.Ink)
                        }
                    } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(0xFF3A78FF.toInt(),0xFFE94D63.toInt(),0xFF202536.toInt(),0xFFF5BC4E.toInt(),0xFF27AD8B.toInt()).forEach { c ->
                            Box(Modifier.size(21.dp).background(Color(c),CircleShape)
                                .border(if (doodleColor==c) 2.dp else 1.dp, if (doodleColor==c) TgfTokens.Blue else TgfTokens.SurfaceOutline,CircleShape)
                                .clickable { onDoodleColorChange(c) })
                        }
                        Spacer(Modifier.weight(1f))
                        Text("色环",fontSize=10.sp,color=TgfTokens.Blue,modifier=Modifier.clickable { customColorOpen=!customColorOpen })
                    }
                    if (customColorOpen) {
                        val hsv=FloatArray(3)
                        android.graphics.Color.colorToHSV(doodleColor,hsv)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            ColorWheelPicker(hue=hsv[0],saturation=hsv[1],brightness=hsv[2]) { hue ->
                                onDoodleColorChange(android.graphics.Color.HSVToColor(floatArrayOf(hue,hsv[1],hsv[2])))
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text("饱和度",fontSize=9.sp,color=TgfTokens.InkSecondary)
                                Slider(value=hsv[1],onValueChange={ onDoodleColorChange(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0],it,hsv[2]))) }, valueRange=0f..1f)
                                Text("明度",fontSize=9.sp,color=TgfTokens.InkSecondary)
                                Slider(value=hsv[2],onValueChange={ onDoodleColorChange(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0],hsv[1],it))) }, valueRange=0f..1f)
                            }
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("粗细",fontSize=10.sp,color=TgfTokens.InkSecondary)
                        Slider(value=doodleStrokeWidth.coerceIn(1f,40f),onValueChange=onDoodleStrokeWidthChange,
                            valueRange=1f..40f,modifier=Modifier.weight(1f).height(27.dp))
                        Text(doodleStrokeWidth.roundToInt().toString(),fontSize=9.sp,color=TgfTokens.Ink)
                        Text("透明",fontSize=10.sp,color=TgfTokens.InkSecondary)
                        Slider(value=doodleOpacity.coerceIn(.1f,1f),onValueChange=onDoodleOpacityChange,
                            valueRange=.1f..1f,modifier=Modifier.weight(1f).height(27.dp))
                    }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("颜色", fontSize = 11.sp, color = TgfTokens.InkSecondary)
                    Spacer(Modifier.width(7.dp))
                    listOf(0xFF3A78FF.toInt(), 0xFFE94D63.toInt(), 0xFF202536.toInt(),
                        0xFFF5BC4E.toInt(), 0xFF27AD8B.toInt()).forEach { argb ->
                        Box(Modifier.padding(end = 6.dp).size(23.dp)
                            .background(Color(argb), CircleShape)
                            .border(if (edit.colorArgb == argb) 2.dp else 1.dp,
                                if (edit.colorArgb == argb) TgfTokens.Blue else Color.White, CircleShape)
                            .clickable { onChange(edit.copy(colorArgb = argb)) })
                    }
                    Spacer(Modifier.weight(1f))
                    Text("自定义", fontSize = 11.sp, color = TgfTokens.Blue,
                        modifier = Modifier.clickable { customColorOpen = !customColorOpen })
                }
                if (customColorOpen) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(edit.colorArgb, hsv)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorWheelPicker(hue = hsv[0], saturation = hsv[1], brightness = hsv[2]) { hue ->
                            onChange(edit.copy(colorArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, hsv[1], hsv[2]))))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("饱和度", fontSize = 9.sp, color = TgfTokens.InkSecondary)
                            Slider(value = hsv[1].coerceIn(0f, 1f), onValueChange = { v ->
                                onChange(edit.copy(colorArgb = android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], v, hsv[2]))))
                            }, valueRange = 0f..1f)
                            Text("明度", fontSize = 9.sp, color = TgfTokens.InkSecondary)
                            Slider(value = hsv[2].coerceIn(0f, 1f), onValueChange = { v ->
                                onChange(edit.copy(colorArgb = android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], v))))
                            }, valueRange = 0f..1f)
                        }
                    }
                }
                // 属性合成一行，不再为角型和描边各占一大块面积。
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (edit.type) {
                        ShapeType.Rectangle -> {
                            Text("角型", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                            TextPropertyButton("直角", selected = !edit.roundedCorners) { onChange(edit.copy(roundedCorners = false)) }
                            TextPropertyButton("圆角", selected = edit.roundedCorners) { onChange(edit.copy(roundedCorners = true)) }
                        }
                        ShapeType.Ellipse -> {
                            Text("比例", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                            TextPropertyButton("椭圆", selected = !edit.perfectCircle) { onChange(edit.copy(perfectCircle = false)) }
                            TextPropertyButton("正圆", selected = edit.perfectCircle) { onChange(edit.copy(perfectCircle = true)) }
                        }
                        else -> Unit
                    }
                    if (edit.type == ShapeType.Rectangle || edit.type == ShapeType.Ellipse) {
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.size(width = 56.dp, height = 30.dp)
                                .background(TgfTokens.SurfaceRaised, RoundedCornerShape(10.dp))
                                .border(1.dp, TgfTokens.Blue.copy(alpha = .30f), RoundedCornerShape(10.dp))
                                .clickable { onChange(edit.copy(filled = !edit.filled)) }
                                .semantics { contentDescription = if (edit.filled) "切换为描边" else "切换为填充" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(Modifier.size(21.dp)) {
                                drawRoundRect(TgfTokens.Blue, topLeft = Offset(size.width * .2f, size.height * .2f),
                                    size = Size(size.width * .6f, size.height * .6f), cornerRadius = CornerRadius(3.dp.toPx()),
                                    style = if (edit.filled) Fill else Stroke(2.3.dp.toPx()))
                            }
                        }
                        Text(if (edit.filled) "填充" else "描边", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("线宽", fontSize = 11.sp, color = TgfTokens.InkSecondary)
                    Slider(value = edit.strokeWidth.coerceIn(2f, 16f),
                        onValueChange = { onChange(edit.copy(strokeWidth = it)) },
                        valueRange = 2f..16f, modifier = Modifier.weight(1f).height(30.dp))
                    Text(edit.strokeWidth.roundToInt().toString(), fontSize = 11.sp, color = TgfTokens.Ink)
                }
            }
        }
    }
}

@Composable
private fun EditorToolBar(
    scrollState: ScrollState,
    singlePhotoMode: Boolean,
    imageSelected: Boolean,
    shapeSelected: Boolean,
    doodleSelected: Boolean,
    mosaicSelected: Boolean,
    collageSelected: Boolean,
    textSelected: Boolean,
    onSingleCrop: () -> Unit,
    onSingleTone: () -> Unit,
    onSingleDecoration: (String) -> Unit,
    onSingleReplace: () -> Unit,
    onCollage: () -> Unit,
    onCollageBackground: () -> Unit,
    onCollageAspect: () -> Unit,
    onShape: () -> Unit,
    onText: () -> Unit,
    onDoodle: () -> Unit,
    onMosaic: () -> Unit
) {
    // DEV8: 两种编辑模式均用横向滚动代替“更多”；单图工具直达图片操作。
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 7.dp).height(78.dp),
        radius = 22.dp,
        elevation = 7.dp,
        padding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (singlePhotoMode) {
                    EditorTool("裁切", TgfIconType.Crop, false, onSingleCrop)
                    EditorTool("文字", TgfIconType.Text, textSelected, onText)
                    EditorTool("调色", TgfIconType.Style, imageSelected, onSingleTone)
                    EditorTool("背景", TgfIconType.Background, false) { onSingleDecoration("背景") }
                    EditorTool("边框", TgfIconType.Shapes, false) { onSingleDecoration("边框") }
                    EditorTool("替换", TgfIconType.Replace, false, onSingleReplace)
                } else {
                    EditorTool("拼图", TgfIconType.Collage, collageSelected, onCollage)
                    EditorTool("文字", TgfIconType.Text, textSelected, onText)
                    EditorTool("背景", TgfIconType.Background, false, onCollageBackground)
                    EditorTool("比例", TgfIconType.Aspect, false, onCollageAspect)
                }
                EditorTool("图形", TgfIconType.Shapes, shapeSelected, onShape)
                EditorTool("涂鸦", TgfIconType.Doodle, doodleSelected, onDoodle)
                EditorTool("马赛克", TgfIconType.Mosaic, mosaicSelected, onMosaic)
                Spacer(Modifier.width(6.dp))
            }
            // 不是额外按钮，只用边缘轻渐变暗示右侧仍可滑动查看工具。
            if (scrollState.value < scrollState.maxValue) {
                Box(Modifier.align(Alignment.CenterEnd).width(9.dp).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color.Transparent,
                        TgfTokens.SurfaceBase.copy(alpha = .85f)))))
            }
        }
    }
}

@Composable
private fun EditorTool(
    title: String,
    type: TgfIconType,
    selected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val corner = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.width(76.dp).height(61.dp)
            .tgfToolSurface(selected, corner)
            .semantics { contentDescription = if (selected) "$title，当前选中" else title }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TgfIcon(type, modifier = Modifier.size(36.dp), selected = selected)
        Spacer(Modifier.height(3.dp))
        Text(
            title,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) TgfTokens.SelectionInk else TgfTokens.Ink
        )
    }
}

@Composable
private fun LayerPanel(
    modifier: Modifier,
    layers: List<ImageLayer>,
    textLayers: List<TextLayer>,
    infoLayers: List<InfoPanelLayer>,
    shapeLayers: List<ShapeLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String?) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onReplacePhoto: (String) -> Unit,
    onToggleTextVisibility: (String) -> Unit,
    onToggleTextLock: (String) -> Unit,
    onDeleteTextLayer: (String) -> Unit,
    onDuplicateTextLayer: (String) -> Unit,
    onEditText: (String) -> Unit,
    onEditInfo: (String) -> Unit,
    onToggleInfoVisibility: (String) -> Unit,
    onDeleteInfoLayer: (String) -> Unit,
    onDeleteShapeLayer: (String) -> Unit,
    onDuplicateShapeLayer: (String) -> Unit,
    onToggleShapeLock: (String) -> Unit,
    onToggleShapeVisibility: (String) -> Unit,
    onClose: () -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 24.dp,
        elevation = 14.dp,
        padding = PaddingValues(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("图层", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Spacer(Modifier.weight(1f))
                Text("${layers.size + textLayers.size + infoLayers.size + shapeLayers.map { it.doodleSessionId ?: it.id }.distinct().size} 层", fontSize = 11.sp, color = TgfTokens.InkSecondary)
                Spacer(Modifier.width(14.dp))
                Text("关闭", fontSize = 11.sp, color = TgfTokens.Blue, modifier = Modifier.clickable(onClick = onClose))
            }
            Spacer(Modifier.height(10.dp))

            if (layers.isEmpty() && textLayers.isEmpty() && infoLayers.isEmpty() && shapeLayers.isEmpty()) {
                Text("暂无图层", fontSize = 12.sp, color = TgfTokens.InkSecondary)
            } else {
                shapeLayers.asReversed().distinctBy { it.doodleSessionId ?: it.id }.forEach { layer ->
                    val doodleCount = if (layer.doodleSessionId != null)
                        shapeLayers.count { it.doodleSessionId == layer.doodleSessionId } else 0
                    val selected = layer.id == selectedLayerId
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(if (selected) TgfTokens.Blue.copy(alpha = .08f) else Color.White.copy(alpha = .26f), RoundedCornerShape(15.dp))
                            .border(1.dp, if (selected) TgfTokens.Blue.copy(alpha = .28f) else Color.White.copy(alpha = .55f), RoundedCornerShape(15.dp))
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TgfIcon(TgfIconType.Shapes, modifier = Modifier.size(30.dp), selected = selected)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (doodleCount > 0) "涂鸦（${doodleCount} 笔）" else if (layer.type == ShapeType.Symbol) "符号 · ${symbolKindTitle(layer.symbolKind)}" else shapeTypeTitle(layer.type),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                            Text(if (layer.locked) "已锁定" else if (doodleCount > 0) "涂鸦 · 独立可隐藏" else if (layer.type == ShapeType.Symbol) "符号 · 向量" else "图形 · 向量",
                                fontSize = 9.sp, color = TgfTokens.InkSecondary)
                        }
                        LayerMiniAction("复制") { onDuplicateShapeLayer(layer.id) }
                        Spacer(Modifier.width(5.dp))
                        LayerMiniAction(if (layer.visible) "隐藏" else "显示") { onToggleShapeVisibility(layer.id) }
                        Spacer(Modifier.width(5.dp))
                        LayerMiniAction(if (layer.locked) "解锁" else "锁定") { onToggleShapeLock(layer.id) }
                        Spacer(Modifier.width(5.dp))
                        LayerMiniAction("删除", danger = true) { onDeleteShapeLayer(layer.id) }
                    }
                }

                infoLayers.asReversed().forEachIndexed { index, layer ->
                    val selected = layer.id == selectedLayerId
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(if (selected) TgfTokens.Blue.copy(alpha = .08f) else Color.White.copy(alpha = .26f), RoundedCornerShape(15.dp))
                            .border(1.dp, if (selected) TgfTokens.Blue.copy(alpha = .28f) else Color.White.copy(alpha = .55f), RoundedCornerShape(15.dp))
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TgfIcon(TgfIconType.Template, modifier = Modifier.size(30.dp), selected = selected)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    infoTemplateDisplayName(layer),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = TgfTokens.Ink
                                )
                                Text("文本模板 · 照片下方", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                            }
                            LayerMiniAction("编辑") { onEditInfo(layer.id) }
                            Spacer(Modifier.width(7.dp))
                            LayerMiniAction(if (layer.visible) "隐藏" else "显示") { onToggleInfoVisibility(layer.id) }
                            Spacer(Modifier.width(7.dp))
                            LayerMiniAction("删除", danger = true) { onDeleteInfoLayer(layer.id) }
                        }
                    }
                }

                textLayers.asReversed().forEachIndexed { index, layer ->
                    val selected = layer.id == selectedLayerId
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(if (selected) TgfTokens.Blue.copy(alpha = .08f) else Color.White.copy(alpha = .26f), RoundedCornerShape(15.dp))
                            .border(1.dp, if (selected) TgfTokens.Blue.copy(alpha = .28f) else Color.White.copy(alpha = .55f), RoundedCornerShape(15.dp))
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TgfIcon(TgfIconType.Text, modifier = Modifier.size(30.dp), selected = selected)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    layer.text.ifBlank { "自由文字 ${textLayers.size - index}" }.take(14),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = TgfTokens.Ink,
                                    maxLines = 1
                                )
                                Text(if (layer.locked) "文字 · 已锁定" else "文字 · 可编辑", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                            }
                            LayerMiniAction("编辑") { onEditText(layer.id) }
                            Spacer(Modifier.width(6.dp))
                            LayerMiniAction("复制") { onDuplicateTextLayer(layer.id) }
                            Spacer(Modifier.width(6.dp))
                            LayerMiniAction(if (layer.visible) "隐藏" else "显示") { onToggleTextVisibility(layer.id) }
                            Spacer(Modifier.width(6.dp))
                            LayerMiniAction(if (layer.locked) "解锁" else "锁定") { onToggleTextLock(layer.id) }
                            Spacer(Modifier.width(6.dp))
                            LayerMiniAction("删除", danger = true) { onDeleteTextLayer(layer.id) }
                        }
                    }
                }

                layers.asReversed().forEachIndexed { index, layer ->
                    val selected = layer.id == selectedLayerId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(
                                if (selected) TgfTokens.Blue.copy(alpha = .08f) else Color.White.copy(alpha = .26f),
                                RoundedCornerShape(15.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) TgfTokens.Blue.copy(alpha = .28f) else Color.White.copy(alpha = .55f),
                                RoundedCornerShape(15.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TgfIcon(TgfIconType.Photo, modifier = Modifier.size(30.dp), selected = selected)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "图片 ${layers.size - index}",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = TgfTokens.Ink
                                )
                                Text(if (layer.locked) "图片 · 已锁定" else "图片 · 可编辑", fontSize = 9.5.sp, color = TgfTokens.InkSecondary)
                            }
                            LayerMiniAction("复制") { onDuplicateLayer(layer.id) }
                            Spacer(Modifier.width(5.dp))
                            LayerMiniAction("替换") { onReplacePhoto(layer.id) }
                            Spacer(Modifier.width(5.dp))
                            LayerMiniAction(if (layer.visible) "隐藏" else "显示") { onToggleVisibility(layer.id) }
                            Spacer(Modifier.width(5.dp))
                            LayerMiniAction(if (layer.locked) "解锁" else "锁定") { onToggleLock(layer.id) }
                            Spacer(Modifier.width(5.dp))
                            LayerMiniAction("删除", danger = true) { onDeleteLayer(layer.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerMiniAction(title: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        title,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Medium,
        color = if (danger) Color(0xFFE35C66) else TgfTokens.Blue,
        modifier = Modifier.clickable(onClick = onClick).padding(2.dp)
    )
}
