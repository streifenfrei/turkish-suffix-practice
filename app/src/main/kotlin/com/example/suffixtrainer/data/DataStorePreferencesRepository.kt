package com.example.suffixtrainer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.suffixtrainer.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PreferencesRepository] backed by Jetpack DataStore (Preferences). Enabled categories are
 * stored as a set of [Category.name] strings (stable across enum reordering). When the key is
 * absent — i.e. first launch — every category is enabled by default.
 */
@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val enabledCategories: Flow<Set<Category>> =
        dataStore.data.map { prefs ->
            val stored = prefs[ENABLED_CATEGORIES_KEY]
            if (stored == null) {
                Category.entries.toSet()
            } else {
                stored.mapNotNullTo(mutableSetOf()) { name ->
                    runCatching { Category.valueOf(name) }.getOrNull()
                }
            }
        }

    override suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        dataStore.edit { prefs ->
            // Materialize the current set (defaulting to all-enabled) before mutating, so the
            // first toggle persists an explicit set rather than relying on the absent-key default.
            val current = prefs[ENABLED_CATEGORIES_KEY]
                ?.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
                ?.toMutableSet()
                ?: Category.entries.toMutableSet()
            if (enabled) current.add(category) else current.remove(category)
            prefs[ENABLED_CATEGORIES_KEY] = current.mapTo(mutableSetOf()) { it.name }
        }
    }

    private companion object {
        val ENABLED_CATEGORIES_KEY = stringSetPreferencesKey("enabled_categories")
    }
}
