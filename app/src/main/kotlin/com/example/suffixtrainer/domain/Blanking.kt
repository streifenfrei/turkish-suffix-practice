package com.example.suffixtrainer.domain

import com.example.suffixtrainer.data.CardData
import com.example.suffixtrainer.model.Category

/**
 * Pure blanking logic. Given the morphology of one sentence and the currently enabled
 * categories, produce the rendered [Card] — or `null` if the sentence has no suffix in an
 * enabled category (such cards are filtered out of the deck).
 *
 * The line is grouped by token, so each [CardPiece.Word] is a pressable unit carrying its lemma;
 * text between tokens (spaces, punctuation) becomes a [CardPiece.Separator]. Within a token, each
 * enabled suffix's surface span (`startInSurface`/`endInSurface`, absolute = `token.charStart +`)
 * is blanked.
 */
fun renderCard(data: CardData, enabled: Set<Category>): Card? {
    val text = data.sentence.turkishText
    val pieces = mutableListOf<CardPiece>()
    var cursor = 0
    var hasBlank = false

    for ((token, suffixes) in data.tokens.sortedBy { it.token.charStart }) {
        val start = token.charStart.coerceIn(0, text.length)
        val end = token.charEnd.coerceIn(start, text.length)
        if (start > cursor) pieces += CardPiece.Separator(text.substring(cursor, start))

        val spans = suffixes
            .filter { it.category in enabled }
            .map { BlankSpan(token.charStart + it.startInSurface, token.charStart + it.endInSurface, it.category) }
            .sortedBy { it.start }

        val parts = buildList {
            var c = start
            for (span in spans) {
                val bStart = span.start.coerceIn(start, end)
                val bEnd = span.end.coerceIn(bStart, end)
                if (bStart > c) add(CardSegment.Text(text.substring(c, bStart)))
                add(CardSegment.Blank(text.substring(bStart, bEnd), span.category))
                hasBlank = true
                c = bEnd
            }
            if (c < end) add(CardSegment.Text(text.substring(c, end)))
        }
        pieces += CardPiece.Word(surface = token.surface, lemma = token.lemma, parts = parts)
        cursor = end
    }
    if (cursor < text.length) pieces += CardPiece.Separator(text.substring(cursor))

    if (!hasBlank) return null
    return Card(sentenceId = data.sentence.id, english = data.sentence.englishText, pieces = pieces)
}

/**
 * The deck for a set of enabled categories: every sentence in the corpus rendered to a card,
 * dropping those with no enabled suffix. This is the deck-filtering logic the ViewModel
 * recomputes whenever the enabled set or the corpus changes.
 */
fun buildDeck(corpus: List<CardData>, enabled: Set<Category>): List<Card> =
    corpus.mapNotNull { renderCard(it, enabled) }

private data class BlankSpan(val start: Int, val end: Int, val category: Category)
