package com.example.museapp.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneUtils {

    data class ParsedPhone(
        val countryDialCode: String?, // "+91" or null if unknown
        val nationalNumber: String     // digits only (no formatting)
    )
    fun formatE164(countryCode: String, localNumber: String): String {
        val cc = countryCode.trim().removePrefix("+").replace("\\s".toRegex(), "")
        val num = localNumber.trim().removePrefix("+").replace("\\s".toRegex(), "")
        return "+$cc$num"
    }
    /**
     * Parse an arbitrary phone hint string (from the Phone Hint API or similar).
     * Returns a ParsedPhone containing an optional country dial code (like "+91")
     * and a digits-only national number (no formatting characters).
     *
     * defaultRegion should be an ISO 3166-1 two-letter region (e.g., "IN", "US") to help parsing.
     * If parsing fails, countryDialCode will be null and nationalNumber will contain all digits extracted.
     */
    fun parsePhoneHint(raw: String, defaultRegion: String = "US"): ParsedPhone {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val trimmed = raw.trim()

        return try {
            val parsed = phoneUtil.parse(trimmed, defaultRegion)
            val dial = "+${parsed.countryCode}"
            val national = parsed.nationalNumber.toString()
            ParsedPhone(dial, national)
        } catch (e: NumberParseException) {
            // Fallback: strip to digits only
            val digitsOnly = trimmed.filter { it.isDigit() }
            ParsedPhone(null, digitsOnly)
        } catch (t: Throwable) {
            val digitsOnly = trimmed.filter { it.isDigit() }
            ParsedPhone(null, digitsOnly)
        }
    }

    /**
     * Optional helper: return E.164 if possible from a raw hint string.
     * Attempts to parse and return full E.164 (e.g., "+911234567890"), or null if unable.
     */
    fun toE164FromHint(raw: String, defaultRegion: String = "US"): String? {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val parsed = phoneUtil.parse(raw.trim(), defaultRegion)
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (_: Exception) {
            null
        }
    }

}
