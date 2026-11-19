package com.personal.lira.runtime

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred

/**
 * Starts yt-dlp and its bundled FFmpeg setup from Application.onCreate,
 * without making app startup wait for the package extraction work.
 */
object MediaRuntime {
    private val ready = CompletableDeferred<Unit>()
    @Volatile private var started = false

    @Synchronized
    fun initialize(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        Thread({
            try {
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                ready.complete(Unit)
            } catch (error: Throwable) {
                ready.completeExceptionally(error)
            }
        }, "lira-media-init").start()
    }

    suspend fun awaitReady() {
        ready.await()
    }
}
