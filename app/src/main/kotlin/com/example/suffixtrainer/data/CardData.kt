package com.example.suffixtrainer.data

import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token

/**
 * The raw morphology for one sentence, assembled from the [Sentence], its [Token]s and each
 * token's [Suffix]es. This is the shape a [CardRepository] returns — a Room-backed
 * implementation produces the same structure from a JOIN, so the fake and the real source are
 * interchangeable. Cards are derived from this by [com.example.suffixtrainer.domain.renderCard].
 */
data class CardData(
    val sentence: Sentence,
    val tokens: List<TokenWithSuffixes>,
)

data class TokenWithSuffixes(
    val token: Token,
    val suffixes: List<Suffix>,
)
