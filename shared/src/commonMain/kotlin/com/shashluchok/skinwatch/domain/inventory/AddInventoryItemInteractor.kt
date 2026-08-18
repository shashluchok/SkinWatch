package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
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

    private companion object {
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
