package com.tugongfang.editor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.TextLayer
import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.ShapeLayer
import com.tugongfang.editor.model.layer.ShapeType
import com.tugongfang.editor.ui.components.GlassCard
import com.tugongfang.editor.ui.components.TgfIcon
import com.tugongfang.editor.ui.components.TgfIconType
import com.tugongfang.editor.ui.design.TgfTokens

/** An overlay. NEVER placed in the canvas measurement Column or changes the viewport. */
@Composable
fun LayerDrawer(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onToggle: () -> Unit,
    layers: List<ImageLayer>,
    textLayers: List<TextLayer>,
    infoLayers: List<InfoPanelLayer>,
    shapeLayers: List<ShapeLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String?) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onReplacePhoto: (String) -> Unit,
    onToggleTextVisibility: (String) -> Unit,
    onToggleTextLock: (String) -> Unit,
    onDeleteTextLayer: (String) -> Unit,
    onDuplicateTextLayer: (String) -> Unit,
    onEditText: (String) -> Unit,
    onEditInfo: (String) -> Unit,
    onToggleInfoVisibility: (String) -> Unit,
    onDeleteInfoLayer: (String) -> Unit,
    onDeleteShapeLayer: (String) -> Unit,
    onDuplicateShapeLayer: (String) -> Unit,
    onToggleShapeLock: (String) -> Unit,
    onToggleShapeVisibility: (String) -> Unit
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (expanded) {
            GlassCard(
                modifier = Modifier.width(263.dp).heightIn(max = 410.dp),
                radius = 20.dp, elevation = 7.dp,
                padding = PaddingValues(9.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("图层管理", fontSize = 14.sp, color = TgfTokens.Ink, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        val count = layers.size + textLayers.size + infoLayers.size +
                            shapeLayers.distinctBy { it.doodleSessionId ?: it.id }.size
                        Text("${count} 层", fontSize = 10.sp, color = TgfTokens.InkSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().heightIn(max=362.dp).verticalScroll(rememberScrollState())) {
                        if (layers.isEmpty() && textLayers.isEmpty() && infoLayers.isEmpty() && shapeLayers.isEmpty()) {
                            Text("暂无图层", modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = TgfTokens.InkSecondary)
                        }
                        shapeLayers.asReversed().distinctBy { it.doodleSessionId ?: it.id }.forEach { item ->
                            val count = item.doodleSessionId?.let { key -> shapeLayers.count { it.doodleSessionId == key } } ?: 0
                            val label = if (count > 0) "涂鸦（$count 笔）" else if (item.type == ShapeType.Symbol) "符号" else "图形"
                            DrawerLayerRow(label, TgfIconType.Shapes, item.id == selectedLayerId,
                                item.visible, item.locked, { onSelectLayer(item.id) },
                                { onToggleShapeVisibility(item.id) }, { onToggleShapeLock(item.id) },
                                listOf("复制" to { onDuplicateShapeLayer(item.id) }, "删除" to { onDeleteShapeLayer(item.id) }))
                        }
                        infoLayers.asReversed().forEach { item ->
                            DrawerLayerRow("信息模板", TgfIconType.Template, item.id == selectedLayerId,
                                item.visible, null, { onSelectLayer(item.id) },
                                { onToggleInfoVisibility(item.id) }, null,
                                listOf("编辑" to { onEditInfo(item.id) }, "删除" to { onDeleteInfoLayer(item.id) }))
                        }
                        textLayers.asReversed().forEach { item ->
                            DrawerLayerRow(item.text.ifBlank { "文字" }.take(15), TgfIconType.Text,
                                item.id == selectedLayerId, item.visible, item.locked,
                                { onSelectLayer(item.id) }, { onToggleTextVisibility(item.id) },
                                { onToggleTextLock(item.id) },
                                listOf("编辑" to { onEditText(item.id) }, "复制" to { onDuplicateTextLayer(item.id) },
                                    "删除" to { onDeleteTextLayer(item.id) }))
                        }
                        layers.asReversed().forEachIndexed { idx, item ->
                            DrawerLayerRow("图片 ${layers.size - idx}", TgfIconType.Photo,
                                item.id == selectedLayerId, item.visible, item.locked,
                                { onSelectLayer(item.id) }, { onToggleVisibility(item.id) },
                                { onToggleLock(item.id) },
                                listOf("复制" to { onDuplicateLayer(item.id) },
                                    "替换" to { onReplacePhoto(item.id) }, "删除" to { onDeleteLayer(item.id) }))
                        }
                    }
                }
            }
        }
        Box(Modifier.width(32.dp).height(58.dp)
            .background(TgfTokens.SurfaceSelected, RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
            .border(1.dp, TgfTokens.Blue.copy(alpha=.42f), RoundedCornerShape(topEnd=14.dp,bottomEnd=14.dp))
            .clickable { onToggle() }, contentAlignment = Alignment.Center) {
            Text(if (expanded) "‹" else "›", fontSize = 27.sp, fontWeight = FontWeight.Bold,
                color = TgfTokens.SelectionInk)
        }
    }
}

/** A locked layer may be copied into a new editable layer, but not modified or deleted. */
internal fun canUseLayerAction(locked: Boolean?, label: String): Boolean =
    locked != true || label == "复制"

@Composable
private fun DrawerLayerRow(
    title: String,
    type: TgfIconType,
    selected: Boolean,
    visible: Boolean,
    locked: Boolean?,
    onSelect: () -> Unit,
    onVisibility: () -> Unit,
    onLock: (() -> Unit)?,
    actions: List<Pair<String, () -> Unit>>
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Popover remains a Material3 surface; only the theme-dependent row colors change.
    val shape = RoundedCornerShape(12.dp)
    // DEV20: fixed-size hit targets avoid accidental visibility/lock/menu changes.
    // The row itself remains selectable, while each action receives its own semantics.
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp)
        .background(if (selected) TgfTokens.SurfaceSelected else TgfTokens.SurfaceRaised.copy(alpha=.68f), shape)
        .border(1.dp, if (selected) TgfTokens.SelectionInk.copy(alpha=.62f)
            else TgfTokens.SurfaceOutline.copy(alpha=.65f), shape)
        .clickable { onSelect() }.padding(horizontal=5.dp, vertical=5.dp),
        verticalAlignment=Alignment.CenterVertically) {
        TgfIcon(type, modifier=Modifier.size(26.dp), selected=selected, bare=true)
        Spacer(Modifier.width(5.dp))
        Text(title, modifier=Modifier.weight(1f), fontSize=11.sp, color=TgfTokens.Ink,
            maxLines=1, overflow=TextOverflow.Ellipsis)
        Box(Modifier.size(42.dp)
            .background(if (!visible) TgfTokens.SurfaceSelected else Color.Transparent, RoundedCornerShape(10.dp))
            .semantics { contentDescription = if (visible) "隐藏图层" else "显示图层" }
            .clickable(onClick=onVisibility), contentAlignment=Alignment.Center) {
            Text(if (visible) "◉" else "○", fontSize=19.sp,
                color=if (visible) TgfTokens.SelectionInk else TgfTokens.InkSecondary)
        }
        if (locked != null && onLock != null) {
            Box(Modifier.size(42.dp)
                .background(if (locked) TgfTokens.SurfaceSelected else Color.Transparent, RoundedCornerShape(10.dp))
                .semantics { contentDescription = if (locked) "解锁图层" else "锁定图层" }
                .clickable(onClick=onLock), contentAlignment=Alignment.Center) {
                TgfIcon(if (locked) TgfIconType.Lock else TgfIconType.Unlock,
                    modifier=Modifier.size(24.dp), selected=locked, bare=true)
            }
        }
        Box {
            Box(Modifier.size(42.dp)
                .semantics { contentDescription = "更多图层操作" }
                .clickable { menuOpen=true }, contentAlignment=Alignment.Center) {
                Text("⋮", fontSize=22.sp, fontWeight=FontWeight.Bold, color=TgfTokens.SelectionInk)
            }
            DropdownMenu(expanded=menuOpen, onDismissRequest={menuOpen=false}) {
                actions.forEach { (label,action) ->
                    // Locked layers cannot be edited, replaced or removed until unlocked.
                    // Visibility and unlocking remain accessible directly in this row.
                    val allowed = canUseLayerAction(locked, label)
                    DropdownMenuItem(
                        text={Text(label, fontSize=12.sp,
                            color=if (!allowed) TgfTokens.InkSecondary.copy(alpha=.55f)
                                else if (label=="删除") Color(0xFFD65765) else TgfTokens.Ink)},
                        onClick={menuOpen=false; action()},
                        enabled=allowed
                    )
                }
            }
        }
    }
}
