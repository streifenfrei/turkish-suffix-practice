package com.example.suffixtrainer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.ui.CASE_CATEGORIES
import com.example.suffixtrainer.ui.TENSE_CATEGORIES
import com.example.suffixtrainer.ui.displayLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One toggle row in Settings. */
data class CategoryToggle(
    val category: Category,
    val label: String,
    val enabled: Boolean,
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
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesRepository.enabledCategories
        .map { enabled ->
            SettingsUiState(
                cases = CASE_CATEGORIES.map { it.toToggle(it in enabled) },
                tenses = TENSE_CATEGORIES.map { it.toToggle(it in enabled) },
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

    private fun Category.toToggle(enabled: Boolean) = CategoryToggle(this, displayLabel(), enabled)
}
