package com.tugongfang.editor.model.layer

/** Records the original list position so redo preserves the ordering of doodle/mosaic effects. */
data class RemovedPhotoStroke(val originalIndex: Int, val stroke: PhotoStroke)

data class PhotoStrokeUndo(val remaining: List<PhotoStroke>, val removed: RemovedPhotoStroke?)

/** Remove the last stroke of the CURRENT tool, not the last stroke of a different tool. */
fun List<PhotoStroke>.undoLatestOf(kind: PhotoStrokeKind): PhotoStrokeUndo {
    val index = indexOfLast { it.kind == kind }
    if (index < 0) return PhotoStrokeUndo(this, null)
    val removed = RemovedPhotoStroke(index, this[index])
    val remainder = toMutableList().apply { removeAt(index) }
    return PhotoStrokeUndo(remainder, removed)
}

/** Insert back at its previous position, preserving mixed doodle/mosaic stack order. */
fun List<PhotoStroke>.redoRemoved(removed: RemovedPhotoStroke): List<PhotoStroke> =
    toMutableList().apply { add(removed.originalIndex.coerceIn(0, size), removed.stroke) }
