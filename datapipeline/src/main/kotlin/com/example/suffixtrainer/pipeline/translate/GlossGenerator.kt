package com.example.suffixtrainer.pipeline.translate

import com.example.suffixtrainer.pipeline.PipelineConfig
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Produces a `lemma → English gloss` map for the corpus. Results are cached to
 * `<cacheDir>/glosses.json` so only previously-unseen lemmas hit the translation API; re-runs are
 * offline and instant. Blank translations are dropped (no tooltip rather than a wrong one).
 */
class GlossGenerator(private val config: PipelineConfig) {

    fun generate(lemmas: Set<String>): Map<String, String> {
        val cacheFile = File(config.cacheDir, "glosses.json")
        val cache = loadCache(cacheFile).toMutableMap()

        val missing = lemmas.filter { it.isNotBlank() && it !in cache }.sorted()
        if (missing.isEmpty()) {
            println("  glosses: all ${lemmas.size} lemmas already cached")
        } else {
            val translator = Translator.fromConfig(config)
            println("  glosses: translating ${missing.size} new lemmas via ${translator.name} (${lemmas.size} total)")
            var done = 0
            for (batch in missing.chunked(Translator.BATCH)) {
                val out = translator.translate(batch)
                batch.forEachIndexed { i, lemma -> cache[lemma] = out.getOrElse(i) { "" }.trim() }
                done += batch.size
                saveCache(cacheFile, cache)
                println("    $done/${missing.size} translated")
                Thread.sleep(THROTTLE_MS)
            }
        }

        // Lowercase glosses (DeepL capitalises some single words) for a consistent tooltip look.
        return lemmas.mapNotNull { lemma ->
            cache[lemma]?.takeIf { it.isNotBlank() }?.let { lemma to it.lowercase(Locale.ENGLISH) }
        }.toMap()
    }

    private fun loadCache(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        val obj = JSONObject(file.readText())
        return obj.keys().asSequence().associateWith { obj.getString(it) }
    }

    private fun saveCache(file: File, cache: Map<String, String>) {
        file.parentFile?.mkdirs()
        val obj = JSONObject()
        cache.forEach { (k, v) -> obj.put(k, v) }
        file.writeText(obj.toString())
    }

    private companion object {
        const val THROTTLE_MS = 150L
    }
}
