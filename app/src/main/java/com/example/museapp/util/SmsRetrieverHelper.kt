package com.example.museapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.OnCompleteListener

/**
 * SMS Retriever helper (updated for Android 14+).
 * - Uses Context.RECEIVER_EXPORTED when required by the platform so registration won't throw.
 * - Keeps older-device behavior unchanged.
 */
object SmsRetrieverHelper {
    private const val TAG = "SmsRetrieverHelper"

    private var receiver: BroadcastReceiver? = null

    fun startListening(
        context: Context,
        listener: (String) -> Unit,
        otpLengthMin: Int = 4,
        otpLengthMax: Int = 6,
    ) {
        stopListening(context) // avoid duplicates

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? com.google.android.gms.common.api.Status
                    if (status == null) return

                    when (status.statusCode) {
                        com.google.android.gms.common.api.CommonStatusCodes.SUCCESS -> {
                            val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                            Log.d(TAG, "SMS retrieved: $message")
                            message?.let {
                                val otp = extractOtpFromText(it, otpLengthMin, otpLengthMax)
                                if (otp != null) listener(otp)
                                else Log.d(TAG, "No OTP found in SMS")
                            }
                        }
                        com.google.android.gms.common.api.CommonStatusCodes.TIMEOUT -> {
                            Log.d(TAG, "SMS Retriever timed out")
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Receiver error", t)
                }
            }
        }

        try {
            // Android 14+ requires explicit exported/not-exported flag on runtime-registered receivers
            if (Build.VERSION.SDK_INT >= 34) {
                // This allows broadcasts from other apps (e.g., Google Play services) to be delivered.
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                // Older devices: use the classic registration
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "registerReceiver failed", t)
            // try fallback (older overload) if the above call fails for any reason
            try {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (ex: Throwable) {
                Log.w(TAG, "fallback registerReceiver also failed", ex)
            }
        }

        // start SmsRetriever client
        val client = SmsRetriever.getClient(context.applicationContext)
        client.startSmsRetriever()
            .addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "SmsRetriever started")
                } else {
                    Log.w(TAG, "SmsRetriever failed to start")
                }
            })
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

    private fun extractOtpFromText(text: String, otpLengthMin: Int, otpLengthMax: Int): String? {
        val regex = Regex("""\b(\d{$otpLengthMin,$otpLengthMax})\b""")
        val match = regex.find(text)
        return match?.groups?.get(1)?.value
    }
}
