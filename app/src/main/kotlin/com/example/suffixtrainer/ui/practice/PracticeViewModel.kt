package com.example.suffixtrainer.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.domain.Card
import com.example.suffixtrainer.domain.buildDeck
import com.example.suffixtrainer.domain.isSuffixCorrect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.random.Random
import javax.inject.Inject

/**
 * Immutable UI state for the Practice screen: a single random card the learner fills in.
 * [inputs] and [results] are indexed in step with [blanks] (reading order).
 */
data class PracticeUiState(
    val card: Card? = null,
    val inputs: List<String> = emptyList(),
    val checked: Boolean = false,
    val results: List<Boolean> = emptyList(),
) {
    val blanks get() = card?.blanks ?: emptyList()
}

/**
 * Owns one random practice card at a time. The pool is `buildDeck(corpus, enabledCategories)`;
 * the shown card is picked at random and replaced on swipe ([newCard]). When the enabled
 * categories change, the current card is kept if it still belongs to the pool, otherwise a fresh
 * one is drawn (so disabling its category can't leave a stale/blank card on screen).
 */
@HiltViewModel
class PracticeViewModel @Inject constructor(
    cardRepository: CardRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val random = Random.Default
    private var deck: List<Card> = emptyList()

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        combine(
            preferencesRepository.enabledCategories,
            cardRepository.observeCorpus(),
        ) { enabled, corpus -> buildDeck(corpus, enabled) }
            .onEach { newDeck ->
                deck = newDeck
                val current = _uiState.value.card
                when {
                    newDeck.isEmpty() -> _uiState.value = PracticeUiState()
                    current == null || newDeck.none { it.sentenceId == current.sentenceId } ->
                        _uiState.value = freshCard(newDeck)
                    // else: keep the current card and the learner's in-progress input.
                }
            }
            .launchIn(viewModelScope)
    }

    /** Update one blank's text. Ignored once the card has been checked. */
    fun onInputChange(index: Int, value: String) {
        val state = _uiState.value
        if (state.checked || index !in state.inputs.indices) return
        _uiState.value = state.copy(
            inputs = state.inputs.toMutableList().also { it[index] = value },
        )
    }

    /** Grade every blank (case-insensitive, trimmed) and reveal the result. */
    fun check() {
        val state = _uiState.value
        val blanks = state.blanks
        if (state.card == null || state.checked || blanks.isEmpty()) return
        val results = blanks.indices.map { i ->
            isSuffixCorrect(state.inputs.getOrElse(i) { "" }, blanks[i].answer)
        }
        _uiState.value = state.copy(checked = true, results = results)
    }

    /** Swipe action: draw a new random card and clear input. */
    fun newCard() {
        _uiState.value = if (deck.isEmpty()) PracticeUiState() else freshCard(deck)
    }

    private fun freshCard(fromDeck: List<Card>): PracticeUiState {
        val previousId = _uiState.value.card?.sentenceId
        var card = fromDeck[random.nextInt(fromDeck.size)]
        // Don't show the same card twice in a row when there's an alternative.
        if (fromDeck.size > 1) {
            while (card.sentenceId == previousId) card = fromDeck[random.nextInt(fromDeck.size)]
        }
        return PracticeUiState(card = card, inputs = List(card.blanks.size) { "" })
    }
}
