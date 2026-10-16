package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEV39 FIX3: data defaults remain compatible with old stored projects. */
class TextAppearanceFix3Test {
    @Test fun oldTextStartsTransparentWithPlainGlyphs() {
        val old = TextLayer(text = "测试文本")
        assertFalse(old.italic)
        assertFalse(old.underline)
        assertEquals(0, old.backgroundColorArgb)
        assertEquals(0xFF202536.toInt(), old.colorArgb)
    }

    @Test fun styleChangesDoNotMoveOrResizeFrame() {
        val base = TextLayer(text = "多行\n示例", offsetXRatio = .24f, offsetYRatio = -.1f,
            boxWidthDp = 245, boxHeightDp = 180, rotation = 24f)
        val styled = base.copy(italic = true, underline = true,
            backgroundColorArgb = 0xFFE6DDFB.toInt(), colorArgb = 0xFF102030.toInt())
        assertTrue(styled.italic)
        assertTrue(styled.underline)
        assertEquals(base.boxWidthDp, styled.boxWidthDp)
        assertEquals(base.boxHeightDp, styled.boxHeightDp)
        assertEquals(base.offsetXRatio, styled.offsetXRatio, 0f)
        assertEquals(base.offsetYRatio, styled.offsetYRatio, 0f)
        assertEquals(base.rotation, styled.rotation, 0f)
    }

    @Test fun backgroundCanBeReturnedToTransparent() {
        val filled = TextLayer(backgroundColorArgb = 0xFF111E37.toInt())
        assertTrue((filled.backgroundColorArgb ushr 24) != 0)
        assertEquals(0, filled.copy(backgroundColorArgb = 0).backgroundColorArgb)
    }

    @Test fun templateDarkStyleIsIndependentFromApplicationTheme() {
        val original = InfoPanelLayer(overlayMode = true, widthRatio = .75f)
        val dark = original.copy(darkBackground = true)
        assertFalse(original.darkBackground)
        assertTrue(dark.darkBackground)
        val before = resolveInfoOverlayGeometry(original, 900f, 1400f)
        val after = resolveInfoOverlayGeometry(dark, 900f, 1400f)
        assertEquals(before, after)
    }
}
