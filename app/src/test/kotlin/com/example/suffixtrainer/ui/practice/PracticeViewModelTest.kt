package com.example.suffixtrainer.ui.practice

import com.example.suffixtrainer.data.FakeCardRepository
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
        PracticeViewModel(FakeCardRepository(), prefs)

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
    fun `newCard clears input and check state`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(setOf(Category.LOCATIVE)))
        vm.onInputChange(0, "de")
        vm.check()

        vm.newCard()
        val state = vm.uiState.value
        assertFalse(state.checked)
        assertTrue(state.results.isEmpty())
        assertEquals(listOf(""), state.inputs) // single blank, reset to empty
        assertEquals("de", state.card!!.blanks.single().answer)
    }
}

private class FakePreferencesRepository(initial: Set<Category>) : PreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val enabledCategories: StateFlow<Set<Category>> = state.asStateFlow()

    override suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        state.update { if (enabled) it + category else it - category }
    }
}
