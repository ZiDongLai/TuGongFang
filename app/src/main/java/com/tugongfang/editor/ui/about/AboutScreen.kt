package com.tugongfang.editor.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tugongfang.editor.R
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.components.TgfBrandMark
import com.tugongfang.editor.ui.design.TgfTokens

/** Single-platform Android About page. All controls follow the current Light/Dark palette. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "未知" }
    }
    var showLicense by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf<SupportQr?>(null) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
        TgfTokens.BackgroundTop, TgfTokens.BackgroundMid, TgfTokens.BackgroundBottom
    )))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                        .background(TgfTokens.SurfaceRaised)
                        .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(15.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 33.sp, color = TgfTokens.Ink) }
                Spacer(Modifier.width(14.dp))
                Text("关于", fontSize = 22.sp, color = TgfTokens.Ink, fontWeight = FontWeight.Bold)
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TgfBrandMark(Modifier.size(58.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(AboutContent.APP_NAME, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TgfTokens.Ink)
                                Text("版本 $version", fontSize = 12.sp, color = TgfTokens.Purple)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(AboutContent.SUMMARY, fontSize = 13.sp, lineHeight = 21.sp, color = TgfTokens.InkSecondary)
                    }
                }
                AboutSection("主要功能") {
                    AboutContent.FEATURES.forEach { feature ->
                        Text("• $feature", fontSize = 13.sp, lineHeight = 22.sp, color = TgfTokens.Ink)
                    }
                }
                AboutSection("后续方向") {
                    Text(AboutContent.ROADMAP, fontSize = 13.sp, lineHeight = 21.sp, color = TgfTokens.InkSecondary)
                }
                AboutSection("项目与许可") {
                    Text("软件许可证：${AboutContent.LICENSE_NAME}", fontSize = 13.sp, color = TgfTokens.Ink)
                    Spacer(Modifier.height(5.dp))
                    Text(AboutContent.REPOSITORY_NOTE, fontSize = 12.sp, color = TgfTokens.InkSecondary)
                    Spacer(Modifier.height(6.dp))
                    AboutAction("查看许可证说明") { showLicense = true }
                }
                AboutSection("反馈交流") {
                    Text("QQ群：${AboutContent.QQ_GROUP_NAME}", fontSize = 13.sp, color = TgfTokens.Ink)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("群号：${AboutContent.QQ_GROUP_NUMBER}", fontSize = 13.sp, color = TgfTokens.Ink)
                        Spacer(Modifier.width(10.dp))
                        AboutAction("复制群号") {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QQ群号", AboutContent.QQ_GROUP_NUMBER))
                            Toast.makeText(context, "群号已复制", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(AboutContent.WECHAT_GROUP, fontSize = 12.sp, color = TgfTokens.InkSecondary)
                    Spacer(Modifier.height(9.dp))
                    Text(AboutContent.FEEDBACK_HINT, fontSize = 12.sp, lineHeight = 19.sp, color = TgfTokens.InkSecondary)
                }
                AboutSection("支持开发") {
                    Text(AboutContent.SUPPORT_NOTE, fontSize = 12.sp, lineHeight = 20.sp, color = TgfTokens.InkSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            AboutAction("微信收款码") { showQr = SupportQr.WECHAT }
                        }
                        Box(Modifier.weight(1f)) {
                            AboutAction("支付宝收款码") { showQr = SupportQr.ALIPAY }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("图工坊 · 让每一张图片更有价值", modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, fontSize = 11.sp, color = TgfTokens.InkSecondary)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text("许可证说明") },
            text = { Text(AboutContent.LICENSE_DETAILS) },
            confirmButton = { TextButton(onClick = { showLicense = false }) { Text("关闭") } }
        )
    }
    showQr?.let { qr ->
        Dialog(onDismissRequest = { showQr = null }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(22.dp))
                    .background(TgfTokens.SurfaceRaised)
                    .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(qr.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = painterResource(qr.resource), contentDescription = qr.title,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))
                Text("完全自愿，不影响任何功能的正常使用", color = TgfTokens.InkSecondary,
                    fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                AboutAction("关闭") { showQr = null }
            }
        }
    }
}

private enum class SupportQr(val title: String, val resource: Int) {
    WECHAT("微信收款码", R.drawable.tgf_support_wechat),
    ALIPAY("支付宝收款码", R.drawable.tgf_support_alipay)
}

@Composable
private fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 19.dp, elevation = 5.dp,
        padding = PaddingValues(17.dp)) {
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
            Spacer(Modifier.height(11.dp))
            content()
        }
    }
}

@Composable
private fun AboutAction(title: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(Brush.horizontalGradient(listOf(TgfTokens.ToolTop, TgfTokens.ToolBottom)))
            .border(1.dp, TgfTokens.ToolOutline, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 12.sp, color = TgfTokens.SelectionInk, fontWeight = FontWeight.Medium)
    }
}
