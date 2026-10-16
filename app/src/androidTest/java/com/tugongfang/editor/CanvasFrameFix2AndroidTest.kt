package com.tugongfang.editor

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tugongfang.editor.editor.export.ProjectExporter
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasFrame
import com.tugongfang.editor.model.canvas.CanvasFrameCorner
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

@RunWith(AndroidJUnit4::class)
class CanvasFrameFix2AndroidTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun redPhoto(dir: File, name:String): ImageLayer {
        val bitmap=Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.RED)
            val f=File(dir,name)
            f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            return ImageLayer(uriString=Uri.fromFile(f).toString())
        } finally { bitmap.recycle() }
    }

    @Test fun roundedAndSquarePhotoFramesBothExportWithoutLegacyWhiteCorners()=runBlocking {
        val dir=File(context.cacheDir,"fix2_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val layers=(0..2).map { redPhoto(dir,"$it.png") }
            val collage=CollageProject(template=CollageTemplate.HeroThree,gapRatio=.035f,
                cornerRatio=.045f,outerInsetRatio=.05f,
                slots=layers.map { CollageSlot(imageId=it.id) })
            for(corner in CanvasFrameCorner.entries) {
                val frame=CanvasFrame(enabled=true,scope=CanvasFrameScope.Photos,
                    corner=corner,colorArgb=Color.MAGENTA,widthRatio=.021f)
                val scene=CanvasScene(
                    background=CanvasBackground(kind=CanvasBackgroundKind.Solid,
                        firstColor=Color.rgb(235,205,115)), frame=frame)
                val output=ProjectExporter.renderPreview(context,layers,emptyList(),emptyList(),
                    emptyList(),collage,scene,maxSourceLongEdge=900).getOrThrow()
                try {
                    val r=collage.rects().first()
                    val x=(r.left*output.width).toInt().coerceIn(0,output.width-1)
                    val y=(r.top*output.height).toInt().coerceIn(0,output.height-1)
                    // The old always-white outline made corner pixels white.
                    val cornerPixel=output.getPixel((x+1).coerceAtMost(output.width-1),
                        (y+1).coerceAtMost(output.height-1))
                    assertNotEquals("legacy white border still visible: $corner",Color.WHITE,cornerPixel)
                    val cx=((r.left+r.right)/2f*output.width).toInt()
                    val cy=((r.top+r.bottom)/2f*output.height).toInt()
                    assertEquals(Color.RED,output.getPixel(cx,cy))
                } finally { output.recycle() }
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun squareCornerAndCustomFrameColorSurviveProjectSaveAndLoad() {
        val store=com.tugongfang.editor.project.LocalProjectStore(context)
        val id=UUID.randomUUID().toString()
        try {
            val frame=CanvasFrame(enabled=true,corner=CanvasFrameCorner.Square,
                scope=CanvasFrameScope.Photos,colorArgb=0xFF8C74DD.toInt(),widthRatio=.013f)
            val original=com.tugongfang.editor.project.LocalProject(id=id,title="Fix2 frame test",
                images=emptyList(),texts=emptyList(),info=emptyList(),shapes=emptyList(),
                collage=null,canvas=CanvasScene(frame=frame))
            store.save(original)
            assertEquals(frame,store.load(id).canvas.frame)
        } finally { store.delete(id) }
    }
}
