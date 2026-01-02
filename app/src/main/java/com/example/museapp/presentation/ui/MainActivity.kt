package com.example.museapp.presentation.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.museapp.R
import com.example.museapp.data.util.AppError
import com.example.museapp.data.util.AppErrorBroadcaster
import com.example.museapp.presentation.ui.navigation.AppNavHost
import com.example.museapp.ui.theme.LokKalaTheme
import com.example.museapp.util.LoadingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // make injected field non-private so Hilt can inject it.
    // keep nullable so Activity doesn't crash if DI binding isn't present.
    @JvmField
    @Inject
    var appErrorBroadcaster: AppErrorBroadcaster? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install system splash early
        val splash: SplashScreen = installSplashScreen()

        // keep splash while our app is not ready
        var isAppReady = false
        splash.setKeepOnScreenCondition { !isAppReady }

        super.onCreate(savedInstanceState)

        // ------------------------
        // Set a full-screen window background from the splash PNG (center-cropped)
        // and attempt to hide the system splash icon overlay asap (no XML changes).
        // ------------------------
        try {
            setFullScreenWindowBackground(R.drawable.splash)
        } catch (t: Throwable) {
            // fallback to previous behavior if something goes wrong
            window.setBackgroundDrawableResource(R.drawable.splash)
        }

        // Try to hide the system splash icon view as early as possible (several short retries).
        // This avoids the centered launcher icon overlay that Android adds on top of the window background.
        try {
            hideSystemSplashIconWithRetries()
        } catch (_: Exception) {
            // ignore silently
        }

        // Observe optional app-wide errors (toasts)
        observeAppErrors()

        // Optional pre-draw listener (keeps previous behavior)
        val contentView = window.decorView
        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            }
        )

        // Exit animation: fade & slight scale, then remove splash and set runtime window background to white
        splash.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            // ensure the splash view shows our full-screen bitmap
            setSplashViewBackgroundToBitmap(splashView, R.drawable.splash)

            // hide the system icon again as a fallback just before animation
            try {
                val iconResId = resources.getIdentifier("splashscreen_icon", "id", packageName)
                if (iconResId != 0) {
                    val iconView = splashView.findViewById<View?>(iconResId)
                    iconView?.visibility = View.GONE
                }
            } catch (_: Exception) { /* ignore */ }

            val fade = ObjectAnimator.ofFloat(splashView, "alpha", 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(splashView, "scaleX", 1f, 1.02f)
            val scaleY = ObjectAnimator.ofFloat(splashView, "scaleY", 1f, 1.02f)

            fade.duration = 220
            scaleX.duration = 220
            scaleY.duration = 220

            fade.interpolator = DecelerateInterpolator(2f)
            scaleX.interpolator = DecelerateInterpolator(2f)
            scaleY.interpolator = DecelerateInterpolator(2f)

            fade.start()
            scaleX.start()
            scaleY.start()

            fade.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // Remove system-provided splash view
                    splashScreenViewProvider.remove()

                    // Now set runtime window background to pure white
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))
                }

                override fun onAnimationCancel(animation: Animator) {
                    splashScreenViewProvider.remove()
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        setContent {
            LokKalaTheme {
                // collect global loading state exposed by LoadingManager
                val isLoading by LoadingManager.isLoading.collectAsState(initial = false)

                // ----------------------------
                // Splash coordination logic:
                // Keep splash until:
                //  - minimum time has passed (MIN_SPLASH_MS),
                //  - Compose has rendered once (compositionReady),
                //  - and global loading is false (isLoading == false).
                // ----------------------------
                val MIN_SPLASH_MS = 900L

                var compositionReady by remember { mutableStateOf(false) }
                var minTimePassed by remember { mutableStateOf(false) }

                // 1) ensure the minimum time has passed
                LaunchedEffect(Unit) {
                    delay(MIN_SPLASH_MS)
                    minTimePassed = true
                }

                // 2) mark composition as ready after first composition
                LaunchedEffect(Unit) {
                    compositionReady = true
                }

                // 3) allow splash to exit only when all three conditions are met
                LaunchedEffect(compositionReady, minTimePassed, isLoading) {
                    if (compositionReady && minTimePassed && !isLoading) {
                        isAppReady = true
                    } else {
                        isAppReady = false
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    // Root white Surface ensures runtime content sits on white background after handoff
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFFFFFFF)
                    ) { /* background only */ }

                    // App content
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        val navController = rememberNavController()
                        AppNavHost(navController = navController, modifier = Modifier.fillMaxSize())
                    }

                    // Compose loading overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x80000000))
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Load a bitmap resource and set it as the window background, scaled center-crop to the device
     * screen size so it fills full screen (no letterbox).
     *
     * Put the splash image in res/drawable-nodpi/splash.png to avoid density scaling.
     */
    private fun setFullScreenWindowBackground(drawableResId: Int) {
        val metrics = resources.displayMetrics
        val targetW = metrics.widthPixels
        val targetH = metrics.heightPixels

        // load source bitmap efficiently
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        val srcBitmap: Bitmap = BitmapFactory.decodeResource(resources, drawableResId, options)
            ?: return

        // calculate scale to cover (centerCrop)
        val scale = maxOf(targetW.toFloat() / srcBitmap.width, targetH.toFloat() / srcBitmap.height)
        val scaledW = (srcBitmap.width * scale).toInt()
        val scaledH = (srcBitmap.height * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, scaledW, scaledH, true)

        // center-crop: take center region of scaledBitmap sized targetW x targetH
        val x = (scaledBitmap.width - targetW) / 2
        val y = (scaledBitmap.height - targetH) / 2
        val finalBitmap = Bitmap.createBitmap(scaledBitmap, x, y, targetW, targetH)

        val drawable = BitmapDrawable(resources, finalBitmap)
        // ensure drawable will fill the window bounds
        drawable.setBounds(0, 0, targetW, targetH)

        window.setBackgroundDrawable(drawable)
    }

    /**
     * Attempt to hide the internal system splash icon (splashscreen_icon) several times
     * with short delays so it disappears quickly on boot across different OEMs.
     *
     * This is a runtime-only approach so no XML theme changes are required.
     */
    private fun hideSystemSplashIconWithRetries() {
        val decor = window.decorView ?: return
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0
        val maxAttempts = 10
        val delayMs = 40L

        val hideRunnable = object : Runnable {
            override fun run() {
                attempts++
                try {
                    val iconResId = resources.getIdentifier("splashscreen_icon", "id", packageName)
                    if (iconResId != 0) {
                        val iconView = decor.findViewById<View?>(iconResId)
                        if (iconView != null) {
                            iconView.visibility = View.GONE
                            return // success — stop retrying
                        }
                    }
                } catch (_: Exception) {
                    // ignore and retry
                }

                if (attempts < maxAttempts) {
                    handler.postDelayed(this, delayMs)
                }
            }
        }

        handler.post(hideRunnable)
    }

    private fun observeAppErrors() {
        appErrorBroadcaster?.let { broadcaster ->
            lifecycleScope.launch {
                broadcaster.errors().collectLatest { error ->
                    val msg = when (error) {
                        is AppError.Network -> error.message
                        is AppError.TooManyRequests -> error.message
                        is AppError.ServerError -> error.message
                        is AppError.AuthFailure -> error.message ?: "Authentication required"
                        is AppError.Unknown -> error.message
                        is AppError.InlineFieldError -> error.message
                        else -> error.toString()
                    } ?: "Something went wrong"

                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setSplashViewBackgroundToBitmap(splashView: View, drawableResId: Int) {
        try {
            val metrics = resources.displayMetrics
            val targetW = metrics.widthPixels
            val targetH = metrics.heightPixels

            val options = BitmapFactory.Options().apply { inScaled = false }
            val srcBitmap: Bitmap = BitmapFactory.decodeResource(resources, drawableResId, options) ?: return

            val scale = maxOf(targetW.toFloat() / srcBitmap.width, targetH.toFloat() / srcBitmap.height)
            val scaledW = (srcBitmap.width * scale).toInt()
            val scaledH = (srcBitmap.height * scale).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, scaledW, scaledH, true)

            val x = (scaledBitmap.width - targetW) / 2
            val y = (scaledBitmap.height - targetH) / 2
            val finalBitmap = Bitmap.createBitmap(scaledBitmap, x, y, targetW, targetH)

            val bd = BitmapDrawable(resources, finalBitmap)
            // apply as view background (fills the view)
            splashView.background = bd
        } catch (t: Throwable) {
            // swallow; nothing fatal (we fallback to window background)
        }
    }
}
