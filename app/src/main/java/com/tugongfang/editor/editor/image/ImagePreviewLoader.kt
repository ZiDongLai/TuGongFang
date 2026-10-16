package com.tugongfang.editor.editor.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlin.math.max

/**
 * 编辑阶段只载入适合预览的位图，避免大照片长期占用过多内存。
 * 原始 Uri 保留在 ImageLayer 中，高清导出时仍读取原图。
 * Single-photo and collage previews share a two-decode memory budget.
 */
private val sharedPreviewDecoder = PhotoPreviewDecodeCoordinator(maxConcurrent = 2)

@Composable
fun rememberImagePreview(
    context: Context,
    uriString: String?,
    maxDimension: Int = 2048
): State<ImageBitmap?> = produceState<ImageBitmap?>(
    initialValue = null,
    key1 = uriString,
    key2 = maxDimension
) {
    if (uriString.isNullOrBlank()) {
        value = null
        return@produceState
    }

    value = try {
        sharedPreviewDecoder.decode(
            create = { decodePreviewBitmap(context, Uri.parse(uriString), maxDimension) },
            discard = { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        )?.asImageBitmap()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        // Failed / inaccessible photo is displayed as its existing fallback tile.
        null
    }
}

private fun decodePreviewBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int
): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > maxDimension) {
                val ratio = maxDimension.toFloat() / longest.toFloat()
                decoder.setTargetSize(
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        decodeLegacy(context, uri, maxDimension)
    }
}

private fun decodeLegacy(
    context: Context,
    uri: Uri,
    maxDimension: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}
