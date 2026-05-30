package com.example.suffixtrainer.domain

import java.util.Locale

private val TR: Locale = Locale.forLanguageTag("tr")

/**
 * Whether the learner's [input] matches the expected suffix [answer]. Comparison is trimmed and
 * case-insensitive in the **Turkish** locale, so dotted/dotless i is handled correctly
 * (e.g. "İ".lowercase(tr) == "i", "I" → "ı") and "DE" / " de " both match "de".
 */
fun isSuffixCorrect(input: String, answer: String): Boolean =
    input.trim().lowercase(TR) == answer.trim().lowercase(TR)
