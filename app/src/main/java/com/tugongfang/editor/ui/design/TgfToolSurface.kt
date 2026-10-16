package com.tugongfang.editor.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** DEV35 shared blue-violet raised tool tile, across light/dark editor panels.
 * Draw-only modifier: preserves caller size, click semantics, enabled state and routing.
 * Never apply to actual photo or user-created canvas layers. */
fun Modifier.tgfToolSurface(selected: Boolean, shape: Shape): Modifier {
    val fill = if (selected) listOf(TgfTokens.ToolSelectedTop, TgfTokens.ToolSelectedBottom)
        else listOf(TgfTokens.ToolTop, TgfTokens.ToolBottom)
    return this.clip(shape)
        .background(Brush.verticalGradient(fill), shape)
        .border(if (selected) 1.25.dp else .7.dp,
            if (selected) TgfTokens.ToolSelectedBorder else TgfTokens.ToolOutline, shape)
}
