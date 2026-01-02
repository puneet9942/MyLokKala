package com.example.museapp.util

import com.google.i18n.phonenumbers.PhoneNumberUtil

object ValidationUtils {

    /**
     * Dynamically map country calling code to ISO code using libphonenumber.
     * Example: "+91" -> "IN", "+93" -> "AF"
     */
    fun getCountryIsoFromCode(countryCode: String): String? {
        val code = countryCode.replace("+", "").trimStart('0')
        return try {
            PhoneNumberUtil.getInstance().getRegionCodesForCountryCode(code.toInt()).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the given phone number is valid for the given country using libphonenumber.
     * @param phone The phone number including country code, e.g. "+919876543210"
     * @param countryIso The ISO code, e.g. "IN" for India, "US" for USA.
     */
    fun isValidPhoneForCountry(phone: String, countryIso: String): Boolean {
        val util = PhoneNumberUtil.getInstance()
        return try {
            val number = util.parse(phone, countryIso)
            util.isValidNumber(number)
        } catch (e: Exception) {
            false
        }
    }
}