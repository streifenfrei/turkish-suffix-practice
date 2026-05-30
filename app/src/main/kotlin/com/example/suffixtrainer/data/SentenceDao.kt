package com.example.suffixtrainer.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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
}
