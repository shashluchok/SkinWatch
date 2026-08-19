package com.shashluchok.skinwatch.data.storage.catalog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsInteractor
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal class CatalogSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result {
        get<SyncCatalogItemsInteractor>().invoke()
        return Result.success()
    }
}
