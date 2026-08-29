package com.shashluchok.skinwatch.presentation.component

internal sealed interface SharedElementKey {
    val itemId: Long

    data class Container(
        override val itemId: Long,
    ) : SharedElementKey

    data class Icon(
        override val itemId: Long,
    ) : SharedElementKey

    data class Title(
        override val itemId: Long,
    ) : SharedElementKey
}
