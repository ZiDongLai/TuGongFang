package com.tugongfang.editor.project

import android.content.Context
import android.graphics.Bitmap
import android.util.AtomicFile
import com.tugongfang.editor.editor.export.ProjectExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Real project-composite thumbnails; never modifies editable layers or project.json. */
class ProjectThumbnailManager(private val context: Context) {
    private val root = File(context.filesDir, "tgf_projects")

    fun hasThumbnail(id: String): Boolean = thumbnailFile(id).let { it.isFile && it.length() > 0L }

    /** A stale thumbnail is only a display cache; project.json remains authoritative. */
    fun hasFreshThumbnail(id: String): Boolean {
        val preview = thumbnailFile(id)
        val json = File(File(root, id), "project.json")
        return preview.isFile && preview.length() > 0L && json.isFile &&
            ProjectSavePolicy.isThumbnailFresh(json.lastModified(), preview.lastModified())
    }

    private fun thumbnailFile(id: String): File {
        require(id.matches(Regex("[A-Za-z0-9-]{8,80}"))) { "Invalid project ID" }
        return File(File(root, id), "thumbnail.png")
    }

    suspend fun renderAndSave(project: LocalProject): Boolean = withContext(Dispatchers.IO) {
        // Compose card displays at most 320px; avoid decoding a 1400px intermediate for each save.
        val original = ProjectExporter.renderPreview(context, project.images, project.texts,
            project.info, project.shapes, project.collage, project.canvas,
            maxSourceLongEdge = ProjectSavePolicy.THUMBNAIL_SOURCE_LONG_EDGE).getOrElse { error ->
            if (error is CancellationException) throw error
            return@withContext false
        }
        try {
            val width = original.width.coerceAtLeast(1)
            val height = original.height.coerceAtLeast(1)
            val factor = (320f / maxOf(width, height)).coerceAtMost(1f)
            val outWidth = (width * factor).roundToInt().coerceAtLeast(1)
            val outHeight = (height * factor).roundToInt().coerceAtLeast(1)
            val small = if (width == outWidth && height == outHeight) original
                else Bitmap.createScaledBitmap(original, outWidth, outHeight, true)
            try {
                val file = AtomicFile(thumbnailFile(project.id))
                val stream = file.startWrite()
                try {
                    check(small.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Thumbnail encoding failed" }
                    file.finishWrite(stream)
                    true
                } catch (e: Exception) {
                    file.failWrite(stream)
                    throw e
                }
            } finally {
                if (small !== original && !small.isRecycled) small.recycle()
            }
        } finally {
            if (!original.isRecycled) original.recycle()
        }
    }
}
