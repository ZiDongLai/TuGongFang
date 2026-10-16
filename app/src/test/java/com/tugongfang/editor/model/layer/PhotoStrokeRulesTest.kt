package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoStrokeRulesTest {
    @Test fun oldPhotoHasNoStrokes() {
        val layer = ImageLayer(uriString = "file:///data/photo.jpg")
        assertTrue(layer.photoStrokes.isEmpty())
    }

    @Test fun sanitationClampsPointsAndBrushSettings() {
        val stroke = PhotoStroke(PhotoStrokeKind.Mosaic,
            listOf(PhotoPoint(-4f, 2.1f), PhotoPoint(.5f, .5f)),
            sizeRatio = 2f, mosaicBlockRatio = -.1f)
        val clean = stroke.sanitized()
        assertEquals(0f, clean.points[0].x, 0.0001f)
        assertEquals(1f, clean.points[0].y, 0.0001f)
        assertEquals(.5f, clean.points[1].x, 0.0001f)
        assertEquals(.35f, clean.sizeRatio, 0.0001f)
        assertEquals(.004f, clean.mosaicBlockRatio, 0.0001f)
    }

    @Test fun duplicatePhotoCanKeepIndependentStrokes() {
        val original = ImageLayer(uriString = "file:///photo.jpg",
            photoStrokes = listOf(PhotoStroke(PhotoStrokeKind.Doodle, listOf(PhotoPoint(.2f,.7f)))))
        val next = original.copy(uriString = "file:///replacement.jpg", photoStrokes = emptyList())
        assertEquals(1, original.photoStrokes.size)
        assertTrue(next.photoStrokes.isEmpty())
    }
}
