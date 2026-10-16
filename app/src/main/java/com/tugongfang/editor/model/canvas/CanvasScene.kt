package com.tugongfang.editor.model.canvas

/** 背景是文档属性，绝不参与任何前景图层的位置/缩放/旋转计算。 */
enum class CanvasBackgroundKind { Transparent, Solid, Gradient, Dots, Grid, Paper, Image }
enum class BackgroundGradientDirection { Diagonal, Horizontal, Vertical }
enum class BackgroundImageFit { Fill, Fit }

data class CanvasBackground(
    val kind: CanvasBackgroundKind = CanvasBackgroundKind.Transparent,
    val firstColor: Int = 0xFFFFFFFF.toInt(),
    val secondColor: Int = 0xFFACC8F4.toInt(),
    val gradientDirection: BackgroundGradientDirection = BackgroundGradientDirection.Diagonal,
    val imageUri: String? = null,
    val imageFit: BackgroundImageFit = BackgroundImageFit.Fill,
    val blur: Int = 0,
    val opacity: Float = 1f
)

/** Frame only draws a stroke; it never changes tile geometry or photo edits. */
enum class CanvasFrameScope { Photos, Canvas }
/** In photo mode the frame and photo are clipped by the same selected outline. */
enum class CanvasFrameCorner { Rounded, Square }
data class CanvasFrame(
    val enabled: Boolean = false,
    val scope: CanvasFrameScope = CanvasFrameScope.Photos,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val widthRatio: Float = .005f,
    val corner: CanvasFrameCorner = CanvasFrameCorner.Rounded
) {
    fun normalized() = copy(widthRatio = widthRatio.coerceIn(.001f, .03f))
}

data class CanvasScene(
    val background: CanvasBackground = CanvasBackground(),
    val aspectRatio: Float = 0f,
    /** 0 preserves legacy full-image canvas geometry. Photo inset range 0..20%. */
    val photoInsetRatio: Float = 0f,
    /** Only new single-photo background workflow uses an extended photo canvas. */
    val singlePhotoCanvas: Boolean = false,
    val frame: CanvasFrame = CanvasFrame()
) {
    fun validAspect(): Float = aspectRatio.takeIf { it in .25f..4f } ?: .75f
}
