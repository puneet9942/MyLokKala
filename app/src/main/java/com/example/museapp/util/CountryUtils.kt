package com.example.museapp.util

import com.example.museapp.domain.model.Country
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

fun isoToFlagEmoji(iso: String): String {
    return iso.uppercase().map {
        if (it in 'A'..'Z') Character.toChars(0x1F1E6 + (it - 'A')).concatToString() else ""
    }.joinToString("")
}

fun getAllCountries(): List<Country> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return phoneUtil.supportedRegions.map { iso ->
        val code = "+" + phoneUtil.getCountryCodeForRegion(iso)
        val locale = Locale("", iso)
        val name = locale.displayCountry
        val flag = isoToFlagEmoji(iso)
        Country(
            name = name,
            iso = iso,
            code = code,
            flagEmoji = flag
        )
    }.sortedBy { it.name }
}