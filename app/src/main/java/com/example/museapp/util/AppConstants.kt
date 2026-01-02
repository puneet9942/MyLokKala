package com.example.museapp.util

object AppConstants {
   const val BASE_URL = "http://10.0.2.2:3000/" // for emulator
//   const val BASE_URL = "http://192.168.1.3:3000/" // for real device
    const val PREFS_NAME = "app_prefs"
    const val TIMEOUT = 30_000L
    const val OTP_LENGTH = 6
    const val DEFAULT_COUNTRY_CODE = "+91"
    const val USE_FAKE_REPO: Boolean = false
 const val SPLASH_SCREEN_DURATION_MILLISECONDS = 2000L
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
    const val DEFAULT_LNG: Double = 57.317787
    const val DEFAULT_LOCATION_NAME: String = "Nearby"

}