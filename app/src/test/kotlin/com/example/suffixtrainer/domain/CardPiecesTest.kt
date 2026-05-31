package com.example.suffixtrainer.domain

import com.example.suffixtrainer.data.SAMPLE_CORPUS
import com.example.suffixtrainer.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CardPiecesTest {

    private val evdeKaldim = SAMPLE_CORPUS.first { it.sentence.turkishText == "Evde kaldım." }

    @Test
    fun `groups the line into pressable words and separators with lemmas`() {
        val card = renderCard(evdeKaldim, Category.entries.toSet())!!
        assertEquals(
            listOf(
                CardPiece.Word(
                    "Evde", "ev",
                    listOf(CardSegment.Text("Ev"), CardSegment.Blank("de", Category.LOCATIVE)),
                ),
                CardPiece.Separator(" "),
                CardPiece.Word(
                    "kaldım", "kal",
                    listOf(
                        CardSegment.Text("kal"),
                        CardSegment.Blank("dı", Category.PAST_DEFINITE),
                        CardSegment.Text("m"),
                    ),
                ),
                CardPiece.Separator("."),
            ),
            card.pieces,
        )
    }

    @Test
    fun `flattened segments stay backwards-compatible and reconstruct the sentence`() {
        for (data in SAMPLE_CORPUS) {
            val card = renderCard(data, Category.entries.toSet()) ?: continue
            // No two adjacent Text segments (they must be merged).
            card.segments.zipWithNext().forEach { (a, b) ->
                require(!(a is CardSegment.Text && b is CardSegment.Text)) { "adjacent Text in $card" }
            }
            val reconstructed = card.segments.joinToString("") {
                when (it) {
                    is CardSegment.Text -> it.text
                    is CardSegment.Blank -> it.answer
                }
            }
            assertEquals(data.sentence.turkishText, reconstructed)
        }
    }

    @Test
    fun `every word piece carries a lemma matching a corpus token`() {
        val card = renderCard(evdeKaldim, Category.entries.toSet())!!
        val words = card.pieces.filterIsInstance<CardPiece.Word>()
        assertEquals(listOf("ev", "kal"), words.map { it.lemma })
    }
}
