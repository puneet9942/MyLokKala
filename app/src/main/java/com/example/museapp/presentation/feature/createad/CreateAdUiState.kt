package com.example.museapp.presentation.feature.createad

import android.net.Uri
import com.example.museapp.util.AppConstants

data class CreateAdUiState(
    val title: String = "",
    val description: String = "",

    // single selected skill (nullable)
    val selectedSkill: String? = null,
    val availableSkills: List<String> = AppConstants.DEFAULT_INTERESTS,

    // UI inline validation errors
    val titleError: String? = null,
    val skillError: String? = null,

    // pricing toggle
    val wantToAddPrice: Boolean = false,

    // pricing type: "FIXED" | "HOURLY" | "VARIABLE" | null
    val pricingType: String? = null,

    // price fields as strings (UI friendly). Validate in ViewModel.
    val standardPrice: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",

    // validation / inline error for price (e.g., "Min must be <= Max")
    val priceError: String? = null,

    // travel radius in kilometers (0..100)
    val travelRadiusKm: Int = 1,

    // image URIs - max 5
    val imageUris: List<Uri> = emptyList(),

    // image-specific inline error (e.g., duplicate image)
    val imageError: String? = null,

    val loading: Boolean = false,
    val error: String? = null
)
