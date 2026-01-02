package com.example.museapp.util

import android.util.Log

object AppLogger {
    private const val TAG = "LokKalaApp"

    fun d(message: String, tag: String = TAG) = Log.d(tag, message)
    fun i(message: String, tag: String = TAG) = Log.i(tag, message)
    fun e(message: String, tag: String = TAG) = Log.e(tag, message)
    fun w(message: String, tag: String = TAG) = Log.w(tag, message)
}