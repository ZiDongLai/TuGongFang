package com.tugongfang.editor.project

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Portable editable project backups. The backup never stores absolute photo paths. */
class ProjectBackupManager(private val context: Context) {
    private val projectRoot = File(context.filesDir, "tgf_projects")
    private val idPattern = Regex("[a-zA-Z0-9-]{8,80}")
    private val imageEntryPattern = Regex("media/image_[0-9]{1,3}\\.img")
    private val maxImageBytes = 100L * 1024L * 1024L
    private val maxArchiveData = 600L * 1024L * 1024L
    private val maxDocumentBytes = 4L * 1024L * 1024L

    /** Export the JSON and all original photo bytes, not a flattened image. */
    @Synchronized
    fun exportProject(id: String, destination: OutputStream) {
        require(idPattern.matches(id)) { "无效项目编号" }
        val folder = File(projectRoot, id)
        val projectFile = File(folder, "project.json")
        val doc = JSONObject(projectFile.readText(Charsets.UTF_8))
        require(doc.optInt("schema") == 1 && doc.optString("id") == id) { "项目数据格式不正确" }
        val photos = doc.optJSONArray("images") ?: error("项目缺少照片列表")
        require(photos.length() <= 100) { "项目照片数量超过备份上限" }
        val references = ArrayList<Pair<File, String>>(photos.length())
        var totalImageBytes = 0L
        for (i in 0 until photos.length()) {
            val photo = photos.getJSONObject(i)
            val uri = Uri.parse(photo.getString("uri"))
            val path = uri.path?.let(::File) ?: error("第${i + 1}张照片路径无效")
            require(uri.scheme == "file" && path.isFile) { "第${i + 1}张照片已丢失，请重新保存项目" }
            require(path.canonicalPath.startsWith(folder.canonicalPath + File.separator)) {
                "照片还没有安全保存到项目内，请先打开编辑页重新保存"
            }
            require(path.length() > 0 && path.length() <= maxImageBytes) { "备份照片为空或超过大小上限" }
            totalImageBytes += path.length()
            require(totalImageBytes <= maxArchiveData) { "项目照片总容量超过本地备份上限" }
            val relative = "media/image_${i}.img"
            photo.put("uri", relative)
            references += path to relative
        }
        val canvasBg = doc.optJSONObject("canvas")?.optJSONObject("background")
        if (canvasBg?.optString("kind") == "Image" &&
            canvasBg.optString("imageUri").isNotBlank()) {
            val uri = Uri.parse(canvasBg.optString("imageUri"))
            val path = uri.path?.let(::File) ?: error("背景图片路径无效")
            require(uri.scheme == "file" && path.isFile &&
                path.canonicalPath.startsWith(folder.canonicalPath + File.separator)) {
                "请先在编辑器保存背景图片，再导出项目备份"
            }
            require(path.length() > 0 && path.length() <= maxImageBytes) { "背景图片大小不合法" }
            totalImageBytes += path.length()
            require(totalImageBytes <= maxArchiveData) { "项目媒体总量超过备份限制" }
            canvasBg.put("imageUri", "media/background.img")
            references += path to "media/background.img"
        }
        doc.put("backupFormat", "TuGongFangProject")
            .put("backupVersion", 1)
        val json = doc.toString().toByteArray(Charsets.UTF_8)
        require(json.size.toLong() <= maxDocumentBytes) { "项目元数据过大" }
        ZipOutputStream(BufferedOutputStream(destination)).use { archive ->
            archive.putNextEntry(ZipEntry("project.json"))
            archive.write(json)
            archive.closeEntry()
            references.forEach { (imageFile, relativePath) ->
                archive.putNextEntry(ZipEntry(relativePath))
                imageFile.inputStream().buffered().use { it.copyTo(archive, 64 * 1024) }
                archive.closeEntry()
            }
        }
    }

    /** Validate paths/sizes before publishing an imported project. Existing projects are never overwritten. */
    @Synchronized
    fun importProject(source: InputStream): ProjectCardInfo {
        val temp = File(context.filesDir, "tgf_import_${UUID.randomUUID()}")
        val tempMedia = File(temp, "media")
        check(tempMedia.mkdirs()) { "无法创建临时目录" }
        var published = false
        try {
            var jsonBytes: ByteArray? = null
            val names = HashSet<String>()
            var archiveData = 0L
            var photoCount = 0
            ZipInputStream(BufferedInputStream(source)).use { archive ->
                while (true) {
                    val entry = archive.nextEntry ?: break
                    val name = entry.name
                    require(!entry.isDirectory && names.add(name)) { "备份中含有重复或非法文件" }
                    when {
                        name == "project.json" -> {
                            jsonBytes = readBounded(archive, maxDocumentBytes)
                            archiveData += jsonBytes!!.size
                        }
                        imageEntryPattern.matches(name) || name == "media/background.img" -> {
                            if(name == "media/background.img") require(!File(temp,name).exists()) { "重复的背景图片" }
                            if (name != "media/background.img")
                                require(++photoCount <= 100) { "备份照片数量过多" }
                            val file = File(temp, name)
                            var size = 0L
                            file.outputStream().buffered().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val n = archive.read(buffer)
                                    if (n < 0) break
                                    size += n
                                    archiveData += n
                                    require(size <= maxImageBytes && archiveData <= maxArchiveData) {
                                        "备份文件过大"
                                    }
                                    output.write(buffer, 0, n)
                                }
                            }
                            require(file.length() > 0) { "备份包含空白照片文件" }
                        }
                        else -> error("备份包含不支持的文件：$name")
                    }
                    require(archiveData <= maxArchiveData) { "备份文件过大" }
                    archive.closeEntry()
                }
            }
            val bytes = jsonBytes ?: error("未找到项目备份数据")
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            require(json.optString("backupFormat") == "TuGongFangProject" &&
                json.optInt("backupVersion") == 1 && json.optInt("schema") == 1) {
                "请选择图工坊导出的项目备份 ZIP 文件"
            }
            val images = json.optJSONArray("images") ?: error("备份照片列表缺失")
            require(images.length() == photoCount) { "备份照片数量不一致" }
            // DEV30: validate the complete one-to-one mapping, not only counts.
            // A broken ZIP could contain two files but point both JSON layers to image_0.
            // Never publish a partially restored project in this situation.
            ProjectArchiveIntegrity.validateImageReferences(
                photoUris = (0 until images.length()).map { images.getJSONObject(it).optString("uri") },
                extractedImageEntries = names.filterTo(HashSet()) { imageEntryPattern.matches(it) }
            )
            val newId = UUID.randomUUID().toString()
            check(projectRoot.isDirectory || projectRoot.mkdirs()) { "无法创建项目目录" }
            val destination = File(projectRoot, newId)
            require(!destination.exists()) { "恢复项目编号重复" }
            for (i in 0 until images.length()) {
                val item = images.getJSONObject(i)
                val relative = item.optString("uri")
                require(imageEntryPattern.matches(relative)) { "照片引用路径异常" }
                val photo = File(temp, relative)
                require(photo.isFile && photo.length() > 0) { "备份图片丢失：$relative" }
                item.put("uri", Uri.fromFile(File(destination, relative)).toString())
            }
            val canvasBg = json.optJSONObject("canvas")?.optJSONObject("background")
            if (canvasBg?.optString("kind") == "Image" &&
                canvasBg.optString("imageUri").isNotBlank()) {
                require(canvasBg.optString("imageUri") == "media/background.img") { "背景图片引用路径不合法" }
                val file = File(temp,"media/background.img")
                require(file.isFile && file.length() > 0) { "背景图片缺失" }
                canvasBg.put("imageUri",Uri.fromFile(File(destination,"media/background.img")).toString())
            } else require(!File(temp,"media/background.img").exists()) { "备份包含未引用的背景图片" }
            val originalTitle = json.optString("title", "未命名项目").trim().ifBlank { "未命名项目" }
            val title = "${originalTitle.take(33)}（导入）"
            val now = System.currentTimeMillis()
            json.put("id", newId).put("title", title).put("updatedAt", now)
                .remove("backupFormat")
            json.remove("backupVersion")
            val atomicFile = AtomicFile(File(temp, "project.json"))
            val write = atomicFile.startWrite()
            try {
                write.write(json.toString().toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(write)
            } catch (error: Exception) {
                atomicFile.failWrite(write)
                throw error
            }
            check(temp.renameTo(destination)) { "恢复项目写入失败" }
            published = true
            return ProjectCardInfo(newId, title, now, images.length())
        } finally {
            if (!published) temp.deleteRecursively()
        }
    }

    private fun readBounded(input: InputStream, maximum: Long): ByteArray {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(bytes.size().toLong() + count <= maximum) { "备份元数据超出允许范围" }
            bytes.write(buffer, 0, count)
        }
        return bytes.toByteArray()
    }
}
