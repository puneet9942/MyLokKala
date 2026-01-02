package com.example.museapp.util

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

object GiphyUtils {

    @Composable
    fun FromUrl(
        gifUrl: String,
        modifier: Modifier = Modifier,
        size: Dp = 120.dp,
        contentScale: ContentScale = ContentScale.Fit
    ) {
        val context = LocalContext.current

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(gifUrl)
                .crossfade(true)
                .size(Size.ORIGINAL)
                .build(),
            contentDescription = "Giphy from URL",
            modifier = modifier.size(size),
            contentScale = contentScale
        )
    }

    @Composable
    fun FromAsset(
        assetPath: String, // e.g., "gifs/welcome.gif" inside assets/
        modifier: Modifier = Modifier,
        size: Dp = 120.dp,
        contentScale: ContentScale = ContentScale.Fit
    ) {
        val context = LocalContext.current

        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/$assetPath")
                .build(),
            imageLoader = imageLoader
        )

        Image(
            painter = painter,
            contentDescription = "Giphy from Asset",
            modifier = modifier.size(size),
            contentScale = contentScale
        )
    }
}

/*
Sample usage:
GiphyUtil.FromUrl(
    gifUrl = "https://media.giphy.com/media/l0HlBO7eyXzSZkJri/giphy.gif",
    size = 150.dp
)

GiphyUtil.FromAsset(
    assetPath = "gifs/welcome.gif",    // app/src/main/assets/gifs/welcome.gif
    size = 120.dp
)
 */
