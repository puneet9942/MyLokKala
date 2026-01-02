package com.example.museapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.OnCompleteListener

/**
 * Helper for the SMS User Consent API.
 *
 * Usage:
 *  - Call startListening(context) and provide a lambda that receives a consent Intent:
 *      UserConsentHelper.startListening(context) { consentIntent -> activityLauncher.launch(consentIntent) }
 *  - When done or on dispose call stopListening(context)
 */
object UserConsentHelper {
    private const val TAG = "UserConsentHelper"
    private var receiver: BroadcastReceiver? = null

    /**
     * Start listening for a single SMS via User Consent API.
     *
     * @param context Application or Activity context (we use it to register/unregister the receiver)
     * @param onConsentIntent Called when SmsRetriever returns a consent Intent. You should call
     *                        startActivityForResult/startActivity via ActivityResultLauncher with that Intent.
     * @param otpLengthMin min OTP digits to extract (default 4)
     * @param otpLengthMax max OTP digits to extract (default 6)
     */
    fun startListening(
        context: Context,
        onConsentIntent: (Intent) -> Unit,
        otpLengthMin: Int = 4,
        otpLengthMax: Int = 6,
    ) {
        stopListening(context)

        // BroadcastReceiver to handle SMS_RETRIEVED_ACTION
        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
                    if (status == null) return

                    when (status.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            // For User Consent API we get an EXTRA_CONSENT_INTENT we must launch
                            val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                            if (consentIntent != null) {
                                try {
                                    onConsentIntent(consentIntent)
                                } catch (t: Throwable) {
                                    Log.w(TAG, "Failed to deliver consent intent to caller", t)
                                }
                            } else {
                                Log.w(TAG, "Consent intent null")
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            Log.d(TAG, "User consent timed out")
                        }
                        else -> {
                            Log.d(TAG, "Status code: ${status.statusCode}")
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Receiver error", t)
                }
            }
        }

        try {
            // API 34+ requires explicit exported/not-exported flag for runtime registration.
            if (Build.VERSION.SDK_INT >= 34) {
                // register with exported flag so broadcasts from Play Services can be received.
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                // Older overload: we pass the SEND_PERMISSION so only SMS Retriever broadcasts reach us.
                // Note: registerReceiver(receiver, filter, receiverPermission, handler?) exists on older APIs.
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    SmsRetriever.SEND_PERMISSION,
                    null,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "registerReceiver attempt with flags/permission failed, trying fallback", t)
            try {
                // fallback to the simple overload if available
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (ex: Throwable) {
                Log.w(TAG, "fallback registerReceiver failed", ex)
                // if registration failed we simply won't be able to listen
            }
        }

        // Start the SmsRetriever user consent client (pass null to listen for any sender)
        try {
            val client = SmsRetriever.getClient(context.applicationContext)
            client.startSmsUserConsent(null)
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "startSmsUserConsent started")
                    } else {
                        Log.w(TAG, "startSmsUserConsent failed to start")
                    }
                })
        } catch (t: Throwable) {
            Log.w(TAG, "startSmsUserConsent exception", t)
        }
    }

    fun stopListening(context: Context) {
        try {
            receiver?.let { context.unregisterReceiver(it) }
        } catch (t: Throwable) {
            Log.w(TAG, "unregisterReceiver failed", t)
        } finally {
            receiver = null
        }
    }
}
