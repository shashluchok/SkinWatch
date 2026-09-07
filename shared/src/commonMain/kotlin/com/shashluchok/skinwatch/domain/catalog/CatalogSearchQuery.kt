package com.shashluchok.skinwatch.domain.catalog

internal const val MAX_SEARCH_TOKENS = 4

/**
 * Punctuation is dropped rather than replaced with a space, so `AK-47` becomes the single word
 * `ak47` and can be typed that way. Stored names and typed words both go through this.
 */
internal fun String.toSearchableName(): String = lowercase()
    .filter { it.isLetterOrDigit() || it.isWhitespace() }

internal fun String.toSearchTokens(): List<String> = split(' ', '\t', '\n')
    .map { it.toSearchableName().trim() }
    .filter { it.isNotEmpty() }
    .take(MAX_SEARCH_TOKENS)
