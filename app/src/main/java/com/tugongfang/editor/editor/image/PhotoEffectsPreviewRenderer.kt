package com.tugongfang.editor.editor.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tugongfang.editor.model.layer.PhotoStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Shared resource budget for photo-local effect PREVIEWS across single and collage modes.
 * Keep full-resolution exports on ProjectExporter / PhotoEffectsRenderer.apply unchanged.
 * In a 9- or 12-image collage this avoids many large render allocations at once.
 */
internal object PhotoEffectsPreviewRenderer {
    private val gate = PhotoPreviewRenderLimiter(maxConcurrent = 2)

    suspend fun render(source: ImageBitmap, strokes: List<PhotoStroke>): ImageBitmap {
        if (strokes.isEmpty()) return source
        return withContext(Dispatchers.Default) {
            val original = source.asAndroidBitmap()
            val rendered = gate.run { PhotoEffectsRenderer.apply(original, strokes) }
            // Rasterization is synchronous; if a new edit cancelled this job,
            // don't retain its abandoned result while Compose renders a newer one.
            if (!currentCoroutineContext().isActive) {
                if (rendered !== original && !rendered.isRecycled) rendered.recycle()
                currentCoroutineContext().ensureActive()
            }
            rendered.asImageBitmap()
        }
    }
}
