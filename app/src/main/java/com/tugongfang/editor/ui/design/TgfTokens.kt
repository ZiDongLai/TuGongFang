package com.tugongfang.editor.ui.design

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * DEV33 visual refinement based on six Pixel 8 screenshots.
 * DEV32 cinematic light/dark palette. Shared with all editor panels; no document colors
 * or exported image pixels are altered by the application theme.
 */
object TgfTokens {
    private var dark by mutableStateOf(false)
    fun setDarkMode(enabled: Boolean) { if (dark != enabled) dark = enabled }

    val isDark: Boolean get() = dark
    val BackgroundTop get() = if (dark) Color(0xFF0B1023) else Color(0xFFF8FAFF)
    val BackgroundMid get() = if (dark) Color(0xFF111933) else Color(0xFFF0F2FD)
    val BackgroundBottom get() = if (dark) Color(0xFF21173B) else Color(0xFFE8E5FA)
    val Ink get() = if (dark) Color(0xFFF4F6FF) else Color(0xFF162440)
    val InkSecondary get() = if (dark) Color(0xFFB4C3DD) else Color(0xFF5E708E)
    val Blue get() = if (dark) Color(0xFF85B7FF) else Color(0xFF2E66D4)
    val SelectionInk get() = if (dark) Color(0xFFDFEBFF) else Color(0xFF184F9C)
    val Cyan get() = if (dark) Color(0xFF6AC9F7) else Color(0xFF4BB6ED)
    val Purple get() = if (dark) Color(0xFFB18BFF) else Color(0xFF7950D7)
    // White-label CTAs use deeper shades than readable link text in dark mode.
    // Using Blue/Purple here previously yielded insufficient white-on-pale contrast.
    val ActionBlue get() = if (dark) Color(0xFF275DCB) else Color(0xFF2E66D4)
    val ActionPurple get() = if (dark) Color(0xFF6546C3) else Color(0xFF7950D7)
    val Pink get() = if (dark) Color(0xFFDE9CFA) else Color(0xFFB56BF0)

    val GlassTop get() = if (dark) Color(0xF0253055) else Color(0xF9FFFFFF)
    val GlassBottom get() = if (dark) Color(0xEF181C3B) else Color(0xEDDCE6FA)
    val GlassBorder get() = if (dark) Color(0xA07988C2) else Color(0xFCFFFFFF)
    val GlassEdgeShade get() = if (dark) Color(0x70526DCE) else Color(0x454E79B5)
    val SurfaceBase get() = if (dark) Color(0xFF1C2545) else Color(0xFFE2EAFA)
    val SurfaceRaised get() = if (dark) Color(0xFF30385D) else Color(0xFFF0F3FD)
    val SurfaceSelected get() = if (dark) Color(0xFF394778) else Color(0xFFD2DAFB)
    val SurfaceOutline get() = if (dark) Color(0x997A8ABF) else Color(0xC9FFFFFF)
    val IconTop get() = if (dark) Color(0xFF3B4F8A) else Color(0xFFF0F4FF)
    val IconBottom get() = if (dark) Color(0xFF292D67) else Color(0xFFC9D7F6)
    // Shared DEV35 micro-elevation for *controls* (never image pixels):
    // pearl-white to icy blue in Light; slate-indigo to midnight in Dark.
    val ToolTop get() = if (dark) Color(0xFF303C66) else Color(0xFFFBFCFF)
    val ToolBottom get() = if (dark) Color(0xFF232C4F) else Color(0xFFE5EDFD)
    val ToolSelectedTop get() = if (dark) Color(0xFF455B92) else Color(0xFFE5EDFF)
    val ToolSelectedBottom get() = if (dark) Color(0xFF303E74) else Color(0xFFCDD9FB)
    val ToolOutline get() = if (dark) Color(0xFF5B6B9E) else Color(0xFFE3EAF9)
    val ToolSelectedBorder get() = if (dark) Color(0xFFBDD1FF) else Color(0xFF4368C4)
    val Canvas get() = if (dark) Color(0xFF222B42) else Color(0xFFD0DCF0)
    val CanvasInner get() = if (dark) Color(0xFF293550) else Color(0xFFE8EFFB)
}
