package com.example.suffixtrainer.ui.practice

import com.example.suffixtrainer.audio.AudioPlayer
import com.example.suffixtrainer.data.FakeCardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    // One eager dispatcher shared by the test scope and viewModelScope, so StateFlow updates
    // propagate synchronously and `uiState.value` is current right after each action.
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prefs: FakePreferencesRepository) =
        PracticeViewModel(FakeCardRepository(), prefs, NoOpAudioPlayer())

    @Test
    fun `deck contains only cards with an enabled suffix and re-filters live`() =
        runTest(dispatcher) {
            val prefs = FakePreferencesRepository(setOf(Category.LOCATIVE))
            val vm = viewModel(prefs)
            backgroundScope.launchCollect(vm.uiState)

            // Only "Evde kaldım." carries LOCATIVE.
            assertEquals(1, vm.uiState.value.deck.size)
            assertTrue(vm.uiState.value.deck.single().blanks.all { it.category == Category.LOCATIVE })

            // Enabling PAST_DEFINITE pulls in more sentences and changes the blanks.
            prefs.setCategoryEnabled(Category.PAST_DEFINITE, true)
            assertTrue(vm.uiState.value.deck.size > 1)
            val evde = vm.uiState.value.deck.first { card -> card.english == "I stayed at home" }
            assertEquals(
                setOf(Category.LOCATIVE, Category.PAST_DEFINITE),
                evde.blanks.map { it.category }.toSet(),
            )
        }

    @Test
    fun `next prev and reveal transitions`() = runTest(dispatcher) {
        val prefs = FakePreferencesRepository(setOf(Category.PAST_DEFINITE)) // 4 cards
        val vm = viewModel(prefs)
        backgroundScope.launchCollect(vm.uiState)

        assertEquals(0, vm.uiState.value.index)
        assertFalse(vm.uiState.value.hasPrev)
        assertTrue(vm.uiState.value.hasNext)

        vm.toggleReveal()
        assertTrue(vm.uiState.value.revealed)

        vm.next()
        assertEquals(1, vm.uiState.value.index)
        assertFalse("reveal resets on navigation", vm.uiState.value.revealed)
        assertTrue(vm.uiState.value.hasPrev)

        vm.prev()
        assertEquals(0, vm.uiState.value.index)
    }

    @Test
    fun `empty deck when enabled categories produce no cards`() = runTest(dispatcher) {
        // NOMINATIVE is zero-width: no sample sentence carries it, so the deck is empty.
        val prefs = FakePreferencesRepository(setOf(Category.NOMINATIVE))
        val vm = viewModel(prefs)
        backgroundScope.launchCollect(vm.uiState)

        val state = vm.uiState.value
        assertTrue("deck should be empty", state.deck.isEmpty())
        assertEquals(null, state.current)
        assertEquals(0, state.total)
        assertEquals(0, state.position)
        assertFalse(state.hasPrev)
        assertFalse(state.hasNext)
    }

    @Test
    fun `index clamps when the deck shrinks`() = runTest(dispatcher) {
        val prefs = FakePreferencesRepository(Category.entries.toSet())
        val vm = viewModel(prefs)
        backgroundScope.launchCollect(vm.uiState)

        repeat(vm.uiState.value.total + 5) { vm.next() }
        assertEquals(vm.uiState.value.total - 1, vm.uiState.value.index)

        // Collapse to a single-card deck; the index must clamp into range.
        prefs.replace(setOf(Category.LOCATIVE))
        assertEquals(1, vm.uiState.value.deck.size)
        assertEquals(0, vm.uiState.value.index)
    }
}

/** Keep the WhileSubscribed-shared [uiState] hot for the duration of the test. */
private fun <T> CoroutineScope.launchCollect(flow: StateFlow<T>) {
    launch { flow.collect {} }
}

private class NoOpAudioPlayer : AudioPlayer {
    override fun play(audioPath: String?) = Unit
    override fun release() = Unit
}

private class FakePreferencesRepository(initial: Set<Category>) : PreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val enabledCategories: StateFlow<Set<Category>> = state.asStateFlow()

    override suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        state.update { if (enabled) it + category else it - category }
    }

    fun replace(set: Set<Category>) {
        state.value = set
    }
}
