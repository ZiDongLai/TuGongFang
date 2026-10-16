package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoStrokePointSamplerTest {
    @Test fun rejectsJitterInVisualPixels() {
        val p = PhotoStrokePointSampler(320f, 640f, 1.5f)
        assertTrue(p.add(PhotoPoint(.2f, .2f)))
        assertFalse(p.add(PhotoPoint(.201f, .201f)))
        assertTrue(p.add(PhotoPoint(.21f, .2f)))
        assertEquals(2, p.size)
    }

    @Test fun penUpKeepsLastCoordinate() {
        val p = PhotoStrokePointSampler(300f, 300f, 2f)
        p.add(PhotoPoint(.1f, .1f))
        p.add(PhotoPoint(.3f, .3f))
        p.add(PhotoPoint(.301f, .302f), penUp = true)
        assertEquals(2, p.size)
        assertEquals(.301f, p.snapshot().last().x, 0.00001f)
    }

    @Test fun tapIsOneSolidPoint() {
        val p = PhotoStrokePointSampler(300f, 600f)
        p.add(PhotoPoint(.4f, .3f))
        assertFalse(p.add(PhotoPoint(.4f, .3f), penUp = true))
        assertEquals(1, p.size)
    }

    @Test fun longStrokeKeepsBothEndpointsAndNeverExceedsBound() {
        val p = PhotoStrokePointSampler(3000f, 3000f, 0.5f, maxSamples = 32)
        for (i in 0..1000) p.add(PhotoPoint(i / 1000f, i / 1000f))
        p.add(PhotoPoint(1f, 1f), penUp = true)
        assertTrue(p.size <= 32)
        assertEquals(PhotoPoint(0f,0f), p.snapshot().first())
        assertEquals(PhotoPoint(1f,1f), p.snapshot().last())
    }

    @Test fun coordinatesClampedAndInvalidInputIgnored() {
        val p = PhotoStrokePointSampler(200f, 400f)
        assertFalse(p.add(PhotoPoint(Float.NaN, .2f)))
        p.add(PhotoPoint(-.4f, 2f))
        assertEquals(PhotoPoint(0f,1f), p.snapshot().single())
    }
}
