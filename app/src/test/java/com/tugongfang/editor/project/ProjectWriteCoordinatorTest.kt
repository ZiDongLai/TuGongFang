package com.tugongfang.editor.project

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ProjectWriteCoordinatorTest {
    @Test fun onlyOneWriterEntersCriticalSection() = runBlocking {
        val writer = ProjectWriteCoordinator()
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val jobs = (1..20).map {
            launch {
                writer.run {
                    val n = active.incrementAndGet()
                    peak.updateAndGet { previous -> maxOf(previous, n) }
                    Thread.sleep(5)
                    active.decrementAndGet()
                }
            }
        }
        jobs.forEach { it.join() }
        assertEquals(1, peak.get())
        assertEquals(0, active.get())
    }

    @Test fun cancelledStaleSaveDoesNotOverwriteLatest() = runBlocking {
        val writer = ProjectWriteCoordinator()
        val entered = CompletableDeferred<Unit>()
        var persisted = ""
        val first = launch {
            writer.run {
                entered.complete(Unit)
                Thread.sleep(100)
                persisted = "old-written-first"
            }
        }
        entered.await()
        val stale = launch { writer.run { persisted = "obsolete" } }
        delay(10)
        stale.cancelAndJoin()
        first.join()
        writer.run { persisted = "newest" }
        assertEquals("newest", persisted)
    }

    @Test fun failedWriteDoesNotDeadlockNextSave() = runBlocking {
        val writer = ProjectWriteCoordinator()
        try { writer.run { error("disk full") } } catch (_: IllegalStateException) {}
        assertEquals(7, writer.run { 7 })
    }
}
