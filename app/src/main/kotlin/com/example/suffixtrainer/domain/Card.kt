package com.example.suffixtrainer.domain

import com.example.suffixtrainer.model.Category

/**
 * A practice card — derived from a [com.example.suffixtrainer.model.Sentence] and its
 * morphology for a given set of enabled categories. The line is a list of [pieces]: pressable
 * [CardPiece.Word]s (one per token, carrying its [CardPiece.Word.lemma] for translation lookup)
 * and non-pressable [CardPiece.Separator]s (spaces/punctuation). Cards are never stored; they
 * are recomputed whenever the enabled categories or the corpus change (see [renderCard]).
 */
data class Card(
    val sentenceId: Long,
    val english: String,
    val pieces: List<CardPiece>,
) {
    /**
     * Flattened literal/blank view of the whole line, merging adjacent text. Kept so code and
     * tests that walk segments (e.g. text reconstruction, blank indexing) are unaffected by the
     * word grouping.
     */
    val segments: List<CardSegment> by lazy { flattenSegments(pieces) }

    /** The blanked suffixes, in reading order — one per input field on the card. */
    val blanks: List<CardSegment.Blank>
        get() = segments.filterIsInstance<CardSegment.Blank>()
}

/** A piece of the rendered Turkish line. */
sealed interface CardPiece {
    /**
     * One word (a token). [surface] is the word as written; [lemma] is its root, used to look up
     * the translation; [parts] are the literal/blank pieces that compose it.
     */
    data class Word(val surface: String, val lemma: String, val parts: List<CardSegment>) : CardPiece

    /** Text between words (spaces, punctuation) — always visible, not pressable. */
    data class Separator(val text: String) : CardPiece
}

/** One piece of a rendered word. */
sealed interface CardSegment {
    /** Literal text that is always visible (stems, unblanked parts). */
    data class Text(val text: String) : CardSegment

    /**
     * A blanked suffix span. The UI shows an input field until checked, then [answer].
     * [category] is the grammatical category that caused this span to be blanked.
     */
    data class Blank(val answer: String, val category: Category) : CardSegment
}

private fun flattenSegments(pieces: List<CardPiece>): List<CardSegment> {
    val out = mutableListOf<CardSegment>()
    fun addText(text: String) {
        if (text.isEmpty()) return
        val last = out.lastOrNull()
        if (last is CardSegment.Text) out[out.lastIndex] = CardSegment.Text(last.text + text)
        else out += CardSegment.Text(text)
    }
    for (piece in pieces) {
        when (piece) {
            is CardPiece.Separator -> addText(piece.text)
            is CardPiece.Word -> piece.parts.forEach { seg ->
                when (seg) {
                    is CardSegment.Text -> addText(seg.text)
                    is CardSegment.Blank -> out += seg
                }
            }
        }
    }
    return out
}
