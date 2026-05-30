package com.example.suffixtrainer.data

import com.example.suffixtrainer.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * The set of grammatical categories currently enabled in Settings. Persisted across restarts.
 * The Practice deck is a function of this set, so the ViewModel observes [enabledCategories]
 * and recomputes the deck whenever it changes.
 */
interface PreferencesRepository {
    val enabledCategories: Flow<Set<Category>>

    suspend fun setCategoryEnabled(category: Category, enabled: Boolean)
}
