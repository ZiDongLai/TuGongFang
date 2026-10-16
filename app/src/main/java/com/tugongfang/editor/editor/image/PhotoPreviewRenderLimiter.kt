package com.tugongfang.editor.editor.image

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Caps concurrent expensive photo effect previews, not high-resolution exports.
 * Queued, obsolete composition requests can be cancelled before they allocate bitmaps.
 */
internal class PhotoPreviewRenderLimiter(maxConcurrent: Int = 2) {
    private val permits = Semaphore(maxConcurrent.also { require(it > 0) })

    suspend fun <T> run(render: () -> T): T = permits.withPermit {
        currentCoroutineContext().ensureActive()
        render()
    }
}
