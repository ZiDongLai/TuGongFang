package com.tugongfang.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.project.LocalProject
import com.tugongfang.editor.project.LocalProjectStore
import com.tugongfang.editor.project.ProjectBackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * DEV30 real-Android smoke tests: touch only temporary test projects, never the
 * user's existing projects. Run with :app:connectedDebugAndroidTest.
 */
@RunWith(AndroidJUnit4::class)
class ProjectArchiveRoundTripAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createPhoto(file: File, color: Int) {
        val image = Bitmap.createBitmap(9, 7, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(color)
            file.outputStream().use { out ->
                check(image.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
        } finally { image.recycle() }
    }

    private fun createTestProject(files: List<File>): LocalProject = LocalProject(
        id = UUID.randomUUID().toString(),
        title = "DEV30 Backup Test",
        images = files.map { ImageLayer(uriString = Uri.fromFile(it).toString()) },
        texts = emptyList(),
        info = emptyList(),
        shapes = emptyList(),
        collage = null,
        canvas = CanvasScene()
    )

    private fun damagedArchive(original: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(original)).use { reader ->
            ZipOutputStream(output).use { writer ->
                while (true) {
                    val entry = reader.nextEntry ?: break
                    val data = reader.readBytes()
                    writer.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == "project.json") {
                        val json = JSONObject(data.toString(Charsets.UTF_8))
                        json.getJSONArray("images").getJSONObject(1)
                            .put("uri", "media/image_0.img")
                        writer.write(json.toString().toByteArray(Charsets.UTF_8))
                    } else writer.write(data)
                    writer.closeEntry()
                    reader.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }

    @Test fun backedUpPhotosRestoreIntoIndependentEditableProject() {
        val store = LocalProjectStore(context)
        val backup = ProjectBackupManager(context)
        val temp = File(context.cacheDir, "dev30_${UUID.randomUUID()}")
        check(temp.mkdirs())
        val photo = File(temp, "original.png")
        var originalId: String? = null
        var importedId: String? = null
        try {
            createPhoto(photo, android.graphics.Color.CYAN)
            val project = createTestProject(listOf(photo))
            originalId = project.id
            store.save(project)
            val output = ByteArrayOutputStream()
            backup.exportProject(project.id, output)
            val imported = backup.importProject(ByteArrayInputStream(output.toByteArray()))
            importedId = imported.id
            assertTrue(imported.id != project.id)
            val restored = store.load(imported.id)
            assertEquals(1, restored.images.size)
            assertTrue(File(Uri.parse(restored.images.first().uriString).path!!).length() > 0L)
            assertEquals("DEV30 Backup Test（导入）", restored.title)
        } finally {
            importedId?.let { store.delete(it) }
            originalId?.let { store.delete(it) }
            temp.deleteRecursively()
        }
    }

    @Test fun duplicateZipPhotoReferencesAreRejectedWithoutPublishing() {
        val store = LocalProjectStore(context)
        val backup = ProjectBackupManager(context)
        val temp = File(context.cacheDir, "dev30_${UUID.randomUUID()}")
        check(temp.mkdirs())
        val images = listOf(File(temp, "1.png"), File(temp, "2.png"))
        var originalId: String? = null
        try {
            images.forEachIndexed { index, file ->
                createPhoto(file, if (index == 0) android.graphics.Color.RED else android.graphics.Color.BLUE)
            }
            val project = createTestProject(images)
            originalId = project.id
            store.save(project)
            val output = ByteArrayOutputStream()
            backup.exportProject(project.id, output)
            val before = store.listProjects().map { it.id }.toSet()
            val failure = runCatching {
                backup.importProject(ByteArrayInputStream(damagedArchive(output.toByteArray())))
            }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertEquals(before, store.listProjects().map { it.id }.toSet())
        } finally {
            originalId?.let { store.delete(it) }
            temp.deleteRecursively()
        }
    }
}
