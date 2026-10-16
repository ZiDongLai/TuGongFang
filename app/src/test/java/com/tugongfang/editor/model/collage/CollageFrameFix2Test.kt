package com.tugongfang.editor.model.collage

import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasScene
import org.junit.Assert.*
import org.junit.Test

class CollageFrameFix2Test {
    @Test fun oldDocumentsPreserveRoundedDefaultAndDisabledFrame() {
        val original = CanvasScene()
        assertFalse(original.frame.enabled)
        assertEquals(CanvasFrameCorner.Rounded, original.frame.corner)
    }

    @Test fun changingCornerNeverChangesAnyOfThe87Templates() {
        assertEquals(87,CollageTemplate.entries.size)
        CollageTemplate.entries.forEach { template ->
            val project=CollageProject(template=template,gapRatio=.02f,outerInsetRatio=.06f)
            val positions=project.rects()
            CanvasFrameCorner.entries.forEach { corner ->
                val scene=CanvasScene(frame=CanvasFrame(enabled=true,
                    corner=corner,scope=CanvasFrameScope.Photos))
                assertEquals(corner,scene.frame.normalized().corner)
                assertEquals(positions,project.rects())
            }
        }
    }

    @Test fun originalThicknessAndCustomHexColorSurviveCornerToggle() {
        val original=CanvasFrame(enabled=true,widthRatio=.028f,
            colorArgb=0xFF8C74DD.toInt(),corner=CanvasFrameCorner.Rounded)
        val square=original.copy(corner=CanvasFrameCorner.Square)
        assertEquals(0xFF8C74DD.toInt(),square.colorArgb)
        assertEquals(.028f,square.normalized().widthRatio,0f)
        assertTrue(square.enabled)
        assertEquals(CanvasFrameCorner.Square,square.normalized().corner)
    }
}
