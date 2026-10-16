package com.tugongfang.editor.editor.export

import kotlin.math.max
import kotlin.math.roundToInt

/** 保留现有高清渲染上限；仅最终合成完成后按需等比缩小，绝不改变图层坐标。 */
enum class ExportImageFormat { JPG, PNG }

data class ExportOptions(
    val format: ExportImageFormat = ExportImageFormat.JPG,
    val maxOutputLongEdge: Int = 0, // 0: 保持现有高清输出；1200/2048: 仅限制上限
    val jpgQuality: Int = 95,
    val namePrefix: String = "图工坊"
)

object ExportFileRules {
    /** 仅过滤不适合作为媒体文件名的字符。用户输入视为文件名前缀，不包含扩展名。 */
    fun safeNamePrefix(input: String): String = input
        .trim()
        .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
        .trim('.', ' ', '_')
        .take(40)
        .ifBlank { "图工坊" }

    /** Stable gallery name convention; the nonce prevents same-millisecond collisions. */
    fun uniqueOutputName(prefix: String, format: ExportImageFormat, stamp: String, nonce: String): String {
        require(Regex("[0-9]{8}_[0-9]{6}_[0-9]{3}").matches(stamp)) { "Invalid export timestamp" }
        require(Regex("[a-fA-F0-9]{8}").matches(nonce)) { "Invalid export identifier" }
        val extension = if (format == ExportImageFormat.PNG) "png" else "jpg"
        return "${safeNamePrefix(prefix)}_${stamp}_${nonce.lowercase()}.${extension}"
    }

    fun boundedQuality(requested: Int) = requested.coerceIn(60, 100)

    /** 返回满足目标最长边的尺寸；原尺寸小于目标时不做任何放大。 */
    fun boundedDimensions(width: Int, height: Int, maxLongEdge: Int): Pair<Int,Int> {
        require(width > 0 && height > 0)
        if (maxLongEdge <= 0 || max(width,height) <= maxLongEdge) return width to height
        val ratio = maxLongEdge.toDouble() / max(width,height).toDouble()
        return (width*ratio).roundToInt().coerceAtLeast(1) to
            (height*ratio).roundToInt().coerceAtLeast(1)
    }
}
