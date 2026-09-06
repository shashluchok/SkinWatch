package com.shashluchok.skinwatch.data.storage.pricesync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncOutcome
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal class PriceSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result = when (get<SyncPriceSnapshotsInteractor>().invoke()) {
        PriceSyncOutcome.HadFailures -> Result.retry()
        PriceSyncOutcome.Completed, PriceSyncOutcome.Skipped -> Result.success()
    }
}
