package com.example.suffixtrainer.data

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Word translations, keyed by lemma (Turkish-lowercased). */
interface GlossRepository {
    /** English gloss for a word's [lemma], or null if none is bundled. */
    suspend fun glossFor(lemma: String): String?

    /** The whole lemma→gloss map (loaded once); empty if the db has no glosses table/data. */
    suspend fun all(): Map<String, String>
}

/**
 * [GlossRepository] over the prepackaged `glosses` table. Loads the (~2k-row) map once and caches
 * it in memory. Keys are lowercased in the Turkish locale so lookups match Zemberek's roots
 * regardless of the surrounding word's casing.
 */
@Singleton
class RoomGlossRepository @Inject constructor(
    private val dao: SentenceDao,
) : GlossRepository {

    private val tr = Locale.forLanguageTag("tr")
    private var cache: Map<String, String>? = null

    override suspend fun all(): Map<String, String> {
        cache?.let { return it }
        val loaded = runCatching { dao.allGlosses() }.getOrDefault(emptyList())
            .associate { it.lemma.lowercase(tr) to it.gloss }
        cache = loaded
        return loaded
    }

    override suspend fun glossFor(lemma: String): String? = all()[lemma.lowercase(tr)]
}
