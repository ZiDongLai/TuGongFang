package com.tugongfang.editor.editor.image

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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

class PhotoPreviewRenderLimiterTest {
    @Test fun atMostTwoRendersAtOnce() = runBlocking {
        val gate = PhotoPreviewRenderLimiter(2)
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val jobs = (1..24).map {
            async(Dispatchers.Default) {
                gate.run {
                    val n = active.incrementAndGet()
                    peak.updateAndGet { old -> maxOf(old, n) }
                    Thread.sleep(8)
                    active.decrementAndGet()
                }
            }
        }
        jobs.forEach { it.await() }
        assertTrue(peak.get() in 1..2)
        assertEquals(0, active.get())
    }

    @Test fun cancelledWaitingPreviewIsNotRendered() = runBlocking {
        val gate = PhotoPreviewRenderLimiter(1)
        val occupied = CompletableDeferred<Unit>()
        val first = launch(Dispatchers.Default) {
            gate.run {
                occupied.complete(Unit)
                Thread.sleep(90)
            }
        }
        occupied.await()
        val called = AtomicInteger()
        val second = launch(Dispatchers.Default) { gate.run { called.incrementAndGet() } }
        delay(10)
        second.cancelAndJoin()
        first.join()
        assertEquals(0, called.get())
    }

    @Test fun releasedPermitAfterRendererThrows() = runBlocking {
        val gate = PhotoPreviewRenderLimiter(1)
        try { gate.run<Unit> { error("fake render failed") } } catch (_: IllegalStateException) {}
        assertEquals(42, gate.run { 42 })
    }

    @Test fun emptyWorkDoesNotBlockAfterPreviousTasks() = runBlocking {
        val gate = PhotoPreviewRenderLimiter(2)
        assertEquals("ok", gate.run { "ok" })
    }
}
