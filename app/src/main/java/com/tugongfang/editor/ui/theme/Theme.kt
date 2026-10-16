package com.tugongfang.editor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.tugongfang.editor.ui.design.TgfTokens

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E66D4), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFF), onPrimaryContainer = Color(0xFF153774),
    secondary = Color(0xFF7950D7), onSecondary = Color.White,
    tertiary = Color(0xFF4AAFE8),
    background = Color(0xFFF8FAFF), onBackground = Color(0xFF162440),
    surface = Color(0xFFF1F5FF), onSurface = Color(0xFF162440),
    surfaceVariant = Color(0xFFE2EAFA), onSurfaceVariant = Color(0xFF516383),
    outline = Color(0xFF889AB9)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF85B7FF), onPrimary = Color(0xFF092453),
    primaryContainer = Color(0xFF263F77), onPrimaryContainer = Color(0xFFDBE9FF),
    secondary = Color(0xFFB18BFF), onSecondary = Color(0xFF26114E),
    tertiary = Color(0xFF6AC9F7),
    background = Color(0xFF0B1023), onBackground = Color(0xFFF4F6FF),
    surface = Color(0xFF1C2545), onSurface = Color(0xFFF4F6FF),
    surfaceVariant = Color(0xFF293354), onSurfaceVariant = Color(0xFFB4C3DD),
    outline = Color(0xFF7182AA)
)

@Composable
fun TuGongFangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Apply after composition; state reads in existing panels are automatically invalidated.
    SideEffect { TgfTokens.setDarkMode(darkTheme) }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}
