package com.example.museapp.presentation.ui.screens

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun SplashScreen() {
    val activity: Activity? = LocalActivity.current

    activity?.let { act ->
        DisposableEffect(act.window, act) {
            val appCompat = act as? AppCompatActivity
            val wasActionBarVisible = appCompat?.supportActionBar?.isShowing ?: false
            if (wasActionBarVisible) appCompat?.supportActionBar?.hide()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.setDecorFitsSystemWindows(act.window, false)
                val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                act.window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                @Suppress("DEPRECATION")
                act.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            onDispose {
                if (wasActionBarVisible) appCompat?.supportActionBar?.show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowCompat.setDecorFitsSystemWindows(act.window, true)
                    val controller = WindowInsetsControllerCompat(act.window, act.window.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    @Suppress("DEPRECATION")
                    act.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // intentionally empty — do NOT draw the full-screen splash image here
    }
}
