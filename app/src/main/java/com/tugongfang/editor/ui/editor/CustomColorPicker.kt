package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.ui.design.TgfTokens

/** DEV42 FIX2: text-style, inline HSV selector, with live updates to the canvas.
 * Reused by background primary/secondary colors and frame color.
 * The wheel itself shows the current color; no redundant preview strip or modal dialog.
 */
@Composable
internal fun CustomColorPickerButton(colorArgb: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val initial = remember { FloatArray(3).also {
        android.graphics.Color.colorToHSV(colorArgb or 0xFF000000.toInt(), it)
    } }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var brightness by remember { mutableFloatStateOf(initial[2]) }
    // Own color emissions must not reset the HSV sliders. External palette/HEX changes do resync.
    var lastEmitted by remember { mutableIntStateOf(colorArgb) }
    LaunchedEffect(colorArgb) {
        if (colorArgb != lastEmitted) {
            val external = FloatArray(3)
            android.graphics.Color.colorToHSV(colorArgb or 0xFF000000.toInt(), external)
            hue = external[0]
            saturation = external[1]
            brightness = external[2]
            lastEmitted = colorArgb
        }
    }
    fun emit(h: Float = hue, s: Float = saturation, v: Float = brightness) {
        val result = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        lastEmitted = result
        onSelected(result)
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SmallChoice("自定义颜色", expanded) { expanded = !expanded }
        if (expanded) {
            Box(
                Modifier.fillMaxWidth()
                    .background(TgfTokens.SurfaceRaised.copy(alpha = .78f), RoundedCornerShape(13.dp))
                    .border(1.dp, TgfTokens.SurfaceOutline, RoundedCornerShape(13.dp))
                    .padding(horizontal = 9.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorWheelPicker(hue, saturation, brightness) { newHue ->
                        hue = newHue
                        emit(h = newHue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("饱和度", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                        Slider(
                            value = saturation.coerceIn(0f, 1f),
                            onValueChange = { newSaturation ->
                                saturation = newSaturation
                                emit(s = newSaturation)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.height(34.dp)
                        )
                        Text("明度", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                        Slider(
                            value = brightness.coerceIn(0f, 1f),
                            onValueChange = { newBrightness ->
                                brightness = newBrightness
                                emit(v = newBrightness)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.height(34.dp)
                        )
                        Text("#%06X".format(colorArgb and 0xFFFFFF), fontSize = 10.sp,
                            color = TgfTokens.InkSecondary)
                    }
                }
            }
        }
    }
}
