package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import com.tugongfang.editor.editor.image.CanvasBackgroundRenderer
import com.tugongfang.editor.model.canvas.BackgroundGradientDirection
import com.tugongfang.editor.model.canvas.BackgroundImageFit
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.ui.design.TgfTokens

/** Same Android Canvas renderer for preview/export. Grid is an editing-only transparency guide. */
@Composable
internal fun CanvasBackgroundSurface(background: CanvasBackground, showTransparencyGrid: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        if (showTransparencyGrid && (background.kind == CanvasBackgroundKind.Transparent ||
                    (background.kind == CanvasBackgroundKind.Image && background.opacity < 1f) ||
                    (background.kind == CanvasBackgroundKind.Image && background.imageFit == BackgroundImageFit.Fit))) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Color(0xFFF6F8FC))
                val side=16.dp.toPx().coerceAtLeast(1f)
                val cols=(size.width/side).toInt()+1; val rows=(size.height/side).toInt()+1
                for(y in 0 until rows) for(x in 0 until cols) if((x+y)%2==1)
                    drawRect(Color(0xFFE3EAF3),topLeft=Offset(x*side,y*side),
                        size=androidx.compose.ui.geometry.Size(side,side))
            }
        } else if(background.kind == CanvasBackgroundKind.Transparent) {
            Box(Modifier.matchParentSize().background(TgfTokens.Canvas))
        }
        val context=LocalContext.current
        val density=LocalDensity.current
        val widthPx=with(density){maxWidth.roundToPx()}.coerceAtLeast(1).coerceAtMost(900)
        val heightPx=with(density){maxHeight.roundToPx()}.coerceAtLeast(1).coerceAtMost(900)
        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,background,widthPx,heightPx) {
            value=if(background.kind == CanvasBackgroundKind.Transparent) null else
                withContext(Dispatchers.IO) {
                    runCatching {
                        val b=Bitmap.createBitmap(widthPx,heightPx,Bitmap.Config.ARGB_8888)
                        CanvasBackgroundRenderer.draw(context,android.graphics.Canvas(b),background,widthPx,heightPx)
                        b.asImageBitmap()
                    }.getOrNull()
                }
        }
        bitmap?.let { Image(it,contentDescription=null,modifier=Modifier.matchParentSize(),
            contentScale=ContentScale.FillBounds) }
    }
}

/** 与“图片 / 拼图”原面板共享，不新建一级工具，不增大面板。 */
@Composable
internal fun CanvasBackgroundControls(value: CanvasBackground, onChange: (CanvasBackground) -> Unit, onPickImage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val variants = listOf(
                CanvasBackgroundKind.Transparent to "透明", CanvasBackgroundKind.Solid to "纯色",
                CanvasBackgroundKind.Gradient to "渐变", CanvasBackgroundKind.Dots to "圆点",
                CanvasBackgroundKind.Grid to "网格", CanvasBackgroundKind.Paper to "纸纹",
                CanvasBackgroundKind.Image to "图片"
            )
            variants.forEach { (type, name) ->
                Text(name, fontSize=11.sp, color=if (value.kind==type) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                    modifier=Modifier.background(if (value.kind==type) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
                        RoundedCornerShape(9.dp))
                        .border(1.dp, if (value.kind==type) TgfTokens.SelectionInk.copy(alpha=.58f)
                            else TgfTokens.SurfaceOutline.copy(alpha=.40f), RoundedCornerShape(9.dp)).clickable { onChange(value.copy(kind=type)) }
                        .padding(horizontal=12.dp,vertical=7.dp))
            }
        }
        if(value.kind == CanvasBackgroundKind.Image) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(value.imageUri.isNullOrBlank()) "＋ 相册选背景" else "更换背景图片",
                    fontSize=11.sp,color=TgfTokens.Blue,
                    modifier=Modifier.background(TgfTokens.SurfaceSelected,RoundedCornerShape(8.dp))
                        .clickable(onClick=onPickImage).padding(horizontal=12.dp,vertical=7.dp))
                listOf(BackgroundImageFit.Fill to "填充",BackgroundImageFit.Fit to "适应").forEach { (fit,label) ->
                    Text(label,fontSize=10.sp,color=if(value.imageFit==fit) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                        modifier=Modifier.background(if(value.imageFit==fit) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,RoundedCornerShape(7.dp))
                            .border(1.dp, if(value.imageFit==fit) TgfTokens.SelectionInk.copy(alpha=.55f)
                                else TgfTokens.SurfaceOutline.copy(alpha=.35f), RoundedCornerShape(7.dp))
                            .clickable { onChange(value.copy(imageFit=fit)) }.padding(horizontal=8.dp,vertical=7.dp))
                }
            }
            listOf(Triple("模糊",value.blur.toFloat(),100f),
                Triple("透明度",value.opacity.coerceIn(0f,1f)*100f,100f)).forEach { (title,current,maxValue) ->
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(title,fontSize=10.sp,color=TgfTokens.InkSecondary,modifier=Modifier.width(38.dp))
                    androidx.compose.material3.Slider(value=current,onValueChange={ slider ->
                        if(title=="模糊") onChange(value.copy(blur=slider.toInt()))
                        else onChange(value.copy(opacity=slider/100f))
                    },valueRange=0f..maxValue,modifier=Modifier.weight(1f))
                    Text("${current.toInt()}%",fontSize=10.sp,color=TgfTokens.InkSecondary)
                }
            }
        } else {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("底色 #", fontSize=10.sp, color=TgfTokens.InkSecondary)
            HexColorField(value.firstColor) { argb ->
                onChange(value.copy(kind=if(value.kind==CanvasBackgroundKind.Transparent) CanvasBackgroundKind.Solid else value.kind,
                    firstColor=argb))
            }
        }
        CustomColorPickerButton(value.firstColor) { argb ->
            onChange(value.copy(kind=if(value.kind==CanvasBackgroundKind.Transparent) CanvasBackgroundKind.Solid else value.kind,
                firstColor=argb))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(0xFFFFFFFF.toInt(),0xFF161C32.toInt(),0xFFAFCEEF.toInt(),0xFFEFDDD9.toInt(),
                0xFFF7E1A7.toInt(),0xFFC8E8D7.toInt(),0xFFE8C9F7.toInt(),0xFF889DE4.toInt()).forEach { argb ->
                Box(Modifier.size(27.dp).background(Color(argb),RoundedCornerShape(8.dp))
                    .border(if(value.firstColor==argb) 2.dp else 1.dp,
                        (if(value.firstColor==argb) TgfTokens.SelectionInk else TgfTokens.SurfaceOutline).copy(alpha=if(value.firstColor==argb) 1f else .65f),RoundedCornerShape(8.dp))
                    .clickable { onChange(value.copy(kind=if(value.kind==CanvasBackgroundKind.Transparent) CanvasBackgroundKind.Solid else value.kind,firstColor=argb)) })
            }
        }
        if (value.kind == CanvasBackgroundKind.Gradient || value.kind == CanvasBackgroundKind.Dots ||
            value.kind == CanvasBackgroundKind.Grid || value.kind == CanvasBackgroundKind.Paper) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("辅助色 #",fontSize=10.sp,color=TgfTokens.InkSecondary)
                HexColorField(value.secondColor) { argb -> onChange(value.copy(secondColor=argb)) }
            }
            CustomColorPickerButton(value.secondColor) { argb -> onChange(value.copy(secondColor=argb)) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(0xFFACC8F4.toInt(),0xFF8967E5.toInt(),0xFFECA5BF.toInt(),0xFFFFFFFF.toInt(),0xFF354C76.toInt()).forEach { argb ->
                    Box(Modifier.size(24.dp).background(Color(argb),RoundedCornerShape(7.dp))
                        .border(if(value.secondColor==argb) 2.dp else 1.dp,
                            if(value.secondColor==argb) TgfTokens.SelectionInk else TgfTokens.SurfaceOutline,
                            RoundedCornerShape(7.dp))
                        .clickable { onChange(value.copy(secondColor=argb)) })
                }
            }
        }
        if (value.kind == CanvasBackgroundKind.Gradient) {
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                listOf(BackgroundGradientDirection.Horizontal to "横向",BackgroundGradientDirection.Vertical to "纵向",
                    BackgroundGradientDirection.Diagonal to "对角").forEach { (direction,title) ->
                    Text(title,fontSize=10.sp,color=if(value.gradientDirection==direction) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                        modifier=Modifier.background(if(value.gradientDirection==direction) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
                            RoundedCornerShape(7.dp))
                            .border(1.dp, if(value.gradientDirection==direction) TgfTokens.SelectionInk.copy(alpha=.55f)
                                else TgfTokens.SurfaceOutline.copy(alpha=.35f), RoundedCornerShape(7.dp)).clickable { onChange(value.copy(gradientDirection=direction)) }
                            .padding(horizontal=10.dp,vertical=5.dp))
                }
            }
        }
        }
    }
}

/** 可输入自定义 6 位 RGB 十六进制；无需额外放大面板。 */
@Composable
internal fun HexColorField(argb: Int, onChange: (Int) -> Unit) {
    var hex by remember(argb) { mutableStateOf("%06X".format(argb and 0xFFFFFF)) }
    BasicTextField(
        value=hex,
        onValueChange={ typed ->
            val clean=typed.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
            hex=clean
            if(clean.length==6) clean.toLongOrNull(16)?.let { onChange(0xFF000000.toInt() or it.toInt()) }
        },
        singleLine=true,
        textStyle=TextStyle(fontSize=11.sp,color=TgfTokens.Ink),
        modifier=Modifier.width(82.dp)
            .background(TgfTokens.SurfaceRaised,RoundedCornerShape(7.dp))
            .padding(horizontal=7.dp,vertical=5.dp)
    )
}
