package com.tugongfang.editor.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEV30: prevent plausible-looking yet damaged editable-project ZIP imports. */
class ProjectArchiveIntegrityTest {
    private fun paths(count: Int) = (0 until count).map { "media/image_${it}.img" }

    private fun rejects(photos: List<String>, files: Set<String>) {
        val failed = runCatching { ProjectArchiveIntegrity.validateImageReferences(photos, files) }.exceptionOrNull()
        assertTrue("corrupted ZIP must be rejected", failed is IllegalArgumentException)
    }

    @Test fun validExportLayoutsIncludingNoImagesAndTwelvePhotos() {
        listOf(0, 1, 2, 12, 100).forEach { count ->
            val refs = paths(count)
            ProjectArchiveIntegrity.validateImageReferences(refs, refs.toSet())
        }
        assertEquals(100, ProjectArchiveIntegrity.MAX_IMAGES)
    }

    @Test fun reorderedReferencesRemainValidWhenAllFilesExist() {
        val refs = paths(4)
        ProjectArchiveIntegrity.validateImageReferences(refs.reversed(), refs.toSet())
    }

    @Test fun duplicateReferencesCannotSilentlyImportWrongPhoto() {
        rejects(listOf("media/image_0.img", "media/image_0.img"), paths(2).toSet())
    }

    @Test fun missingPhotoFileFailsBeforePublishingProject() {
        rejects(paths(3), paths(2).toSet())
    }

    @Test fun unrelatedImageFilesCannotStandInForMissingFile() {
        rejects(paths(2), setOf("media/image_0.img", "media/image_9.img"))
    }

    @Test fun extraUnreferencedPhotoFileIsNotAccepted() {
        rejects(paths(2), paths(3).toSet())
    }

    @Test fun referencesMustStayInsideCanonicalZipMediaFolder() {
        rejects(listOf("../image_0.img"), setOf("media/image_0.img"))
        rejects(listOf("/media/image_0.img"), setOf("media/image_0.img"))
        rejects(listOf("media\\image_0.img"), setOf("media/image_0.img"))
    }

    @Test fun sparseArchiveIsNotSilentlyAccepted() {
        rejects(listOf("media/image_0.img", "media/image_9.img"),
            setOf("media/image_0.img", "media/image_9.img"))
    }

    @Test fun exceedingHundredPhotosIsRejected() {
        rejects(paths(101), paths(101).toSet())
    }
}
