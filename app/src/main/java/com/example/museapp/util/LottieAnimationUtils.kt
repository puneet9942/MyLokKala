package com.example.museapp.util


import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.airbnb.lottie.compose.*

object LottieAnimationUtil {

    /**
     * Generic composable for Lottie animations.
     * Works for raw resources, assets, or URLs.
     * All playback settings are configurable.
     */
    @Composable
    fun Play(
        source: LottieSource,
        modifier: Modifier = Modifier,
        size: Dp = 120.dp,
        isPlaying: Boolean = true,
        speed: Float = 1.0f,
        iterations: Int = LottieConstants.IterateForever,
        restartOnPlay: Boolean = false,
        clipToCompositionBounds: Boolean = true,
        maintainOriginalImageBounds: Boolean = true
    ) {
        val composition by rememberLottieComposition(
            when (source) {
                is LottieSource.Raw -> LottieCompositionSpec.RawRes(source.resId)
                is LottieSource.Asset -> LottieCompositionSpec.Asset(source.assetName)
                is LottieSource.Url -> LottieCompositionSpec.Url(source.url)
            }
        )

        val progress by animateLottieCompositionAsState(
            composition = composition,
            isPlaying = isPlaying,
            iterations = iterations,
            speed = speed,
            restartOnPlay = restartOnPlay
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier.size(size),
            clipToCompositionBounds = clipToCompositionBounds,
            maintainOriginalImageBounds = maintainOriginalImageBounds
        )
    }
}

/**
 * Wrapper class to cleanly represent source type.
 */
sealed class LottieSource {
    data class Raw(val resId: Int) : LottieSource()
    data class Asset(val assetName: String) : LottieSource()
    data class Url(val url: String) : LottieSource()
}


/*
Sample usages:

LottieAnimationUtil.Play(
    source = LottieSource.Raw(R.raw.loading_animation),
    size = 100.dp,
    speed = 1.2f,
    iterations = LottieConstants.IterateForever
)

LottieAnimationUtil.Play(
    source = LottieSource.Asset("lottie/success_tick.json"),
    size = 90.dp,
    iterations = 1,      // play once
    isPlaying = true
)

LottieAnimationUtil.Play(
    source = LottieSource.Url("https://lottie.host/sample_animation.json"),
    size = 120.dp,
    speed = 0.8f,
    iterations = 2
)




 */