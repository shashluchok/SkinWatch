package com.shashluchok.skinwatch.domain.catalog

/**
 * Result of fetching one [CatalogCategory] from [ItemCatalogRemoteSource].
 */
internal sealed interface CatalogFetchResult<out T> {
    data class Success<T>(
        val data: T,
    ) : CatalogFetchResult<T>

    data class Failure(
        val error: CatalogFetchError,
    ) : CatalogFetchResult<Nothing>
}

internal sealed interface CatalogFetchError {
    data object Network : CatalogFetchError

    data object InvalidResponse : CatalogFetchError

    data class Unknown(
        val message: String?,
    ) : CatalogFetchError
}
