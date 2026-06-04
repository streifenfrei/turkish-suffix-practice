package com.example.suffixtrainer.ui.practice

import com.example.suffixtrainer.data.FakeCardRepository
import com.example.suffixtrainer.data.GlossRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

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
        PracticeViewModel(FakeCardRepository(), prefs, FakeGlossRepository)

    @Test
    fun `empty deck shows no card when enabled categories produce nothing`() = runTest(dispatcher) {
        // NOMINATIVE is zero-width: no sample card carries it.
        val vm = viewModel(FakePreferencesRepository(setOf(Category.NOMINATIVE)))
        assertNull(vm.uiState.value.card)
    }

    @Test
    fun `single-blank card grades a correct answer case-insensitively`() = runTest(dispatcher) {
        // Only "Evde kaldım." carries LOCATIVE → a one-blank card whose answer is "de".
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        val card = vm.uiState.value.card!!
        assertEquals(1, card.blanks.size)
        assertEquals("de", card.blanks.single().answer)

        vm.onInputChange(0, "DE")
        vm.check()
        assertTrue(vm.uiState.value.checked)
        assertEquals(listOf(true), vm.uiState.value.results)
    }

    @Test
    fun `wrong answer is graded incorrect`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        vm.onInputChange(0, "xx")
        vm.check()
        assertEquals(listOf(false), vm.uiState.value.results)
    }

    @Test
    fun `input is locked once checked`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        vm.onInputChange(0, "de")
        vm.check()
        vm.onInputChange(0, "xx") // ignored after checking
        assertEquals(listOf("de"), vm.uiState.value.inputs)
    }

    @Test
    fun `nextCard clears input and check state`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        vm.onInputChange(0, "de")
        vm.check()

        vm.nextCard()
        val state = vm.uiState.value
        assertFalse(state.checked)
        assertTrue(state.results.isEmpty())
        assertEquals(listOf(""), state.inputs) // single blank, reset to empty
        assertEquals("de", state.card!!.blanks.single().answer)
    }

    @Test
    fun `previousCard revisits the prior card`() = runTest(dispatcher) {
        // PAST_DEFINITE spans several sample sentences, so the deck has more than one card.
        val vm = viewModel(FakePreferencesRepository(setOf(Category.PAST_DEFINITE)))
        val first = vm.uiState.value.card!!.sentenceId

        vm.nextCard()
        val second = vm.uiState.value.card!!.sentenceId
        assertNotEquals(first, second) // never repeats the current card in a row

        vm.previousCard()
        assertEquals(first, vm.uiState.value.card!!.sentenceId)

        // Going forward again returns to the same already-seen second card (history, not re-drawn).
        vm.nextCard()
        assertEquals(second, vm.uiState.value.card!!.sentenceId)
    }

    @Test
    fun `previousCard is a no-op at the start of history`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.PAST_DEFINITE)))
        val first = vm.uiState.value.card!!.sentenceId
        vm.previousCard()
        assertEquals(first, vm.uiState.value.card!!.sentenceId)
    }

    @Test
    fun `blank input is capped at the longest suffix length`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        vm.onInputChange(0, "abcdef")
        assertEquals(listOf("abcd"), vm.uiState.value.inputs) // capped to 4 chars
    }
}

private object FakeGlossRepository : GlossRepository {
    override suspend fun glossFor(word: String): String? = null
    override suspend fun all(): Map<String, String> = emptyMap()
}

private class FakePreferencesRepository(initial: Set<Category>) : PreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val enabledCategories: StateFlow<Set<Category>> = state.asStateFlow()

    override suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        state.update { if (enabled) it + category else it - category }
    }

    private val glossesShown = MutableStateFlow(false)
    override val showGlosses: StateFlow<Boolean> = glossesShown.asStateFlow()

    override suspend fun setShowGlosses(enabled: Boolean) {
        glossesShown.value = enabled
    }
}
