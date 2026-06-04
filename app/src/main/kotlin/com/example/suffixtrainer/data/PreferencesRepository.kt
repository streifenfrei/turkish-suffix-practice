package com.example.suffixtrainer.data

import com.example.suffixtrainer.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * User settings persisted across restarts. The Practice deck is a function of [enabledCategories],
 * so the ViewModel observes it and recomputes the deck whenever it changes. [showGlosses] toggles
 * the permanent per-word translations shown above the sentence on the Practice screen.
 */
interface PreferencesRepository {
    val enabledCategories: Flow<Set<Category>>

    suspend fun setCategoryEnabled(category: Category, enabled: Boolean)

    /** Whether to show each word's English translation permanently above it (default off). */
    val showGlosses: Flow<Boolean>

    suspend fun setShowGlosses(enabled: Boolean)
}
