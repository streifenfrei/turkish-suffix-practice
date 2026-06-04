package com.example.suffixtrainer.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A word→translation row from the prepackaged `glosses` table (keyed by full surface form). */
data class GlossRow(val word: String, val gloss: String)

/**
 * DAO over the prepackaged corpus. [observeCorpus] returns every sentence with its tokens and
 * each token's suffixes; [RoomCardRepository] maps the rows into [CardData]. Deck filtering and
 * blanking happen downstream in the domain layer, so this stays a dumb JOIN.
 */
@Dao
interface SentenceDao {
    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM sentences")
    fun observeCorpus(): Flow<List<SentenceWithTokensRow>>

    /**
     * Every word gloss. `glosses` is a non-Room table shipped in the asset db (the pipeline
     * writes it), so it isn't part of the Room schema — [SkipQueryVerification] tells Room not to
     * verify this query at compile time. Returns empty if the table is absent.
     */
    @SkipQueryVerification
    @Query("SELECT word, gloss FROM glosses")
    suspend fun allGlosses(): List<GlossRow>
}
