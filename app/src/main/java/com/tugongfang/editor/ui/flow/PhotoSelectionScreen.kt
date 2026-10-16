package com.tugongfang.editor.ui.flow

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.editor.image.rememberImagePreview
import com.tugongfang.editor.ui.design.TgfTokens

/**
 * First-class selection step, not an image-replacement panel.
 * Existing project contents are unchanged until onConfirm is invoked.
 * Android's system picker does not restore checked items, so the app previews its own draft here.
 */
@Composable
fun PhotoSelectionScreen(
    isReselection: Boolean,
    selectedUris: List<Uri>,
    onOpenGallery: () -> Unit,
    onOpenSystemMulti: () -> Unit,
    onAddGallery: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val valid = selectedUris.size in 1..12
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(TgfTokens.BackgroundTop, TgfTokens.BackgroundBottom)))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, color = TgfTokens.Ink,
                modifier = Modifier.clickable(onClick = onCancel).padding(horizontal = 10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isReselection) "重选拼图照片" else "选择拼图照片",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TgfTokens.Ink)
                Text(if (isReselection) "取消返回编辑，不更改当前作品" else "先选照片，确认后进入编辑",
                    fontSize = 11.sp, color = TgfTokens.InkSecondary)
            }
        }
        Spacer(Modifier.height(15.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(TgfTokens.ActionBlue, TgfTokens.ActionPurple)))
                .clickable(onClick = onOpenGallery).padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("▣  ${if (selectedUris.isEmpty()) "从相册选择照片" else "打开相册重新选择"}",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        // A clear fallback if Huawei Gallery returns only one image from ACTION_PICK.
        // This launches the Android system photo picker with real 12-item multi-select.
        Text("图库无法一次勾选多张？点击系统多选相册", fontSize = 12.sp,
            color = TgfTokens.Blue,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSystemMulti)
                .padding(vertical = 8.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("已选 ${selectedUris.size} 张", fontSize = 15.sp,
                color = TgfTokens.Ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("1–12张 · 每张对应布局",
                fontSize = 11.sp, color = TgfTokens.InkSecondary)
        }
        Spacer(Modifier.height(9.dp))
        if (selectedUris.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, TgfTokens.SurfaceSelected,
                RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text("尚未选择照片\n点击上方按钮打开手机相册",
                    color = TgfTokens.InkSecondary, textAlign = TextAlign.Center,
                    fontSize = 14.sp, lineHeight = 24.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(selectedUris, key = { index, uri -> "photo_${index}_${uri}" }) { index, uri ->
                    SelectedPhotoItem(uri, index, onRemoveAt)
                }
                if (selectedUris.size < 12) {
                    item(key = "append_gallery") {
                        Box(Modifier.fillMaxWidth().aspectRatio(.85f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TgfTokens.SurfaceSelected)
                            .border(1.2.dp,TgfTokens.Blue,RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddGallery), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("＋",fontSize = 33.sp,color = TgfTokens.Blue, fontWeight = FontWeight.Bold)
                                Text("添加照片",fontSize = 12.sp,color = TgfTokens.Blue)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(49.dp)) {
                Text(if (isReselection) "取消重选" else "返回首页")
            }
            Button(onClick = onConfirm, enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TgfTokens.ActionBlue,
                    contentColor = Color.White),
                modifier = Modifier.weight(1.25f).height(49.dp)) {
                Text(if (isReselection) "确认重选" else "进入编辑")
            }
        }
    }
}

@Composable
private fun SelectedPhotoItem(uri: Uri, index: Int, onRemoveAt: (Int) -> Unit) {
    val context = LocalContext.current
    val bitmap by rememberImagePreview(context, uri.toString(), maxDimension = 440)
    Box(Modifier.fillMaxWidth().aspectRatio(.85f).clip(RoundedCornerShape(12.dp))
        .background(TgfTokens.CanvasInner)) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!, contentDescription = "第${index + 1}张照片",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("图片读取中", fontSize = 10.sp, color = TgfTokens.InkSecondary,
                modifier = Modifier.align(Alignment.Center))
        }
        Text("${index + 1}", fontSize = 11.sp, color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).background(Color(0xB9182B48),
                RoundedCornerShape(bottomEnd = 9.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
        Text("×", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = Color.White, modifier = Modifier.align(Alignment.TopEnd)
                .background(Color(0xE03B465A), RoundedCornerShape(bottomStart = 10.dp))
                .clickable { onRemoveAt(index) }.size(40.dp).wrapContentHeight(Alignment.CenterVertically))
    }
}
