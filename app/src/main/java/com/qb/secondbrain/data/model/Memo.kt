package com.qb.secondbrain.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memo",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isDeleted"]),
        Index(value = ["reminderTime"])
    ]
)
data class Memo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val content: String = "",
    val rawText: String = "",
    val tags: List<String> = emptyList(),
    val imagePaths: List<ImagePath> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class ImagePath(
    val path: String,
    val source: ImageSource
)

enum class ImageSource {
    VOICE_SCREENSHOT,
    CAMERA,
    GALLERY
}