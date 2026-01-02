package com.example.museapp.util

import com.modernmt.text.profanity.ProfanityFilter

object Stopwords {
    val set = setOf(
        "the", "and", "but", "or", "for", "with", "of", "a", "an", "in", "on", "to", "by"
        // Add more as needed
    )
}

/**
 * Heuristic-only interest validator (no asset/dictionary required).
 * - Multi-word allowed (tokens split on whitespace/hyphen)
 * - Uses profanity filter + heuristics to reject gibberish
 * - Includes prior checks (stopwords, digits, special chars, all-caps, emoji/symbol detection)
 */
object InterestValidator {
    private val profanityFilter = ProfanityFilter()
    private val vowels = setOf('a','e','i','o','u','y')

    fun isValidInterest(input: String): Boolean {
        val raw = input.trim()
        val clean = raw.lowercase()

        // --- old checks applied at input level (adapted for multi-word) ---
        if (clean.isBlank() || clean.length < 3) return false

        // profanity check on whole input
        if (profanityFilter.test("en", clean)) return false

        // if single token equals a stopword, reject (keeps original intent)
        val tokensTopLevel = clean.split(Regex("[\\s\\-]+")).filter { it.isNotEmpty() }
        if (tokensTopLevel.size == 1 && Stopwords.set.contains(tokensTopLevel[0])) return false

        // reject if any digit anywhere
        if (clean.any { it.isDigit() }) return false

        // reject if any disallowed char (allow letters, spaces, hyphen only)
        if (clean.any { ch -> !(ch.isLetter() || ch == ' ' || ch == '-') }) return false

        // check original-case all caps (use letters only). If user typed ALL CAPS, reject.
        val lettersOnlyRaw = raw.filter { it.isLetter() }
        if (lettersOnlyRaw.isNotEmpty() && lettersOnlyRaw.all { it.isUpperCase() }) return false

        // detect OTHER_SYMBOL type (emoji/other unusual symbols) on the clean string
        if (clean.any { java.lang.Character.getType(it).toByte() == java.lang.Character.OTHER_SYMBOL }) return false

        // quick repeating-chars check on the entire cleaned input without separators
        val compact = clean.replace(Regex("[\\s\\-]+"), "")
        if (hasRepeatingChars(compact)) return false

        // quick gibberish pattern check on whole input
        if (looksLikeGibberish(clean)) return false

        // --- tokenized heuristics (per token) ---
        // Split tokens on whitespace and hyphen (allow multi-word like "drum player" or "folk-singer")
        val tokens = tokensTopLevel
        if (tokens.isEmpty()) return false

        for (token in tokens) {
            if (!isValidToken(token)) return false
        }

        // passed all checks
        return true
    }

    private fun isValidToken(token: String): Boolean {
        // token length
        if (token.length < 2 || token.length > 20) return false

        // only letters a-z allowed in tokens
        if (token.any { !it.isLetter() }) return false

        // block single-character repeated tokens (aaaa)
        if (hasRepeatingChars(token)) return false

        // reject obvious gibberish substrings
        if (looksLikeGibberish(token)) return false

        // require at least one vowel for tokens longer than 2 (handles many english words)
        if (token.length > 2 && token.none { vowels.contains(it) }) return false

        // reject too many consonants overall (ratio)
        if (tooManyConsonants(token)) return false

        // reject long runs of consonants (e.g., >3 in a row)
        if (hasLongConsonantRun(token, maxRun = 3)) return false

        // Passed heuristics
        return true
    }

    private fun hasRepeatingChars(word: String): Boolean {
        if (word.length < 4) return false
        return word.toSet().size == 1
    }

    private fun looksLikeGibberish(word: String): Boolean {
        val patterns = listOf("asd", "qwe", "zxc", "lol", "omg", "wtf", "asdf", "qwerty", "kjh", "mnb", "zzz")
        return patterns.any { word.contains(it) }
    }

    private fun tooManyConsonants(word: String): Boolean {
        if (word.isEmpty()) return true
        val consonantCount = word.count { it.isLetter() && !vowels.contains(it) }
        val ratio = consonantCount.toDouble() / word.length.toDouble()
        // if length >=4 and >75% consonants, reject
        return word.length >= 4 && ratio > 0.75
    }

    private fun hasLongConsonantRun(word: String, maxRun: Int = 3): Boolean {
        var run = 0
        for (c in word) {
            if (!vowels.contains(c)) {
                run++
                if (run > maxRun) return true
            } else {
                run = 0
            }
        }
        return false
    }
}
