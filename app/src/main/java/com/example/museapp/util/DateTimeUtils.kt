package com.example.museapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val defaultFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    fun formatDate(date: Date): String = defaultFormat.format(date)
    fun currentTimestamp(): Long = System.currentTimeMillis()
}