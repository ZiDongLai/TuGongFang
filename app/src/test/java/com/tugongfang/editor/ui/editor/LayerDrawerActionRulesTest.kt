package com.tugongfang.editor.ui.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEV20 regression: a locked layer must not expose destructive edits in the drawer. */
class LayerDrawerActionRulesTest {
    @Test fun lockedLayersOnlyAllowDuplicationFromActionMenu() {
        assertTrue(canUseLayerAction(true, "复制"))
        listOf("编辑", "替换", "删除").forEach {
            assertFalse(canUseLayerAction(true, it))
        }
    }

    @Test fun unlockedAndLocklessLayersKeepAllTheirActions() {
        listOf("编辑", "替换", "删除", "复制").forEach {
            assertTrue(canUseLayerAction(false, it))
            assertTrue(canUseLayerAction(null, it))
        }
    }
}
