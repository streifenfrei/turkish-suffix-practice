package com.example.suffixtrainer.pipeline.tatoeba

/**
 * A Turkish sentence paired with one English translation, plus whether Tatoeba
 * has an audio recording for it. [turkishId] is the Tatoeba sentence id.
 */
data class SentencePair(
    val turkishId: Long,
    val turkishText: String,
    val englishText: String,
    val hasAudio: Boolean,
)
