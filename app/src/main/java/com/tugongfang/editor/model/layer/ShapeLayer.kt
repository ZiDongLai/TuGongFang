package com.tugongfang.editor.model.layer

import java.util.UUID

/** 自由手绘笔迹相对自身边界的归一化路径点。 */
data class ShapePoint(val x: Float, val y: Float)

enum class ShapeType { Rectangle, Ellipse, Line, Arrow, Freehand, Symbol }

/** 符号样式作为图形第三模式，复用向量图层与照片关联。 */
enum class SymbolKind { Check, Cross, Star, Heart, Alert, Pin, Flag, Info }

/** 涂鸦笔触类型。涂鸦只附加向量笔迹，不修改原始图片。 */
enum class DoodleBrush { Pencil, Marker, Highlighter }

/** 与预览、历史记录和高清导出共享的非破坏式图层模型。 */
data class ShapeLayer(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType = ShapeType.Rectangle,
    val symbolKind: SymbolKind = SymbolKind.Check,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val widthRatio: Float = .42f,
    val heightRatio: Float = .18f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val colorArgb: Int = 0xFF3A78FF.toInt(),
    val strokeWidth: Float = 5f,
    val filled: Boolean = false,
    /** true=圆角矩形；false=直角矩形。 */
    val roundedCorners: Boolean = true,
    /** true=正圆；false=椭圆。 */
    val perfectCircle: Boolean = false,
    val points: List<ShapePoint> = emptyList(),
    /** 同一绘画会话的笔迹归到一个逻辑图层；null 是普通图形。 */
    val doodleSessionId: String? = null,
    val doodleBrush: DoodleBrush = DoodleBrush.Pencil,
    /** 真实透明度由预览和高清导出一致处理。 */
    val doodleOpacity: Float = 1f,
    /** 绑定的照片图层；照片移动、旋转、缩放时图形保持相对位置。 */
    val parentImageId: String? = null,
    /** 创建图形时的画布宽高比，用于换算旋转后的坐标。 */
    val attachmentCanvasAspect: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false
)
