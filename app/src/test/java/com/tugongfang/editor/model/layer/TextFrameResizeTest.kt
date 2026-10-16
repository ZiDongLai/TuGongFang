package com.tugongfang.editor.model.layer

import org.junit.Assert.*
import org.junit.Test

class TextFrameResizeTest {
    @Test fun oldProjectsDefaultToAutomaticHeight() {
        val old = TextLayer(text = "旧项目文字")
        assertEquals(268, old.boxWidthDp)
        assertEquals(0, old.boxHeightDp)
        assertEquals(1f, old.scale)
    }
    @Test fun widthAndHeightAreIndependentOfFontAndRotation() {
        val base = TextLayer(text = "自动换行换行换行", fontSizeSp = 31f, rotation = 19f)
        val resized = base.copy(boxWidthDp = 190, boxHeightDp = 300)
        assertEquals(190, resized.boxWidthDp)
        assertEquals(300, resized.boxHeightDp)
        assertEquals(31f, resized.fontSizeSp)
        assertEquals(19f, resized.rotation)
        assertEquals(base.text, resized.text)
    }
    @Test fun duplicatingPreservesIndependentDimensions() {
        val resized = TextLayer(boxWidthDp = 117, boxHeightDp = 480)
        assertEquals(117, resized.copy(id="copy").boxWidthDp)
        assertEquals(480, resized.copy(id="copy").boxHeightDp)
    }
}
