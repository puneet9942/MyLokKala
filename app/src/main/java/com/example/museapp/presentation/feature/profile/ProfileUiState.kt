package com.example.museapp.presentation.feature.profile

import android.net.Uri

/**
 * UI state for profile setup. Kept fields and aliases to maintain compatibility
 * with screens that referenced 'desc', 'bio', 'mobile', 'MAX_STEP', etc.
 */
data class ProfileUiState(
    val step: Int = 1,
    val name: String = "",
    val dob: String = "",
    val gender: String? = null,
    val description: String = "",
    val biography: String = "",
    val mobile: String? = null,
    val pricingType: String = "fixed",
    val standardPrice: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val travelRadiusKm: String = "",
    val isEventManager: Boolean? = false,
    val profilePicUri: Uri? = null,
    // Keep photos/videos as List<Uri?> for Compose screens; ViewModel will normalize inputs.
    val photos: List<Uri?> = emptyList(),
    val videos: List<Uri?> = emptyList(),
    val instaId: String? = null,
    val twitterId: String? = null,
    val youtubeId: String? = null,
    val facebookId: String? = null,
    val interests: List<String> = emptyList(),
    val customInterest: String? = null,
    val loading: Boolean = false,
    val error: String? = null
) {
    companion object {
        const val MAX_STEP: Int = 4
    }

    // Backwards compatible aliases
    val desc: String get() = description
    val bio: String get() = biography
}
