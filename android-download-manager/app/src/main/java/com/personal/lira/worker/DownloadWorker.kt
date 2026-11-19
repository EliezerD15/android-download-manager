package com.personal.lira.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.personal.lira.data.AppDatabase
import com.personal.lira.data.DownloadItem
import com.personal.lira.data.LyricsRepository
import com.personal.lira.data.MediaPublisher
import com.personal.lira.runtime.MediaRuntime
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val dao = AppDatabase.get(appContext).downloadDao()

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val item = dao.getById(id) ?: return Result.failure()
        return try {
            MediaRuntime.awaitReady()
            val workingDirectory = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: applicationContext.filesDir,
                "pending/$id",
            )
            if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
                error("No se pudo preparar el almacenamiento temporal.")
            }

            dao.upsert(item.copy(status = DownloadItem.STATUS_DOWNLOADING, progress = 0, error = null))
            setForeground(DownloadNotifications.foregroundInfo(applicationContext, item, 0))

            val request = YoutubeDLRequest(item.url).apply {
                addOption("--no-mtime")
                addOption("--no-playlist")
                addOption("--newline")
                addOption(
                    "-o",
                    File(
                        workingDirectory,
                        "%(title)s - ${safeQuality(outputQuality(item))}.%(ext)s",
                    ).absolutePath,
                )
                addOption("--embed-thumbnail")
                addOption("--convert-thumbnails", "jpg")
                addOption("--embed-metadata")
                addOption("--add-metadata")
                if (item.kind == DownloadItem.KIND_VIDEO) {
                    addOption("-f", item.formatSelector)
                    addOption("--merge-output-format", "mp4")
                } else {
                    addOption("-f", "bestaudio/best")
                    addOption("--extract-audio")
                    addOption("--audio-format", item.extension)
                    addOption("--audio-quality", item.formatSelector)
                }
            }

            var lastProgress = -1
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().execute(
                    request = request,
                    processId = id,
                    redirectErrorStream = false,
                    callback = { progress, _, _ ->
                        val current = progress.toInt().coerceIn(0, 100)
                        if (current != lastProgress) {
                            lastProgress = current
                            runBlocking(Dispatchers.IO) {
                                val updated = item.copy(
                                    status = DownloadItem.STATUS_DOWNLOADING,
                                    progress = current,
                                    error = null,
                                )
                                dao.upsert(updated)
                                setProgress(workDataOf("progress" to current))
                            }
                            DownloadNotifications.update(applicationContext, item, current)
                        }
                    },
                )
            }

            if (isStopped) throw InterruptedException("Descarga cancelada.")
            val mediaFile = workingDirectory.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in MEDIA_EXTENSIONS }
                ?.maxByOrNull(File::lastModified)
                ?: error("La descarga terminó, pero no se encontró el archivo multimedia.")

            val published = MediaPublisher.publishMedia(applicationContext, mediaFile, item)
            val finalItem = item.copy(
                status = DownloadItem.STATUS_COMPLETED,
                progress = 100,
                contentUri = published.contentUri,
                localPath = published.localPath,
                error = null,
            )
            dao.upsert(finalItem)

            if (item.kind == DownloadItem.KIND_AUDIO) {
                try {
                    val lyrics = LyricsRepository().findLyrics(item.title, item.artist)
                    if (!lyrics.isNullOrBlank()) {
                        val lrc = File(workingDirectory, "${mediaFile.nameWithoutExtension}.lrc")
                        lrc.writeText(lyrics, Charsets.UTF_8)
                        val lyricFile = MediaPublisher.publishLyrics(
                            applicationContext,
                            lrc,
                            lrc.name,
                        )
                        dao.upsert(
                            finalItem.copy(
                                lyricsText = lyrics,
                                lyricsUri = lyricFile.contentUri,
                                lyricsPath = lyricFile.localPath,
                            ),
                        )
                    }
                } catch (_: Exception) {
                    // Lyrics are optional; an API or file-write issue must not undo the media download.
                }
            }

            mediaFile.delete()
            workingDirectory.deleteRecursively()
            DownloadNotifications.completed(applicationContext, item)
            Result.success()
        } catch (error: Exception) {
            if (isStopped) {
                dao.upsert(item.copy(status = DownloadItem.STATUS_CANCELLED, error = null))
                Result.failure()
            } else {
                val message = error.message?.takeIf(String::isNotBlank)
                    ?: "No se pudo completar la descarga."
                dao.upsert(item.copy(status = DownloadItem.STATUS_FAILED, error = message))
                DownloadNotifications.failed(applicationContext, item, "No se pudo descargar")
                Result.failure(workDataOf("error" to message))
            }
        }
    }

    override fun onStopped() {
        inputData.getString(KEY_DOWNLOAD_ID)?.let { id ->
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (_: Exception) {
                // The media runtime may not have finished initializing yet.
            }
        }
        super.onStopped()
    }

    private fun outputQuality(item: DownloadItem): String =
        if (item.kind == DownloadItem.KIND_AUDIO) {
            "${item.extension.uppercase()}_${item.formatSelector}"
        } else {
            item.quality
        }

    private fun safeQuality(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_").trim('_').ifBlank { "media" }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private val MEDIA_EXTENSIONS = setOf("mp4", "mp3", "m4a", "mkv", "webm", "opus", "aac")
    }
}
