package com.tugongfang.editor.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import com.tugongfang.editor.ui.about.AboutScreen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tugongfang.editor.ui.theme.ThemeMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.components.TgfBrandMark
import com.tugongfang.editor.ui.components.TgfIcon
import com.tugongfang.editor.ui.components.TgfIconType
import com.tugongfang.editor.ui.design.TgfTokens
import com.tugongfang.editor.project.ProjectCardInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v0.6.5 视觉校准：参照已批准的四屏设计，压缩信息密度、取消方块反光。
 * 不伪造“最近项目”，也不改变当前导航/相册事件的业务行为。
 */
@Composable
fun HomeScreen(
    onNewProject: () -> Unit,
    onImportPhoto: () -> Unit,
    onStartCollage: () -> Unit,
    onCamera: () -> Unit,
    recentProjects: List<ProjectCardInfo>,
    onOpenProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onExportProject: (String) -> Unit,
    onImportProject: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    var showAllProjects by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectCardInfo?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ProjectCardInfo?>(null) }
    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }
    Box(Modifier.fillMaxSize()) {
        CinematicBackdrop()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(15.dp))
                BrandHeader(themeMode, onThemeModeChange)
                Spacer(Modifier.height(22.dp))
                QuickActions(onImportPhoto, onStartCollage, onCamera)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(
                            TgfTokens.SurfaceRaised.copy(alpha = .94f),
                            TgfTokens.SurfaceSelected.copy(alpha = .73f)
                        )), RoundedCornerShape(50)
                    )
                    .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(50))
                    .clickable(onClick = onNewProject)
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("＋ 空白画布", fontSize = 12.sp, color = TgfTokens.SelectionInk,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(23.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("最近项目", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                    Spacer(Modifier.weight(1f))
                    Text("导入备份", modifier = Modifier.clickable(onClick = onImportProject),
                        fontSize = 12.sp, color = TgfTokens.Blue, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(13.dp))
                    if (recentProjects.size > 3) {
                        Text(if (showAllProjects) "收起  ⌃" else "全部  ›",
                            modifier = Modifier.clickable { showAllProjects = !showAllProjects },
                            fontSize = 12.sp, color = TgfTokens.Blue)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (recentProjects.isEmpty()) {
                    RecentProjectEmpty()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        (if (showAllProjects) recentProjects else recentProjects.take(3)).forEach { item ->
                            RecentProjectRow(
                                item,
                                onClick = { onOpenProject(item.id) },
                                onRename = { renameTarget = item; renameValue = item.title },
                                onDelete = { deleteTarget = item },
                                onBackup = { onExportProject(item.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                // Quiet footer entry stays after recent projects; it does not displace editors.
                Row(Modifier.fillMaxWidth().padding(bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    Text("关于", modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .clickable { showAbout = true }
                            .padding(horizontal = 23.dp, vertical = 13.dp),
                        fontSize = 13.sp, color = TgfTokens.InkSecondary,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
        renameTarget?.let { project ->
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("重命名项目") },
                text = {
                    OutlinedTextField(value = renameValue, onValueChange = { if (it.length <= 40) renameValue = it },
                        label = { Text("项目名称") }, singleLine = true)
                },
                confirmButton = {
                    TextButton(enabled = renameValue.trim().isNotEmpty(), onClick = {
                        onRenameProject(project.id, renameValue.trim())
                        renameTarget = null
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
            )
        }
        deleteTarget?.let { project ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("删除项目？") },
                text = { Text("“${project.title}”以及项目内保存的原始照片副本将被永久删除，无法撤销。建议先通过项目菜单创建备份。") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteProject(project.id)
                        deleteTarget = null
                    }) { Text("确定删除", color = Color(0xFFC34B5C)) }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
            )
        }
    }
}

/** Lightweight, asset-free cinematic ribbons; decorative only, never exported. */
@Composable
private fun CinematicBackdrop() {
    val dark = TgfTokens.isDark
    val background = listOf(TgfTokens.BackgroundTop, TgfTokens.BackgroundMid, TgfTokens.BackgroundBottom)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(background))
        val w = size.width
        val h = size.height
        // Top-right atmospheric lighting, intentionally subtle behind the header.
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(Color(0x666750CF), Color(0x001B2854))
                         else listOf(Color(0x664D93F5), Color(0x006AB5F8)),
                center = Offset(w * .97f, h * .06f), radius = w * .75f
            ), center = Offset(w * .97f, h * .06f), radius = w * .75f
        )
        // A second purple-side glow breaks the uniform blue wash in Light,
        // and gives Dark a quieter cinematic depth without wallpaper bitmaps.
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (dark) listOf(Color(0x333C2CAC), Color.Transparent)
                         else listOf(Color(0x337C66D7), Color.Transparent),
                center = Offset(w * -.12f, h * .33f), radius = w * .88f
            ),
            center = Offset(w * -.12f, h * .33f), radius = w * .88f
        )
        // Low flowing blue-purple glass layers from the approved cinematic scheme.
        val wave1 = Path().apply {
            moveTo(0f, h * .82f)
            cubicTo(w * .24f, h * .74f, w * .46f, h * .91f, w, h * .78f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(wave1, Brush.linearGradient(
            if (dark) listOf(Color(0x494C52B8), Color(0x8C3B1B7F))
            else listOf(Color(0x5576BEFF), Color(0x857E7CEB)),
            start = Offset(0f, h * .8f), end = Offset(w, h)))
        val wave2 = Path().apply {
            moveTo(0f, h * .90f)
            cubicTo(w * .36f, h * .84f, w * .60f, h * .96f, w, h * .85f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(wave2, Brush.linearGradient(
            if (dark) listOf(Color(0x57263F94), Color(0x844C1883))
            else listOf(Color(0x4CFFFFFF), Color(0x857EA7FF))))
        val crest = Path().apply {
            moveTo(0f, h * .82f)
            cubicTo(w * .24f, h * .74f, w * .46f, h * .91f, w, h * .78f)
        }
        drawPath(crest, color = if (dark) Color(0x66779BFF) else Color.White.copy(alpha = .64f),
            style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun BrandHeader(themeMode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 品牌结构维持 v0.1.1 已确认的折叠图框、山形和太阳，单独提升材质质感。
        TgfBrandMark(Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("图工坊", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TgfTokens.Ink)
            Text("让每一张图片更有价值", fontSize = 11.sp, color = TgfTokens.InkSecondary)
        }
        Spacer(Modifier.weight(1f))
        // DEV39: one-tap theme toggle; no hidden third mode or dropdown.
        Box(
            Modifier.size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(TgfTokens.SurfaceRaised.copy(alpha = .89f), RoundedCornerShape(15.dp))
                .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(15.dp))
                .clickable { onModeChange(if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK) }
                .semantics { contentDescription = if (themeMode == ThemeMode.DARK) "切换为浅色主题" else "切换为深色主题" },
            contentAlignment = Alignment.Center
        ) {
            Text(if (themeMode == ThemeMode.DARK) "☾" else "☼", fontSize = 25.sp,
                color = TgfTokens.Purple, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun NewProjectCard(onNewProject: () -> Unit) {
    val shape = RoundedCornerShape(23.dp)
    Box(
        modifier = Modifier.fillMaxWidth().height(113.dp)
            .shadow(13.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3AABF1), Color(0xFF367CEE), Color(0xFF625DE6), Color(0xFF9A50D6))
                )
            )
            .clickable(onClick = onNewProject)
    ) {
        // 受圆角容器裁剪的圆形柔光，不使用不受裁剪的白色矩形高光。
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .09f), size.width * .55f, Offset(size.width * .22f, -size.height * .44f))
            drawCircle(Color(0xFF2435C6).copy(alpha = .10f), size.width * .48f, Offset(size.width * .90f, size.height * 1.25f))
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(50.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFE0EEFF), Color(0xFFC6DDFA))), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 34.sp, fontWeight = FontWeight.Light, color = Color(0xFF306CF6))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("新建项目", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(5.dp))
                Text("照片编辑 · 拼图排版 · 图文模板", fontSize = 10.5.sp, color = Color.White.copy(alpha = .92f), maxLines = 1)
            }
            Box(
                Modifier.size(29.dp).background(Color(0xFFAECDF8).copy(alpha = .27f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("›", fontSize = 25.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuickActions(onSingle: () -> Unit, onCollage: () -> Unit, onCamera: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction("单图", TgfIconType.Photo, Modifier.weight(1f), onSingle)
        QuickAction("拼图", TgfIconType.Projects, Modifier.weight(1f), onCollage)
        QuickAction("拍照", TgfIconType.Camera, Modifier.weight(1f), onCamera)
    }
}

/** Home-only cinematic action. Feature callbacks and navigation remain unchanged. */
@Composable
private fun QuickAction(title: String, type: TgfIconType, modifier: Modifier, onClick: () -> Unit) {
    val dark = TgfTokens.isDark
    val (top, bottom) = when (type) {
        TgfIconType.Photo -> if (dark) Color(0xFF1B427A) to Color(0xFF14254D)
                              else Color(0xFFB9E5FF) to Color(0xFFE2EAFF)
        TgfIconType.Projects -> if (dark) Color(0xFF413775) to Color(0xFF251C4F)
                                 else Color(0xFFD5D3FF) to Color(0xFFF2EDFF)
        else -> if (dark) Color(0xFF542C65) to Color(0xFF2A1944)
                else Color(0xFFEDCDF8) to Color(0xFFF7EFFF)
    }
    val outline = if (dark) when (type) {
        TgfIconType.Photo -> Color(0xFF5A9EF8)
        TgfIconType.Projects -> Color(0xFF9B82F6)
        else -> Color(0xFFBB73E9)
    } else Color.White.copy(alpha = .95f)
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.height(122.dp)
            .shadow(if (dark) 5.dp else 9.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)), shape)
            .border(1.dp, outline.copy(alpha = if (dark) .82f else 1f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Fine glass highlight is clipped to each rounded card; decorative only.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Color.White.copy(alpha = if (dark) .055f else .23f),
                radius = size.width * .65f,
                center = Offset(size.width * .12f, -size.height * .29f)
            )
            drawLine(
                color = Color.White.copy(alpha = if (dark) .18f else .55f),
                start = Offset(size.width * .15f, 1.dp.toPx()),
                end = Offset(size.width * .82f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        Column(Modifier.fillMaxSize().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            TgfIcon(type, Modifier.size(42.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = TgfTokens.Ink, textAlign = TextAlign.Center, maxLines = 1)
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(50))
                .background(TgfTokens.SurfaceRaised.copy(alpha = if (dark) .85f else .95f)),
                contentAlignment = Alignment.Center) {
                Text("›", fontSize = 17.sp, color = TgfTokens.Blue, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun RecentProjectEmpty() {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(110.dp),
        radius = 21.dp,
        elevation = 5.dp,
        padding = PaddingValues(horizontal = 17.dp, vertical = 15.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(60.dp)
                    .background(Brush.linearGradient(listOf(TgfTokens.IconTop, TgfTokens.IconBottom)), RoundedCornerShape(17.dp))
                    .border(1.dp, TgfTokens.GlassBorder, RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                TgfIcon(TgfIconType.Projects, Modifier.size(38.dp), bare = true)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("还没有编辑项目", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TgfTokens.Ink)
                Text("从上方新建项目开始", fontSize = 11.sp, color = TgfTokens.InkSecondary)
            }
            Text("›", fontSize = 25.sp, color = TgfTokens.Blue)
        }
    }
}

@Composable
private fun RecentProjectRow(
    info: ProjectCardInfo,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBackup: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(97.dp).clickable(onClick = onClick),
        radius = 19.dp, elevation = 4.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            ProjectThumbnail(info)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = TgfTokens.Ink, maxLines = 1)
                val date = remember(info.modifiedAt) {
                    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(info.modifiedAt))
                }
                Text("$date  ·  ${info.photoCount} 张图片  ·  点击继续编辑",
                    fontSize = 10.5.sp, color = TgfTokens.InkSecondary, maxLines = 1)
            }
            Box(contentAlignment = Alignment.Center) {
                Text("•••", modifier = Modifier.clickable { menuOpen = true }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TgfTokens.Blue)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("重命名") }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(text = { Text("备份项目") }, onClick = { menuOpen = false; onBackup() })
                    DropdownMenuItem(text = { Text("删除项目", color = Color(0xFFC34B5C)) },
                        onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}


/** The checkerboard is display-only. The actual PNG keeps transparent background pixels. */
@Composable
private fun ProjectThumbnail(info: ProjectCardInfo) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, info.thumbnailPath, info.modifiedAt) {
        value = withContext(Dispatchers.IO) {
            info.thumbnailPath?.let { path ->
                runCatching {
                    val options = BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
                    BitmapFactory.decodeFile(path, options)
                }.getOrNull()
            }
        }
    }
    val corners = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(width = 74.dp, height = 81.dp).clip(corners)
            .background(Color(0xFFE5ECF6)).border(1.dp, Color.White, corners),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = 8.dp.toPx()
            var y = 0f
            var r = 0
            while (y < size.height) {
                var x = 0f
                var c = 0
                while (x < size.width) {
                    drawRect(if ((r + c) % 2 == 0) Color.White else Color(0xFFD3DDEA),
                        topLeft = Offset(x,y), size = androidx.compose.ui.geometry.Size(cell,cell))
                    x += cell; c++
                }
                y += cell; r++
            }
        }
        val available = bitmap
        if (available != null) {
            Image(available.asImageBitmap(), contentDescription = "${info.title}缩略图",
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(2.dp))
        } else {
            TgfIcon(TgfIconType.Projects, Modifier.size(34.dp), bare = true)
        }
    }
}
