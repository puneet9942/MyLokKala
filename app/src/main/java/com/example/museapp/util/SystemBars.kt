package com.example.museapp.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides system bars while [active] is true, and restores them on dispose.
 * Keep edge-to-edge enabled in your Activity (setDecorFitsSystemWindows=false).
 */
@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("ContextCastToActivity", "WrongConstant")
@Composable
fun HideSystemBarsDuring(active: Boolean) {
    val activity = LocalContext.current as Activity
    val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)

    DisposableEffect(active) {
        if (active) {
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        onDispose {
            controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    }
}
