package com.tugongfang.editor.editor.image

import com.tugongfang.editor.model.layer.PhotoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geometry and memory bounding for the DEV27 antialiased eraser compositor. */
class PhotoStrokeLayerBoundsTest {
    @Test fun emptyAndInvalidPhotosCannotAllocateLayers() {
        assertNull(photoStrokeLayerBounds(emptyList(), 100, 100, 10f))
        assertNull(photoStrokeLayerBounds(listOf(PhotoPoint(.5f,.5f)), 0, 100, 10f))
        assertNull(photoStrokeLayerBounds(listOf(PhotoPoint(Float.NaN,.5f)), 100, 100, 10f))
    }

    @Test fun singlePointHasRoundCapAndTwoPixelAntialiasMargin() {
        val b = photoStrokeLayerBounds(listOf(PhotoPoint(.5f,.5f)), 200, 100, 20f)!!
        assertEquals(88f,b.left,0.001f)
        assertEquals(38f,b.top,0.001f)
        assertEquals(112f,b.right,0.001f)
        assertEquals(62f,b.bottom,0.001f)
    }

    @Test fun boundsAreClippedToPhotoWithoutDroppingEdgePixels() {
        val b=photoStrokeLayerBounds(listOf(PhotoPoint(0f,0f),PhotoPoint(1f,1f)),
            200, 300, 16f)!!
        assertEquals(0f,b.left,0f)
        assertEquals(0f,b.top,0f)
        assertEquals(200f,b.right,0f)
        assertEquals(300f,b.bottom,0f)
    }

    @Test fun smallStrokeDoesNotAllocateFullPhotoLayer() {
        val b=photoStrokeLayerBounds(listOf(PhotoPoint(.48f,.50f),PhotoPoint(.52f,.50f)),
            4096, 3072, 50f)!!
        assertTrue(b.right-b.left < 250f)
        assertTrue(b.bottom-b.top < 60f)
    }

    @Test fun preservesCoverageForEveryPointAndBrushCap() {
        val points=listOf(PhotoPoint(.21f,.83f),PhotoPoint(.38f,.64f),PhotoPoint(.79f,.12f))
        val radius=14f
        val b=photoStrokeLayerBounds(points,1200,2200,radius*2)!!
        assertNotNull(b)
        for (p in points) {
            val x=p.x*1200; val y=p.y*2200
            assertTrue(x-radius >= b.left-0.001f)
            assertTrue(x+radius <= b.right+0.001f)
            assertTrue(y-radius >= b.top-0.001f)
            assertTrue(y+radius <= b.bottom+0.001f)
        }
    }
}
