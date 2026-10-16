package com.tugongfang.editor.ui.export

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tugongfang.editor.editor.export.ExportImageFormat
import com.tugongfang.editor.editor.export.ExportOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.design.TgfTokens

/**
 * 使用与正式导出完全相同的排版引擎生成预览。
 * 因此这里看到的照片/自由文字/顶部或底部信息栏边界，就是最终输出结构。
 */
@Composable
fun ExportPreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (ExportOptions) -> Unit,
    initialFormat: ExportImageFormat = ExportImageFormat.JPG,
    defaultFileName: String = "图工坊"
) {
    var format by remember { mutableStateOf(initialFormat) }
    var maxEdge by remember { mutableStateOf(0) }
    var jpgQuality by remember { mutableStateOf(95) }
    var fileName by remember(defaultFileName) { mutableStateOf(defaultFileName) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (TgfTokens.isDark) Color(0xD80B1023) else Color(0x880E1424))
                .padding(horizontal = 18.dp, vertical = 28.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxSize(),
                radius = 28.dp,
                elevation = 18.dp,
                padding = PaddingValues(14.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                "导出预览",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TgfTokens.Ink
                            )
                            Text(
                                "最终输出边界 · 同版式渲染",
                                fontSize = 10.sp,
                                color = TgfTokens.InkSecondary
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "×",
                            fontSize = 25.sp,
                            color = TgfTokens.InkSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(TgfTokens.Canvas, RoundedCornerShape(22.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "最终导出预览",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(ExportImageFormat.PNG to "PNG · 支持透明", ExportImageFormat.JPG to "JPG · 空白变白").forEach { (choice,label) ->
                            Text(label, fontSize=10.sp, color=if(format==choice) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                                modifier=Modifier.background(if(format==choice) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
                                    RoundedCornerShape(9.dp))
                                    .border(1.dp, if(format==choice) TgfTokens.SelectionInk.copy(alpha=.55f)
                                        else TgfTokens.SurfaceOutline.copy(alpha=.40f), RoundedCornerShape(9.dp)).clickable {format=choice}
                                    .padding(horizontal=10.dp,vertical=7.dp))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("清晰度", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                        Spacer(Modifier.width(8.dp))
                        listOf(0 to "默认高清", 2048 to "≤2048", 1200 to "≤1200").forEach { (edge, label) ->
                            Text(label, fontSize = 10.sp,
                                color = if (maxEdge == edge) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                                modifier = Modifier
                                    .padding(end = 5.dp)
                                    .background(if (maxEdge == edge) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
                                        RoundedCornerShape(9.dp))
                                    .border(1.dp, if(maxEdge==edge) TgfTokens.SelectionInk.copy(alpha=.55f)
                                        else TgfTokens.SurfaceOutline.copy(alpha=.35f), RoundedCornerShape(9.dp))
                                    .clickable { maxEdge = edge }
                                    .padding(horizontal = 7.dp, vertical = 8.dp))
                        }
                    }
                    if (format == ExportImageFormat.JPG) {
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("JPG 质量", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                            Spacer(Modifier.width(8.dp))
                            listOf(85, 95, 100).forEach { q ->
                                Text("$q", fontSize = 10.sp,
                                    color = if (jpgQuality == q) TgfTokens.SelectionInk else TgfTokens.InkSecondary,
                                    modifier = Modifier
                                        .padding(end = 7.dp)
                                        .background(if (jpgQuality == q) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised,
                                            RoundedCornerShape(9.dp))
                                        .border(1.dp, if(jpgQuality==q) TgfTokens.SelectionInk.copy(alpha=.55f)
                                            else TgfTokens.SurfaceOutline.copy(alpha=.35f), RoundedCornerShape(9.dp))
                                        .clickable { jpgQuality = q }
                                        .padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it.take(40) },
                        singleLine = true,
                        label = { Text("导出文件名前缀", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TgfTokens.Ink)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "文件名自动附加时间；缩小尺寸不改变画布布局。PNG保留透明，JPG空白填白。",
                        fontSize = 10.sp, lineHeight = 14.sp, color = TgfTokens.InkSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth()) {
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(onClick = onDismiss),
                            radius = 16.dp,
                            elevation = 5.dp,
                            padding = PaddingValues(0.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("返回编辑", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        GlassCard(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .clickable { onConfirm(ExportOptions(format, maxEdge, jpgQuality, fileName)) },
                            radius = 16.dp,
                            elevation = 7.dp,
                            padding = PaddingValues(0.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if(format==ExportImageFormat.PNG) "导出 PNG" else "导出 JPG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
