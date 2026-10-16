package com.tugongfang.editor.model.layer

import java.time.LocalDate
import java.util.UUID

enum class InfoTemplateType {
    MovieContinuity,
    PhotoNote,
    WorkRecord,
    SiteRecord,
    SimpleTitle,
    Custom
}

/**
 * 结构化信息模板：旧项目保留顶部/底部模式，新项目可自由叠加。
 * 叠加模式允许移动、手动调宽高和旋转；所有参数参与存档和导出。
 *
 * v0.4.6：
 * - “信息文字”统一更名为“文本模板”。
 * - 模板本体既是展示区，也是直接编辑区；不再在底部复制一套字段编辑表单。
 * - Custom 模板允许用户自定义模板名称。
 */
data class InfoPanelLayer(
    val id: String = UUID.randomUUID().toString(),
    val templateType: InfoTemplateType = InfoTemplateType.MovieContinuity,
    val customTemplateName: String = "自定义模板",
    val date: String = LocalDate.now().toString(),
    val autoDate: Boolean = true,
    val primary: String = "",
    val secondary: String = "",
    val note: String = "",
    val positionTop: Boolean = false,
    // DEV3: old projects default to a footer; new templates can be placed over the image.
    val overlayMode: Boolean = false,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = .26f,
    val widthRatio: Float = .82f,
    val heightRatio: Float = .26f,
    val rotation: Float = 0f,
    val darkBackground: Boolean = false,
    // DEV15: global typography for all fields inside an information template.
    val fontFamilyId: Int = 0,       // 0 sans, 1 serif, 2 monospace
    val textScale: Float = 1f,       // .90 compact, 1 normal, 1.15 large
    val visible: Boolean = true
)
