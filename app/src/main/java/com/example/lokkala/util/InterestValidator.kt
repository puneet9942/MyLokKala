package com.example.lokkala.util

import com.example.lokkala.util.InterestValidator.profanityFilter
import com.modernmt.text.profanity.ProfanityFilter


object Stopwords {
    val set = setOf(
        "the", "and", "but", "or", "for", "with", "of", "a", "an", "in", "on", "to", "by"
        // Add more as needed
    )
}

object InterestValidator {
    private val profanityFilter = ProfanityFilter()

    fun isValidInterest(input: String): Boolean {
        val clean = input.trim().lowercase()
        if (clean.isBlank() || clean.length < 3) return false
        if (profanityFilter.test("en", clean)) return false
        if (Stopwords.set.contains(clean)) return false
        if (clean.any { it.isDigit() }) return false
        if (clean.any { !it.isLetter() }) return false // no special chars
        if (clean.all { it.isUpperCase() }) return false // block all caps
        if (clean.any { Character.getType(it) == Character.OTHER_SYMBOL.toInt() }) return false // emojis/symbols
        if (hasRepeatingChars(clean)) return false
        if (looksLikeGibberish(clean)) return false
        return true
    }

    private fun hasRepeatingChars(word: String): Boolean {
        if (word.length < 4) return false
        return word.toSet().size == 1
    }

    private fun looksLikeGibberish(word: String): Boolean {
        val gibberishPatterns = listOf("asd", "qwe", "zxc", "lol", "omg", "wtf", "asdf", "qwerty")
        return gibberishPatterns.any { word.contains(it) }
    }
}
