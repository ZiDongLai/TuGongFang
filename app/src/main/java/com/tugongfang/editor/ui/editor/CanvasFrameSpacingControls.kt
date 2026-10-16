package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
import com.tugongfang.editor.ui.design.TgfTokens
import kotlin.math.roundToInt

/** DEV42 secondary controls. Background, borders and spacing are independently persisted. */
@Composable
internal fun FrameStyleControls(frame: CanvasFrame, onChange: (CanvasFrame) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallChoice("关闭", !frame.enabled) { onChange(frame.copy(enabled=false)) }
            SmallChoice("启用", frame.enabled) { onChange(frame.copy(enabled=true)) }
            Text("作用对象",fontSize=10.sp,color=TgfTokens.InkSecondary)
            SmallChoice("照片", frame.scope == CanvasFrameScope.Photos) {
                onChange(frame.copy(enabled=true,scope=CanvasFrameScope.Photos))
            }
            SmallChoice("画布", frame.scope == CanvasFrameScope.Canvas) {
                onChange(frame.copy(enabled=true,scope=CanvasFrameScope.Canvas))
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("边角",fontSize=11.sp,color=TgfTokens.InkSecondary)
            SmallChoice("圆角",frame.corner==CanvasFrameCorner.Rounded) {
                onChange(frame.copy(enabled=true,corner=CanvasFrameCorner.Rounded))
            }
            SmallChoice("直角",frame.corner==CanvasFrameCorner.Square) {
                onChange(frame.copy(enabled=true,corner=CanvasFrameCorner.Square))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("粗细",fontSize=11.sp,color=TgfTokens.InkSecondary)
            Slider(value=frame.widthRatio.coerceIn(.001f,.03f), onValueChange={
                onChange(frame.copy(enabled=true,widthRatio=it))
            },valueRange=.001f.. .03f,modifier=Modifier.weight(1f))
            Text("${(frame.widthRatio*1000).roundToInt()/10f}%",fontSize=10.sp,
                color=TgfTokens.Ink,modifier=Modifier.width(36.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Text("边框颜色 #",fontSize=11.sp,color=TgfTokens.InkSecondary)
            HexColorField(frame.colorArgb) { onChange(frame.copy(enabled=true,colorArgb=it)) }
        }
        CustomColorPickerButton(frame.colorArgb) { onChange(frame.copy(enabled=true,colorArgb=it)) }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(0xFFFFFFFF.toInt(),0xFF16233B.toInt(),0xFF397CE0.toInt(),
                0xFFD65D91.toInt(),0xFFF0C65E.toInt(),0xFF71C4A4.toInt(),
                0xFF8C74DD.toInt()).forEach { color ->
                Box(Modifier.size(30.dp).background(Color(color),RoundedCornerShape(8.dp))
                    .border(if(frame.colorArgb==color) 2.dp else 1.dp,
                        if(frame.colorArgb==color) TgfTokens.Blue else TgfTokens.SurfaceOutline,
                        RoundedCornerShape(8.dp)).clickable { onChange(frame.copy(enabled=true,colorArgb=color)) })
            }
        }
    }
}

@Composable
internal fun SpacingControls(
    photoGap: Float?, outer: Float, onPhotoGap: ((Float)->Unit)? = null, onOuter: (Float)->Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (photoGap != null && onPhotoGap != null) {
            SpacingSlider("照片间距",photoGap,0f,.045f,onPhotoGap)
        }
        SpacingSlider("外侧留边",outer,0f,.20f,onOuter)
        Text("背景显示在照片间距与画布留边中；边框粗细单独设置。",
            fontSize=10.sp,color=TgfTokens.InkSecondary)
    }
}

@Composable
private fun SpacingSlider(name:String,value:Float,min:Float,max:Float,onChange:(Float)->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(name,fontSize=11.sp,color=TgfTokens.InkSecondary,modifier=Modifier.width(64.dp))
        Slider(value=value.coerceIn(min,max),valueRange=min..max,onValueChange=onChange,
            modifier=Modifier.weight(1f))
        Text("${(value*100).roundToInt()}%",fontSize=10.sp,color=TgfTokens.Ink,
            modifier=Modifier.width(36.dp))
    }
}

@Composable
internal fun SmallChoice(label:String,active:Boolean,onClick:()->Unit) {
    Text(label,fontSize=11.sp,
        color=if(active) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
        modifier=Modifier.background(if(active) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
            RoundedCornerShape(8.dp))
            .border(1.dp,if(active) TgfTokens.SelectionInk.copy(alpha=.55f)
                else TgfTokens.SurfaceOutline.copy(alpha=.40f),RoundedCornerShape(8.dp))
            .clickable(onClick=onClick).padding(horizontal=9.dp,vertical=7.dp))
}
