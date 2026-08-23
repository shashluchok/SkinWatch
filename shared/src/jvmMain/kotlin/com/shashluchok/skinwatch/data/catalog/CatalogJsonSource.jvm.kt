package com.shashluchok.skinwatch.data.catalog

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asSource
import kotlinx.io.Source
import kotlinx.io.buffered

internal actual suspend fun ByteReadChannel.toJsonSource(): Source = asSource().buffered()
