package com.shashluchok.skinwatch.domain.steam

internal class SearchMarketItemsInteractor(
    private val steamMarketRepository: SteamMarketRepository,
    private val resolveDisplayCurrency: ResolveDisplayCurrencyInteractor,
) {
    suspend operator fun invoke(query: String): SteamMarketResult<List<SteamMarketItem>> {
        val currency = resolveDisplayCurrency()
        return steamMarketRepository.searchItems(query = query, currency = currency)
    }
}
