package com.example.suffixtrainer.pipeline.report

import java.io.File

/**
 * Accumulates tokens that Zemberek found ambiguous (more than one candidate
 * analysis before disambiguation) and writes them to a TSV for human review —
 * so low-confidence decisions are logged, never silently trusted.
 */
class LowConfidenceReport {

    private data class Row(val sentenceId: Long, val surface: String, val lemma: String, val suffixes: String)

    private val rows = mutableListOf<Row>()

    fun record(sentenceId: Long, surface: String, lemma: String, suffixes: String) {
        rows += Row(sentenceId, surface, lemma, suffixes)
    }

    val count: Int get() = rows.size

    fun writeTo(file: File) {
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { w ->
            w.appendLine("sentenceId\tsurface\tlemma\tsuffixes")
            rows.forEach { w.appendLine("${it.sentenceId}\t${it.surface}\t${it.lemma}\t${it.suffixes}") }
        }
    }
}
