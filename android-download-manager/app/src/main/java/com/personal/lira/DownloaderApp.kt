package com.personal.lira

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // 1. Inicializa el motor de descargas
            YoutubeDL.getInstance().init(this)

            // 2. Lo actualiza en segundo plano para evitar bloqueos
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        this@DownloaderApp,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                    Log.d("YTDL", "Motor actualizado correctamente")
                } catch (e: Exception) {
                    Log.e("YTDL", "Error actualizando el motor", e)
                }
            }
        } catch (e: Exception) {
            Log.e("YTDL", "Error inicializando el motor", e)
        }
    }
}
