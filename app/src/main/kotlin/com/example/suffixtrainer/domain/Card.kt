package com.example.suffixtrainer.domain

import com.example.suffixtrainer.model.Category

/**
 * A practice card — derived from a [com.example.suffixtrainer.model.Sentence] and its
 * morphology for a given set of enabled categories. Cards are never stored; they are
 * recomputed whenever the enabled categories or the corpus change (see [renderCard]).
 */
data class Card(
    val sentenceId: Long,
    val english: String,
    /** The Turkish line, split into ordered literal/blank segments for inline rendering. */
    val segments: List<CardSegment>,
    val audioPath: String?,
) {
    /** The suffixes hidden on this card, in reading order — what the reveal control shows. */
    val blanks: List<CardSegment.Blank>
        get() = segments.filterIsInstance<CardSegment.Blank>()
}

/** One piece of a rendered Turkish line. */
sealed interface CardSegment {
    /** Literal text that is always visible (stems, spaces, punctuation, unblanked words). */
    data class Text(val text: String) : CardSegment

    /**
     * A blanked suffix span. The UI shows a placeholder (e.g. "___") until revealed, then
     * [answer]. [category] is the grammatical category that caused this span to be blanked.
     */
    data class Blank(val answer: String, val category: Category) : CardSegment
}
