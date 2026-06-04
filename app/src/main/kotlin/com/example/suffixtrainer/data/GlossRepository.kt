package com.example.suffixtrainer.data

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Word translations, keyed by full surface form (Turkish-lowercased). */
interface GlossRepository {
    /** English gloss for a [word]'s full surface form, or null if none is bundled. */
    suspend fun glossFor(word: String): String?

    /** The whole word→gloss map (loaded once); empty if the db has no glosses table/data. */
    suspend fun all(): Map<String, String>
}

/**
 * [GlossRepository] over the prepackaged `glosses` table. Loads the map once and caches it in
 * memory. Keys are full inflected surface forms, lowercased in the Turkish locale so lookups match
 * the sentence's words regardless of casing.
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
            .associate { it.word.lowercase(tr) to it.gloss }
        cache = loaded
        return loaded
    }

    override suspend fun glossFor(word: String): String? = all()[word.lowercase(tr)]
}
