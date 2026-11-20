package com.personal.lira

import android.app.Application
import com.personal.lira.runtime.MediaRuntime
import com.personal.lira.worker.DownloadNotifications

class DownloaderApp : Application() {
 override fun onCreate() {
        super.onCreate()
        try {
            // 1. Inicializa el motor
            YoutubeDL.getInstance().init(this)

            // 2. Lo actualiza en segundo plano para que no se congele la app
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        this@DownloaderApp,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                } catch (e: Exception) {
                    android.util.Log.e("YTDL", "Error actualizando", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YTDL", "Error inicializando", e)
        }
    }
