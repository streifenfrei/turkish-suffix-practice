package com.example.suffixtrainer.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One word in a [Sentence]. `surface` is the word as written (e.g. "evde"),
 * `lemma` its root (e.g. "ev"). `charStart`/`charEnd` locate the word within
 * the sentence's Turkish text so suffix ranges can be blanked accurately.
 */
@Entity(
    tableName = "tokens",
    foreignKeys = [
        ForeignKey(
            entity = Sentence::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sentenceId")],
)
data class Token(
    @PrimaryKey val id: Long,
    val sentenceId: Long,
    val position: Int,
    val surface: String,
    val lemma: String,
    val charStart: Int,
    val charEnd: Int,
)
