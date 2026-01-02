package com.example.museapp.util

/**
 * Keep only digits and a single dot. If multiple dots are present in the input,
 * only the first dot is preserved. Use this to sanitize numeric text inputs
 * (travel radius, prices, etc.).
 *
 * Examples:
 * "12a.3b4" -> "12.34"
 * ".123" -> ".123"
 * "1.2.3" -> "1.23" (second dot removed)
 */
fun String.filterDigitsAndDot(): String {
    // Avoid named argument on Java StringBuilder constructor (named args prohibited on non-Kotlin functions)
    val sb = StringBuilder(this.length)
    var dotSeen = false
    for (c in this) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && !dotSeen -> {
                sb.append(c)
                dotSeen = true
            }
            // skip everything else
        }
    }
    return sb.toString()
}
