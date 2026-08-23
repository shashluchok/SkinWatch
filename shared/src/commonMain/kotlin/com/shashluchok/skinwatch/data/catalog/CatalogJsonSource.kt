package com.shashluchok.skinwatch.data.catalog

import io.ktor.utils.io.ByteReadChannel
import kotlinx.io.Source

/**
 * Bridges an HTTP response body to a [Source] the JSON decoder can stream from without
 * materializing the whole body in memory first. Real streaming needs a way to suspend while
 * waiting for more bytes off the wire, which is only available where a thread can block
 * (JVM/POSIX targets) -- js/wasmJs fall back to reading the whole channel eagerly.
 */
internal expect suspend fun ByteReadChannel.toJsonSource(): Source
