package com.tugongfang.editor.camera

import android.graphics.BitmapFactory
import java.io.File

/** Private, full-resolution camera outputs. Never placed in public gallery unless exported. */
internal object CameraCaptureFiles {
    private const val FOLDER = "tgf_camera"
    private const val PREFIX = "TGF_CAP_"
    private const val SUFFIX = ".jpg"
    private const val STALE_MS = 24L * 60L * 60L * 1000L

    fun create(cacheDir: File): File {
        val dir = File(cacheDir, FOLDER)
        check(dir.isDirectory || dir.mkdirs()) { "无法创建临时照片目录" }
        return File.createTempFile(PREFIX, SUFFIX, dir)
    }

    /** Decode only the image header: no full-size bitmap allocation. */
    fun hasValidPhoto(photo: File): Boolean {
        if (!photo.isFile || photo.length() == 0L) return false
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.absolutePath, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        }.getOrDefault(false)
    }

    /** Delete old camera-cache files only; never touch project images or active capture. */
    fun pruneStale(cacheDir: File, activePath: String?, nowMs: Long = System.currentTimeMillis()): Int {
        val dir = File(cacheDir, FOLDER)
        var count = 0
        for (file in dir.listFiles().orEmpty()) {
            if (!file.isFile || !file.name.startsWith(PREFIX) || !file.name.endsWith(SUFFIX)) continue
            if (file.absolutePath == activePath) continue
            if (nowMs - file.lastModified() >= STALE_MS && file.delete()) count++
        }
        return count
    }
}
