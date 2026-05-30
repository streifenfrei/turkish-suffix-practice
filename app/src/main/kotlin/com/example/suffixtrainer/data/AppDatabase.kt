package com.example.suffixtrainer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token

/**
 * Read-only Room database over the prepackaged `trainer.db` (shipped in assets
 * and opened via `createFromAsset` — see DatabaseModule). Entities are the frozen
 * contract from :core-model.
 */
@Database(
    entities = [Sentence::class, Token::class, Suffix::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao
}
