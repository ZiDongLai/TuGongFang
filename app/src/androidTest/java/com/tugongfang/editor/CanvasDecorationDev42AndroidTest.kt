package com.tugongfang.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tugongfang.editor.editor.export.ProjectExporter
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameScope
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.collage.CollageProject
import com.tugongfang.editor.model.collage.CollageSlot
import com.tugongfang.editor.model.collage.CollageTemplate
import com.tugongfang.editor.model.layer.ImageLayer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Test generated color images only. Never changes real user projects. */
@RunWith(AndroidJUnit4::class)
class CanvasDecorationDev42AndroidTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext

    private fun image(file: File,color:Int): ImageLayer {
        val bitmap=Bitmap.createBitmap(48,60,Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(color)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        } finally { bitmap.recycle() }
        return ImageLayer(uriString=Uri.fromFile(file).toString())
    }

    @Test fun collageBackgroundAppearsInActualPreviewBehindAllTiles()= runBlocking {
        val dir=File(context.cacheDir,"dev42_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val photos=(0..2).map { i -> image(File(dir,"$i.png"),android.graphics.Color.RED) }
            val collage=CollageProject(template=CollageTemplate.HeroThree,gapRatio=.018f,
                outerInsetRatio=.09f,slots=photos.map { CollageSlot(imageId=it.id) })
            val scene=CanvasScene(background=CanvasBackground(kind=CanvasBackgroundKind.Solid,
                firstColor=android.graphics.Color.GREEN))
            val output=ProjectExporter.renderPreview(context,photos,emptyList(),emptyList(),
                emptyList(),collage,scene,maxSourceLongEdge=680).getOrThrow()
            try {
                assertEquals(android.graphics.Color.GREEN,output.getPixel(3,3))
                assertEquals(android.graphics.Color.RED,output.getPixel(output.width/2,output.height/2))
            } finally { output.recycle() }
        } finally { dir.deleteRecursively() }
    }

    @Test fun singlePhotoInsetRendersBackgroundAndPhotoBorderInExportPath()=runBlocking {
        val dir=File(context.cacheDir,"dev42_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val photo=image(File(dir,"single.png"),android.graphics.Color.RED)
            val scene=CanvasScene(background=CanvasBackground(kind=CanvasBackgroundKind.Solid,
                firstColor=android.graphics.Color.BLUE),photoInsetRatio=.08f,singlePhotoCanvas=true,
                frame=CanvasFrame(enabled=true,scope=CanvasFrameScope.Photos,
                    colorArgb=android.graphics.Color.YELLOW,widthRatio=.008f))
            val output=ProjectExporter.renderPreview(context,listOf(photo),emptyList(),emptyList(),
                emptyList(),canvasScene=scene,maxSourceLongEdge=680).getOrThrow()
            try {
                assertEquals(android.graphics.Color.BLUE,output.getPixel(2,2))
                assertEquals(android.graphics.Color.RED,output.getPixel(output.width/2,output.height/2))
                assertTrue(output.width>48 && output.height>60)
            } finally { output.recycle() }
        } finally { dir.deleteRecursively() }
    }
    @Test fun projectSaveReloadRestoresDecorationWithoutChangingPhotoSlots() {
        val dir=File(context.cacheDir,"dev42_${UUID.randomUUID()}").apply { mkdirs() }
        val store=com.tugongfang.editor.project.LocalProjectStore(context)
        val id=UUID.randomUUID().toString()
        try {
            val photo=image(File(dir,"source.png"),android.graphics.Color.RED)
            val collage=CollageProject(template=CollageTemplate.OnePhoto,
                gapRatio=.03f,outerInsetRatio=.11f,slots=listOf(CollageSlot(imageId=photo.id)))
            val frame=CanvasFrame(enabled=true,scope=CanvasFrameScope.Canvas,
                colorArgb=android.graphics.Color.MAGENTA,widthRatio=.013f)
            val scene=CanvasScene(background=CanvasBackground(kind=CanvasBackgroundKind.Gradient),
                frame=frame,photoInsetRatio=.04f,singlePhotoCanvas=false)
            store.save(com.tugongfang.editor.project.LocalProject(id=id,title="DEV42 test",
                images=listOf(photo),texts=emptyList(),info=emptyList(),shapes=emptyList(),
                collage=collage,canvas=scene))
            val restored=store.load(id)
            assertEquals(collage.template,restored.collage!!.template)
            assertEquals(collage.outerInsetRatio,restored.collage!!.outerInsetRatio,0.0001f)
            assertEquals(collage.gapRatio,restored.collage!!.gapRatio,0.0001f)
            assertEquals(collage.slots.single().imageId,restored.collage!!.slots.single().imageId)
            assertEquals(scene.frame,restored.canvas.frame)
            assertEquals(scene.background.kind,restored.canvas.background.kind)
            assertEquals(scene.photoInsetRatio,restored.canvas.photoInsetRatio,0.0001f)
        } finally { store.delete(id);dir.deleteRecursively() }
    }

}
