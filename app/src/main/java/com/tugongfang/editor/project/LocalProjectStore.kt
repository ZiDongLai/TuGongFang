package com.tugongfang.editor.project

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.tugongfang.editor.model.canvas.CanvasBackground
import com.tugongfang.editor.model.canvas.CanvasBackgroundKind
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.model.collage.CollageAspect
import com.tugongfang.editor.model.collage.CollageFitMode
import com.tugongfang.editor.model.collage.CollageMode
import com.tugongfang.editor.model.collage.CollageProject
import com.tugongfang.editor.model.collage.CollageSlot
import com.tugongfang.editor.model.collage.CollageTemplate
import com.tugongfang.editor.model.layer.DoodleBrush
import com.tugongfang.editor.model.layer.PhotoStroke
import com.tugongfang.editor.model.layer.PhotoPoint
import com.tugongfang.editor.model.layer.PhotoStrokeKind
import com.tugongfang.editor.model.layer.sanitized
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.InfoTemplateType
import com.tugongfang.editor.model.layer.ShapeLayer
import com.tugongfang.editor.model.layer.ShapePoint
import com.tugongfang.editor.model.layer.ShapeType
import com.tugongfang.editor.model.layer.SymbolKind
import com.tugongfang.editor.model.layer.TextLayer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Project files live in app-private storage. No network and no storage permission required. */
data class LocalProject(
    val id: String,
    val title: String,
    val images: List<ImageLayer>,
    val texts: List<TextLayer>,
    val info: List<InfoPanelLayer>,
    val shapes: List<ShapeLayer>,
    val collage: CollageProject?,
    val canvas: CanvasScene,
    val selectedLayerId: String? = null,
    val updatedAt: Long = 0L
)

data class ProjectCardInfo(
    val id: String,
    val title: String,
    val modifiedAt: Long,
    val photoCount: Int,
    val thumbnailPath: String? = null
)

/** Versioned JSON project + local copies of the original images (not flattened JPEGs). */
class LocalProjectStore(private val context: Context) {
    private val root = File(context.filesDir, "tgf_projects")

    @Synchronized
    fun listProjects(): List<ProjectCardInfo> = root.listFiles().orEmpty()
        .filter { it.isDirectory && it.name.matches(Regex("[a-zA-Z0-9-]{8,80}")) }
        .mapNotNull { folder ->
            runCatching {
                val file = File(folder, "project.json")
                if (!file.isFile) return@runCatching null
                val json = JSONObject(AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
                if (json.optInt("schema", 0) != 1) return@runCatching null
                ProjectCardInfo(
                    id = folder.name,
                    title = json.optString("title", "未命名项目"),
                    modifiedAt = json.optLong("updatedAt", file.lastModified()),
                    photoCount = json.optJSONArray("images")?.length() ?: 0,
                    thumbnailPath = File(folder, "thumbnail.png").takeIf { it.isFile && it.length() > 0 }?.absolutePath
                )
            }.getOrNull()
        }.sortedByDescending { it.modifiedAt }

    /** Atomic project file replacement. Copy first; never replace a valid document on failure. */
    @Synchronized
    fun save(project: LocalProject): ProjectCardInfo {
        require(project.id.matches(Regex("[a-zA-Z0-9-]{8,80}"))) { "无效的项目编号" }
        val folder = File(root, project.id)
        val mediaDir = File(folder, "media")
        check(mediaDir.isDirectory || mediaDir.mkdirs()) { "无法创建项目保存目录" }
        val localImages = project.images.map { image ->
            val uri = Uri.parse(image.uriString)
            val original = if (uri.scheme == "file") {
                val path = uri.path?.let(::File)
                if (path?.isFile == true && path.canonicalPath.startsWith(folder.canonicalPath + File.separator)) uri
                else copyImageToProject(image, mediaDir)
            } else copyImageToProject(image, mediaDir)
            image.copy(uriString = original.toString())
        }
        // Make the chosen gallery background portable just like all foreground photos.
        val chosenBg = project.canvas.background
        val localCanvas = when {
            chosenBg.kind == CanvasBackgroundKind.Image && chosenBg.imageUri.isNullOrBlank() ->
                project.canvas.copy(background=chosenBg.copy(kind=CanvasBackgroundKind.Transparent))
            chosenBg.kind == CanvasBackgroundKind.Image -> {
                val saved = copyImageToProject(ImageLayer(id="canvas_background",uriString=chosenBg.imageUri!!),mediaDir)
                project.canvas.copy(background=chosenBg.copy(imageUri=saved.toString()))
            }
            else -> project.canvas
        }
        val time = System.currentTimeMillis()
        val doc = JSONObject()
            .put("schema", 1).put("id", project.id)
            .put("title", project.title).put("updatedAt", time)
            .put("selectedLayerId", project.selectedLayerId)
            .put("images", JSONArray().apply { localImages.forEach { put(it.toJson()) } })
            .put("texts", JSONArray().apply { project.texts.forEach { put(it.toJson()) } })
            .put("info", JSONArray().apply { project.info.forEach { put(it.toJson()) } })
            .put("shapes", JSONArray().apply { project.shapes.forEach { put(it.toJson()) } })
            .put("collage", project.collage?.toJson())
            .put("canvas", localCanvas.toJson())
        val atomicFile = AtomicFile(File(folder, "project.json"))
        val stream = atomicFile.startWrite()
        try {
            stream.write(doc.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }
        return ProjectCardInfo(project.id, project.title, time, localImages.size,
            File(folder, "thumbnail.png").takeIf { it.isFile && it.length() > 0 }?.absolutePath)
    }

    /** Rename the stored document without touching photo URIs or edit layers. */
    @Synchronized
    fun rename(id: String, title: String): ProjectCardInfo {
        require(id.matches(Regex("[a-zA-Z0-9-]{8,80}"))) { "无效项目编号" }
        val clean = title.trim()
        require(clean.isNotEmpty() && clean.length <= 40 && clean.none { it.isISOControl() }) {
            "项目名称应为1到40个字符"
        }
        val file = File(File(root, id), "project.json")
        val json = JSONObject(AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        check(json.optInt("schema", 0) == 1 && json.optString("id") == id) { "项目文件异常" }
        val now = System.currentTimeMillis()
        json.put("title", clean).put("updatedAt", now)
        val atomic = AtomicFile(file)
        val out = atomic.startWrite()
        try {
            out.write(json.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(out)
        } catch (e: Exception) {
            atomic.failWrite(out)
            throw e
        }
        return ProjectCardInfo(id, clean, now, json.optJSONArray("images")?.length() ?: 0,
            File(File(root, id), "thumbnail.png").takeIf { it.isFile && it.length() > 0 }?.absolutePath)
    }

    /** A confirmed delete removes the private project folder and its copied images. */
    @Synchronized
    fun delete(id: String) {
        require(id.matches(Regex("[a-zA-Z0-9-]{8,80}"))) { "无效项目编号" }
        val folder = File(root, id)
        check(folder.isDirectory && File(folder, "project.json").isFile) { "未找到需要删除的项目" }
        check(folder.deleteRecursively()) { "项目删除未完成，请检查存储空间" }
    }

    /** Images use immutable per-URI filenames, so replacing a picture doesn't corrupt undo history. */
    private fun copyImageToProject(image: ImageLayer, mediaDir: File): Uri {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(image.uriString.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        val id = image.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val output = File(mediaDir, "${id}_$digest.img")
        if (!output.exists() || output.length() == 0L) {
            val tmp = File(mediaDir, "${id}_${digest}.part")
            try {
                context.contentResolver.openInputStream(Uri.parse(image.uriString))?.use { source ->
                    tmp.outputStream().use { target -> source.copyTo(target, 64 * 1024) }
                } ?: error("无法读取原始图片，请重新从相册添加")
                check(tmp.length() > 0L && tmp.renameTo(output)) { "图片保存到项目失败" }
            } finally { tmp.delete() }
        }
        return Uri.fromFile(output)
    }

    @Synchronized
    fun load(id: String): LocalProject {
        require(id.matches(Regex("[a-zA-Z0-9-]{8,80}"))) { "无效的项目编号" }
        val json = JSONObject(AtomicFile(File(File(root, id), "project.json"))
            .openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(json.optInt("schema", 0) == 1 && json.optString("id") == id) { "项目文件版本不兼容" }
        val images = json.array("images").objects { it.toImage() }
        // Do not silently load a project with missing photos, as saving it could lose the missing layers.
        images.forEach { layer ->
            val uri = Uri.parse(layer.uriString)
            check(uri.scheme == "file" && uri.path?.let { File(it).isFile } == true) {
                "项目图片丢失，无法安全恢复：${layer.id}"
            }
        }
        val restoredCanvas = json.optJSONObject("canvas")?.toCanvas() ?: CanvasScene()
        val bg = restoredCanvas.background
        if (bg.kind == CanvasBackgroundKind.Image && !bg.imageUri.isNullOrBlank()) {
            val uri = Uri.parse(bg.imageUri)
            check(uri.scheme == "file" && uri.path?.let { File(it).isFile } == true) {
                "项目背景图片丢失，请从备份恢复"
            }
        }
        return LocalProject(
            id = id, title = json.optString("title", "未命名项目"),
            images = images,
            texts = json.array("texts").objects { it.toText() },
            info = json.array("info").objects { it.toInfo() },
            shapes = json.array("shapes").objects { it.toShape() },
            collage = json.optJSONObject("collage")?.toCollage(),
            canvas = restoredCanvas,
            selectedLayerId = json.optionalString("selectedLayerId"),
            updatedAt = json.optLong("updatedAt", 0L)
        )
    }
}

private fun JSONObject.array(key: String) = optJSONArray(key) ?: JSONArray()
private inline fun <T> JSONArray.objects(map: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { optJSONObject(it)?.let(map) }
private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JSONObject.number(key: String, default: Float): Float = optDouble(key, default.toDouble()).toFloat()
private inline fun <reified E: Enum<E>> JSONObject.choice(key: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == optString(key) } ?: default

private fun ImageLayer.toJson() = JSONObject()
    .put("id", id).put("uri", uriString).put("x", offsetXRatio).put("y", offsetYRatio)
    .put("scale", scale).put("rotation", rotation).put("visible", visible).put("locked", locked)
    .put("flipH", flipHorizontal).put("flipV", flipVertical)
    .put("cropL", cropLeft).put("cropT", cropTop).put("cropR", cropRight).put("cropB", cropBottom)
    .put("brightness", brightness).put("contrast", contrast).put("saturation", saturation)
    .put("temperature", temperature).put("tint", tint)
    .put("highlights", highlights).put("shadows", shadows)
    .put("freeFitMode", freeFitMode.name)
    .put("photoStrokes", JSONArray().apply { photoStrokes.forEach { stroke ->
        put(JSONObject().put("kind",stroke.kind.name).put("size",stroke.sizeRatio)
            .put("block",stroke.mosaicBlockRatio).put("color",stroke.colorArgb)
            .put("erase",stroke.isEraser)
            .put("points",JSONArray().apply { stroke.points.forEach { point ->
                put(JSONObject().put("x",point.x).put("y",point.y)) } })) } })
private fun JSONObject.toImage() = ImageLayer(
    id = optString("id"), uriString = optString("uri"),
    offsetXRatio = number("x", 0f), offsetYRatio = number("y", 0f),
    scale = number("scale", 1f), rotation = number("rotation", 0f),
    visible = optBoolean("visible", true), locked = optBoolean("locked", false),
    flipHorizontal = optBoolean("flipH", false), flipVertical = optBoolean("flipV", false),
    cropLeft = number("cropL", 0f), cropTop = number("cropT", 0f),
    cropRight = number("cropR", 1f), cropBottom = number("cropB", 1f),
    brightness = number("brightness", 0f).coerceIn(-1f,1f),
    contrast = number("contrast", 0f).coerceIn(-1f,1f),
    saturation = number("saturation", 0f).coerceIn(-1f,1f),
    temperature = number("temperature", 0f).coerceIn(-1f,1f),
    tint = number("tint", 0f).coerceIn(-1f,1f),
    highlights = number("highlights", 0f).coerceIn(-1f,1f),
    shadows = number("shadows", 0f).coerceIn(-1f,1f),
    freeFitMode = choice("freeFitMode", CollageFitMode.Fit),
    photoStrokes = array("photoStrokes").objects { obj -> PhotoStroke(
        kind = obj.choice("kind", PhotoStrokeKind.Mosaic),
        points = obj.array("points").objects { point ->
            PhotoPoint(point.number("x",0f),point.number("y",0f)) },
        sizeRatio = obj.number("size",.055f),
        mosaicBlockRatio = obj.number("block",.025f),
        colorArgb = obj.optInt("color", 0xFF3A78FF.toInt()),
        isEraser = obj.optBoolean("erase", false)
    ).sanitized() }.take(1200)
)

private fun TextLayer.toJson() = JSONObject()
    .put("id", id).put("text", text).put("x", offsetXRatio).put("y", offsetYRatio)
    .put("scale", scale).put("rotation", rotation).put("visible", visible).put("locked", locked)
    .put("fontSize", fontSizeSp).put("bold", bold)
    .put("italic", italic).put("underline", underline)
    .put("textBackground", backgroundColorArgb)
    .put("fontFamily", fontFamilyId).put("lineSpacing", lineSpacing)
    .put("boxWidthDp", boxWidthDp).put("boxHeightDp", boxHeightDp)
    .put("color", colorArgb).put("align", textAlign)
private fun JSONObject.toText() = TextLayer(
    id = optString("id"), text = optString("text"), offsetXRatio = number("x", 0f),
    offsetYRatio = number("y", 0f), scale = number("scale", 1f), rotation = number("rotation", 0f),
    visible = optBoolean("visible", true), locked = optBoolean("locked", false),
    fontSizeSp = number("fontSize", 32f), bold = optBoolean("bold", false),
    italic = optBoolean("italic", false), underline = optBoolean("underline", false),
    backgroundColorArgb = optInt("textBackground", 0x00000000),
    fontFamilyId = optInt("fontFamily", 0).coerceIn(0, 2),
    lineSpacing = number("lineSpacing", 1.22f).coerceIn(1f, 2f),
    boxWidthDp = optInt("boxWidthDp", 268).coerceIn(88, 640),
    boxHeightDp = optInt("boxHeightDp", 0).coerceIn(0, 720),
    colorArgb = optInt("color", 0xFF202536.toInt()), textAlign = optInt("align", 1)
)

private fun InfoPanelLayer.toJson() = JSONObject()
    .put("id", id).put("templateType", templateType.name).put("name", customTemplateName)
    .put("date", date).put("autoDate", autoDate).put("primary", primary)
    .put("secondary", secondary).put("note", note).put("positionTop", positionTop)
    .put("overlayMode", overlayMode).put("infoX", offsetXRatio).put("infoY", offsetYRatio)
    .put("infoWidth", widthRatio).put("infoHeight", heightRatio)
    .put("infoRotation", rotation).put("infoDark", darkBackground)
    .put("infoFont", fontFamilyId).put("infoTextScale", textScale)
    .put("visible", visible)
private fun JSONObject.toInfo() = InfoPanelLayer(
    id = optString("id"), templateType = choice("templateType", InfoTemplateType.MovieContinuity),
    customTemplateName = optString("name", "自定义模板"),
    date = optString("date", ""), autoDate = optBoolean("autoDate", true),
    primary = optString("primary"), secondary = optString("secondary"), note = optString("note"),
    positionTop = optBoolean("positionTop", false),
    overlayMode = optBoolean("overlayMode", false),
    offsetXRatio = number("infoX", 0f).coerceIn(-.5f, .5f),
    offsetYRatio = number("infoY", .26f).coerceIn(-.5f, .5f),
    widthRatio = number("infoWidth", .82f).coerceIn(.28f, 1f),
    heightRatio = number("infoHeight", .26f).coerceIn(.16f, .88f),
    rotation = number("infoRotation", 0f).coerceIn(-180f, 180f),
    darkBackground = optBoolean("infoDark", false),
    fontFamilyId = optInt("infoFont", 0).coerceIn(0, 2),
    textScale = number("infoTextScale", 1f).coerceIn(.90f, 1.15f),
    visible = optBoolean("visible", true)
)

private fun ShapeLayer.toJson() = JSONObject()
    .put("id", id).put("type", type.name).put("symbol", symbolKind.name)
    .put("x", offsetXRatio).put("y", offsetYRatio).put("width", widthRatio).put("height", heightRatio)
    .put("scale", scale).put("rotation", rotation).put("color", colorArgb)
    .put("stroke", strokeWidth).put("filled", filled).put("rounded", roundedCorners)
    .put("circle", perfectCircle)
    .put("points", JSONArray().apply { points.forEach { put(JSONObject().put("x", it.x).put("y", it.y)) } })
    .put("session", doodleSessionId).put("brush", doodleBrush.name).put("opacity", doodleOpacity)
    .put("parent", parentImageId).put("aspect", attachmentCanvasAspect)
    .put("visible", visible).put("locked", locked)
private fun JSONObject.toShape() = ShapeLayer(
    id = optString("id"), type = choice("type", ShapeType.Rectangle),
    symbolKind = choice("symbol", SymbolKind.Check),
    offsetXRatio = number("x", 0f), offsetYRatio = number("y", 0f),
    widthRatio = number("width", .42f), heightRatio = number("height", .18f),
    scale = number("scale", 1f), rotation = number("rotation", 0f),
    colorArgb = optInt("color", 0xFF3A78FF.toInt()), strokeWidth = number("stroke", 5f),
    filled = optBoolean("filled", false), roundedCorners = optBoolean("rounded", true),
    perfectCircle = optBoolean("circle", false),
    points = array("points").objects { ShapePoint(it.number("x", 0f), it.number("y", 0f)) },
    doodleSessionId = optionalString("session"),
    doodleBrush = choice("brush", DoodleBrush.Pencil),
    doodleOpacity = number("opacity", 1f),
    parentImageId = optionalString("parent"),
    attachmentCanvasAspect = number("aspect", 1f),
    visible = optBoolean("visible", true), locked = optBoolean("locked", false)
)

private fun CollageProject.toJson() = JSONObject()
    .put("mode", mode.name).put("template", template.name).put("aspect", aspect.name)
    .put("gap", gapRatio).put("corner", cornerRatio).put("outer", outerInsetRatio).put("active", activeSlot)
    .put("slots", JSONArray().apply {
        slots.forEach { slot -> put(JSONObject().put("image", slot.imageId)
            .put("zoom", slot.zoom).put("panX", slot.panX).put("panY", slot.panY)
            .put("rotation", slot.rotation).put("fit", slot.fitMode.name)) }
    })
private fun JSONObject.toCollage() = CollageProject(
    mode = choice("mode", CollageMode.Template),
    template = choice("template", CollageTemplate.FourGrid),
    aspect = choice("aspect", CollageAspect.Square),
    gapRatio = number("gap", .012f), cornerRatio = number("corner", .025f),
    outerInsetRatio = number("outer", 0f),
    slots = array("slots").objects { obj -> CollageSlot(
        imageId = obj.optionalString("image"), zoom = obj.number("zoom", 1f),
        panX = obj.number("panX", 0f), panY = obj.number("panY", 0f),
        rotation = obj.number("rotation", 0f), fitMode = obj.choice("fit", CollageFitMode.Fill)
    ) }, activeSlot = optInt("active", 0)
).normalized()

private fun CanvasScene.toJson() = JSONObject().put("aspect", aspectRatio)
    .put("photoInset", photoInsetRatio.coerceIn(0f,.20f))
    .put("singlePhotoCanvas", singlePhotoCanvas)
    .put("frame", JSONObject().put("enabled", frame.enabled).put("scope", frame.scope.name)
        .put("color",frame.colorArgb).put("width",frame.widthRatio).put("corner",frame.corner.name))
    .put("background", JSONObject().put("kind", background.kind.name)
        .put("first", background.firstColor).put("second", background.secondColor)
        .put("direction", background.gradientDirection.name)
        .put("imageUri", background.imageUri)
        .put("fit", background.imageFit.name)
        .put("blur", background.blur.coerceIn(0,100))
        .put("opacity", background.opacity.coerceIn(0f,1f)))
private fun JSONObject.toCanvas(): CanvasScene {
    val bg = optJSONObject("background") ?: JSONObject()
    return CanvasScene(CanvasBackground(
        kind = bg.choice("kind", CanvasBackgroundKind.Transparent),
        firstColor = bg.optInt("first", 0xFFFFFFFF.toInt()),
        secondColor = bg.optInt("second", 0xFFACC8F4.toInt()),
        gradientDirection = bg.choice("direction", com.tugongfang.editor.model.canvas.BackgroundGradientDirection.Diagonal),
        imageUri = bg.optionalString("imageUri"),
        imageFit = bg.choice("fit", com.tugongfang.editor.model.canvas.BackgroundImageFit.Fill),
        blur = bg.optInt("blur",0).coerceIn(0,100),
        opacity = bg.number("opacity",1f).coerceIn(0f,1f)
    ), aspectRatio = number("aspect", 0f),
        photoInsetRatio = number("photoInset",0f).coerceIn(0f,.20f),
        singlePhotoCanvas = optBoolean("singlePhotoCanvas",false),
        frame = (optJSONObject("frame") ?: JSONObject()).let { f ->
            com.tugongfang.editor.model.canvas.CanvasFrame(
                enabled = f.optBoolean("enabled",false),
                scope = f.choice("scope",com.tugongfang.editor.model.canvas.CanvasFrameScope.Photos),
                colorArgb = f.optInt("color",0xFFFFFFFF.toInt()),
                widthRatio = f.number("width",.005f).coerceIn(.001f,.03f),
                corner = f.choice("corner",com.tugongfang.editor.model.canvas.CanvasFrameCorner.Rounded))
        })
}
