package com.tugongfang.editor

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.compose.runtime.saveable.rememberSaveable
import com.tugongfang.editor.camera.CameraCaptureFiles
import java.io.File
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tugongfang.editor.editor.export.ProjectExporter
import com.tugongfang.editor.model.collage.CollageProject
import com.tugongfang.editor.model.collage.CollageMode
import com.tugongfang.editor.model.collage.CollageSlot
import com.tugongfang.editor.model.collage.CollageTemplate
import com.tugongfang.editor.model.collage.collageForInitialPhotos
import com.tugongfang.editor.model.collage.defaultTemplateForPhotoCount
import com.tugongfang.editor.model.canvas.CanvasScene
import com.tugongfang.editor.editor.export.ExportImageFormat
import com.tugongfang.editor.model.layer.ImageLayer
import com.tugongfang.editor.model.layer.TextLayer
import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.ShapeLayer
import com.tugongfang.editor.model.layer.followImageTransform
import com.tugongfang.editor.ui.editor.EditorScreen
import com.tugongfang.editor.ui.home.HomeScreen
import com.tugongfang.editor.ui.flow.PhotoSelectionScreen
import com.tugongfang.editor.ui.export.ExportResultDialog
import com.tugongfang.editor.ui.export.ExportPreviewDialog
import com.tugongfang.editor.ui.theme.TuGongFangTheme
import com.tugongfang.editor.ui.theme.ThemeMode
import com.tugongfang.editor.ui.theme.ThemeModeStore
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.widget.Toast
import com.tugongfang.editor.project.LocalProject
import com.tugongfang.editor.project.LocalProjectStore
import com.tugongfang.editor.project.ProjectBackupManager
import com.tugongfang.editor.project.ProjectThumbnailManager
import com.tugongfang.editor.project.ProjectCardInfo
import com.tugongfang.editor.project.ProjectSavePolicy
import com.tugongfang.editor.project.ProjectWriteCoordinator
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themeStore = ThemeModeStore(applicationContext)
        setContent {
            var themeMode by remember { mutableStateOf(themeStore.read()) }
            val darkTheme = themeMode == ThemeMode.DARK
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            TuGongFangTheme(darkTheme = darkTheme) {
                val uiScope = rememberCoroutineScope()
                val projectStore = remember { LocalProjectStore(applicationContext) }
                val thumbnailManager = remember { ProjectThumbnailManager(applicationContext) }
                val backupManager = remember { ProjectBackupManager(applicationContext) }
                val projectWriter = remember { ProjectWriteCoordinator() }
                var projectList by remember { mutableStateOf<List<ProjectCardInfo>>(emptyList()) }
                var activeProjectId by remember { mutableStateOf(UUID.randomUUID().toString()) }
                var activeProjectTitle by remember { mutableStateOf("未命名项目") }
                var page by remember { mutableStateOf(AppPage.Home) }
                var selectionMode by remember { mutableStateOf(PhotoSelectionMode.CreateCollage) }
                var draftPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var appendOnPhotoSelection by remember { mutableStateOf(false) }
                var exportedUriString by remember { mutableStateOf<String?>(null) }
                var layers by remember { mutableStateOf<List<ImageLayer>>(emptyList()) }
                var textLayers by remember { mutableStateOf<List<TextLayer>>(emptyList()) }
                var infoLayers by remember { mutableStateOf<List<InfoPanelLayer>>(emptyList()) }
                var shapeLayers by remember { mutableStateOf<List<ShapeLayer>>(emptyList()) }
                var collage by remember { mutableStateOf<CollageProject?>(null) }
                var canvasScene by remember { mutableStateOf(CanvasScene()) }
                var selectedLayerId by remember { mutableStateOf<String?>(null) }
                var exportPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var editorCloseInProgress by remember { mutableStateOf(false) }

                var history by remember {
                    mutableStateOf(listOf(EditorSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), null)))
                }
                var redoStack by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
                // 恢复历史时仅跳过恢复产生的那一帧，不吞掉紧接着发生的新编辑。
                var restoredSnapshot by remember { mutableStateOf<EditorSnapshot?>(null) }

                fun snapshotNow() = EditorSnapshot(
                    layers, textLayers, infoLayers, shapeLayers, selectedLayerId, collage, canvasScene
                )

                fun commitHistoryNow() {
                    val snapshot = snapshotNow()
                    if (!history.lastOrNull().sameDocument(snapshot)) {
                        history = (history + snapshot).takeLast(MAX_EDITOR_HISTORY)
                        redoStack = emptyList()
                    }
                }

                // v0.9.0 项目持久化：只存业务数据和原始图片副本，撤销历史仍属于当前编辑会话。
                fun projectDocument(snapshot: EditorSnapshot): LocalProject = LocalProject(
                    id = activeProjectId, title = activeProjectTitle,
                    images = snapshot.layers, texts = snapshot.textLayers,
                    info = snapshot.infoLayers, shapes = snapshot.shapeLayers,
                    collage = snapshot.collage, canvas = snapshot.canvasScene,
                    selectedLayerId = snapshot.selectedLayerId
                )

                LaunchedEffect(page) {
                    if (page == AppPage.Home) {
                        val loaded = withContext(Dispatchers.IO) { projectStore.listProjects() }
                        projectList = loaded
                        // Lazy one-time migration of older projects without thumbnails. Keep home responsive.
                        for (item in loaded) {
                            if (!thumbnailManager.hasFreshThumbnail(item.id)) {
                                try {
                                    val project = withContext(Dispatchers.IO) { projectStore.load(item.id) }
                                    thumbnailManager.renderAndSave(project)
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    // A failed thumbnail never blocks opening the original project.
                                }
                            }
                        }
                        projectList = withContext(Dispatchers.IO) { projectStore.listProjects() }
                    }
                }

                // DEV22: save the editable JSON and original images promptly; don't re-render
                // a composite preview for each pause during typing/dragging.
                LaunchedEffect(page, activeProjectId, layers, textLayers, infoLayers, shapeLayers, collage, canvasScene) {
                    if (page != AppPage.Editor) return@LaunchedEffect
                    val document = projectDocument(snapshotNow())
                    delay(ProjectSavePolicy.AUTO_SAVE_IDLE_MS)
                    try {
                        withContext(Dispatchers.IO) { projectWriter.run { projectStore.save(document) } }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        Toast.makeText(this@MainActivity,
                            "自动保存失败：${error.message ?: "请检查存储空间"}", Toast.LENGTH_LONG).show()
                    }
                }

                // Thumbnails are only a cache; refresh them after a longer idle period.
                // Back/closeEditor still writes the document and refreshes the card immediately.
                LaunchedEffect(page, activeProjectId, layers, textLayers, infoLayers, shapeLayers, collage, canvasScene) {
                    if (page != AppPage.Editor) return@LaunchedEffect
                    val projectId = activeProjectId
                    delay(ProjectSavePolicy.THUMBNAIL_IDLE_MS)
                    try {
                        if (thumbnailManager.hasFreshThumbnail(projectId)) return@LaunchedEffect
                        val persisted = withContext(Dispatchers.IO) { projectStore.load(projectId) }
                        thumbnailManager.renderAndSave(persisted)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        // Thumbnail failure must never block edits or turn a successful save into an error.
                        // A missing/stale preview will be rebuilt lazily when Home next opens.
                    }
                }

                fun closeEditor() {
                    if (page != AppPage.Editor || editorCloseInProgress) return
                    editorCloseInProgress = true
                    commitHistoryNow()
                    val document = projectDocument(snapshotNow())
                    uiScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                projectWriter.run { projectStore.save(document) }
                            }
                            val stored = withContext(Dispatchers.IO) { projectStore.load(document.id) }
                            try {
                                thumbnailManager.renderAndSave(stored)
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (_: Exception) {
                                // Thumbnail is a cache; document has already been saved.
                            }
                            page = AppPage.Home
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (error: Exception) {
                            Toast.makeText(this@MainActivity,
                                "保存失败，仍停留在编辑页：${error.message ?: "请检查存储空间"}",
                                Toast.LENGTH_LONG).show()
                        } finally {
                            editorCloseInProgress = false
                        }
                    }
                }

                BackHandler(enabled = page == AppPage.Editor && exportedUriString == null && !editorCloseInProgress) { closeEditor() }
                BackHandler(enabled = page == AppPage.PhotoSelect) {
                    page = if (selectionMode == PhotoSelectionMode.ReselectCollage) AppPage.Editor else AppPage.Home
                }

                var pickerMode by remember { mutableStateOf(PickerMode.Add) }
                var replaceTargetId by remember { mutableStateOf<String?>(null) }
                var replaceCollageSlot by remember { mutableStateOf<Int?>(null) }

                fun setProjectState(snapshot: EditorSnapshot, suppressHistory: Boolean) {
                    if (suppressHistory) restoredSnapshot = snapshot
                    layers = snapshot.layers
                    textLayers = snapshot.textLayers
                    infoLayers = snapshot.infoLayers
                    shapeLayers = snapshot.shapeLayers
                    collage = snapshot.collage
                    canvasScene = snapshot.canvasScene
                    selectedLayerId = snapshot.selectedLayerId
                }

                fun resetProject(
                    newLayers: List<ImageLayer>,
                    selectedId: String?,
                    newTextLayers: List<TextLayer> = emptyList(),
                    newInfoLayers: List<InfoPanelLayer> = emptyList(),
                    newShapeLayers: List<ShapeLayer> = emptyList()
                ) {
                    val snapshot = EditorSnapshot(newLayers, newTextLayers, newInfoLayers, newShapeLayers, selectedId)
                    restoredSnapshot = snapshot
                    layers = newLayers
                    textLayers = newTextLayers
                    infoLayers = newInfoLayers
                    shapeLayers = newShapeLayers
                    collage = null
                    canvasScene = CanvasScene()
                    selectedLayerId = selectedId
                    history = listOf(snapshot)
                    redoStack = emptyList()
                }

                // 连续拖动合并为单次记录；连续独立操作可存储多步（最多 100 步）。
                // 还原状态时使用内容比较，防止把撤销本身再次写进历史。
                LaunchedEffect(layers, textLayers, infoLayers, shapeLayers, collage, canvasScene) {
                    val candidate = snapshotNow()
                    val restored = restoredSnapshot
                    if (restored != null) {
                        restoredSnapshot = null
                        if (restored.sameDocument(candidate)) return@LaunchedEffect
                    }
                    delay(260)
                    if (!history.lastOrNull().sameDocument(candidate)) {
                        history = (history + candidate).takeLast(MAX_EDITOR_HISTORY)
                        redoStack = emptyList()
                    }
                }

                fun handlePickedImage(uri: android.net.Uri?) {
                    if (uri == null) return
                    commitHistoryNow()

                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    when (pickerMode) {
                        PickerMode.NewProject -> {
                            activeProjectId = UUID.randomUUID().toString()
                            activeProjectTitle = "未命名项目"
                            val layer = ImageLayer(uriString = uri.toString())
                            resetProject(listOf(layer), layer.id)
                            page = AppPage.Editor
                        }

                        PickerMode.Add -> {
                            val layer = ImageLayer(uriString = uri.toString())
                            layers = layers + layer
                            selectedLayerId = layer.id
                            page = AppPage.Editor
                        }

                        PickerMode.ReplaceCollage -> {
                            val index=replaceCollageSlot
                            if (index!=null && collage!=null) {
                                val layer=ImageLayer(uriString=uri.toString())
                                layers=layers+layer
                                collage=collage?.changeSlot(index,com.tugongfang.editor.model.collage.CollageSlot(layer.id))
                                selectedLayerId=layer.id
                            }
                        }
                        PickerMode.Replace -> {
                            val targetId = replaceTargetId
                            if (targetId != null) {
                                layers = layers.map { layer ->
                                    if (layer.id == targetId) {
                                        layer.copy(
                                            uriString = uri.toString(),
                                            offsetXRatio = 0f,
                                            offsetYRatio = 0f,
                                            scale = 1f,
                                            rotation = 0f,
                                            flipHorizontal = false,
                                            flipVertical = false,
                                            cropLeft = 0f,
                                            cropTop = 0f,
                                            cropRight = 1f,
                                            cropBottom = 1f,
                                            photoStrokes = emptyList()
                                        )
                                    } else layer
                                }
                                selectedLayerId = targetId
                            }
                        }
                    }
                    commitHistoryNow()
                    pickerMode = PickerMode.Add
                    replaceTargetId = null
                    replaceCollageSlot = null
                }

                // 默认从系统相册/图库选择照片。HarmonyOS 4.x 上 ACTION_PICK 会优先进入图库体验，
                // 不再把“手机存储文件夹”作为默认入口。
                val galleryPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    handlePickedImage(result.data?.data ?: result.data?.clipData?.getItemAt(0)?.uri)
                }

                // 保留 Android Photo Picker 作为兼容后备入口；部分系统若无法处理图库 Intent，
                // 后续可以无损切回该 launcher。
                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    handlePickedImage(uri)
                }

                // 拼图多图回传：优先补当前空格，其次按空格顺序填充；不要忽略已存在于
                // 普通图片图层中的照片（选中同一张照片同样允许重新使用）。
                fun acceptCollagePhotos(uris: List<android.net.Uri>) {
                    val activeCount = if (collage?.mode == CollageMode.Template)
                        collage?.slots?.count { it.imageId != null } ?: 0
                    else layers.count { it.visible }
                    val uniqueUris = uris.distinctBy { it.toString() }
                        .take((12-activeCount).coerceAtLeast(0))
                    if (uniqueUris.isEmpty()) return // 取消选择不得修改任何项目数据
                    val project = collage?.normalized() ?: CollageProject().normalized()
                    commitHistoryNow()

                    // 首次选择：数量决定初始布局。只有在没有现有照片与格位内容时执行；
                    // 之后“添加照片”、手动换模板和其它工具切换一律不自动重排。
                    val firstSelection = layers.none { it.visible } && project.slots.all { it.imageId == null }
                    if (firstSelection) {
                        val added = uniqueUris.map { uri ->
                            runCatching {
                                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ImageLayer(uriString = uri.toString())
                        }
                        val layout = collageForInitialPhotos(added.map { it.id })
                        // 只有真正的空项目，首次选图时才确定文档的拼图页面比例。
                        // 导出器本就以 layout.aspect 计算拼图成品，预览要采用相同的比例，
                        // 否则会把方形拼图放进纵向超高的透明画布里。
                        // 已有照片/文字/图形/涂鸦的项目不做任何自动画布调整。
                        if (collage == null && layers.isEmpty() && textLayers.isEmpty() &&
                            infoLayers.isEmpty() && shapeLayers.isEmpty()) {
                            canvasScene = canvasScene.copy(aspectRatio = layout.aspect.ratio)
                        }
                        layers = layers + added.mapIndexed { index, layer ->
                            if (layout.mode == CollageMode.Free) layer.copy(
                                scale = .72f,
                                offsetXRatio = ((index % 5) - 2) * .055f,
                                offsetYRatio = (((index / 5) % 4) - 1) * .055f
                            ) else layer
                        }
                        collage = layout
                        selectedLayerId = added.last().id
                        commitHistoryNow()
                        return
                    }
                    if (project.mode == CollageMode.Free) {
                        val added = uniqueUris.mapIndexed { index, uri ->
                            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            ImageLayer(uriString=uri.toString(), scale=.72f,
                                offsetXRatio=((index % 5)-2) * .055f,
                                offsetYRatio=((index / 5) % 4 - 1) * .055f)
                        }
                        layers = layers + added
                        selectedLayerId = added.last().id
                        commitHistoryNow()
                        return
                    }
                    val added = uniqueUris.map { uri ->
                        runCatching { contentResolver.takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        ImageLayer(uriString = uri.toString())
                    }
                    val existingSlots = project.normalized().slots.filter { it.imageId != null }
                    val combined = existingSlots.mapNotNull { it.imageId } + added.map { it.id }
                    if (combined.size !in 1..12) return
                    val newTemplate = if (project.template.slotCount == combined.size)
                        project.template else defaultTemplateForPhotoCount(combined.size)
                    // Every new photo has an occupied slot; preserve transforms for old slots.
                    val oldById = existingSlots.associateBy { it.imageId }
                    val updated = project.copy(template = newTemplate, slots = combined.map { id ->
                        oldById[id] ?: CollageSlot(imageId = id)
                    },activeSlot = 0).normalized()
                    layers = layers + added
                    collage = updated
                    selectedLayerId = added.lastOrNull()?.id
                    commitHistoryNow()
                }

                // 优先启动系统图库/相册多选，不以文件管理器为默认入口。
                // 部分手机图库仅回传单张，单张同样立即填充，用户可继续点击下一个空格位。
                val collageGalleryPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val data = result.data
                        val selected = buildList {
                            val clip = data?.clipData
                            if (clip != null) {
                                for (i in 0 until clip.itemCount) {
                                    clip.getItemAt(i).uri?.let { add(it) }
                                }
                            }
                            data?.data?.let { add(it) }
                        }
                        acceptCollagePhotos(selected)
                    }
                }

                // 系统照片选择器作多选后备，图库不能处理批量选择时无需跳转文件管理器。
                val collagePhotoMultiPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia(12)
                ) { uris -> acceptCollagePhotos(uris) }

                // DEV37: system camera writes the full-resolution image to our private
                // temporary file through FileProvider; no CAMERA/storage permission needed.
                // Saveable path survives Activity recreation while the camera app is open.
                var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
                val takePictureLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { captured ->
                    val path = pendingCameraPath
                    pendingCameraPath = null
                    val photoFile = path?.let(::File)
                    if (!captured) {
                        photoFile?.delete()
                    } else if (photoFile == null) {
                        Toast.makeText(this@MainActivity,
                            "拍照结果已丢失，请重新拍摄", Toast.LENGTH_LONG).show()
                    } else {
                        uiScope.launch {
                            // Persist first, before opening the editor or removing the camera
                            // cache file, to avoid photo loss on a quick Back/close action.
                            try {
                                val persisted = withContext(Dispatchers.IO) {
                                    check(CameraCaptureFiles.hasValidPhoto(photoFile)) {
                                        "相机未保存有效照片，请重试"
                                    }
                                    val newId = UUID.randomUUID().toString()
                                    val layer = ImageLayer(uriString = Uri.fromFile(photoFile).toString())
                                    val doc = LocalProject(
                                        id = newId, title = "未命名项目", images = listOf(layer),
                                        texts = emptyList(), info = emptyList(), shapes = emptyList(),
                                        collage = null, canvas = CanvasScene(), selectedLayerId = layer.id
                                    )
                                    projectWriter.run { projectStore.save(doc) }
                                    projectStore.load(newId)
                                }
                                activeProjectId = persisted.id
                                activeProjectTitle = persisted.title
                                resetProject(persisted.images, persisted.selectedLayerId)
                                page = AppPage.Editor
                                photoFile.delete() // Original is now inside private project media.
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (error: Exception) {
                                // Keep a valid original cached on failure instead of silently
                                // discarding the user's photo; OS manages old cache files.
                                if (!CameraCaptureFiles.hasValidPhoto(photoFile)) photoFile.delete()
                                Toast.makeText(this@MainActivity,
                                    "拍照保存失败：${error.message ?: "请检查存储空间"}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                // DEV2: 首页“单图”直接启动系统照片选择器。
                // 取消时 callback 得到 null：不创建空项目，不跳页面，也不修改最近项目。
                // 成功时立即创建一张图片的项目并进入编辑器，省去“已选1张/进入编辑”中转页。
                val createSinglePhotoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val uri = if (result.resultCode == RESULT_OK) result.data?.data
                        ?: result.data?.clipData?.getItemAt(0)?.uri else null
                    if (uri != null) {
                        runCatching {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val layer = ImageLayer(uriString = uri.toString())
                        activeProjectId = UUID.randomUUID().toString()
                        activeProjectTitle = "未命名项目"
                        resetProject(listOf(layer), layer.id)
                        page = AppPage.Editor
                    }
                }
                // Official Android photo picker as a multi-select fallback on devices
                // whose vendor Gallery ignores EXTRA_ALLOW_MULTIPLE.
                val collagePhotoMultiFallback = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia(12)
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        uris.forEach { uri ->
                            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        }
                        val picked = uris.distinctBy { it.toString() }
                        draftPhotoUris = if (appendOnPhotoSelection) {
                            (draftPhotoUris + picked).distinctBy { it.toString() }.take(12)
                        } else picked.take(12)
                        appendOnPhotoSelection = false
                    }
                }

                // Huawei/HarmonyOS: start the actual image gallery instead of
                // GetContent, which often defaults to a document/file browser.
                fun launchNativeAlbumSingle() {
                    val album = Intent(Intent.ACTION_PICK).apply {
                        setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { createSinglePhotoPicker.launch(album) }.onFailure {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                val collageNativeMultiSelection = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val intent = result.data
                        val selected = buildList {
                            intent?.clipData?.let { clip ->
                                for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { add(it) }
                            }
                            intent?.data?.let { add(it) }
                        }.distinctBy { it.toString() }
                        if (selected.isNotEmpty()) {
                            selected.forEach { uri ->
                                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            }
                            draftPhotoUris = if (appendOnPhotoSelection)
                                (draftPhotoUris + selected).distinctBy { it.toString() }.take(12)
                            else selected.take(12)
                            appendOnPhotoSelection = false
                        }
                    }
                }
                fun launchNativeAlbumMultiple(forSelection: Boolean) {
                    val album = Intent(Intent.ACTION_PICK).apply {
                        setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        if (forSelection) collageNativeMultiSelection.launch(album)
                        else collageGalleryPicker.launch(album)
                    }.onFailure {
                        if (forSelection) {
                            collagePhotoMultiFallback.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            collagePhotoMultiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                }

                fun launchPicker(mode: PickerMode, replaceId: String? = null) {
                    pickerMode = mode
                    replaceTargetId = replaceId

                    val galleryIntent = Intent(Intent.ACTION_PICK).apply {
                        setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        galleryPicker.launch(galleryIntent)
                    }.onFailure {
                        if (mode == PickerMode.ReplaceCollage) {
                            Toast.makeText(this@MainActivity,
                                "无法打开系统相册，请检查图库应用", Toast.LENGTH_SHORT).show()
                        } else {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                }

                // Background image is an independent canvas property, never added as a photo layer.
                fun acceptBackground(uri: android.net.Uri?) {
                    if (uri == null) return
                    runCatching { contentResolver.takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    commitHistoryNow()
                    canvasScene = canvasScene.copy(background=canvasScene.background.copy(
                        kind=com.tugongfang.editor.model.canvas.CanvasBackgroundKind.Image,
                        imageUri=uri.toString()),
                        singlePhotoCanvas = canvasScene.singlePhotoCanvas ||
                            (collage == null && layers.count { it.visible } == 1),
                        photoInsetRatio = if (collage == null && layers.count { it.visible } == 1 &&
                            !canvasScene.singlePhotoCanvas &&
                            canvasScene.photoInsetRatio == 0f) .04f else canvasScene.photoInsetRatio)
                    commitHistoryNow()
                }
                val backgroundGalleryPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result -> if(result.resultCode == RESULT_OK) acceptBackground(result.data?.data) }
                val backgroundPhotoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { acceptBackground(it) }
                fun launchBackgroundGallery() {
                    val intent=Intent(Intent.ACTION_PICK).apply {
                        setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { backgroundGalleryPicker.launch(intent) }.onFailure {
                        backgroundPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }

                // SAF writes to a user-selected folder; no broad file-access permission needed.
                var backupProjectId by remember { mutableStateOf<String?>(null) }
                val backupSavePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/zip")
                ) { uri ->
                    val id = backupProjectId
                    backupProjectId = null
                    if (uri != null && id != null) {
                        uiScope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val stream = contentResolver.openOutputStream(uri)
                                        ?: error("无法写入所选位置")
                                    stream.use { backupManager.exportProject(id, it) }
                                }
                            }.onSuccess {
                                Toast.makeText(this@MainActivity, "项目备份已保存", Toast.LENGTH_LONG).show()
                            }.onFailure { error ->
                                Toast.makeText(this@MainActivity,
                                    "备份失败：${error.message ?: "请检查存储空间"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                val backupOpenPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        uiScope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val stream = contentResolver.openInputStream(uri)
                                        ?: error("无法读取备份文件")
                                    stream.use { backupManager.importProject(it) }
                                }
                            }.onSuccess { restored ->
                                try {
                                    val restoredProject = withContext(Dispatchers.IO) { projectStore.load(restored.id) }
                                    thumbnailManager.renderAndSave(restoredProject)
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    // ZIP restore remains successful if the optional preview fails.
                                }
                                projectList = withContext(Dispatchers.IO) { projectStore.listProjects() }
                                Toast.makeText(this@MainActivity,
                                    "项目已恢复：${restored.title}", Toast.LENGTH_LONG).show()
                            }.onFailure { error ->
                                Toast.makeText(this@MainActivity,
                                    "导入失败：${error.message ?: "备份文件不可用"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                // 只有拼图保留“已选照片/确认”步骤，供首次多选和编辑中整组重选使用。
                fun launchSelectionGallery(append: Boolean = false) {
                    appendOnPhotoSelection = append
                    launchNativeAlbumMultiple(forSelection = true)
                }

                fun startNewCollageFlow() {
                    selectionMode = PhotoSelectionMode.CreateCollage
                    draftPhotoUris = emptyList()
                    appendOnPhotoSelection = false
                    page = AppPage.PhotoSelect
                    // One tap on 拼图 enters the actual album picker immediately.
                    // After selecting photos the existing confirm/reselect step remains.
                    launchNativeAlbumMultiple(forSelection = true)
                }

                fun confirmPhotoSelection() {
                    val draft = draftPhotoUris.take(12)
                    if (draft.size !in 1..12) return
                    if (selectionMode == PhotoSelectionMode.CreateCollage) {
                        val newLayers = draft.map { ImageLayer(uriString = it.toString()) }
                        val layout = collageForInitialPhotos(newLayers.map { it.id })
                        val prepared = if (layout.mode == CollageMode.Free) newLayers.mapIndexed { i, layer ->
                            layer.copy(scale = .72f, offsetXRatio = ((i % 5) - 2) * .055f,
                                offsetYRatio = (((i / 5) % 4) - 1) * .055f)
                        } else newLayers
                        activeProjectId = UUID.randomUUID().toString()
                        activeProjectTitle = "未命名拼图"
                        val newScene = CanvasScene(aspectRatio = layout.aspect.ratio)
                        val snapshot = EditorSnapshot(prepared, emptyList(), emptyList(), emptyList(),
                            prepared.lastOrNull()?.id, layout, newScene)
                        setProjectState(snapshot, suppressHistory = true)
                        history = listOf(snapshot)
                        redoStack = emptyList()
                        page = AppPage.Editor
                        return
                    }
                    // Full-set reselect: identical selection is a strict no-op. Preserve
                    // surviving image ids, transforms and previously configured slot settings.
                    val original = collage ?: return
                    val activeOldIds = if (original.mode == CollageMode.Template) {
                        original.normalized().slots.mapNotNull { it.imageId }
                    } else layers.filter { it.visible }.map { it.id }
                    val oldImages = activeOldIds.mapNotNull { id -> layers.firstOrNull { it.id == id } }
                    if (draft.map { it.toString() } == oldImages.map { it.uriString }) {
                        page = AppPage.Editor
                        return
                    }
                    commitHistoryNow()
                    val available = oldImages.groupBy { it.uriString }
                        .mapValues { (_, items) -> items.toMutableList() }
                    val selected = draft.mapIndexed { i, uri ->
                        val old = available[uri.toString()]?.let { if (it.isNotEmpty()) it.removeAt(0) else null }
                        old ?: ImageLayer(uriString = uri.toString(),
                            scale = if (original.mode == CollageMode.Free) .72f else 1f,
                            offsetXRatio = if (original.mode == CollageMode.Free) ((i % 5) - 2) * .055f else 0f,
                            offsetYRatio = if (original.mode == CollageMode.Free) (((i / 5) % 4) - 1) * .055f else 0f)
                    }
                    val selectedIds = selected.map { it.id }.toSet()
                    // Retain unused source layers invisibly for undo/reference safety.
                    layers = selected + layers.filterNot { it.id in selectedIds }.map { it.copy(visible = false) }
                    val keepOldTemplate = original.mode == CollageMode.Template &&
                        original.template.slotCount == selected.size
                    val configured = original.copy(
                        mode = original.mode,
                        template = if (keepOldTemplate) original.template else
                            defaultTemplateForPhotoCount(selected.size)
                    )
                    val oldSlots = original.normalized().slots
                    collage = configured.copy(slots = (0 until configured.template.slotCount).map { i ->
                        val id = selected.getOrNull(i)?.id
                        oldSlots.firstOrNull { it.imageId == id }?.copy(imageId = id)
                            ?: CollageSlot(imageId = id)
                    }, activeSlot = 0).normalized()
                    selectedLayerId = selected.lastOrNull()?.id
                    commitHistoryNow()
                    page = AppPage.Editor
                }

                when (page) {
                    AppPage.PhotoSelect -> PhotoSelectionScreen(
                        isReselection = selectionMode == PhotoSelectionMode.ReselectCollage,
                        selectedUris = draftPhotoUris,
                        onOpenGallery = { launchSelectionGallery(false) },
                        onOpenSystemMulti = {
                            appendOnPhotoSelection = false
                            collagePhotoMultiFallback.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onAddGallery = { launchSelectionGallery(true) },
                        onRemoveAt = { index -> draftPhotoUris = draftPhotoUris.filterIndexed { i, _ -> i != index } },
                        onCancel = {
                            page = if (selectionMode == PhotoSelectionMode.ReselectCollage)
                                AppPage.Editor else AppPage.Home
                        },
                        onConfirm = { confirmPhotoSelection() }
                    )
                    AppPage.Home -> HomeScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            themeStore.write(mode)
                        },
                        onNewProject = {
                            activeProjectId = UUID.randomUUID().toString()
                            activeProjectTitle = "未命名项目"
                            resetProject(emptyList(), null)
                            page = AppPage.Editor
                        },
                        onImportPhoto = {
                            runCatching {
                                launchNativeAlbumSingle()
                            }.onFailure {
                                Toast.makeText(this@MainActivity, "无法打开系统相册", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartCollage = { startNewCollageFlow() },
                        onCamera = {
                            runCatching {
                                CameraCaptureFiles.pruneStale(cacheDir, pendingCameraPath)
                                val imageFile = CameraCaptureFiles.create(cacheDir)
                                try {
                                    val output = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${packageName}.fileprovider", imageFile
                                    )
                                    pendingCameraPath = imageFile.absolutePath
                                    takePictureLauncher.launch(output)
                                } catch (error: Exception) {
                                    pendingCameraPath = null
                                    imageFile.delete()
                                    throw error
                                }
                            }.onFailure { error ->
                                Toast.makeText(this@MainActivity,
                                    "无法启动系统相机：${error.message ?: "请检查是否安装相机应用"}",
                                    Toast.LENGTH_LONG).show()
                            }
                        },
                        recentProjects = projectList,
                        onRenameProject = { id, title ->
                            uiScope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { projectStore.rename(id, title) }
                                }.onSuccess {
                                    projectList = withContext(Dispatchers.IO) { projectStore.listProjects() }
                                }.onFailure { error ->
                                    Toast.makeText(this@MainActivity,
                                        "重命名失败：${error.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDeleteProject = { id ->
                            uiScope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { projectStore.delete(id) }
                                }.onSuccess {
                                    projectList = withContext(Dispatchers.IO) { projectStore.listProjects() }
                                    Toast.makeText(this@MainActivity, "项目已删除", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    Toast.makeText(this@MainActivity,
                                        "删除失败：${error.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onExportProject = { id ->
                            backupProjectId = id
                            val title = projectList.firstOrNull { it.id == id }?.title ?: "图工坊项目"
                            val safeName = title.map { c -> if (c in "\\/:*?\"<>|" || c == '\n' || c == '\r') '_' else c }.joinToString("")
                            backupSavePicker.launch("图工坊_${safeName.take(32)}.tgf.zip")
                        },
                        onImportProject = { backupOpenPicker.launch(arrayOf("*/*")) },
                        onOpenProject = { id ->
                            uiScope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { projectStore.load(id) }
                                }.onSuccess { saved ->
                                    val snapshot = EditorSnapshot(
                                        saved.images, saved.texts, saved.info, saved.shapes,
                                        saved.selectedLayerId, saved.collage, saved.canvas
                                    )
                                    activeProjectId = saved.id
                                    activeProjectTitle = saved.title
                                    setProjectState(snapshot, suppressHistory = true)
                                    history = listOf(snapshot)
                                    redoStack = emptyList()
                                    page = AppPage.Editor
                                }.onFailure { error ->
                                    Toast.makeText(this@MainActivity,
                                        "打开项目失败：${error.message ?: "项目文件损坏"}",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )

                    AppPage.Editor -> EditorScreen(
                        projectKey = activeProjectId ?: "unsaved-project",
                        layers = layers,
                        textLayers = textLayers,
                        infoLayers = infoLayers,
                        shapeLayers = shapeLayers,
                        collage = collage,
                        canvasScene = canvasScene,
                        onUpdateScene = { canvasScene = it },
                        onPickBackgroundImage = { launchBackgroundGallery() },
                        onUpdateCollage = { collage = it?.normalized() },
                        onAddCollagePhotos = {
                            // Append only, with an explicit 12-photo limit.
                            val currentCount = if (collage?.mode == CollageMode.Template)
                                collage?.slots?.count { it.imageId != null } ?: 0
                                else layers.count { it.visible }
                            if (currentCount >= 12) {
                                Toast.makeText(this@MainActivity,"拼图最多12张照片",Toast.LENGTH_SHORT).show()
                            } else {
                            // ACTION_PICK often silently ignores EXTRA_ALLOW_MULTIPLE on
                            // Huawei gallery apps. GET_CONTENT requests an actual multi-picker.
                            launchNativeAlbumMultiple(forSelection = false)
                            }
                        },
                        onReselectCollagePhotos = {
                            if (collage != null) {
                                commitHistoryNow()
                                val document = projectDocument(snapshotNow())
                                uiScope.launch {
                                    runCatching {
                                        withContext(Dispatchers.IO) { projectWriter.run { projectStore.save(document) } }
                                    }.onSuccess {
                                        val project = collage ?: return@onSuccess
                                        val ids = if (project.mode == CollageMode.Template)
                                            project.normalized().slots.mapNotNull { it.imageId }
                                        else layers.filter { it.visible }.map { it.id }
                                        selectionMode = PhotoSelectionMode.ReselectCollage
                                        draftPhotoUris = ids.mapNotNull { id -> layers.firstOrNull { it.id == id } }
                                            .map { Uri.parse(it.uriString) }
                                        page = AppPage.PhotoSelect
                                    }.onFailure { error ->
                                        Toast.makeText(this@MainActivity,
                                            "保存失败，无法重选：${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onReplaceCollageSlot = { index ->
                            replaceCollageSlot=index
                            launchPicker(PickerMode.ReplaceCollage)
                        },
                        selectedLayerId = selectedLayerId,
                        canUndo = history.size > 1 || !history.lastOrNull().sameDocument(snapshotNow()),
                        canRedo = redoStack.isNotEmpty() && history.lastOrNull().sameDocument(snapshotNow()),
                        onSelectLayer = { selectedLayerId = it },
                        onUpdateLayer = { updated ->
                            val before = layers.firstOrNull { it.id == updated.id }
                            if (before != null && before != updated) {
                                shapeLayers = shapeLayers.map { it.followImageTransform(before, updated) }
                            }
                            layers = layers.map { if (it.id == updated.id) updated else it }
                        },
                        onAddTextLayer = { textLayer ->
                            commitHistoryNow()
                            textLayers = textLayers + textLayer
                            selectedLayerId = textLayer.id
                            commitHistoryNow()
                        },
                        onUpdateTextLayer = { updated ->
                            textLayers = textLayers.map { if (it.id == updated.id) updated else it }
                        },
                        onDuplicateTextLayer = { id ->
                            commitHistoryNow()
                            textLayers.firstOrNull { it.id == id }?.let { source ->
                                val copy = source.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    offsetXRatio = (source.offsetXRatio + .045f).coerceAtMost(1.5f),
                                    offsetYRatio = (source.offsetYRatio + .045f).coerceAtMost(1.5f),
                                    locked = false
                                )
                                textLayers = textLayers + copy
                                selectedLayerId = copy.id
                            }
                            commitHistoryNow()
                        },
                        onDeleteTextLayer = { id ->
                            commitHistoryNow()
                            textLayers = textLayers.filterNot { it.id == id }
                            if (selectedLayerId == id) {
                                selectedLayerId = textLayers.lastOrNull()?.id ?: layers.lastOrNull()?.id
                            }
                            commitHistoryNow()
                        },
                        onToggleTextLock = { id ->
                            commitHistoryNow()
                            textLayers = textLayers.map { if (it.id == id) it.copy(locked = !it.locked) else it }
                            commitHistoryNow()
                        },
                        onToggleTextVisibility = { id ->
                            commitHistoryNow()
                            textLayers = textLayers.map { if (it.id == id) it.copy(visible = !it.visible) else it }
                            commitHistoryNow()
                        },
                        onAddInfoLayer = { info ->
                            commitHistoryNow()
                            infoLayers = infoLayers + info
                            selectedLayerId = info.id
                            commitHistoryNow()
                        },
                        onUpdateInfoLayer = { updated ->
                            infoLayers = infoLayers.map { if (it.id == updated.id) updated else it }
                        },
                        onDeleteInfoLayer = { id ->
                            commitHistoryNow()
                            infoLayers = infoLayers.filterNot { it.id == id }
                            if (selectedLayerId == id) {
                                selectedLayerId = textLayers.lastOrNull()?.id ?: layers.lastOrNull()?.id
                            }
                            commitHistoryNow()
                        },
                        onToggleInfoVisibility = { id ->
                            commitHistoryNow()
                            infoLayers = infoLayers.map { if (it.id == id) it.copy(visible = !it.visible) else it }
                            commitHistoryNow()
                        },
                        onAddShapeLayer = { shape ->
                            commitHistoryNow()
                            val next = shapeLayers + shape
                            shapeLayers = next
                            selectedLayerId = if (shape.doodleSessionId != null) null else shape.id
                            if (shape.doodleSessionId != null) {
                                // 每落一笔单独存一条历史，不依赖 320ms 连续变换节流。
                                val snapshot = EditorSnapshot(layers, textLayers, infoLayers, next, null, collage, canvasScene)
                                if (history.lastOrNull()?.shapeLayers != next) {
                                    history = (history + snapshot).takeLast(MAX_EDITOR_HISTORY)
                                    redoStack = emptyList()
                                }
                            }
                            commitHistoryNow()
                        },
                        onEraseDoodleStrokes = { changes ->
                            commitHistoryNow()
                            // 整笔擦除：空列表；局部擦除：以保留的路径片段替换原路径。
                            // 一次手势只记录一次历史，复制/锁定/图片联动元数据不丢失。
                            if (changes.isNotEmpty()) {
                                val next = shapeLayers.flatMap { original ->
                                    changes[original.id] ?: listOf(original)
                                }
                                if (next != shapeLayers) {
                                    shapeLayers = next
                                    selectedLayerId = null
                                    val snapshot = EditorSnapshot(layers, textLayers, infoLayers, next, null, collage, canvasScene)
                                    history = (history + snapshot).takeLast(MAX_EDITOR_HISTORY)
                                    redoStack = emptyList()
                                }
                            }
                        },
                        onUpdateShapeLayer = { updated ->
                            shapeLayers = shapeLayers.map { if (it.id == updated.id) updated else it }
                        },
                        onDuplicateShapeLayer = { id ->
                            commitHistoryNow()
                            shapeLayers.firstOrNull { it.id == id }?.let { source ->
                                val members = shapeLayers.filter {
                                    source.doodleSessionId != null && it.doodleSessionId == source.doodleSessionId || it.id == id
                                }
                                val session = if (source.doodleSessionId != null) java.util.UUID.randomUUID().toString() else null
                                val duplicates = members.map { item ->
                                    item.copy(id = java.util.UUID.randomUUID().toString(),
                                        doodleSessionId = session,
                                        offsetXRatio = item.offsetXRatio + .04f,
                                        offsetYRatio = item.offsetYRatio + .04f,
                                        locked = false)
                                }
                                shapeLayers = shapeLayers + duplicates
                                selectedLayerId = duplicates.lastOrNull()?.id
                            }
                            commitHistoryNow()
                        },
                        onDeleteShapeLayer = { id ->
                            commitHistoryNow()
                            val source = shapeLayers.firstOrNull { it.id == id }
                            shapeLayers = shapeLayers.filterNot {
                                it.id == id || (source?.doodleSessionId != null && it.doodleSessionId == source.doodleSessionId)
                            }
                            if (selectedLayerId == id) selectedLayerId = shapeLayers.lastOrNull()?.id
                                ?: textLayers.lastOrNull()?.id ?: layers.lastOrNull()?.id
                            commitHistoryNow()
                        },
                        onToggleShapeLock = { id ->
                            commitHistoryNow()
                            val source = shapeLayers.firstOrNull { it.id == id }
                            shapeLayers = shapeLayers.map {
                                if (it.id == id || (source?.doodleSessionId != null && it.doodleSessionId == source.doodleSessionId))
                                    it.copy(locked = !(source?.locked ?: false)) else it
                            }
                            commitHistoryNow()
                        },
                        onToggleShapeVisibility = { id ->
                            commitHistoryNow()
                            val source = shapeLayers.firstOrNull { it.id == id }
                            shapeLayers = shapeLayers.map {
                                if (it.id == id || (source?.doodleSessionId != null && it.doodleSessionId == source.doodleSessionId))
                                    it.copy(visible = !(source?.visible ?: true)) else it
                            }
                            commitHistoryNow()
                        },
                        onAddPhoto = { launchPicker(PickerMode.Add) },
                        onReplacePhoto = { id -> launchPicker(PickerMode.Replace, id) },
                        onDuplicateLayer = { id ->
                            commitHistoryNow()
                            layers.firstOrNull { it.id == id }?.let { source ->
                                val copy = source.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    offsetXRatio = (source.offsetXRatio + .045f).coerceAtMost(1.5f),
                                    offsetYRatio = (source.offsetYRatio + .045f).coerceAtMost(1.5f),
                                    locked = false
                                )
                                layers = layers + copy
                                shapeLayers = shapeLayers + shapeLayers.filter { it.parentImageId == id }.map { attached ->
                                    attached.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        parentImageId = copy.id,
                                        offsetXRatio = attached.offsetXRatio + copy.offsetXRatio - source.offsetXRatio,
                                        offsetYRatio = attached.offsetYRatio + copy.offsetYRatio - source.offsetYRatio,
                                        locked = false
                                    )
                                }
                                selectedLayerId = copy.id
                            }
                            commitHistoryNow()
                        },
                        onDeleteLayer = { id ->
                            commitHistoryNow()
                            shapeLayers = shapeLayers.filterNot { it.parentImageId == id }
                            val index = layers.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val next = layers.toMutableList().apply { removeAt(index) }
                                layers = next
                                selectedLayerId = next.lastOrNull()?.id
                            }
                            commitHistoryNow()
                        },
                        onToggleLock = { id ->
                            commitHistoryNow()
                            layers = layers.map { if (it.id == id) it.copy(locked = !it.locked) else it }
                            commitHistoryNow()
                        },
                        onToggleVisibility = { id ->
                            commitHistoryNow()
                            layers = layers.map { if (it.id == id) it.copy(visible = !it.visible) else it }
                            commitHistoryNow()
                        },
                        onUndo = {
                            // 用户可能在拖动后立刻点击撤销：先提交尚未结束的 260ms 手势记录。
                            commitHistoryNow()
                            if (history.size > 1) {
                                val current = history.last()
                                val previous = history[history.lastIndex - 1]
                                history = history.dropLast(1)
                                redoStack = (redoStack + current).takeLast(MAX_EDITOR_HISTORY)
                                setProjectState(previous, suppressHistory = true)
                            }
                        },
                        onRedo = {
                            // 新编辑尚未提交时不能重做旧分支。
                            if (!history.lastOrNull().sameDocument(snapshotNow())) {
                                commitHistoryNow()
                            } else {
                                val redo = redoStack.lastOrNull()
                                if (redo != null) {
                                    redoStack = redoStack.dropLast(1)
                                    history = (history + redo).takeLast(MAX_EDITOR_HISTORY)
                                    setProjectState(redo, suppressHistory = true)
                                }
                            }
                        },
                        onExport = {
                            // 先生成最终输出预览。预览与正式导出共用同一套排版引擎，
                            // 方便在开发阶段提前校准照片 + 信息文字的真实成品边界。
                            uiScope.launch {
                                val result = ProjectExporter.renderPreview(
                                    context = this@MainActivity,
                                    imageLayers = layers,
                                    textLayers = textLayers,
                                    infoLayers = infoLayers,
                                    shapeLayers = shapeLayers,
                                    collage = collage,
                                    canvasScene = canvasScene
                                )
                                result.fold(
                                    onSuccess = { preview ->
                                        exportPreviewBitmap?.let { old ->
                                            if (old !== preview && !old.isRecycled) old.recycle()
                                        }
                                        exportPreviewBitmap = preview
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            error.message ?: "无法生成导出预览",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        },
                        onBack = { closeEditor() }
                    )
                }

                exportedUriString?.let { resultUri ->
                    ExportResultDialog(
                        savedUri = resultUri,
                        onContinue = { exportedUriString = null },
                        onHome = { exportedUriString = null; closeEditor() },
                        onView = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(resultUri), "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(intent)
                            }.onFailure {
                                Toast.makeText(this@MainActivity, "无法打开图片查看器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(resultUri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(Intent.createChooser(intent, "分享图片"))
                            }.onFailure {
                                Toast.makeText(this@MainActivity, "无法打开分享面板", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                exportPreviewBitmap?.let { preview ->
                    ExportPreviewDialog(
                        bitmap = preview,
                        initialFormat = if ((collage != null || layers.count { it.visible } > 1) &&
                            canvasScene.background.kind == com.tugongfang.editor.model.canvas.CanvasBackgroundKind.Transparent)
                            ExportImageFormat.PNG else ExportImageFormat.JPG,
                        defaultFileName = activeProjectTitle,
                        onDismiss = {
                            exportPreviewBitmap = null
                            if (!preview.isRecycled) preview.recycle()
                        },
                        onConfirm = { options ->
                            exportPreviewBitmap = null
                            if (!preview.isRecycled) preview.recycle()
                            uiScope.launch {
                                val result = ProjectExporter.exportImage(
                                    context = this@MainActivity,
                                    imageLayers = layers,
                                    textLayers = textLayers,
                                    infoLayers = infoLayers,
                                    shapeLayers = shapeLayers,
                                    collage = collage,
                                    canvasScene = canvasScene,
                                    options = options
                                )
                                result.fold(
                                    onSuccess = { saved -> exportedUriString = saved },
                                    onFailure = { error ->
                                        Toast.makeText(this@MainActivity,
                                            error.message ?: "导出失败", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private const val MAX_EDITOR_HISTORY = 101 // 初始状态 + 最多 100 次可撤销动作

private fun EditorSnapshot?.sameDocument(other: EditorSnapshot): Boolean =
    this != null && layers == other.layers && textLayers == other.textLayers &&
    infoLayers == other.infoLayers && shapeLayers == other.shapeLayers &&
    collage == other.collage && canvasScene == other.canvasScene

private data class EditorSnapshot(
    val layers: List<ImageLayer>,
    val textLayers: List<TextLayer>,
    val infoLayers: List<InfoPanelLayer>,
    val shapeLayers: List<ShapeLayer>,
    val selectedLayerId: String?,
    val collage: CollageProject? = null,
    val canvasScene: CanvasScene = CanvasScene()
)

private enum class AppPage {
    Home,
    PhotoSelect,
    Editor
}

private enum class PickerMode {
    NewProject,
    Add,
    Replace,
    ReplaceCollage
}

private enum class PhotoSelectionMode { CreateCollage, ReselectCollage }
