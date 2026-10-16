package com.tugongfang.editor.project

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/** DEV22: an outdated cached thumbnail must not be confused with unsaved project data. */
class ProjectSavePolicyTest {
    @Test fun autosaveDoesNotWaitForThumbnail() {
        assertTrue(ProjectSavePolicy.AUTO_SAVE_IDLE_MS < ProjectSavePolicy.THUMBNAIL_IDLE_MS)
        assertTrue(ProjectSavePolicy.THUMBNAIL_SOURCE_LONG_EDGE >= 320)
        assertTrue(ProjectSavePolicy.THUMBNAIL_SOURCE_LONG_EDGE < 1400)
    }

    @Test fun staleAndMissingThumbnailMustRegenerate() {
        assertFalse(ProjectSavePolicy.isThumbnailFresh(2_000, 1_999))
        assertFalse(ProjectSavePolicy.isThumbnailFresh(2_000, 0))
        assertFalse(ProjectSavePolicy.isThumbnailFresh(0, 2_000))
        assertTrue(ProjectSavePolicy.isThumbnailFresh(2_000, 2_000))
        assertTrue(ProjectSavePolicy.isThumbnailFresh(2_000, 2_001))
    }

    @Test fun lastModifiedEdgesAreStable() {
        for (t in 1L..250L) {
            assertFalse(ProjectSavePolicy.isThumbnailFresh(t, t - 1L))
            assertTrue(ProjectSavePolicy.isThumbnailFresh(t, t))
            assertTrue(ProjectSavePolicy.isThumbnailFresh(t, t + 1L))
        }
    }
}
