package com.example.suffixtrainer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One Turkish sentence paired with its English translation, sourced from
 * Tatoeba. `audioPath` is null when no audio is available.
 */
@Entity(tableName = "sentences")
data class Sentence(
    @PrimaryKey val id: Long,
    val turkishText: String,
    val englishText: String,
    val audioPath: String?,
    val tatoebaId: Long,
)
