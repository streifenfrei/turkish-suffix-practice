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
import java.io.File

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

    val dbFile = File(config.outputDir, "trainer.db")
    DbWriter(schema).write(dbFile, corpus.sentences, corpus.tokens, corpus.suffixes)
    println("Wrote ${dbFile.absolutePath} (${dbFile.length() / 1024} KiB)")

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
)

/**
 * Pairs → filter → analyze → entity rows. A sentence is kept only if it is short
 * enough and yields at least one blankable suffix (so it can become a practice
 * card). Ids: sentenceId/tatoebaId = Tatoeba id; token & suffix ids are sequential.
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

        for (pair in pairs) {
            if (sentences.size >= config.maxSentences) break
            val text = pair.turkishText
            if (text.length > config.maxChars) continue
            if (text.split(WHITESPACE).size > config.maxTokens) continue

            val analyzed = analyzer.analyzeSentence(text)
            if (analyzed.none { it.suffixes.isNotEmpty() }) continue

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
                if (token.ambiguous) {
                    report.record(
                        sentenceId, token.surface, token.lemma,
                        token.suffixes.joinToString(",") { "${it.category}${it.morpheme}" },
                    )
                }
            }
        }
        return CorpusResult(sentences, tokens, suffixes, report)
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private const val AUDIO_URL_PREFIX = "https://tatoeba.org/audio/download/"
    }
}

private fun printSummary(result: CorpusResult, reportFile: File) {
    val coverage = result.suffixes.groupingBy { it.category }.eachCount()
    val withAudio = result.sentences.count { it.audioPath != null }

    println("\n========== SUMMARY ==========")
    println("Sentences (cards): ${result.sentences.size}  (with audio reference: $withAudio)")
    println("Tokens: ${result.tokens.size}   Suffixes: ${result.suffixes.size}")
    println("Category coverage:")
    for (category in Category.entries) {
        println("  ${category.name.padEnd(20)} ${coverage[category] ?: 0}")
    }
    println("Low-confidence tokens: ${result.report.count}  ->  ${reportFile.absolutePath}")
    println("=============================")
}
