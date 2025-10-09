package com.example.lokkala.util

object AppConstants {
 /*   const val BASE_URL = "https://api.example.com/"
    const val PREFS_NAME = "app_prefs"
    const val TIMEOUT = 30_000L
    const val OTP_LENGTH = 6
    const val DEFAULT_COUNTRY_CODE = "+91" */
 val DEFAULT_INTERESTS: List<String> = listOf(
     "Dancer",
     "Singer",
     "DJ",
     "Comedian",
     "Drama Actor",
     "Magician",
     "Band",
     "Dhol Player",
     "Choreographer",
     "Folk Singer",
     "Folk Dancer",
     "Instrument Player",
     "Poet",
     "Sketch Artist",
     "Makeup Artist",
     "Influencer"
 )

    // Default coords for Faridabad (used as fallback)
    const val DEFAULT_LAT: Double = 28.408913
    const val DEFAULT_LNG: Double = 77.317787
    const val DEFAULT_LOCATION_NAME: String = "Faridabad (default)"

}