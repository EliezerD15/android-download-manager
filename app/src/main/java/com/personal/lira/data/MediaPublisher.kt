package com.personal.lira.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import java.io.File

data class PublishedFile(val contentUri: String?, val localPath: String?)

object MediaPublisher {
    fun publishMedia(context: Context, source: File, item: DownloadItem): PublishedFile {
        val audio = item.kind == DownloadItem.KIND_AUDIO
        val mediaFolder = if (audio) "Music" else "Video"
        val subfolder = "$DOWNLOADS_FOLDER/$mediaFolder"
        val mimeType = when (source.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "webm" -> if (audio) "audio/webm" else "video/webm"
            "mkv" -> "video/x-matroska"
            "opus" -> "audio/ogg"
            "aac" -> "audio/aac"
            else -> if (audio) "audio/mp4" else "video/mp4"
        }
        return publish(context, source, source.name, mimeType, subfolder)
    }

    fun publishLyrics(context: Context, source: File, displayName: String): PublishedFile =
        publish(context, source, displayName, "text/plain", "$DOWNLOADS_FOLDER/Music")

    private fun publish(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        subfolder: String,
    ): PublishedFile {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )
            val directory = File(downloads, subfolder)
            if (!directory.exists() && !directory.mkdirs()) {
                error("No se pudo crear la carpeta pública de descargas.")
            }
            val destination = File(directory, displayName)
            source.copyTo(destination, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destination.absolutePath),
                arrayOf(mimeType),
                null,
            )
            return PublishedFile(contentUri = null, localPath = destination.absolutePath)
        }

        val collection: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$subfolder",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: error("Android no pudo registrar el archivo descargado.")

        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("No se pudo escribir el archivo descargado.")
            val readyValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, readyValues, null, null)
            return PublishedFile(contentUri = uri.toString(), localPath = null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private const val DOWNLOADS_FOLDER = "Descargas LeonGO"
}
