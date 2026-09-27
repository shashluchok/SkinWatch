package com.shashluchok.skinwatch.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

/**
 * An item's icon, with a placeholder for the stretch before it arrives.
 *
 * Icons come from the network on screens made of them, so the wait is the normal case rather than the
 * exception. A failed load settles into a still block: a shimmer that never ends reads as an image
 * still on its way.
 *
 * Drawn as a painter with the placeholder over it rather than with Coil's `SubcomposeAsyncImage`,
 * which wraps every image in a `SubcomposeLayout` -- these are drawn per row of a lazy list, and the
 * bounds are always given by the caller anyway, so there is nothing to subcompose against.
 */
@Composable
internal fun ItemImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val painter = rememberAsyncImagePainter(model = model, contentScale = contentScale)
    val state = painter.state.collectAsState().value

    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> ShimmerBlock(
                sweep = rememberShimmerSweep(),
                modifier = Modifier.fillMaxSize(),
            )

            is AsyncImagePainter.State.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Success,
            -> Unit
        }
    }
}
