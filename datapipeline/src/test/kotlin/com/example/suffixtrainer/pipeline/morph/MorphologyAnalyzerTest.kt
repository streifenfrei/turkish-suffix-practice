package com.example.suffixtrainer.pipeline.morph

import com.example.suffixtrainer.model.Category
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration tests over the real Zemberek model (the highest-value tests per
 * CLAUDE.md). One shared analyzer is built for the whole class — model load is
 * expensive. Each known word must segment into the expected root, [Category],
 * and exact surface span.
 */
class MorphologyAnalyzerTest {

    companion object {
        private lateinit var analyzer: MorphologyAnalyzer

        @BeforeClass
        @JvmStatic
        fun setUp() {
            analyzer = MorphologyAnalyzer()
        }
    }

    private fun assertSingleSuffix(
        word: String,
        expectedRoot: String,
        expectedCategory: Category,
        expectedSurface: String,
        expectedStart: Int,
        expectedEnd: Int,
    ) {
        val token = assertNotNull(analyzer.analyzeWord(word), "no analysis for $word")
        assertEquals(expectedRoot, token.lemma, "$word root")
        val match = token.suffixes.singleOrNull { it.category == expectedCategory }
        assertNotNull(match, "$word missing $expectedCategory in ${token.suffixes}")
        assertEquals(expectedStart, match.startInSurface, "$word start")
        assertEquals(expectedEnd, match.endInSurface, "$word end")
        // Span must point at the actual surface substring (vowel-harmony form).
        assertEquals(expectedSurface, word.substring(match.startInSurface, match.endInSurface), "$word span text")
        assertEquals("-$expectedSurface", match.morpheme, "$word morpheme")
    }

    @Test fun evde_is_locative() = assertSingleSuffix("evde", "ev", Category.LOCATIVE, "de", 2, 4)

    @Test fun eve_is_dative() = assertSingleSuffix("eve", "ev", Category.DATIVE, "e", 2, 3)

    @Test fun evden_is_ablative() = assertSingleSuffix("evden", "ev", Category.ABLATIVE, "den", 2, 5)

    @Test fun geldim_is_past_definite() = assertSingleSuffix("geldim", "gel", Category.PAST_DEFINITE, "di", 3, 5)

    @Test
    fun gidiyorum_is_present_continuous() =
        assertSingleSuffix("gidiyorum", "git", Category.PRESENT_CONTINUOUS, "iyor", 3, 7)

    // Proper nouns insert an apostrophe Zemberek omits from its morpheme
    // surfaces; the span must still land on the real characters ("da" at 7..9).
    // Zemberek lowercases the dictionary root, so the lemma is "ankara".
    @Test
    fun proper_noun_with_apostrophe_keeps_correct_span() =
        assertSingleSuffix("Ankara'da", "ankara", Category.LOCATIVE, "da", 7, 9)
}
