package com.shashluchok.skinwatch.data.storage.catalog

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shashluchok.skinwatch.domain.catalog.CATALOG_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import kotlin.time.toJavaDuration

private const val CATALOG_SYNC_WORK_NAME = "catalog-sync"

internal class AndroidCatalogSyncScheduler(
    private val context: Context,
) : CatalogSyncScheduler {
    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(CATALOG_SYNC_INTERVAL.toJavaDuration())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CATALOG_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
