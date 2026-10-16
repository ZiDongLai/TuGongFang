package com.tugongfang.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tugongfang.editor.ui.design.TgfTokens

/**
 * DEV33: restrained cinematic glass, common to both themes.
 * No real blur or offscreen rendering: keeps scrolling panels responsive.
 * Preserve BoxScope and hit testing for existing controls.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    elevation: Dp = 14.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    backgroundOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .shadow(elevation.coerceAtMost(if (TgfTokens.isDark) 5.dp else 10.dp), shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (backgroundOverride != null) listOf(
                        backgroundOverride.copy(alpha = 1f),
                        backgroundOverride
                    ) else listOf(
                        TgfTokens.GlassTop,
                        TgfTokens.GlassTop.copy(alpha = if (TgfTokens.isDark) .97f else .94f),
                        TgfTokens.GlassBottom
                    )
                ),
                shape
            )
            .border(1.dp, if (backgroundOverride != null) Color(0xFF60738F) else TgfTokens.GlassBorder, shape)
            .padding(padding),
        content = content
    )
}
