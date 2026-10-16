package com.tugongfang.editor.editor.image

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PhotoPreviewDecodeCoordinatorTest {
    @Test fun onlyTwoSourceDecodesRunAtOnce() = runBlocking {
        val decoder = PhotoPreviewDecodeCoordinator(2)
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val tasks = (1..24).map {
            async {
                decoder.decode(create = {
                    val count = active.incrementAndGet()
                    peak.updateAndGet { old -> maxOf(old, count) }
                    Thread.sleep(8)
                    active.decrementAndGet()
                    it
                }, discard = {})
            }
        }
        tasks.forEach { it.await() }
        assertTrue(peak.get() in 1..2)
        assertEquals(0, active.get())
    }

    @Test fun cancelledQueuedDecodeDoesNotStart() = runBlocking {
        val decoder = PhotoPreviewDecodeCoordinator(1)
        val entered = CompletableDeferred<Unit>()
        val first = launch {
            decoder.decode(create = {
                entered.complete(Unit)
                Thread.sleep(100)
                1
            }, discard = {})
        }
        entered.await()
        val started = AtomicInteger()
        val second = launch {
            decoder.decode(create = { started.incrementAndGet(); 2 }, discard = {})
        }
        delay(10)
        second.cancelAndJoin()
        first.join()
        assertEquals(0, started.get())
    }

    @Test fun cancelledDecodeDiscardsJustProducedBitmap() = runBlocking {
        val decoder = PhotoPreviewDecodeCoordinator(1)
        val entered = CompletableDeferred<Unit>()
        val discarded = AtomicInteger()
        val job = launch {
            decoder.decode(create = {
                entered.complete(Unit)
                Thread.sleep(110) // BitmapFactory/ImageDecoder is synchronous
                Any()
            }, discard = { discarded.incrementAndGet() })
        }
        entered.await()
        delay(10)
        job.cancelAndJoin()
        assertEquals(1, discarded.get())
    }

    @Test fun decoderCanRecoverAfterFailure() = runBlocking {
        val decoder = PhotoPreviewDecodeCoordinator(1)
        try { decoder.decode<Any>(create = { error("bad file") }, discard = {}) }
        catch (_: IllegalStateException) {}
        assertEquals(42, decoder.decode(create = { 42 }, discard = {}))
    }

    @Test fun missingBitmapReturnsNull() = runBlocking {
        val decoder = PhotoPreviewDecodeCoordinator(1)
        assertEquals(null, decoder.decode<Any>(create = { null }, discard = {}))
    }
}
