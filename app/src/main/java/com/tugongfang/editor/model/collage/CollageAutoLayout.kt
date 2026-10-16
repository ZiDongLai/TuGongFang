package com.tugongfang.editor.model.collage

/** 首次选择多少张，便建立对应的模板格位（1..12），不自动创建空格。 */
fun collageForInitialPhotos(imageIds: List<String>): CollageProject {
    val selected = imageIds.take(12)
    val template = defaultTemplateForPhotoCount(selected.size.coerceAtLeast(1))
    return CollageProject(mode = CollageMode.Template, template = template)
        .assignNewImages(selected).normalized()
}
