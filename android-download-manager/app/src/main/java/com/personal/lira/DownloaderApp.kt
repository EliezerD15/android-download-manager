package com.personal.lira

import android.app.Application
import com.personal.lira.runtime.MediaRuntime
import com.personal.lira.worker.DownloadNotifications

class DownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadNotifications.createChannel(this)
        MediaRuntime.initialize(this)
    }
}
