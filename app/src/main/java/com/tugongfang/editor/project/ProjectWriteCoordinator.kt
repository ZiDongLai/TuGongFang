package com.tugongfang.editor.project

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes pending auto-save, leave-editor, and reselect-photo writes.
 * Unlike a synchronized file writer, a cancelled *waiting* coroutine never starts
 * writing an obsolete snapshot after a newer snapshot has been saved.
 */
internal class ProjectWriteCoordinator {
    private val gate = Mutex()

    suspend fun <T> run(write: () -> T): T = gate.withLock {
        currentCoroutineContext().ensureActive()
        write()
    }
}
