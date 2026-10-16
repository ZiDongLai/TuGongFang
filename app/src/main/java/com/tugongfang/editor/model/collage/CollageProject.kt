package com.tugongfang.editor.model.collage

/** 独立于屏幕像素的拼图布局，编辑和导出共用。 */
/** New layout families are added without changing the identifiers of the 33 legacy templates. */
enum class CollageLayoutStyle {
    Legacy, WideLeft, WideRight, WideTop, WideBottom,
    HeroRight, HeroTop, HeroBottom, HeroCenter, Stagger
}

enum class CollageTemplate(
    val title: String,
    val slotCount: Int,
    val layoutStyle: CollageLayoutStyle = CollageLayoutStyle.Legacy
) {
    // Stable legacy names: previously saved projects must continue to deserialize.
    TwoColumns("双列", 2), TwoRows("上下", 2),
    ThreeColumns("三列", 3), HeroThree("主图+双图", 3),
    FourGrid("四宫格", 4), SixGrid("六宫格", 6),
    OnePhoto("单张", 1),
    ThreeRows("三行", 3),
    FourColumns("四列", 4),
    FourHero("主图组合", 4),
    N5Grid("均衡", 5),
    N5Rows("横向", 5),
    N5Hero("主图", 5),
    SixRows("行式", 6),
    SixHero("主图组合", 6),
    N7Grid("均衡", 7),
    N7Rows("横向", 7),
    N7Hero("主图", 7),
    N8Grid("均衡", 8),
    N8Rows("横向", 8),
    N8Hero("主图", 8),
    N9Grid("均衡", 9),
    N9Rows("横向", 9),
    N9Hero("主图", 9),
    N10Grid("均衡", 10),
    N10Rows("横向", 10),
    N10Hero("主图", 10),
    N11Grid("均衡", 11),
    N11Rows("横向", 11),
    N11Hero("主图", 11),
    N12Grid("均衡", 12),
    N12Rows("横向", 12),
    N12Hero("主图", 12),
    // DEV41: new layouts are appended to keep persisted legacy enum names/order stable.
    TwoWideLeft("左宽右窄", 2, CollageLayoutStyle.WideLeft),
    TwoWideRight("左窄右宽", 2, CollageLayoutStyle.WideRight),
    TwoWideTop("上宽下窄", 2, CollageLayoutStyle.WideTop),
    TwoWideBottom("上窄下宽", 2, CollageLayoutStyle.WideBottom),
    N3HeroRight("右侧主图", 3, CollageLayoutStyle.HeroRight),
    N3HeroTop("上方主图", 3, CollageLayoutStyle.HeroTop),
    N3HeroBottom("下方主图", 3, CollageLayoutStyle.HeroBottom),
    N3HeroCenter("中央主图", 3, CollageLayoutStyle.HeroCenter),
    N3Stagger("错位拼版", 3, CollageLayoutStyle.Stagger),
    N4HeroRight("右侧主图", 4, CollageLayoutStyle.HeroRight),
    N4HeroTop("上方主图", 4, CollageLayoutStyle.HeroTop),
    N4HeroBottom("下方主图", 4, CollageLayoutStyle.HeroBottom),
    N4HeroCenter("中央主图", 4, CollageLayoutStyle.HeroCenter),
    N4Stagger("错位拼版", 4, CollageLayoutStyle.Stagger),
    N5HeroRight("右侧主图", 5, CollageLayoutStyle.HeroRight),
    N5HeroTop("上方主图", 5, CollageLayoutStyle.HeroTop),
    N5HeroBottom("下方主图", 5, CollageLayoutStyle.HeroBottom),
    N5HeroCenter("中央主图", 5, CollageLayoutStyle.HeroCenter),
    N5Stagger("错位拼版", 5, CollageLayoutStyle.Stagger),
    N6HeroRight("右侧主图", 6, CollageLayoutStyle.HeroRight),
    N6HeroTop("上方主图", 6, CollageLayoutStyle.HeroTop),
    N6HeroBottom("下方主图", 6, CollageLayoutStyle.HeroBottom),
    N6HeroCenter("中央主图", 6, CollageLayoutStyle.HeroCenter),
    N6Stagger("错位拼版", 6, CollageLayoutStyle.Stagger),
    N7HeroRight("右侧主图", 7, CollageLayoutStyle.HeroRight),
    N7HeroTop("上方主图", 7, CollageLayoutStyle.HeroTop),
    N7HeroBottom("下方主图", 7, CollageLayoutStyle.HeroBottom),
    N7HeroCenter("中央主图", 7, CollageLayoutStyle.HeroCenter),
    N7Stagger("错位拼版", 7, CollageLayoutStyle.Stagger),
    N8HeroRight("右侧主图", 8, CollageLayoutStyle.HeroRight),
    N8HeroTop("上方主图", 8, CollageLayoutStyle.HeroTop),
    N8HeroBottom("下方主图", 8, CollageLayoutStyle.HeroBottom),
    N8HeroCenter("中央主图", 8, CollageLayoutStyle.HeroCenter),
    N8Stagger("错位拼版", 8, CollageLayoutStyle.Stagger),
    N9HeroRight("右侧主图", 9, CollageLayoutStyle.HeroRight),
    N9HeroTop("上方主图", 9, CollageLayoutStyle.HeroTop),
    N9HeroBottom("下方主图", 9, CollageLayoutStyle.HeroBottom),
    N9HeroCenter("中央主图", 9, CollageLayoutStyle.HeroCenter),
    N9Stagger("错位拼版", 9, CollageLayoutStyle.Stagger),
    N10HeroRight("右侧主图", 10, CollageLayoutStyle.HeroRight),
    N10HeroTop("上方主图", 10, CollageLayoutStyle.HeroTop),
    N10HeroBottom("下方主图", 10, CollageLayoutStyle.HeroBottom),
    N10HeroCenter("中央主图", 10, CollageLayoutStyle.HeroCenter),
    N10Stagger("错位拼版", 10, CollageLayoutStyle.Stagger),
    N11HeroRight("右侧主图", 11, CollageLayoutStyle.HeroRight),
    N11HeroTop("上方主图", 11, CollageLayoutStyle.HeroTop),
    N11HeroBottom("下方主图", 11, CollageLayoutStyle.HeroBottom),
    N11HeroCenter("中央主图", 11, CollageLayoutStyle.HeroCenter),
    N11Stagger("错位拼版", 11, CollageLayoutStyle.Stagger),
    N12HeroRight("右侧主图", 12, CollageLayoutStyle.HeroRight),
    N12HeroTop("上方主图", 12, CollageLayoutStyle.HeroTop),
    N12HeroBottom("下方主图", 12, CollageLayoutStyle.HeroBottom),
    N12HeroCenter("中央主图", 12, CollageLayoutStyle.HeroCenter),
    N12Stagger("错位拼版", 12, CollageLayoutStyle.Stagger);
}

/** 只展示与当前照片数量一致的模板；从不产生空格或丢弃已有照片。 */
fun templatesForPhotoCount(count: Int): List<CollageTemplate> =
    CollageTemplate.entries.filter { it.slotCount == count.coerceIn(1, 12) }

fun defaultTemplateForPhotoCount(count: Int): CollageTemplate = when (count.coerceIn(1, 12)) {
    1 -> CollageTemplate.OnePhoto
    2 -> CollageTemplate.TwoColumns
    3 -> CollageTemplate.HeroThree
    4 -> CollageTemplate.FourGrid
    6 -> CollageTemplate.SixGrid
    else -> templatesForPhotoCount(count).first()
}


enum class CollageAspect(val label: String, val width: Int, val height: Int) {
    Square("1:1", 1, 1), Portrait("4:5", 4, 5),
    Tall("9:16", 9, 16), Landscape("16:9", 16, 9);
    val ratio: Float get() = width.toFloat() / height
}

data class CollageRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right-left).coerceAtLeast(0f)
    val height: Float get() = (bottom-top).coerceAtLeast(0f)
}

enum class CollageFitMode { Fill, Fit }

enum class CollageMode { Template, Free }

data class CollageSlot(
    val imageId: String? = null,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f,
    val fitMode: CollageFitMode = CollageFitMode.Fill
)

data class CollageProject(
    val mode: CollageMode = CollageMode.Template,
    val template: CollageTemplate = CollageTemplate.FourGrid,
    val aspect: CollageAspect = CollageAspect.Square,
    val gapRatio: Float = .012f,
    val cornerRatio: Float = .025f,
    val outerInsetRatio: Float = 0f,
    val slots: List<CollageSlot> = emptyList(),
    val activeSlot: Int = 0
) {
    fun normalized(): CollageProject = copy(
        gapRatio=gapRatio.coerceIn(0f,.045f),
        cornerRatio=cornerRatio.coerceIn(0f,.08f),
        outerInsetRatio=outerInsetRatio.coerceIn(0f,.20f),
        activeSlot=activeSlot.coerceIn(0,template.slotCount-1),
        slots=(0 until template.slotCount).map { slots.getOrNull(it) ?: CollageSlot() }
    )

    fun changeTemplate(next: CollageTemplate): CollageProject {
        // Switching visual style cannot modify the number of photographs.
        val current = normalized().slots
        val filled = current.filter { it.imageId != null }
        // Legacy 5-photo projects might still use a 6-slot template.
        if (next.slotCount != template.slotCount && next.slotCount != filled.size) return this
        val resized = (0 until next.slotCount).map { filled.getOrNull(it) ?: CollageSlot() }
        return copy(template=next, slots=resized, activeSlot=activeSlot.coerceIn(0,next.slotCount-1))
    }

    fun assignNewImages(ids: List<String>): CollageProject {
        val updated=normalized().slots.toMutableList()
        val already=updated.mapNotNull { it.imageId }.toSet()
        ids.filterNot { it in already }.forEach { id ->
            val empty=updated.indexOfFirst { it.imageId == null }
            if (empty>=0) updated[empty]=CollageSlot(id)
        }
        return copy(slots=updated)
    }

    /** 只从拼图格位中移除照片，不销毁原照片图层；撤销可完整恢复。 */
    fun clearSlot(index: Int): CollageProject = changeSlot(index, CollageSlot())

    fun changeSlot(index: Int, next: CollageSlot): CollageProject {
        val list=normalized().slots.toMutableList()
        if (index in list.indices) list[index]=next
        return copy(slots=list)
    }

    /** 所有输出均为归一化坐标；编辑预览与导出读取同一组 rects。 */
    fun rects(): List<CollageRect> {
        val count = template.slotCount
        val full = when (template.layoutStyle) {
            CollageLayoutStyle.WideLeft -> listOf(rect(0f,0f,.65f,1f),rect(.65f,0f,1f,1f))
            CollageLayoutStyle.WideRight -> listOf(rect(0f,0f,.35f,1f),rect(.35f,0f,1f,1f))
            CollageLayoutStyle.WideTop -> listOf(rect(0f,0f,1f,.65f),rect(0f,.65f,1f,1f))
            CollageLayoutStyle.WideBottom -> listOf(rect(0f,0f,1f,.35f),rect(0f,.35f,1f,1f))
            CollageLayoutStyle.HeroRight -> heroRightGrid(count)
            CollageLayoutStyle.HeroTop -> horizontalHeroGrid(count, top = true)
            CollageLayoutStyle.HeroBottom -> horizontalHeroGrid(count, top = false)
            CollageLayoutStyle.HeroCenter -> centerHeroGrid(count)
            CollageLayoutStyle.Stagger -> staggerGrid(count)
            CollageLayoutStyle.Legacy -> when (template) {
            CollageTemplate.TwoColumns -> listOf(rect(0f,0f,.5f,1f),rect(.5f,0f,1f,1f))
            CollageTemplate.TwoRows -> listOf(rect(0f,0f,1f,.5f),rect(0f,.5f,1f,1f))
            CollageTemplate.ThreeColumns -> (0..2).map { i -> rect(i/3f,0f,(i+1)/3f,1f) }
            CollageTemplate.HeroThree -> heroGrid(3)
            CollageTemplate.FourGrid -> balancedGrid(4,2)
            CollageTemplate.SixGrid -> balancedGrid(6,3)
            else -> when {
                count == 1 -> listOf(rect(0f,0f,1f,1f))
                template.name.endsWith("Hero") -> heroGrid(count)
                template.name.endsWith("Rows") -> balancedGrid(count, 2)
                template == CollageTemplate.FourColumns -> balancedGrid(4,4)
                else -> balancedGrid(count, kotlin.math.ceil(kotlin.math.sqrt(count.toDouble())).toInt())
            }
            }
        }
        val inset = gapRatio.coerceIn(0f,.045f)/2f
        val outer = outerInsetRatio.coerceIn(0f,.20f)
        // Legacy spacing stays intact when outer == 0; no template IDs change.
        return full.map { r ->
            rect(outer + (r.left + inset) * (1f - 2f * outer),
                outer + (r.top + inset) * (1f - 2f * outer),
                outer + (r.right - inset) * (1f - 2f * outer),
                outer + (r.bottom - inset) * (1f - 2f * outer))
        }
    }
}

/** Each row fills the full width. Rows distribute 1..12 images without placeholder cells. */
private fun balancedGrid(count: Int, requestedColumns: Int): List<CollageRect> {
    val columns = requestedColumns.coerceIn(1,count)
    val rows = (count + columns - 1) / columns
    return buildList {
        var index = 0
        for (row in 0 until rows) {
            val inRow = kotlin.math.min(columns,count-index)
            for (col in 0 until inRow) {
                add(rect(col.toFloat()/inRow,row.toFloat()/rows,(col+1f)/inRow,(row+1f)/rows))
            }
            index += inRow
        }
    }
}

private fun heroGrid(count: Int): List<CollageRect> {
    if (count < 3) return balancedGrid(count,count)
    val remaining = count-1
    val rightColumns = kotlin.math.ceil(kotlin.math.sqrt(remaining.toDouble())).toInt()
    return listOf(rect(0f,0f,.54f,1f)) + balancedGrid(remaining,rightColumns).map {
        rect(.54f + it.left*.46f,it.top,.54f+it.right*.46f,it.bottom)
    }
}


/** Right-side feature photo, small tiles on the left. Coordinates match DEV41 approved preview. */
private fun heroRightGrid(count: Int): List<CollageRect> =
    listOf(rect(.48f,0f,1f,1f)) + balancedGrid(
        count - 1, kotlin.math.ceil(kotlin.math.sqrt((count - 1).toDouble())).toInt()
    ).map { rect(it.left*.48f,it.top,it.right*.48f,it.bottom) }

/** Single horizontal feature photo, remaining tiles exactly cover the other strip. */
private fun horizontalHeroGrid(count: Int, top: Boolean): List<CollageRect> {
    val small = balancedGrid(count - 1,
        kotlin.math.max(2,kotlin.math.ceil(kotlin.math.sqrt((count - 1).toDouble())).toInt()))
    return if (top) {
        listOf(rect(0f,0f,1f,.44f)) + small.map {
            rect(it.left,.44f + it.top*.56f,it.right,.44f + it.bottom*.56f)
        }
    } else {
        listOf(rect(0f,.56f,1f,1f)) + small.map {
            rect(it.left,it.top*.56f,it.right,it.bottom*.56f)
        }
    }
}

/** Center full-height photograph, two independently stacked narrow side columns. */
private fun centerHeroGrid(count: Int): List<CollageRect> {
    val leftCount = (count - 1) / 2
    val rightCount = count - 1 - leftCount
    return listOf(rect(.29f,0f,.71f,1f)) +
        balancedGrid(leftCount,1).map {
            rect(it.left*.29f,it.top,it.right*.29f,it.bottom)
        } + balancedGrid(rightCount,1).map {
            rect(.71f + it.left*.29f,it.top,.71f + it.right*.29f,it.bottom)
        }
}

/** Uneven-height, uneven-width columns; no gaps and no placeholder tiles. */
private fun staggerGrid(count: Int): List<CollageRect> {
    val distribution = when {
        count == 3 -> listOf(1,2)
        count <= 5 -> listOf(2,count-2)
        count == 6 -> listOf(1,2,3)
        else -> {
            val q = count / 3
            val mid = q + if (count % 3 == 2) 1 else 0
            listOf(q,mid,count-q-mid)
        }
    }
    val widths = if (distribution.size == 2) {
        if (count == 3) listOf(.39f,.61f) else listOf(.43f,.57f)
    } else listOf(.31f,.34f,.35f)
    val result = mutableListOf<CollageRect>()
    var left = 0f
    distribution.forEachIndexed { col, cellCount ->
        val right = if (col == distribution.lastIndex) 1f else left + widths[col]
        for (row in 0 until cellCount) {
            result += rect(left,row.toFloat()/cellCount,right,(row+1f)/cellCount)
        }
        left = right
    }
    return result
}

private fun rect(left:Float,top:Float,right:Float,bottom:Float)=CollageRect(left,top,right,bottom)
