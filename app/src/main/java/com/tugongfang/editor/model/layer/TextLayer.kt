package com.tugongfang.editor.model.layer

import java.util.UUID

/**
 * v0.4.0 自由文字图层。
 * 坐标、缩放、旋转继续采用非破坏式参数保存，后续信息模板会复用同一编辑坐标体系。
 */
data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val fontSizeSp: Float = 32f,
    val bold: Boolean = false,
    /** Style flags are independent of the font family and persist across edits. */
    val italic: Boolean = false,
    val underline: Boolean = false,
    /** Transparent by default. Solid fill covers the complete editable text frame. */
    val backgroundColorArgb: Int = 0x00000000,
    /** 0=系统无衬线，1=衬线，2=等宽；均使用 Android 系统内置字体。 */
    val fontFamilyId: Int = 0,
    /** 预览和导出共用的行高倍数；旧项目默认为1.22。 */
    val lineSpacing: Float = 1.22f,
    /** 文字框自动换行的内容宽度（dp）。历史项目采用268dp的原有默认表现。 */
    val boxWidthDp: Int = 268,
    /** Minimum frame height (dp), 0 = content adapts automatically; text always wraps to width. */
    val boxHeightDp: Int = 0,
    val colorArgb: Int = 0xFF202536.toInt(),
    /** 0=左对齐，1=居中，2=右对齐 */
    val textAlign: Int = 1
)
