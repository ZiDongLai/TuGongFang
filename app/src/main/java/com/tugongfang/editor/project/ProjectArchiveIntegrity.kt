package com.tugongfang.editor.project

/**
 * Pure JVM safety guard for the TuGongFangProject ZIP layout.
 * A ZIP can contain the right number of photos but refer to only one of them;
 * that must fail before an imported project is made visible on the home page.
 * Files in an export are always numbered image_0 through image_(n-1).
 */
internal object ProjectArchiveIntegrity {
    const val MAX_IMAGES = 100

    fun validateImageReferences(photoUris: List<String>, extractedImageEntries: Set<String>) {
        require(photoUris.size <= MAX_IMAGES) { "备份照片数量超过上限" }
        val expected = photoUris.indices.map { "media/image_${it}.img" }.toSet()
        require(extractedImageEntries == expected) {
            "备份照片文件编号不完整，或包含未使用的文件"
        }
        require(photoUris.size == photoUris.toSet().size && photoUris.toSet() == expected) {
            "备份照片引用不完整或重复，已拒绝导入以避免照片错配"
        }
    }
}
