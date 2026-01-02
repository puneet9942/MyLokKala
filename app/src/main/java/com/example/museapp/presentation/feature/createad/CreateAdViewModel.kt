package com.example.museapp.presentation.feature.createad

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CreateAdViewModel @Inject constructor() : ViewModel() {

    companion object {
        // 100 Crores in INR (1 Crore = 10^7) => 100 * 10^7 = 1_000_000_000
        private const val MAX_PRICE_INR: Long = 1_000_000_000L
    }

    private val _state = MutableStateFlow(CreateAdUiState())
    val state: StateFlow<CreateAdUiState> = _state

    fun onEvent(event: CreateAdEvent) {
        when (event) {
            is CreateAdEvent.TitleChanged -> {
                _state.update { it.copy(title = event.value, titleError = null) }
            }
            is CreateAdEvent.DescriptionChanged -> {
                _state.update { it.copy(description = event.value) }
            }
            is CreateAdEvent.SkillSelected -> {
                // Mandatory selection: selecting an already-selected skill does nothing.
                _state.update {
                    if (it.selectedSkill == event.skill) it
                    else it.copy(selectedSkill = event.skill, skillError = null)
                }
            }
            is CreateAdEvent.WantToAddPriceToggled -> {
                _state.update {
                    if (!event.want) {
                        it.copy(
                            wantToAddPrice = false,
                            pricingType = null,
                            standardPrice = "",
                            minPrice = "",
                            maxPrice = "",
                            priceError = null
                        )
                    } else it.copy(wantToAddPrice = true)
                }
            }
            is CreateAdEvent.PricingTypeSelected -> {
                _state.update {
                    val newType = if (it.pricingType == event.type) null else event.type
                    when (newType) {
                        "FIXED", "HOURLY" -> it.copy(
                            pricingType = newType,
                            standardPrice = it.standardPrice,
                            minPrice = "",
                            maxPrice = "",
                            priceError = null
                        )
                        "VARIABLE" -> it.copy(
                            pricingType = newType,
                            standardPrice = "",
                            minPrice = it.minPrice,
                            maxPrice = it.maxPrice,
                            priceError = null
                        )
                        else -> it.copy(pricingType = null)
                    }
                }
            }
            is CreateAdEvent.StandardPriceChanged -> {
                // digits only
                val digitsOnly = event.value.filter { it.isDigit() }
                // enforce upper limit (as numeric) for quick inline feedback
                val asLong = digitsOnly.toLongOrNull()
                val err = when {
                    asLong != null && asLong > MAX_PRICE_INR -> "Maximum allowed is ₹100 Cr"
                    else -> null
                }
                _state.update { it.copy(standardPrice = digitsOnly, priceError = err) }
            }
            is CreateAdEvent.MinPriceChanged -> {
                // digits only; min >= 0
                val digitsOnly = event.value.filter { it.isDigit() }
                val min = digitsOnly.toLongOrNull()
                val err = when {
                    digitsOnly.isNotEmpty() && min == null -> "Invalid value"
                    min != null && min < 0L -> "Min can't be less than 0"
                    else -> null
                }
                _state.update { it.copy(minPrice = digitsOnly, priceError = err) }
                validateMinMax()
            }
            is CreateAdEvent.MaxPriceChanged -> {
                val digitsOnly = event.value.filter { it.isDigit() }
                val max = digitsOnly.toLongOrNull()
                val err = when {
                    digitsOnly.isNotEmpty() && max == null -> "Invalid value"
                    max != null && max > MAX_PRICE_INR -> "Maximum allowed is ₹100 Cr"
                    else -> null
                }
                _state.update { it.copy(maxPrice = digitsOnly, priceError = err) }
                validateMinMax()
            }
            is CreateAdEvent.TravelRadiusChanged -> {
                val km = event.km.coerceIn(0, 100)
                _state.update { it.copy(travelRadiusKm = km) }
            }
            is CreateAdEvent.AddImages -> {
                val nonNulls = event.uris.filterNotNull()
                if (nonNulls.isEmpty()) return
                _state.update {
                    val existingStrings = it.imageUris.map { uri -> uri.toString() }.toSet()
                    val uniquesToAdd = nonNulls.filter { uri -> !existingStrings.contains(uri.toString()) }
                    val duplicatesFound = nonNulls.any { uri -> existingStrings.contains(uri.toString()) }
                    val combined = (it.imageUris + uniquesToAdd).distinct()
                    val trimmed = combined.take(5)
                    val imageErr = if (duplicatesFound) "Image already added" else null
                    it.copy(imageUris = trimmed, imageError = imageErr)
                }
            }
            is CreateAdEvent.AddImageUri -> {
                _state.update {
                    val uri = event.uri
                    if (uri == null) it
                    else {
                        val alreadyPresent = it.imageUris.any { existing -> existing.toString() == uri.toString() }
                        if (alreadyPresent) {
                            it.copy(imageError = "Image already added")
                        } else {
                            val combined = (it.imageUris + uri).distinct().take(5)
                            it.copy(imageUris = combined, imageError = null)
                        }
                    }
                }
            }
            is CreateAdEvent.RemoveImageAt -> {
                _state.update {
                    val mutable = it.imageUris.toMutableList()
                    if (event.index in mutable.indices) {
                        mutable.removeAt(event.index)
                    }
                    it.copy(imageUris = mutable.toList(), imageError = null)
                }
            }
            is CreateAdEvent.ClearImageError -> {
                _state.update { it.copy(imageError = null) }
            }
            is CreateAdEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is CreateAdEvent.Submit -> {
                validateBeforeSubmit()
            }
        }
    }

    private fun validateMinMax() {
        val s = _state.value
        val min = s.minPrice.toLongOrNull()
        val max = s.maxPrice.toLongOrNull()
        val err = when {
            s.minPrice.isNotBlank() && min == null -> "Invalid min price"
            s.maxPrice.isNotBlank() && max == null -> "Invalid max price"
            min != null && min < 0L -> "Min can't be less than 0"
            max != null && max > MAX_PRICE_INR -> "Maximum allowed is ₹100 Cr"
            min != null && max != null && min > max -> "Min price must be ≤ Max price"
            else -> null
        }
        _state.update { it.copy(priceError = err) }
    }

    private fun validateBeforeSubmit() {
        var anyError = false
        val cur = _state.value

        // Title required
        if (cur.title.isBlank()) {
            anyError = true
            _state.update { it.copy(titleError = "Please enter a title") }
        } else {
            _state.update { it.copy(titleError = null) }
        }

        // Skill required (mandatory)
        if (cur.selectedSkill == null) {
            anyError = true
            _state.update { it.copy(skillError = "Please choose a primary skill") }
        } else {
            _state.update { it.copy(skillError = null) }
        }

        // Pricing validation if user chose to add price
        if (cur.wantToAddPrice) {
            when (cur.pricingType) {
                "FIXED", "HOURLY" -> {
                    val p = cur.standardPrice.toLongOrNull()
                    if (p == null) {
                        anyError = true
                        _state.update { it.copy(priceError = "Enter a valid standard price") }
                    } else if (p > MAX_PRICE_INR) {
                        anyError = true
                        _state.update { it.copy(priceError = "Current value is beyond maximum limit") }
                    } else {
                        _state.update { it.copy(priceError = null) }
                    }
                }
                "VARIABLE" -> {
                    val min = cur.minPrice.toLongOrNull()
                    val max = cur.maxPrice.toLongOrNull()
                    when {
                        min == null || max == null -> {
                            anyError = true
                            _state.update { it.copy(priceError = "Enter both min and max price") }
                        }
                        min < 0L -> {
                            anyError = true
                            _state.update { it.copy(priceError = "Min can't be less than 0") }
                        }
                        max > MAX_PRICE_INR -> {
                            anyError = true
                            _state.update { it.copy(priceError = "Current value is beyond maximum limit") }
                        }
                        min > max -> {
                            anyError = true
                            _state.update { it.copy(priceError = "Min price must be ≤ Max price") }
                        }
                        else -> _state.update { it.copy(priceError = null) }
                    }
                }
                else -> {
                    anyError = true
                    _state.update { it.copy(priceError = "Select a pricing type") }
                }
            }
        } else {
            _state.update { it.copy(priceError = null) }
        }

        if (!anyError) {
            // proceed to submit: set loading and call repository etc.
            _state.update { it.copy(loading = true, error = null) }
            // TODO: wire repository call and set loading=false after result
        }
    }
}

/**
 * Events for CreateAd screen - typed events.
 */
sealed interface CreateAdEvent {
    data class TitleChanged(val value: String) : CreateAdEvent
    data class DescriptionChanged(val value: String) : CreateAdEvent
    data class SkillSelected(val skill: String) : CreateAdEvent

    data class WantToAddPriceToggled(val want: Boolean) : CreateAdEvent
    data class PricingTypeSelected(val type: String) : CreateAdEvent

    data class StandardPriceChanged(val value: String) : CreateAdEvent
    data class MinPriceChanged(val value: String) : CreateAdEvent
    data class MaxPriceChanged(val value: String) : CreateAdEvent

    data class TravelRadiusChanged(val km: Int) : CreateAdEvent

    // Accept nullable Uri entries from pickers; ViewModel sanitizes filterNotNull()
    data class AddImages(val uris: List<Uri?>) : CreateAdEvent
    data class AddImageUri(val uri: Uri?) : CreateAdEvent
    data class RemoveImageAt(val index: Int) : CreateAdEvent

    object ClearImageError : CreateAdEvent
    object Submit : CreateAdEvent
    object ClearError : CreateAdEvent
}
