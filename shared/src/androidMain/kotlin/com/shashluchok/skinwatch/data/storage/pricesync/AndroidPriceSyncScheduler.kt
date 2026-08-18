package com.shashluchok.skinwatch.data.storage.pricesync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import kotlin.time.toJavaDuration

private const val PRICE_SYNC_WORK_NAME = "price-sync"

internal class AndroidPriceSyncScheduler(
    private val context: Context,
) : PriceSyncScheduler {
    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(PRICE_SYNC_INTERVAL.toJavaDuration())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRICE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
