package com.example.suffixtrainer.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.GlossRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.domain.Card
import com.example.suffixtrainer.domain.buildDeck
import com.example.suffixtrainer.domain.isSuffixCorrect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** Bumped on every card load; the UI keys focus on it so the first blank refocuses per card. */
    val nonce: Int = 0,
) {
    val blanks get() = card?.blanks ?: emptyList()
}

/**
 * Owns one practice card at a time. The pool is `buildDeck(corpus, enabledCategories)`; swiping
 * left draws the [nextCard] (a new random card, or forward through history) and swiping right
 * revisits the [previousCard]. When the enabled categories change, the current card is kept if it
 * still belongs to the pool, otherwise a fresh one is drawn (so disabling its category can't leave
 * a stale/blank card on screen).
 */
@HiltViewModel
class PracticeViewModel @Inject constructor(
    cardRepository: CardRepository,
    preferencesRepository: PreferencesRepository,
    glossRepository: GlossRepository,
) : ViewModel() {

    private val random = Random.Default
    private var deck: List<Card> = emptyList()
    private var loadCounter = 0

    // Card history so swipes are bidirectional: swipe-left advances (drawing a new random card when
    // at the front), swipe-right steps back through cards already seen. [position] indexes [history].
    private val history = mutableListOf<Card>()
    private var position = 0

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    /** word → English gloss (full surface forms), loaded once for the per-word translations. */
    private val _glosses = MutableStateFlow<Map<String, String>>(emptyMap())
    val glosses: StateFlow<Map<String, String>> = _glosses.asStateFlow()

    /** When on, each word's translation is shown permanently above the sentence (Settings toggle). */
    val showGlosses: StateFlow<Boolean> = preferencesRepository.showGlosses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { _glosses.value = glossRepository.all() }

        combine(
            preferencesRepository.enabledCategories,
            cardRepository.observeCorpus(),
        ) { enabled, corpus -> buildDeck(corpus, enabled) }
            .onEach { newDeck ->
                deck = newDeck
                val current = _uiState.value.card
                when {
                    newDeck.isEmpty() -> {
                        history.clear()
                        position = 0
                        _uiState.value = PracticeUiState()
                    }
                    current == null || newDeck.none { it.sentenceId == current.sentenceId } -> {
                        // The deck changed under us: start a fresh history from a new card.
                        val card = drawRandom(exclude = null)
                        history.clear()
                        history += card
                        position = 0
                        _uiState.value = stateFor(card)
                    }
                    // else: keep the current card, history, and the learner's in-progress input.
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Update one blank's text. Ignored once the card has been checked. The input is capped at
     * [MAX_BLANK_LEN] characters — no Turkish case/tense suffix surface form is longer, so this
     * stops accidental over-typing without ever truncating a legitimate answer.
     */
    fun onInputChange(index: Int, value: String) {
        val state = _uiState.value
        if (state.checked || index !in state.inputs.indices) return
        _uiState.value = state.copy(
            inputs = state.inputs.toMutableList().also { it[index] = value.take(MAX_BLANK_LEN) },
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

    /** Swipe-left: advance — re-show the next card in history, or draw a new random one at the front. */
    fun nextCard() {
        if (deck.isEmpty()) {
            _uiState.value = PracticeUiState()
            return
        }
        if (position < history.lastIndex) {
            position++
        } else {
            val card = drawRandom(exclude = history.getOrNull(position)?.sentenceId)
            history += card
            position = history.lastIndex
        }
        _uiState.value = stateFor(history[position])
    }

    /** Swipe-right: step back to the previously seen card. No-op at the start of history. */
    fun previousCard() {
        if (position <= 0) return
        position--
        _uiState.value = stateFor(history[position])
    }

    /** Pick a random card from the deck, avoiding [exclude] (the current card) when possible. */
    private fun drawRandom(exclude: Long?): Card {
        var card = deck[random.nextInt(deck.size)]
        if (deck.size > 1) {
            while (card.sentenceId == exclude) card = deck[random.nextInt(deck.size)]
        }
        return card
    }

    private fun stateFor(card: Card): PracticeUiState =
        PracticeUiState(card = card, inputs = List(card.blanks.size) { "" }, nonce = ++loadCounter)

    private companion object {
        /** Longest Turkish suffix surface form we blank (e.g. -iyor, -ecek, -meli) is 4 chars. */
        const val MAX_BLANK_LEN = 4
    }
}
