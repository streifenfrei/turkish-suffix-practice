package com.example.suffixtrainer.domain

import com.example.suffixtrainer.data.CardData
import com.example.suffixtrainer.model.Category

/**
 * Pure blanking logic. Given the morphology of one sentence and the currently enabled
 * categories, produce the rendered [Card] — or `null` if the sentence has no suffix in an
 * enabled category (such cards are filtered out of the deck).
 *
 * A suffix's surface span is relative to its token (`startInSurface`/`endInSurface`); its
 * absolute span in the sentence text is `token.charStart + startInSurface ..
 * token.charStart + endInSurface`. Every enabled suffix is blanked.
 */
fun renderCard(data: CardData, enabled: Set<Category>): Card? {
    val text = data.sentence.turkishText

    val spans = data.tokens
        .flatMap { (token, suffixes) ->
            suffixes
                .filter { it.category in enabled }
                .map { suffix ->
                    BlankSpan(
                        start = token.charStart + suffix.startInSurface,
                        end = token.charStart + suffix.endInSurface,
                        category = suffix.category,
                    )
                }
        }
        .sortedBy { it.start }

    if (spans.isEmpty()) return null

    val segments = buildList {
        var cursor = 0
        for (span in spans) {
            if (span.start > cursor) {
                add(CardSegment.Text(text.substring(cursor, span.start)))
            }
            add(CardSegment.Blank(text.substring(span.start, span.end), span.category))
            cursor = span.end
        }
        if (cursor < text.length) {
            add(CardSegment.Text(text.substring(cursor)))
        }
    }

    return Card(
        sentenceId = data.sentence.id,
        english = data.sentence.englishText,
        segments = segments,
    )
}

/**
 * The deck for a set of enabled categories: every sentence in the corpus rendered to a card,
 * dropping those with no enabled suffix. This is the deck-filtering logic the ViewModel
 * recomputes whenever the enabled set or the corpus changes.
 */
fun buildDeck(corpus: List<CardData>, enabled: Set<Category>): List<Card> =
    corpus.mapNotNull { renderCard(it, enabled) }

private data class BlankSpan(val start: Int, val end: Int, val category: Category)
