package com.personal.lira.data

import com.personal.lira.runtime.MediaRuntime
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaExtractor {
    suspend fun analyze(url: String): MediaDetails = withContext(Dispatchers.IO) {
        MediaRuntime.awaitReady()
        val info = YoutubeDL.getInstance().getInfo(url)
        val title = info.title?.takeIf(String::isNotBlank)
            ?: info.fulltitle?.takeIf(String::isNotBlank)
            ?: "Descarga multimedia"
        val formats = info.formats.orEmpty()
        val audioOnly = formats.filter {
            it.acodec != null && it.acodec != "none" &&
                (it.vcodec == null || it.vcodec == "none")
        }
        val bestAudio = audioOnly.maxByOrNull { it.abr }
        val videoOptions = listOf(144, 240, 360, 480, 720, 1080).map { height ->
            val video = formats
                .filter {
                    it.height in 1..height && it.vcodec != null && it.vcodec != "none"
                }
                .maxByOrNull { it.height }
            val videoSize = video?.estimatedSize()
            val audioSize = if (video?.acodec == "none") bestAudio?.estimatedSize() else null
            MediaOption(
                id = "video_$height",
                label = "${height}p",
                kind = DownloadItem.KIND_VIDEO,
                quality = "${height}p",
                extension = "auto",
                formatSelector = "bestvideo[height<=$height]+bestaudio/best[height<=$height]",
                estimatedBytes = sumKnownSizes(videoSize, audioSize),
            )
        }
        val bestVideo = formats
            .filter { it.vcodec != null && it.vcodec != "none" }
            .maxByOrNull { it.height }
        val maxVideoSize = bestVideo?.estimatedSize()
        val maxAudioSize = if (bestVideo?.acodec == "none") bestAudio?.estimatedSize() else null
        val maximumVideoOption = MediaOption(
            id = "video_max",
            label = "Máxima",
            kind = DownloadItem.KIND_VIDEO,
            quality = "Máxima",
            extension = "auto",
            formatSelector = "bestvideo+bestaudio/best",
            estimatedBytes = sumKnownSizes(maxVideoSize, maxAudioSize),
        )
        val allVideoOptions = videoOptions + maximumVideoOption

        val audioOptions = listOf(
            Triple("mp3_128", "MP3 · 128 kbps", "128K"),
            Triple("mp3_192", "MP3 · 192 kbps", "192K"),
            Triple("mp3_320", "MP3 · 320 kbps", "320K"),
            Triple("m4a_128", "M4A · 128 kbps", "128K"),
            Triple("m4a_256", "M4A · 256 kbps", "256K"),
        ).map { (id, label, bitrate) ->
            val extension = if (id.startsWith("mp3")) "mp3" else "m4a"
            val bitrateKbps = bitrate.removeSuffix("K").toInt()
            val estimated = if (info.duration > 0) {
                info.duration.toLong() * bitrateKbps * 1000L / 8L
            } else {
                bestAudio?.estimatedSize()
            }
            MediaOption(
                id = id,
                label = label,
                kind = DownloadItem.KIND_AUDIO,
                quality = label,
                extension = extension,
                formatSelector = bitrate,
                estimatedBytes = estimated,
            )
        }

        MediaDetails(
            title = title,
            artist = info.uploader?.takeIf(String::isNotBlank) ?: "Artista desconocido",
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnail,
            options = allVideoOptions + audioOptions,
        )
    }

    private fun VideoFormat.estimatedSize(): Long? =
        fileSize.takeIf { it > 0L } ?: fileSizeApproximate.takeIf { it > 0L }

    private fun sumKnownSizes(first: Long?, second: Long?): Long? = when {
        first != null && second != null -> first + second
        first != null -> first
        else -> second
    }
}
