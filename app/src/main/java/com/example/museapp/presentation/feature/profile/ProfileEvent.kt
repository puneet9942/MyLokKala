package com.example.museapp.presentation.feature.profile

import android.net.Uri

/**
 * Events sent from UI to ViewModel. Accept nullable Strings and mixed lists (Any?) for robustness.
 * This prevents argument-type mismatches when some callers pass String ids/paths and others pass Uri.
 */
sealed class ProfileEvent {
    data class NameChanged(val name: String?) : ProfileEvent()
    data class DobChanged(val dob: String?) : ProfileEvent()
    data class GenderChanged(val gender: String?) : ProfileEvent()
    data class DescriptionChanged(val desc: String?) : ProfileEvent()
    data class BiographyChanged(val bio: String?) : ProfileEvent()

    data class MobileChanged(val mobile: String?) : ProfileEvent()

    data class InterestToggled(val interest: String) : ProfileEvent()
    data class CustomInterestChanged(val value: String?) : ProfileEvent()
    object AddCustomInterest : ProfileEvent()

    data class PricingTypeSelected(val type: String?) : ProfileEvent()
    data class StandardPriceChanged(val value: String?) : ProfileEvent()
    data class MinPriceChanged(val value: String?) : ProfileEvent()
    data class MaxPriceChanged(val value: String?) : ProfileEvent()
    data class TravelRadiusChanged(val value: String?) : ProfileEvent()
    data class SetEventManager(val isEventManager: Boolean) : ProfileEvent()

    // Accept mixed lists (Any?) because pickers/callers sometimes use String and sometimes Uri
    data class AddPhotos(val uris: List<Any?>) : ProfileEvent()
    data class AddVideos(val uris: List<Any?>) : ProfileEvent()
    // Accept Any? for single changes/removals as well
    data class ProfilePicChanged(val uri: Any?) : ProfileEvent()
    data class RemovePhoto(val uri: Any?) : ProfileEvent()
    data class RemoveVideo(val uri: Any?) : ProfileEvent()

    data class InstaChanged(val id: String?) : ProfileEvent()
    data class TwitterChanged(val id: String?) : ProfileEvent()
    data class YoutubeChanged(val id: String?) : ProfileEvent()
    data class FacebookChanged(val id: String?) : ProfileEvent()

    object NextStep : ProfileEvent()
    object PrevStep : ProfileEvent()
    object ClearError : ProfileEvent()
    object Submit : ProfileEvent()
}
