package com.shashluchok.skinwatch.data.catalog

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.Source

// The single-threaded js/wasmJs event loop can't block a thread to wait for more network bytes,
// so real streaming (asSource()) isn't available here. This is dead code at runtime -- web wires
// ItemCatalogRemoteSource.EMPTY instead of KtorItemCatalogApi -- so an eager, whole-channel read
// is enough to satisfy the contract without over-engineering unused code.
internal actual suspend fun ByteReadChannel.toJsonSource(): Source = readRemaining()
