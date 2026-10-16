package com.tugongfang.editor.editor.image

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Bounds source-photo decoding separately from effect preview rendering.
 * A stale preview cancelled while ImageDecoder/BitmapFactory was busy is discarded
 * immediately after the non-suspending decode finishes; queued work never starts.
 * No bitmap cache is retained across projects or editor sessions.
 */
internal class PhotoPreviewDecodeCoordinator(
    maxConcurrent: Int = 2
) {
    private val gate = PhotoPreviewRenderLimiter(maxConcurrent)

    suspend fun <T : Any> decode(
        create: () -> T?,
        discard: (T) -> Unit
    ): T? = withContext(Dispatchers.IO) {
        val decodeContext = currentCoroutineContext()
        gate.run {
            val decoded = create() ?: return@run null
            try {
                decodeContext.ensureActive()
                decoded
            } catch (cancel: CancellationException) {
                discard(decoded)
                throw cancel
            }
        }
    }
}
