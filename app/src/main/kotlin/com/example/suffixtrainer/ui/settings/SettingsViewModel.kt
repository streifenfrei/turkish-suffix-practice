package com.example.suffixtrainer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.domain.cardCountByCategory
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.ui.CASE_CATEGORIES
import com.example.suffixtrainer.ui.TENSE_CATEGORIES
import com.example.suffixtrainer.ui.displayLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One toggle row in Settings. [count] is how many cards exist for this category. */
data class CategoryToggle(
    val category: Category,
    val label: String,
    val enabled: Boolean,
    val count: Int,
)

/** Immutable UI state for the Settings screen: the two grouped sections of toggles. */
data class SettingsUiState(
    val cases: List<CategoryToggle> = emptyList(),
    val tenses: List<CategoryToggle> = emptyList(),
)

/**
 * Exposes the enabled-category toggles and persists changes. Writing back through
 * [PreferencesRepository] is what causes the Practice deck to re-filter, since both screens
 * observe the same preferences flow.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    cardRepository: CardRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.enabledCategories,
        cardRepository.observeCorpus(),
    ) { enabled, corpus ->
        val counts = cardCountByCategory(corpus)
        SettingsUiState(
            cases = CASE_CATEGORIES.map { it.toToggle(it in enabled, counts[it] ?: 0) },
            tenses = TENSE_CATEGORIES.map { it.toToggle(it in enabled, counts[it] ?: 0) },
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun toggle(category: Category, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCategoryEnabled(category, enabled)
        }
    }

    private fun Category.toToggle(enabled: Boolean, count: Int) =
        CategoryToggle(this, displayLabel(), enabled, count)
}
