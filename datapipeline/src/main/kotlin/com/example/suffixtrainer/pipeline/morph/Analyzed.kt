package com.example.suffixtrainer.pipeline.morph

import com.example.suffixtrainer.model.Category

/**
 * A suffix found on a token: the surface morpheme text, its mapped [category],
 * and the character span it occupies *within the token surface*. The span is
 * what the app blanks out.
 */
data class AnalyzedSuffix(
    val morpheme: String,
    val category: Category,
    val startInSurface: Int,
    val endInSurface: Int,
)

/**
 * One analyzed word. [charStart]/[charEnd] are the span within the *sentence*;
 * the suffix spans inside [suffixes] are relative to [surface].
 *
 * [ambiguous] is true when Zemberek had more than one candidate analysis before
 * disambiguation — surfaced to the low-confidence report rather than silently
 * trusted.
 */
data class AnalyzedToken(
    val position: Int,
    val surface: String,
    val lemma: String,
    val charStart: Int,
    val charEnd: Int,
    val suffixes: List<AnalyzedSuffix>,
    val ambiguous: Boolean,
)
