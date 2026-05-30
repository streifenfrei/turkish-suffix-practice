package com.example.suffixtrainer.data

import androidx.room.Dao
import androidx.room.Query

/**
 * Stub DAO over the prepackaged corpus. Real card/deck queries are added in a
 * later milestone; for now it only exposes a row count so the Room schema and
 * wiring compile and can be smoke-tested.
 */
@Dao
interface SentenceDao {
    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun count(): Int
}
