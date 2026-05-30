package com.example.suffixtrainer.pipeline.morph

import java.util.Locale
import zemberek.morphology.TurkishMorphology

/**
 * Wraps Zemberek morphological analysis. Construction loads the default Turkish
 * model (RAM-heavy, ~seconds) — build ONE instance and reuse it for the whole
 * corpus.
 */
class MorphologyAnalyzer(
    private val morphology: TurkishMorphology = TurkishMorphology.createWithDefaults(),
) {

    /**
     * Analyzes a single Turkish word in isolation. Used by tests and ad-hoc
     * lookups; the corpus path uses [analyzeSentence] so the disambiguator has
     * sentence context. Returns null if the input has no word token.
     */
    fun analyzeWord(word: String): AnalyzedToken? = analyzeSentence(word).singleOrNull()

    /**
     * Analyzes a full sentence with the perceptron disambiguator and returns one
     * [AnalyzedToken] per *word* token (punctuation and numbers are skipped).
     * [charStart]/[charEnd] are located in [text] via a running cursor so repeats
     * and apostrophe splits stay aligned.
     */
    fun analyzeSentence(text: String): List<AnalyzedToken> {
        val analysis = morphology.analyzeAndDisambiguate(text)
        val tokens = mutableListOf<AnalyzedToken>()
        var cursor = 0
        var position = 0
        for (wordAnalysis in analysis.wordAnalyses) {
            val word = wordAnalysis.wordAnalysis
            val surface = word.input

            // Locate this token's surface in the original text from the cursor.
            val start = text.indexOf(surface, cursor)
            val charStart = if (start >= 0) start else cursor
            val charEnd = charStart + surface.length
            cursor = charEnd

            // Only emit word tokens; skip punctuation / numbers.
            if (!surface.any { it.isLetter() }) continue

            val best = wordAnalysis.bestAnalysis
            val morphemes = ZemberekInterop.morphemes(best)
            // Zemberek's morpheme surfaces concatenate WITHOUT the apostrophe
            // Turkish inserts in proper nouns (Ankara'da). Align each morpheme
            // onto the real surface so blanking spans stay correct.
            val offsets = alignMorphemes(surface, morphemes)
            val suffixes = mutableListOf<AnalyzedSuffix>()
            for ((idx, md) in morphemes.withIndex()) {
                // Skip zero-width morphemes (Nom, agreement, …) and anything that
                // isn't a tracked case / tense-mood.
                if (md.surface.isEmpty()) continue
                val category = CategoryMapper.categoryFor(md.id) ?: continue
                suffixes += AnalyzedSuffix(
                    morpheme = "-" + md.surface,
                    category = category,
                    startInSurface = offsets[idx],
                    endInSurface = offsets[idx] + md.surface.length,
                )
            }

            tokens += AnalyzedToken(
                position = position++,
                surface = surface,
                lemma = best.dictionaryItem.root,
                charStart = charStart,
                charEnd = charEnd,
                suffixes = suffixes,
                ambiguous = word.analysisCount() > 1,
            )
        }
        return tokens
    }

    /**
     * Returns the start offset *within [surface]* of each morpheme. Walks the
     * real surface with a cursor, skipping characters Zemberek drops from its
     * morpheme surfaces (notably the apostrophe in proper nouns like `Ankara'da`)
     * so suffix spans land on the right characters. Comparison is tr-locale
     * case-insensitive (the root keeps its original casing in the surface).
     */
    private fun alignMorphemes(surface: String, morphemes: List<ZemberekInterop.Morph>): IntArray {
        val low = surface.lowercase(TR)
        val starts = IntArray(morphemes.size)
        var cursor = 0
        for ((i, md) in morphemes.withIndex()) {
            if (md.surface.isEmpty()) {
                starts[i] = cursor
                continue
            }
            val first = md.surface.lowercase(TR)[0]
            while (cursor < low.length && low[cursor] != first) cursor++
            starts[i] = cursor
            cursor = minOf(cursor + md.surface.length, surface.length)
        }
        return starts
    }

    private companion object {
        private val TR = Locale("tr", "TR")
    }
}
