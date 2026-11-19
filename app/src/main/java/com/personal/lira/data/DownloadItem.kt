package com.personal.lira.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val kind: String,
    val quality: String,
    val formatSelector: String,
    val extension: String,
    val status: String = STATUS_QUEUED,
    val progress: Int = 0,
    val contentUri: String? = null,
    val localPath: String? = null,
    val lyricsUri: String? = null,
    val lyricsPath: String? = null,
    val lyricsText: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_QUEUED = "queued"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
        const val STATUS_CANCELLED = "cancelled"

        const val KIND_VIDEO = "video"
        const val KIND_AUDIO = "audio"
        const val DIRECT_DOWNLOAD_TITLE = "Descarga directa"
    }
}

data class MediaOption(
    val id: String,
    val label: String,
    val kind: String,
    val quality: String,
    val extension: String,
    val formatSelector: String,
    val estimatedBytes: Long?,
)

data class MediaDetails(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
    val options: List<MediaOption>,
)
