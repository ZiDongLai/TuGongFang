package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Non-destructive photo erasing and mixed-tool chronological history regression. */
class PhotoStrokeEraserTest {
    private fun mark(kind: PhotoStrokeKind, erase: Boolean = false) = PhotoStroke(
        kind = kind, points = listOf(PhotoPoint(.5f, .5f)), isEraser = erase
    )

    @Test fun oldSavedBrushesDefaultToPaintMode() {
        assertFalse(mark(PhotoStrokeKind.Mosaic).isEraser)
        assertFalse(PhotoStroke(PhotoStrokeKind.Doodle, emptyList()).isEraser)
    }

    @Test fun eraserIsStillBoundToTheSelectedPhotoTool() {
        val source = listOf(mark(PhotoStrokeKind.Doodle), mark(PhotoStrokeKind.Mosaic),
            mark(PhotoStrokeKind.Doodle, erase = true))
        val undo = source.undoLatestOf(PhotoStrokeKind.Doodle)
        assertTrue(undo.removed!!.stroke.isEraser)
        assertEquals(source, undo.remaining.redoRemoved(undo.removed!!))
        assertEquals(mark(PhotoStrokeKind.Mosaic), undo.remaining.last())
    }

    @Test fun mosaicEraserUndoesWithoutRemovingDoodle() {
        val doodle = mark(PhotoStrokeKind.Doodle)
        val mosaic = mark(PhotoStrokeKind.Mosaic)
        val erase = mark(PhotoStrokeKind.Mosaic, erase = true)
        val source = listOf(doodle, mosaic, erase)
        val undo = source.undoLatestOf(PhotoStrokeKind.Mosaic)
        assertEquals(erase, undo.removed!!.stroke)
        assertEquals(listOf(doodle, mosaic), undo.remaining)
        assertEquals(source, undo.remaining.redoRemoved(undo.removed!!))
    }

    @Test fun newPaintAfterEraserRemainsAVisibleActionInHistory() {
        val first = mark(PhotoStrokeKind.Mosaic)
        val erase = mark(PhotoStrokeKind.Mosaic, erase = true)
        val after = mark(PhotoStrokeKind.Mosaic)
        val source = listOf(first, erase, after)
        val undo = source.undoLatestOf(PhotoStrokeKind.Mosaic)
        assertEquals(after, undo.removed!!.stroke)
        assertEquals(listOf(first, erase), undo.remaining)
        assertEquals(source, undo.remaining.redoRemoved(undo.removed!!))
    }

    @Test fun sanitationPreservesEraseAction() {
        val erase = PhotoStroke(PhotoStrokeKind.Doodle,
            listOf(PhotoPoint(-2f, 5f)), sizeRatio=2f, isEraser=true).sanitized()
        assertTrue(erase.isEraser)
        assertEquals(0f, erase.points.single().x, 0f)
        assertEquals(1f, erase.points.single().y, 0f)
        assertEquals(.35f, erase.sizeRatio, .0001f)
    }
}
