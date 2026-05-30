package com.example.suffixtrainer.domain

import com.example.suffixtrainer.data.CardData
import com.example.suffixtrainer.model.Category

/**
 * How many cards (sentences) are available for each category — i.e. for category `c`,
 * the number of sentences with at least one suffix of `c` (equivalently
 * `buildDeck(corpus, setOf(c)).size`). Surfaces thin categories in Settings. A category
 * absent from the corpus is omitted from the map (callers default it to 0).
 */
fun cardCountByCategory(corpus: List<CardData>): Map<Category, Int> {
    val counts = mutableMapOf<Category, Int>()
    for (data in corpus) {
        val categoriesInCard = data.tokens
            .flatMap { it.suffixes }
            .mapTo(mutableSetOf()) { it.category }
        for (category in categoriesInCard) {
            counts[category] = (counts[category] ?: 0) + 1
        }
    }
    return counts
}
