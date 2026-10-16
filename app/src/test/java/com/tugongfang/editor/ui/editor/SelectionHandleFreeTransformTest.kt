package com.tugongfang.editor.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionHandleFreeTransformTest {
    @Test fun horizontalDragChangesWidthWithoutFreezingAutoHeight() {
        val a = topRightTextFreeTransformDrag(268, 0, 314f, 260f, 30f, 0f, 2f)
        assertEquals(283, a.widthDp)
        assertEquals(0, a.minimumHeightDp)
    }
    @Test fun verticalDragChangesHeightWithoutChangingWidth() {
        val a = topRightTextFreeTransformDrag(268, 0, 314f, 260f, 0f, -20f, 2f)
        assertEquals(268, a.widthDp)
        assertEquals(100, a.minimumHeightDp)
    }
    @Test fun diagonalDragChangesBothAxesAndKeepsRotation() {
        val a = topRightTextFreeTransformDrag(268, 0, 314f, 260f, 20f, 15f, 2f)
        assertEquals(278, a.widthDp)
        assertEquals(83, a.minimumHeightDp)
        assertTrue(a.rotationDeltaDegrees.isFinite())
    }
    @Test fun resizeLimitsProtectOldProjects() {
        val a = topRightTextFreeTransformDrag(268, 0, 314f, 260f, -3000f, 3000f, 2f)
        assertEquals(88, a.widthDp)
        assertEquals(0, a.minimumHeightDp)
        val b = topRightTextFreeTransformDrag(268, 0, 314f, 260f, 3000f, -3000f, 2f)
        assertEquals(640, b.widthDp)
        assertEquals(720, b.minimumHeightDp)
    }
}
