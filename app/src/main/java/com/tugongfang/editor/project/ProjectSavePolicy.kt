package com.tugongfang.editor.project

/** DEV22: editable project JSON stays durable; derived home thumbnails can lag safely. */
object ProjectSavePolicy {
    const val AUTO_SAVE_IDLE_MS = 1100L
    const val THUMBNAIL_IDLE_MS = 3500L
    const val THUMBNAIL_SOURCE_LONG_EDGE = 640

    /** Filesystem modified times are used only to detect an obsolete derived thumbnail. */
    fun isThumbnailFresh(projectModifiedAt: Long, thumbnailModifiedAt: Long): Boolean =
        projectModifiedAt > 0L && thumbnailModifiedAt > 0L &&
            thumbnailModifiedAt >= projectModifiedAt
}
