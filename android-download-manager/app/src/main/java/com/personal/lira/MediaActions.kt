package com.personal.lira

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.personal.lira.data.DownloadItem
import java.io.File

object MediaActions {
    fun uriFor(context: Context, item: DownloadItem): Uri? {
        item.contentUri?.let { return Uri.parse(it) }
        val path = item.localPath ?: return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(path),
        )
    }
}
