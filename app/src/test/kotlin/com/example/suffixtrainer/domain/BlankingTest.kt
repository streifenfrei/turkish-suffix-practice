package com.example.suffixtrainer.domain

import com.example.suffixtrainer.data.SAMPLE_CORPUS
import com.example.suffixtrainer.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankingTest {

    private val evdeKaldim = SAMPLE_CORPUS.first { it.sentence.turkishText == "Evde kaldım." }

    @Test
    fun `blanks every enabled suffix at the right span`() {
        val card = renderCard(evdeKaldim, setOf(Category.LOCATIVE, Category.PAST_DEFINITE))!!
        assertEquals(
            listOf(
                CardSegment.Text("Ev"),
                CardSegment.Blank("de", Category.LOCATIVE),
                CardSegment.Text(" kal"),
                CardSegment.Blank("dı", Category.PAST_DEFINITE),
                CardSegment.Text("m."),
            ),
            card.segments,
        )
    }

    @Test
    fun `blanks only suffixes whose category is enabled`() {
        val card = renderCard(evdeKaldim, setOf(Category.LOCATIVE))!!
        assertEquals(
            listOf(
                CardSegment.Text("Ev"),
                CardSegment.Blank("de", Category.LOCATIVE),
                CardSegment.Text(" kaldım."),
            ),
            card.segments,
        )
    }

    @Test
    fun `renderCard returns null when no suffix is in an enabled category`() {
        assertNull(renderCard(evdeKaldim, setOf(Category.FUTURE)))
    }

    @Test
    fun `every sample card's blanks reconstruct the original Turkish text`() {
        for (data in SAMPLE_CORPUS) {
            val card = renderCard(data, Category.entries.toSet()) ?: continue
            val reconstructed = buildString {
                card.segments.forEach { segment ->
                    when (segment) {
                        is CardSegment.Text -> append(segment.text)
                        is CardSegment.Blank -> append(segment.answer)
                    }
                }
            }
            assertEquals(data.sentence.turkishText, reconstructed)
        }
    }

    @Test
    fun `buildDeck drops sentences with no enabled suffix`() {
        val futureOnly = buildDeck(SAMPLE_CORPUS, setOf(Category.FUTURE))
        assertEquals(1, futureOnly.size)
        assertEquals("I will come tomorrow", futureOnly.single().english)
    }

    @Test
    fun `buildDeck is empty when nothing is enabled and full when everything is`() {
        assertTrue(buildDeck(SAMPLE_CORPUS, emptySet()).isEmpty())
        assertEquals(SAMPLE_CORPUS.size, buildDeck(SAMPLE_CORPUS, Category.entries.toSet()).size)
    }
}
