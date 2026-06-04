package com.example.suffixtrainer.pipeline

import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import com.example.suffixtrainer.pipeline.db.DbWriter
import com.example.suffixtrainer.pipeline.db.SchemaProvider
import com.example.suffixtrainer.pipeline.morph.MorphologyAnalyzer
import com.example.suffixtrainer.pipeline.report.LowConfidenceReport
import com.example.suffixtrainer.pipeline.tatoeba.TatoebaIngest
import com.example.suffixtrainer.pipeline.translate.GlossGenerator
import java.io.File
import java.util.Locale

/**
 * Offline pipeline: Tatoeba ingest → Zemberek analysis → `trainer.db`.
 *
 * `./gradlew :datapipeline:run` builds a curated db from cached input. Pass
 * `--args="--dev"` to use the small committed fixture for a fast end-to-end run.
 */
fun main(args: Array<String>) {
    val config = PipelineConfig.fromArgs(args)
    val corpus = CorpusBuilder(config).build()

    println("\nResolving schema…")
    val schema = SchemaProvider.resolve(config.schemaJson, config.schemaVersion)
    println("  using ${schema.source}")
    if (schema.identityHash == null) {
        println(
            "  WARNING: no Room identity hash — db is structurally correct but will NOT\n" +
                "  pass Room's createFromAsset check until :app exports schemas/.../1.json\n" +
                "  (set exportSchema=true + room.schemaLocation, then point --schema at it).",
        )
    }

    val glosses = if (config.generateGlosses) {
        println("\nGenerating word glosses…")
        GlossGenerator(config).generate(corpus.uniqueSurfaces)
    } else {
        println("\nSkipping glosses (dev/--no-glosses).")
        emptyMap()
    }

    val dbFile = File(config.outputDir, "trainer.db")
    DbWriter(schema).write(dbFile, corpus.sentences, corpus.tokens, corpus.suffixes, glosses)
    println("Wrote ${dbFile.absolutePath} (${dbFile.length() / 1024} KiB)")
    println("Glosses: ${glosses.size} of ${corpus.uniqueSurfaces.size} words")

    val reportFile = File(config.outputDir, "low-confidence.tsv")
    corpus.report.writeTo(reportFile)

    printSummary(corpus, reportFile)
}

/** The assembled corpus plus the run's review report. */
private class CorpusResult(
    val sentences: List<Sentence>,
    val tokens: List<Token>,
    val suffixes: List<Suffix>,
    val report: LowConfidenceReport,
    /** Ambiguous tokens that had ≥1 suffix we deliberately did not emit as a blank target. */
    val excludedTargetTokens: Int,
    val excludedTargetSuffixes: Int,
    /**
     * Diagnostic only: per-category suffix count over all analyzed sentences *before* the
     * low-confidence exclusion. Compare to the emitted ("after") coverage to see how many
     * suffixes each category lost to the ambiguity filter. Does not affect what is emitted.
     */
    val beforeExclusionCoverage: Map<Category, Int>,
    /**
     * Distinct token surface forms (Turkish-lowercased) — the keys to translate for the per-word
     * translations. Full inflected words (with their suffixes), not roots, so the gloss reflects
     * the word as it appears in the sentence (e.g. `evde` → "at home", not `ev` → "home").
     */
    val uniqueSurfaces: Set<String>,
)

/**
 * Pairs → filter → analyze → entity rows. A sentence is kept only if it is short
 * enough and yields at least one *confident* blankable suffix (so it can become a
 * practice card). Suffixes on low-confidence (Zemberek-ambiguous) tokens are never
 * emitted, so they are never quizzed — the word still appears in the sentence text,
 * just never as the blank. Ids: sentenceId/tatoebaId = Tatoeba id; token & suffix
 * ids are sequential.
 */
private class CorpusBuilder(private val config: PipelineConfig) {

    fun build(): CorpusResult {
        val pairs = TatoebaIngest(config).loadPairs()

        println("\nLoading Zemberek model…")
        val analyzer = MorphologyAnalyzer()
        println("Analyzing (target ${config.maxSentences} cards)…")

        val sentences = mutableListOf<Sentence>()
        val tokens = mutableListOf<Token>()
        val suffixes = mutableListOf<Suffix>()
        val report = LowConfidenceReport()
        var nextTokenId = 1L
        var nextSuffixId = 1L
        var excludedTargetTokens = 0
        var excludedTargetSuffixes = 0
        val beforeExclusionCoverage = mutableMapOf<Category, Int>()
        val uniqueSurfaces = sortedSetOf<String>()

        for (pair in pairs) {
            if (sentences.size >= config.maxSentences) break
            val text = pair.turkishText
            if (text.length > config.maxChars) continue
            if (text.split(WHITESPACE).size > config.maxTokens) continue

            val analyzed = analyzer.analyzeSentence(text)
            // Diagnostic: tally every suffix the analyzer produced (ambiguous or not, kept or
            // dropped) so we can compare against what survives the exclusion below.
            for (t in analyzed) {
                for (s in t.suffixes) beforeExclusionCoverage.merge(s.category, 1, Int::plus)
            }
            // Keep only sentences with a confident suffix to quiz; ambiguous suffixes
            // are never blank targets so they don't count toward keeping a sentence.
            if (analyzed.none { !it.ambiguous && it.suffixes.isNotEmpty() }) continue

            val sentenceId = pair.turkishId
            sentences += Sentence(
                id = sentenceId,
                turkishText = text,
                englishText = pair.englishText,
                audioPath = if (pair.hasAudio) AUDIO_URL_PREFIX + pair.turkishId else null,
                tatoebaId = pair.turkishId,
            )
            for (token in analyzed) {
                val tokenId = nextTokenId++
                tokens += Token(
                    id = tokenId,
                    sentenceId = sentenceId,
                    position = token.position,
                    surface = token.surface,
                    lemma = token.lemma,
                    charStart = token.charStart,
                    charEnd = token.charEnd,
                )
                if (token.surface.isNotBlank()) uniqueSurfaces += token.surface.lowercase(TR)
                if (token.ambiguous) {
                    // Low-confidence: log for review and never emit its suffixes as targets.
                    if (token.suffixes.isNotEmpty()) {
                        excludedTargetTokens++
                        excludedTargetSuffixes += token.suffixes.size
                    }
                    report.record(
                        sentenceId, token.surface, token.lemma,
                        token.suffixes.joinToString(",") { "${it.category}${it.morpheme}" },
                    )
                } else {
                    for (sfx in token.suffixes) {
                        suffixes += Suffix(
                            id = nextSuffixId++,
                            tokenId = tokenId,
                            morpheme = sfx.morpheme,
                            category = sfx.category,
                            startInSurface = sfx.startInSurface,
                            endInSurface = sfx.endInSurface,
                        )
                    }
                }
            }
        }
        return CorpusResult(
            sentences, tokens, suffixes, report,
            excludedTargetTokens, excludedTargetSuffixes,
            beforeExclusionCoverage, uniqueSurfaces,
        )
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private const val AUDIO_URL_PREFIX = "https://tatoeba.org/audio/download/"
        private val TR: Locale = Locale.forLanguageTag("tr")
    }
}

private fun printSummary(result: CorpusResult, reportFile: File) {
    val coverage = result.suffixes.groupingBy { it.category }.eachCount()
    val withAudio = result.sentences.count { it.audioPath != null }

    println("\n========== SUMMARY ==========")
    println("Sentences (cards): ${result.sentences.size}  (with audio reference: $withAudio)")
    println("Tokens: ${result.tokens.size}   Suffixes: ${result.suffixes.size}")
    println("Category coverage: after  (before, -excluded by low-confidence)")
    for (category in Category.entries) {
        val after = coverage[category] ?: 0
        val before = result.beforeExclusionCoverage[category] ?: 0
        println("  ${category.name.padEnd(20)} $after  (before=$before, -${before - after})")
    }
    println("Low-confidence tokens: ${result.report.count}  ->  ${reportFile.absolutePath}")
    println(
        "Excluded as blank target: ${result.excludedTargetTokens} tokens " +
            "(${result.excludedTargetSuffixes} suffixes)",
    )
    println("=============================")
}
