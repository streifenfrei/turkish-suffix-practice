package com.example.suffixtrainer.data

import kotlinx.coroutines.flow.Flow

/**
 * Source of the practice corpus — the raw sentences with their morphology. The deck (filtered,
 * blanked cards) is derived from this in the ViewModel, so this interface stays a dumb data
 * source. A Room-backed implementation can replace the fake by emitting the same [CardData]
 * shape from a JOIN; no UI or ViewModel change is required.
 */
interface CardRepository {
    fun observeCorpus(): Flow<List<CardData>>
}
