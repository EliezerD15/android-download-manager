package com.personal.lira.runtime

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/**
 * Prepares yt-dlp and FFmpeg from Application.onCreate without blocking the
 * main thread. The updated yt-dlp script must be ready before requests run:
 * the Android wrapper adds --js-runtimes to every command.
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
                // The library injects --js-runtimes into every request, so its
                // bundled yt-dlp must be upgraded before the runtime is marked ready.
                val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(
                    appContext,
                    YoutubeDL.UpdateChannel.STABLE,
                )
                Log.i(TAG, "yt-dlp stable runtime ready: $updateStatus")
                ready.complete(Unit)
            } catch (error: Throwable) {
                Log.e(TAG, "Could not prepare yt-dlp runtime", error)
                ready.completeExceptionally(error)
            }
        }, "lira-media-init").start()
    }

    suspend fun awaitReady() {
        withTimeout(120_000L) {
            ready.await()
        }
    }

    private const val TAG = "MediaRuntime"
    }
