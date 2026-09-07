package com.shashluchok.skinwatch.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogSearchQueryTest {
    @Test
    fun `a name keeps its letters, digits and spaces and loses everything else`() {
        assertEquals("ak47  redline fieldtested", "AK-47 | Redline (Field-Tested)".toSearchableName())
    }

    @Test
    fun `dropping punctuation closes the gap it left`() {
        assertEquals("ak47", "AK-47".toSearchableName())
        assertEquals("stattraksouvenir", "StatTrak/Souvenir".toSearchableName())
    }

    @Test
    fun `a query becomes the words that all have to match`() {
        assertEquals(listOf("ak", "redline"), "ak redline".toSearchTokens())
    }

    @Test
    fun `words the user typed with punctuation match the same way names are stored`() {
        assertEquals(listOf("ak47", "fieldtested"), "AK-47 (Field-Tested)".toSearchTokens())
    }

    @Test
    fun `runs of whitespace do not become empty words`() {
        assertEquals(listOf("ak", "redline"), "  ak   redline  ".toSearchTokens())
    }

    @Test
    fun `a query of nothing but punctuation asks for nothing`() {
        assertEquals(emptyList(), "|()-".toSearchTokens())
        assertEquals(emptyList(), "   ".toSearchTokens())
    }

    @Test
    fun `no more words are taken than the query can bind`() {
        val tokens = "one two three four five six".toSearchTokens()

        assertEquals(MAX_SEARCH_TOKENS, tokens.size)
        assertEquals(listOf("one", "two", "three", "four"), tokens)
    }
}
