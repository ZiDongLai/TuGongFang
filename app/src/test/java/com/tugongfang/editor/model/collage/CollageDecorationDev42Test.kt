package com.tugongfang.editor.model.collage

import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasScene
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CollageDecorationDev42Test {
    @Test fun originalTemplateBoundsRemainUnchanged() {
        for (template in CollageTemplate.entries) {
            val baseline=CollageProject(template=template,gapRatio=.012f)
            val explicitlyZero=baseline.copy(outerInsetRatio=0f)
            assertEquals(baseline.rects(),explicitlyZero.rects())
        }
    }

    @Test fun all87TemplatesRespectOuterMarginAndStillHaveCorrectTileCounts() {
        assertEquals(87,CollageTemplate.entries.size)
        for (template in CollageTemplate.entries) for (margin in listOf(0f,.02f,.08f,.20f)) {
            val rects=CollageProject(template=template,gapRatio=0f,outerInsetRatio=margin).rects()
            assertEquals(template.slotCount,rects.size)
            for (r in rects) {
                assertTrue("${template.name} left",r.left >= margin-1e-5f)
                assertTrue("${template.name} right",r.right <= 1f-margin+1e-5f)
                assertTrue(r.top >= margin-1e-5f)
                assertTrue(r.bottom <= 1f-margin+1e-5f)
                assertTrue(r.width > 0f && r.height > 0f)
            }
            val area=rects.sumOf { (it.width*it.height).toDouble() }
            val expected=(1.0-2.0*margin)*(1.0-2.0*margin)
            assertTrue("${template.name} margin=$margin actual=$area",abs(area-expected)<.00004)
        }
    }

    @Test fun borderAndBackgroundDoNotChangePhotoGeometry() {
        val base=CollageProject(template=CollageTemplate.HeroThree,gapRatio=.02f,
            outerInsetRatio=.06f,slots=listOf(CollageSlot("a"),CollageSlot("b"),CollageSlot("c")))
        val original=base.rects()
        val changed=base.changeTemplate(CollageTemplate.ThreeColumns)
        assertEquals(base.slots,changed.slots)
        assertEquals(base.gapRatio,changed.gapRatio,0f)
        assertEquals(base.outerInsetRatio,changed.outerInsetRatio,0f)
        assertTrue(original!=changed.rects())
        val scene=CanvasScene(frame=CanvasFrame(enabled=true,scope=CanvasFrameScope.Photos,
            colorArgb=0xFFFF0000.toInt(),widthRatio=.015f))
        assertEquals(CanvasBackgroundKind.Transparent,scene.background.kind)
        assertEquals(original,base.rects())
    }

    @Test fun previousProjectsDefaultToZeroOuterMarginAndDisabledFrame() {
        assertEquals(0f,CollageProject().outerInsetRatio,0f)
        val scene=CanvasScene()
        assertFalse(scene.singlePhotoCanvas)
        assertEquals(0f,scene.photoInsetRatio,0f)
        assertFalse(scene.frame.enabled)
        assertEquals(CanvasFrameScope.Photos,scene.frame.scope)
        assertEquals(.03f,CanvasFrame(widthRatio=.5f).normalized().widthRatio,0f)
        assertEquals(.001f,CanvasFrame(widthRatio=-1f).normalized().widthRatio,0f)
    }
}
