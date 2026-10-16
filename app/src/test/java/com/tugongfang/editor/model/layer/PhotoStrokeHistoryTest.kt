package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Protect mixed-type undo/redo and order preservation during non-destructive editing. */
class PhotoStrokeHistoryTest {
    private fun stroke(kind: PhotoStrokeKind, x: Float): PhotoStroke =
        PhotoStroke(kind, listOf(PhotoPoint(x, .5f)))

    @Test fun currentToolUndoDoesNotEraseOtherTool() {
        val mosaic1 = stroke(PhotoStrokeKind.Mosaic, .1f)
        val doodle = stroke(PhotoStrokeKind.Doodle, .2f)
        val mosaic2 = stroke(PhotoStrokeKind.Mosaic, .3f)
        val source = listOf(mosaic1, doodle, mosaic2)
        val result = source.undoLatestOf(PhotoStrokeKind.Doodle)
        assertEquals(listOf(mosaic1, mosaic2), result.remaining)
        assertEquals(1, result.removed!!.originalIndex)
        assertEquals(source, result.remaining.redoRemoved(result.removed))
    }

    @Test fun undoStackThenRedoStackRestoresExactOriginalOrder() {
        val source = listOf(stroke(PhotoStrokeKind.Doodle, .11f),
            stroke(PhotoStrokeKind.Mosaic, .22f),
            stroke(PhotoStrokeKind.Doodle, .33f),
            stroke(PhotoStrokeKind.Mosaic, .44f))
        val u1 = source.undoLatestOf(PhotoStrokeKind.Mosaic)
        val u2 = u1.remaining.undoLatestOf(PhotoStrokeKind.Mosaic)
        assertEquals(source.filter { it.kind == PhotoStrokeKind.Doodle }, u2.remaining)
        val redone = u2.remaining.redoRemoved(u2.removed!!).redoRemoved(u1.removed!!)
        assertEquals(source, redone)
    }

    @Test fun noMatchingStrokeIsNoOp() {
        val source = listOf(stroke(PhotoStrokeKind.Doodle, .9f))
        val result = source.undoLatestOf(PhotoStrokeKind.Mosaic)
        assertSame(source, result.remaining)
        assertNull(result.removed)
    }

    @Test fun safelyClampReinsertIndexWhenOlderListWasTrimmed() {
        val original = stroke(PhotoStrokeKind.Mosaic, .35f)
        val current = listOf(stroke(PhotoStrokeKind.Doodle, .7f))
        assertEquals(listOf(current[0], original), current.redoRemoved(RemovedPhotoStroke(99, original)))
    }
}
