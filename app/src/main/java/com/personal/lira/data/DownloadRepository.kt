package com.personal.lira.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.personal.lira.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow

class DownloadRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.get(appContext).downloadDao()
    private val workManager = WorkManager.getInstance(appContext)

    fun observeDownloads(): Flow<List<DownloadItem>> = dao.observeAll()

    suspend fun enqueue(item: DownloadItem) {
        dao.upsert(item)
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_DOWNLOAD_ID to item.id,
                ),
            )
            .addTag("lira-download-${item.id}")
            .build()

        workManager.enqueueUniqueWork(
            "lira-download-${item.id}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun getById(id: String): DownloadItem? = dao.getById(id)

    suspend fun save(item: DownloadItem) = dao.upsert(item)

    suspend fun delete(id: String) {
        workManager.cancelUniqueWork("lira-download-$id")
        dao.delete(id)
    }

    suspend fun cancel(id: String) {
        workManager.cancelUniqueWork("lira-download-$id")
        dao.getById(id)?.let { item ->
            dao.upsert(
                item.copy(
                    status = DownloadItem.STATUS_CANCELLED,
                    error = null,
                ),
            )
        }
    }
}
