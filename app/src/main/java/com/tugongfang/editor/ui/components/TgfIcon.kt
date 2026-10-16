package com.tugongfang.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tugongfang.editor.ui.design.TgfTokens

enum class TgfIconType {
    Photo, AddPhoto, Crop, Collage, Text, Shapes, Doodle, Mosaic, Symbol, Layers, More, Camera, Blank,
    Home, Template, Projects, Profile, Aspect,
    Reset, FlipHorizontal, FlipVertical, Duplicate, Lock, Unlock, Replace, Delete, RotateLeft, RotateRight, Style, Background
}

@Composable
fun TgfIcon(
    type: TgfIconType,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    bare: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = if (selected) {
        Brush.linearGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple))
    } else {
        Brush.verticalGradient(listOf(TgfTokens.IconTop, TgfTokens.IconBottom, TgfTokens.ToolSelectedBottom))
    }
    val glyph = if (bare) {
        if (selected) TgfTokens.Blue else if (TgfTokens.isDark) Color(0xFFC4D4FC) else Color(0xFF627DAB)
    } else if (selected) Color.White else if (TgfTokens.isDark) Color(0xFFE0E9FF) else Color(0xFF285EBB)

    Box(
        modifier = if (bare) modifier else modifier
            .size(48.dp)
            .shadow(if (selected) 5.dp else 3.dp, shape, clip = false)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, if (selected) TgfTokens.ToolSelectedBorder else TgfTokens.GlassBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        // A subtle rounded highlight, not a rectangular Canvas overlay.
        if (!bare) {
            Box(Modifier.align(Alignment.TopCenter).width(25.dp).height(2.dp)
                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                .background(Color.White.copy(alpha = if (selected) .45f else if (TgfTokens.isDark) .18f else .65f)))
        }

        Canvas(Modifier.size(if (bare) 22.dp else 25.dp)) {
            val w = size.width
            val h = size.height
            val s = 1.95.dp.toPx()
            val stroke = Stroke(width = s, cap = StrokeCap.Round)

            when (type) {
                TgfIconType.Photo, TgfIconType.AddPhoto -> {
                    drawRoundRect(glyph, Offset(w * .10f, h * .15f), Size(w * .80f, h * .70f), CornerRadius(w * .12f), style = stroke)
                    drawCircle(glyph, radius = w * .07f, center = Offset(w * .70f, h * .34f))
                    val p = Path().apply {
                        moveTo(w * .20f, h * .70f); lineTo(w * .42f, h * .49f); lineTo(w * .55f, h * .61f)
                        lineTo(w * .67f, h * .50f); lineTo(w * .82f, h * .69f)
                    }
                    drawPath(p, glyph, style = stroke)
                    if (type == TgfIconType.AddPhoto) {
                        drawCircle(Color.White, radius = w * .18f, center = Offset(w * .78f, h * .20f))
                        drawCircle(TgfTokens.Blue, radius = w * .18f, center = Offset(w * .78f, h * .20f), style = Stroke(width = s * .75f))
                        drawLine(TgfTokens.Blue, Offset(w * .78f, h * .11f), Offset(w * .78f, h * .29f), s * .78f, StrokeCap.Round)
                        drawLine(TgfTokens.Blue, Offset(w * .69f, h * .20f), Offset(w * .87f, h * .20f), s * .78f, StrokeCap.Round)
                    }
                }
                TgfIconType.Crop -> {
                    drawLine(glyph, Offset(w * .18f, h * .10f), Offset(w * .18f, h * .72f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .18f, h * .72f), Offset(w * .80f, h * .72f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .38f, h * .28f), Offset(w * .86f, h * .28f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .38f, h * .28f), Offset(w * .38f, h * .90f), s, StrokeCap.Round)
                }
                TgfIconType.Collage -> {
                    val cr = CornerRadius(w * .07f)
                    drawRoundRect(glyph, Offset(w * .10f, h * .12f), Size(w * .34f, h * .76f), cr, style = stroke)
                    drawRoundRect(glyph, Offset(w * .55f, h * .12f), Size(w * .35f, h * .34f), cr, style = stroke)
                    drawRoundRect(glyph, Offset(w * .55f, h * .56f), Size(w * .35f, h * .32f), cr, style = stroke)
                }
                TgfIconType.Text -> {
                    drawLine(glyph, Offset(w * .18f, h * .20f), Offset(w * .82f, h * .20f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .50f, h * .20f), Offset(w * .50f, h * .82f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .32f, h * .82f), Offset(w * .68f, h * .82f), s, StrokeCap.Round)
                }
                TgfIconType.Shapes -> {
                    drawCircle(glyph, radius = w * .18f, center = Offset(w * .33f, h * .35f), style = stroke)
                    drawRoundRect(glyph, Offset(w * .48f, h * .47f), Size(w * .36f, h * .34f), CornerRadius(w * .06f), style = stroke)
                }
                TgfIconType.Doodle -> {
                    // 专用涂鸦：笔尖与连续手绘线，区别于几何图形工具。
                    drawLine(glyph, Offset(w * .28f, h * .78f), Offset(w * .72f, h * .23f), s * 1.35f, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .65f, h * .16f), Offset(w * .79f, h * .29f), s, StrokeCap.Round)
                    val trail = Path().apply {
                        moveTo(w * .14f, h * .80f)
                        cubicTo(w * .24f, h * .57f, w * .35f, h * .98f, w * .49f, h * .75f)
                        cubicTo(w * .57f, h * .63f, w * .72f, h * .82f, w * .87f, h * .66f)
                    }
                    drawPath(trail, glyph, style = stroke)
                }
                TgfIconType.Mosaic -> {
                    val cell=w*.19f
                    for (row in 0..2) for (col in 0..2) {
                        val t=Offset(w*(.16f+col*.23f),h*(.16f+row*.23f))
                        drawRoundRect(if ((row+col)%2==0) glyph else TgfTokens.Blue,
                            t, Size(cell,cell), CornerRadius(w*.025f))
                    }
                }
                TgfIconType.Symbol -> {
                    val p = Path().apply {
                        moveTo(w * .50f, h * .10f); lineTo(w * .59f, h * .39f); lineTo(w * .88f, h * .50f)
                        lineTo(w * .59f, h * .61f); lineTo(w * .50f, h * .90f); lineTo(w * .41f, h * .61f)
                        lineTo(w * .12f, h * .50f); lineTo(w * .41f, h * .39f); close()
                    }
                    drawPath(p, glyph, style = stroke)
                }
                TgfIconType.Layers -> {
                    repeat(3) { i ->
                        val dy = i * h * .17f
                        drawRoundRect(glyph, Offset(w * .18f, h * .13f + dy), Size(w * .64f, h * .34f), CornerRadius(w * .08f), style = stroke)
                    }
                }
                TgfIconType.More -> {
                    drawCircle(glyph, w * .065f, Offset(w * .24f, h * .50f)); drawCircle(glyph, w * .065f, Offset(w * .50f, h * .50f)); drawCircle(glyph, w * .065f, Offset(w * .76f, h * .50f))
                }
                TgfIconType.Camera -> {
                    drawRoundRect(glyph, Offset(w * .12f, h * .28f), Size(w * .76f, h * .52f), CornerRadius(w * .10f), style = stroke)
                    drawCircle(glyph, radius = w * .17f, center = Offset(w * .50f, h * .54f), style = stroke)
                    drawLine(glyph, Offset(w * .33f, h * .28f), Offset(w * .40f, h * .17f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .40f, h * .17f), Offset(w * .60f, h * .17f), s, StrokeCap.Round)
                }
                TgfIconType.Blank -> {
                    drawRoundRect(glyph, Offset(w * .14f, h * .14f), Size(w * .72f, h * .72f), CornerRadius(w * .10f), style = stroke)
                    drawLine(glyph, Offset(w * .50f, h * .32f), Offset(w * .50f, h * .68f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .32f, h * .50f), Offset(w * .68f, h * .50f), s, StrokeCap.Round)
                }
                TgfIconType.Home -> {
                    val p = Path().apply {
                        moveTo(w * .16f, h * .48f); lineTo(w * .50f, h * .20f); lineTo(w * .84f, h * .48f)
                        lineTo(w * .78f, h * .48f); lineTo(w * .78f, h * .82f); lineTo(w * .58f, h * .82f)
                        lineTo(w * .58f, h * .62f); lineTo(w * .42f, h * .62f); lineTo(w * .42f, h * .82f)
                        lineTo(w * .22f, h * .82f); lineTo(w * .22f, h * .48f); close()
                    }
                    drawPath(p, glyph, style = stroke)
                }
                TgfIconType.Template -> {
                    drawRoundRect(glyph, Offset(w * .14f, h * .14f), Size(w * .72f, h * .72f), CornerRadius(w * .10f), style = stroke)
                    drawLine(glyph, Offset(w * .50f, h * .15f), Offset(w * .50f, h * .85f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .15f, h * .50f), Offset(w * .85f, h * .50f), s, StrokeCap.Round)
                }
                TgfIconType.Projects -> {
                    val p = Path().apply {
                        moveTo(w * .13f, h * .31f); lineTo(w * .39f, h * .31f); lineTo(w * .47f, h * .40f)
                        lineTo(w * .87f, h * .40f); lineTo(w * .81f, h * .78f); lineTo(w * .19f, h * .78f); close()
                    }
                    drawPath(p, glyph, style = stroke)
                }
                TgfIconType.Profile -> {
                    drawCircle(glyph, radius = w * .16f, center = Offset(w * .50f, h * .34f), style = stroke)
                    val p = Path().apply {
                        moveTo(w * .22f, h * .82f)
                        cubicTo(w * .25f, h * .60f, w * .75f, h * .60f, w * .78f, h * .82f)
                    }
                    drawPath(p, glyph, style = stroke)
                }
                TgfIconType.Aspect -> {
                    drawRoundRect(glyph, Offset(w*.12f,h*.20f),Size(w*.68f,h*.48f),
                        CornerRadius(w*.05f),style=stroke)
                    drawLine(glyph,Offset(w*.26f,h*.78f),Offset(w*.85f,h*.78f),s,StrokeCap.Round)
                    drawLine(glyph,Offset(w*.85f,h*.78f),Offset(w*.85f,h*.34f),s,StrokeCap.Round)
                }
                TgfIconType.Background -> {
                    drawRoundRect(glyph, Offset(w * .13f, h * .17f), Size(w * .74f, h * .66f),
                        CornerRadius(w * .13f), style = stroke)
                    drawLine(glyph, Offset(w * .18f, h * .63f), Offset(w * .82f, h * .63f),
                        s * .7f, StrokeCap.Round)
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .34f, h * .41f))
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .55f, h * .41f))
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .75f, h * .41f))
                }
                TgfIconType.Style -> {
                    drawLine(glyph, Offset(w * .18f, h * .28f), Offset(w * .82f, h * .28f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .18f, h * .50f), Offset(w * .82f, h * .50f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .18f, h * .72f), Offset(w * .82f, h * .72f), s, StrokeCap.Round)
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .38f, h * .28f))
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .66f, h * .50f))
                    drawCircle(glyph, radius = w * .075f, center = Offset(w * .46f, h * .72f))
                }
                TgfIconType.Reset -> {
                    drawArc(glyph, 35f, 285f, false, Offset(w * .16f, h * .16f), Size(w * .68f, h * .68f), style = stroke)
                    val p = Path().apply {
                        moveTo(w * .17f, h * .33f); lineTo(w * .16f, h * .16f); lineTo(w * .34f, h * .18f)
                    }
                    drawPath(p, glyph, style = stroke)
                }
                TgfIconType.FlipHorizontal -> {
                    drawLine(glyph, Offset(w * .50f, h * .12f), Offset(w * .50f, h * .88f), s * .75f, StrokeCap.Round)
                    val l = Path().apply { moveTo(w * .42f, h * .26f); lineTo(w * .18f, h * .50f); lineTo(w * .42f, h * .74f); close() }
                    val r = Path().apply { moveTo(w * .58f, h * .26f); lineTo(w * .82f, h * .50f); lineTo(w * .58f, h * .74f); close() }
                    drawPath(l, glyph, style = stroke); drawPath(r, glyph, style = stroke)
                }
                TgfIconType.FlipVertical -> {
                    drawLine(glyph, Offset(w * .12f, h * .50f), Offset(w * .88f, h * .50f), s * .75f, StrokeCap.Round)
                    val t = Path().apply { moveTo(w * .26f, h * .42f); lineTo(w * .50f, h * .18f); lineTo(w * .74f, h * .42f); close() }
                    val b = Path().apply { moveTo(w * .26f, h * .58f); lineTo(w * .50f, h * .82f); lineTo(w * .74f, h * .58f); close() }
                    drawPath(t, glyph, style = stroke); drawPath(b, glyph, style = stroke)
                }
                TgfIconType.Duplicate -> {
                    drawRoundRect(glyph, Offset(w * .16f, h * .14f), Size(w * .52f, h * .56f), CornerRadius(w * .08f), style = stroke)
                    drawRoundRect(glyph, Offset(w * .32f, h * .30f), Size(w * .52f, h * .56f), CornerRadius(w * .08f), style = stroke)
                }
                TgfIconType.Lock, TgfIconType.Unlock -> {
                    drawRoundRect(glyph, Offset(w * .22f, h * .44f), Size(w * .56f, h * .40f), CornerRadius(w * .08f), style = stroke)
                    val p = Path().apply {
                        moveTo(if (type == TgfIconType.Unlock) w * .40f else w * .32f, h * .44f)
                        cubicTo(w * .32f, h * .18f, w * .68f, h * .18f, w * .68f, h * .44f)
                    }
                    drawPath(p, glyph, style = stroke)
                    drawCircle(glyph, radius = w * .045f, center = Offset(w * .50f, h * .62f))
                }
                TgfIconType.Replace -> {
                    drawArc(glyph, 200f, 220f, false, Offset(w * .15f, h * .15f), Size(w * .70f, h * .70f), style = stroke)
                    val p1 = Path().apply { moveTo(w * .73f, h * .16f); lineTo(w * .86f, h * .31f); lineTo(w * .66f, h * .34f) }
                    val p2 = Path().apply { moveTo(w * .27f, h * .84f); lineTo(w * .14f, h * .69f); lineTo(w * .34f, h * .66f) }
                    drawPath(p1, glyph, style = stroke); drawPath(p2, glyph, style = stroke)
                }
                TgfIconType.Delete -> {
                    drawRoundRect(glyph, Offset(w * .27f, h * .30f), Size(w * .46f, h * .54f), CornerRadius(w * .06f), style = stroke)
                    drawLine(glyph, Offset(w * .22f, h * .24f), Offset(w * .78f, h * .24f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .39f, h * .16f), Offset(w * .61f, h * .16f), s, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .43f, h * .42f), Offset(w * .43f, h * .71f), s * .75f, StrokeCap.Round)
                    drawLine(glyph, Offset(w * .57f, h * .42f), Offset(w * .57f, h * .71f), s * .75f, StrokeCap.Round)
                }
                TgfIconType.RotateLeft, TgfIconType.RotateRight -> {
                    val start = if (type == TgfIconType.RotateLeft) 25f else 155f
                    drawArc(glyph, start, 285f, false, Offset(w * .16f, h * .16f), Size(w * .68f, h * .68f), style = stroke)
                    val p = if (type == TgfIconType.RotateLeft) {
                        Path().apply { moveTo(w * .18f, h * .38f); lineTo(w * .15f, h * .18f); lineTo(w * .35f, h * .22f) }
                    } else {
                        Path().apply { moveTo(w * .82f, h * .38f); lineTo(w * .85f, h * .18f); lineTo(w * .65f, h * .22f) }
                    }
                    drawPath(p, glyph, style = stroke)
                }
            }
        }
    }
}
