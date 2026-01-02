package com.example.museapp.data.util

import org.json.JSONObject

/**
 * Defensive JSON parser for common API error shapes.
 */
fun parseApiErrorBody(body: String?): ParsedApiError? {
    if (body.isNullOrBlank()) return null
    return try {
        val root = JSONObject(body)
        val status = root.optString("status", null)
        val statusCode = if (root.has("status_code") && !root.isNull("status_code")) root.optInt("status_code") else null
        val message = if (root.has("message") && !root.isNull("message")) root.optString("message", null) else null
        val requestId = if (root.has("request_id") && !root.isNull("request_id")) root.optString("request_id", null) else null

        var errorDetail: ParsedApiError.ErrorDetail? = null
        if (root.has("error") && !root.isNull("error")) {
            val err = root.opt("error")
            if (err is JSONObject) {
                val code = if (err.has("code") && !err.isNull("code")) err.optString("code", null) else null
                val type = if (err.has("type") && !err.isNull("type")) err.optString("type", null) else null
                val details = if (err.has("details") && !err.isNull("details")) {
                    try { err.get("details") } catch (_: Throwable) { null }
                } else null
                errorDetail = ParsedApiError.ErrorDetail(code = code, type = type, details = details)
            } else {
                val errStr = root.optString("error", null)
                if (!errStr.isNullOrBlank()) {
                    errorDetail = ParsedApiError.ErrorDetail(code = errStr, type = null, details = null)
                }
            }
        }

        ParsedApiError(
            status = status,
            status_code = statusCode,
            message = message,
            error = errorDetail,
            request_id = requestId
        )
    } catch (t: Throwable) {
        ParsedApiError(message = body)
    }
}
