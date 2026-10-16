package com.tugongfang.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tugongfang.editor.camera.CameraCaptureFiles
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.project.LocalProject
import com.tugongfang.editor.project.LocalProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** DEV37: verify URI grants, full-resolution image persistence and safe cleanup on real Android. */
@RunWith(AndroidJUnit4::class)
class CameraCaptureAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun providerCaptureOutputCanBeSavedBeforeTemporaryFileIsDeleted() {
        val photo = CameraCaptureFiles.create(context.cacheDir)
        val store = LocalProjectStore(context)
        var projectId: String? = null
        try {
            val cameraUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photo
            )
            assertEquals("content", cameraUri.scheme)
            val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.CYAN)
                context.contentResolver.openOutputStream(cameraUri, "w")!!.use { stream ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream))
                }
            } finally { bitmap.recycle() }
            assertTrue(CameraCaptureFiles.hasValidPhoto(photo))
            val layer = ImageLayer(uriString = Uri.fromFile(photo).toString())
            val project = LocalProject(
                id = UUID.randomUUID().toString(), title = "Camera Test",
                images = listOf(layer), texts = emptyList(), info = emptyList(),
                shapes = emptyList(), collage = null, canvas = CanvasScene(),
                selectedLayerId = layer.id
            )
            projectId = project.id
            store.save(project)
            val saved = store.load(project.id)
            assertEquals(1, saved.images.size)
            val copied = File(Uri.parse(saved.images.first().uriString).path!!)
            assertTrue(copied.isFile && copied.length() > 0)
            assertEquals(1200, android.graphics.BitmapFactory.Options().let { bounds ->
                bounds.inJustDecodeBounds = true
                android.graphics.BitmapFactory.decodeFile(copied.absolutePath, bounds)
                bounds.outWidth
            })
            assertTrue(photo.delete())
            assertTrue(copied.isFile && copied.length() > 0)
        } finally {
            projectId?.let { runCatching { store.delete(it) } }
            photo.delete()
        }
    }

    @Test fun canceledAndOldCameraOutputsAreCleanedWithoutTouchingOtherFiles() {
        val active = CameraCaptureFiles.create(context.cacheDir)
        val stale = CameraCaptureFiles.create(context.cacheDir)
        val unrelated = File(active.parentFile, "unrelated.txt").apply { writeText("keep") }
        try {
            val old = System.currentTimeMillis() - 26L * 60L * 60L * 1000L
            active.setLastModified(old)
            stale.setLastModified(old)
            val count = CameraCaptureFiles.pruneStale(context.cacheDir, active.absolutePath)
            assertTrue(count >= 1)
            assertTrue(active.exists())
            assertFalse(stale.exists())
            assertTrue(unrelated.exists())
        } finally {
            active.delete()
            stale.delete()
            unrelated.delete()
        }
    }
}
