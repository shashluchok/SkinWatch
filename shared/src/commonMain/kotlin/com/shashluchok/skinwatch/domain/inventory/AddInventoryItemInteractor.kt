package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong
import kotlin.time.Clock

internal class AddInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
    private val steamMarketRepository: SteamMarketRepository,
    private val priceSnapshotRepository: PriceSnapshotRepository,
    private val resolveDisplayCurrency: ResolveDisplayCurrencyInteractor,
) {
    suspend operator fun invoke(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePriceAmount: Double,
    ) {
        val currency = resolveDisplayCurrency()
        val purchasePrice = Money(
            minorUnits = (purchasePriceAmount * MINOR_UNITS_PER_MAJOR_UNIT).roundToLong(),
            currency = currency,
        )
        inventoryRepository.addItem(
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            quantity = quantity,
            purchasePrice = purchasePrice,
        )
        if (needsFreshPrice(marketHashName)) {
            val overview = steamMarketRepository.getPriceOverview(marketHashName = marketHashName, currency = currency)
            if (overview is SteamMarketResult.Success) {
                priceSnapshotRepository.record(
                    marketHashName = marketHashName,
                    overview = overview.data,
                    currency = currency,
                    capturedAt = Clock.System.now(),
                )
            }
        }
    }

    /**
     * Skips the add-time fetch when this marketHashName was already priced recently -- otherwise
     * adding a duplicate of an already-tracked item would write another near-simultaneous snapshot
     * that every item sharing that hash sees too.
     */
    private suspend fun needsFreshPrice(marketHashName: String): Boolean {
        val latestCapturedAt = priceSnapshotRepository
            .observeSnapshots(marketHashName)
            .first()
            .maxOfOrNull { it.capturedAt }
        return latestCapturedAt == null || Clock.System.now() - latestCapturedAt >= PRICE_SYNC_INTERVAL
    }

    private companion object {
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
