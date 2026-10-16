package com.tugongfang.editor.editor.image

import android.graphics.Bitmap
import android.graphics.Matrix
import com.tugongfang.editor.model.layer.ImageLayer
import kotlin.math.roundToInt

/** 拼图预览与导出沿用相同的基础裁切、翻转、图片旋转逻辑。 */
fun prepareCollagePreview(source: Bitmap, layer: ImageLayer): Bitmap {
    val left=(layer.cropLeft.coerceIn(0f,1f)*source.width).roundToInt().coerceIn(0,source.width-1)
    val top=(layer.cropTop.coerceIn(0f,1f)*source.height).roundToInt().coerceIn(0,source.height-1)
    val right=(layer.cropRight.coerceIn(0f,1f)*source.width).roundToInt().coerceIn(left+1,source.width)
    val bottom=(layer.cropBottom.coerceIn(0f,1f)*source.height).roundToInt().coerceIn(top+1,source.height)
    val cropped=Bitmap.createBitmap(source,left,top,right-left,bottom-top)
    if (!layer.flipHorizontal && !layer.flipVertical && kotlin.math.abs(layer.rotation)<.01f) return cropped
    val matrix=Matrix().apply {
        postScale(if(layer.flipHorizontal)-1f else 1f,if(layer.flipVertical)-1f else 1f,
            cropped.width/2f,cropped.height/2f)
        postRotate(layer.rotation,cropped.width/2f,cropped.height/2f)
    }
    return Bitmap.createBitmap(cropped,0,0,cropped.width,cropped.height,matrix,true)
}
