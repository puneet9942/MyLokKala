package com.example.lokkala.domain.model

data class Country(
    val name: String,       // "United States"
    val iso: String,        // "US"     <-- ADD THIS
    val code: String,       // "+1"
    val flagEmoji: String
)