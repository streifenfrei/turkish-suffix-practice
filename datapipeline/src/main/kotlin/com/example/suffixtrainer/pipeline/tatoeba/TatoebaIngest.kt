package com.example.suffixtrainer.pipeline.tatoeba

import com.example.suffixtrainer.pipeline.PipelineConfig
import java.io.File

/**
 * Resolves raw Tatoeba inputs (download+cache, or dev fixtures) and pairs each
 * Turkish sentence with one English translation. Pairing is deterministic
 * (ascending Turkish id; shortest English partner) so runs are reproducible.
 */
class TatoebaIngest(private val config: PipelineConfig) {

    private data class Inputs(val tur: File, val eng: File, val links: File, val audio: File)

    fun loadPairs(): List<SentencePair> {
        val inputs = resolveInputs()

        println("Reading Turkish sentences…")
        val tur = TatoebaParser.readSentences(inputs.tur)
        println("  ${tur.size} Turkish sentences")

        println("Reading audio availability…")
        val audioIds = TatoebaParser.readAudioIds(inputs.audio)
        println("  ${audioIds.size} sentences with audio")

        // Collect candidate partner ids for our Turkish sentences from links
        // (partners may be any language; English is filtered in the next step).
        println("Reading translation links…")
        val partners = HashMap<Long, MutableList<Long>>()
        TatoebaParser.readLinks(inputs.links) { a, b ->
            if (tur.containsKey(a)) partners.getOrPut(a) { mutableListOf() }.add(b)
        }
        val candidateEng = partners.values.flatten().toHashSet()
        println("  ${partners.size} Turkish sentences have at least one translation")

        println("Reading English sentences…")
        val eng = HashMap<Long, String>()
        TatoebaParser.forEachLine(inputs.eng) { cols ->
            if (cols.size >= 3) {
                val id = cols[0].toLongOrNull() ?: return@forEachLine
                if (id in candidateEng) eng[id] = cols[2]
            }
        }
        println("  ${eng.size} English sentences linked to Turkish")

        // Build pairs deterministically: shortest English partner wins.
        val pairs = ArrayList<SentencePair>()
        for (turId in tur.keys.sorted()) {
            val english = partners[turId]
                ?.mapNotNull { eng[it] }
                ?.minByOrNull { it.length }
                ?: continue
            pairs += SentencePair(
                turkishId = turId,
                turkishText = tur.getValue(turId),
                englishText = english,
                hasAudio = turId in audioIds,
            )
        }
        println("Paired ${pairs.size} Turkish↔English sentences")
        return pairs
    }

    private fun resolveInputs(): Inputs {
        if (config.devMode) {
            val dir = config.fixtureDir
            check(dir.isDirectory) { "Dev fixture dir not found: ${dir.absolutePath}" }
            return Inputs(
                tur = File(dir, "tur_sentences.tsv"),
                eng = File(dir, "eng_sentences.tsv"),
                links = File(dir, "links.tsv"),
                audio = File(dir, "sentences_with_audio.tsv"),
            )
        }
        val dl = Downloader(config.cacheDir)
        val base = "https://downloads.tatoeba.org/exports"
        return Inputs(
            tur = dl.ensure("$base/per_language/tur/tur_sentences.tsv.bz2", "tur_sentences.tsv.bz2"),
            eng = dl.ensure("$base/per_language/eng/eng_sentences.tsv.bz2", "eng_sentences.tsv.bz2"),
            links = dl.ensure("$base/links.tar.bz2", "links.tar.bz2"),
            audio = dl.ensure("$base/sentences_with_audio.tar.bz2", "sentences_with_audio.tar.bz2"),
        )
    }
}
