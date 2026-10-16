package com.tugongfang.editor.ui.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.editor.image.rememberImagePreview
import com.tugongfang.editor.ui.design.TgfTokens

/** Shown only after ProjectExporter returned a saved gallery URI. */
@Composable
fun ExportResultDialog(
    savedUri: String,
    onView: () -> Unit,
    onShare: () -> Unit,
    onContinue: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val image by rememberImagePreview(context, savedUri, maxDimension = 680)
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text("✓ 导出成功", color = TgfTokens.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("图片已保存到相册，可查看、分享或继续编辑。",
                    fontSize = 12.sp, color = TgfTokens.InkSecondary)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(220.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(TgfTokens.Canvas)
                    .border(1.dp, TgfTokens.SurfaceOutline, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center) {
                    if (image != null) {
                        Image(bitmap = image!!, contentDescription = "导出作品缩略图",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else Text("作品已保存", fontSize = 13.sp, color = TgfTokens.InkSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) { Text("▣ 查看", fontSize = 12.sp) }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("↗ 分享", fontSize = 12.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = TgfTokens.ActionPurple)) {
                Text("继续编辑", color = Color.White)
            }
        },
        dismissButton = { OutlinedButton(onClick = onHome) { Text("返回首页") } },
        containerColor = TgfTokens.GlassTop
    )
}
