package com.example.suffixtrainer.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suffixtrainer.audio.AudioPlayer
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.domain.Card
import com.example.suffixtrainer.domain.buildDeck
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Immutable UI state for the Practice screen. The deck is derived, never stored. */
data class PracticeUiState(
    val deck: List<Card> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
) {
    val current: Card? get() = deck.getOrNull(index)
    val hasPrev: Boolean get() = index > 0
    val hasNext: Boolean get() = index < deck.size - 1
    val total: Int get() = deck.size

    /** 1-based position for display; 0 when the deck is empty. */
    val position: Int get() = if (deck.isEmpty()) 0 else index + 1
}

/**
 * Owns the Practice deck. The deck is a pure function of (enabled categories, corpus): whenever
 * either changes the deck is recomputed via [buildDeck], so toggling a category in Settings
 * immediately re-filters the deck and changes which suffixes are blanked.
 */
@HiltViewModel
class PracticeViewModel @Inject constructor(
    cardRepository: CardRepository,
    preferencesRepository: PreferencesRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val nav = MutableStateFlow(NavState())

    private val deck = combine(
        preferencesRepository.enabledCategories,
        cardRepository.observeCorpus(),
    ) { enabled, corpus -> buildDeck(corpus, enabled) }

    val uiState: StateFlow<PracticeUiState> = combine(deck, nav) { deck, nav ->
        // Clamp the index defensively: the deck shrinks when categories are disabled.
        val index = nav.index.coerceIn(0, maxOf(0, deck.size - 1))
        PracticeUiState(deck = deck, index = index, revealed = nav.revealed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PracticeUiState(),
    )

    fun next() {
        val state = uiState.value
        val target = (state.index + 1).coerceAtMost(maxOf(0, state.deck.size - 1))
        nav.value = NavState(index = target, revealed = false)
    }

    fun prev() {
        val target = (uiState.value.index - 1).coerceAtLeast(0)
        nav.value = NavState(index = target, revealed = false)
    }

    fun toggleReveal() {
        nav.value = nav.value.copy(revealed = !nav.value.revealed)
    }

    fun playAudio() {
        audioPlayer.play(uiState.value.current?.audioPath)
    }

    override fun onCleared() {
        audioPlayer.release()
    }

    private data class NavState(val index: Int = 0, val revealed: Boolean = false)
}
