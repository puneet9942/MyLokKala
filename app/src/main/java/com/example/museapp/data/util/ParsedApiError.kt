package com.example.museapp.data.util

data class ParsedApiError(
    val status: String? = null,
    val status_code: Int? = null,
    val message: String? = null,
    val error: ErrorDetail? = null,
    val request_id: String? = null
) {
    data class ErrorDetail(
        val code: String? = null,
        val type: String? = null,
        val details: Any? = null
    )
}
