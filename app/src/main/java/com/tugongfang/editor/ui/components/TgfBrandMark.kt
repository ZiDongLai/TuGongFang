package com.tugongfang.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 图工坊冻结品牌结构：蓝紫折叠图框 + 白色照片窗 + 山形 + 金色太阳。
 * v0.6.5 仅提升渐变、轮廓细节与光泽；不更改符号识别结构。
 */
@Composable
fun TgfBrandMark(modifier: Modifier = Modifier) {
    val plate = RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(5.dp, plate, clip = false)
            .clip(plate)
            .background(
                Brush.linearGradient(listOf(Color(0xFFD8E9FF), Color(0xFFB9D0F6)))
            )
            .border(1.dp, Color.White.copy(alpha = .78f), plate),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val ribbon = Path().apply {
                moveTo(w * .18f, h * .24f)
                cubicTo(w * .18f, h * .15f, w * .25f, h * .12f, w * .35f, h * .12f)
                lineTo(w * .70f, h * .12f)
                cubicTo(w * .82f, h * .12f, w * .88f, h * .20f, w * .88f, h * .31f)
                lineTo(w * .88f, h * .55f)
                cubicTo(w * .88f, h * .67f, w * .81f, h * .74f, w * .69f, h * .74f)
                lineTo(w * .43f, h * .74f)
                lineTo(w * .43f, h * .87f)
                cubicTo(w * .43f, h * .93f, w * .37f, h * .95f, w * .32f, h * .91f)
                lineTo(w * .21f, h * .82f)
                cubicTo(w * .18f, h * .79f, w * .17f, h * .75f, w * .17f, h * .69f)
                close()
            }
            // 高饱和但不发荧光的蓝紫金属渐层。
            drawPath(ribbon, brush = Brush.linearGradient(
                listOf(Color(0xFF48C0F6), Color(0xFF287EF6), Color(0xFF4D61E6), Color(0xFF7954D4)),
                start = Offset(w * .12f, h * .10f), end = Offset(w * .88f, h * .88f)
            ))
            drawPath(ribbon, color = Color(0xFF2556DB).copy(alpha = .32f),
                style = Stroke(width = w * .012f))
            // 柔和的边缘镜面高光沿着形状走，不叠矩形底块。
            val sheen = Path().apply {
                moveTo(w * .23f, h * .37f)
                lineTo(w * .23f, h * .25f)
                cubicTo(w * .23f, h * .19f, w * .28f, h * .17f, w * .36f, h * .17f)
                lineTo(w * .67f, h * .17f)
            }
            drawPath(sheen, Color.White.copy(alpha = .58f),
                style = Stroke(width = w * .019f, cap = StrokeCap.Round))
            // 保留原白色照片窗口与圆角比例，内侧加淡蓝边缘和浅阴影。
            drawRoundRect(Color(0xFFCBDDFC).copy(alpha = .8f),
                topLeft = Offset(w * .338f, h * .288f), size = Size(w * .418f, h * .35f),
                cornerRadius = CornerRadius(w * .081f))
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFCFEFF), Color(0xFFE5F1FF))),
                topLeft = Offset(w * .35f, h * .29f), size = Size(w * .39f, h * .325f),
                cornerRadius = CornerRadius(w * .07f)
            )
            val mountain = Path().apply {
                moveTo(w * .40f, h * .55f)
                lineTo(w * .50f, h * .43f)
                lineTo(w * .57f, h * .50f)
                lineTo(w * .63f, h * .44f)
                lineTo(w * .70f, h * .55f)
                close()
            }
            drawPath(mountain, brush = Brush.linearGradient(
                listOf(Color(0xFF43B8FF), Color(0xFF4D67F4))
            ))
            drawCircle(
                brush = Brush.linearGradient(listOf(Color(0xFFFFDB76), Color(0xFFFFA83B))),
                radius = w * .038f, center = Offset(w * .658f, h * .357f)
            )
            // 折叠尾部的浅色边线凸显结构而非额外加形。
            val fold = Path().apply {
                moveTo(w * .43f, h * .73f)
                lineTo(w * .43f, h * .86f)
            }
            drawPath(fold, Color(0xFFC8DBFF).copy(alpha = .62f),
                style = Stroke(width = w * .012f, cap = StrokeCap.Round))
        }
    }
}
