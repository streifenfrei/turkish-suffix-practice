package com.example.suffixtrainer.ui.settings

import com.example.suffixtrainer.data.FakeCardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.data.SAMPLE_CORPUS
import com.example.suffixtrainer.domain.cardCountByCategory
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
        SettingsViewModel(prefs, FakeCardRepository())

    @Test
    fun `each toggle reports the corpus card count for its category`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(Category.entries.toSet()))
        backgroundScope.launchCollect(vm.uiState)

        val expected = cardCountByCategory(SAMPLE_CORPUS)
        val allToggles = vm.uiState.value.cases + vm.uiState.value.tenses
        assertEquals(Category.entries.size, allToggles.size)
        for (toggle in allToggles) {
            assertEquals(
                "count for ${toggle.category}",
                expected[toggle.category] ?: 0,
                toggle.count,
            )
        }
        // Counts are independent of which categories are enabled (it's inventory, not the deck).
        assertTrue("some category should have cards", allToggles.any { it.count > 0 })
    }

    @Test
    fun `a category absent from the corpus shows zero`() = runTest(dispatcher) {
        val vm = viewModel(FakePreferencesRepository(Category.entries.toSet()))
        backgroundScope.launchCollect(vm.uiState)

        // NOMINATIVE is the zero-width nominative case; the sample corpus has no such suffix.
        val nominative = vm.uiState.value.cases.first { it.category == Category.NOMINATIVE }
        assertEquals(0, nominative.count)
    }

    @Test
    fun `toggle reflects enabled state without affecting counts`() = runTest(dispatcher) {
        val prefs = FakePreferencesRepository(setOf(Category.LOCATIVE))
        val vm = viewModel(prefs)
        backgroundScope.launchCollect(vm.uiState)

        val locative = { vm.uiState.value.cases.first { it.category == Category.LOCATIVE } }
        assertTrue(locative().enabled)
        val countBefore = locative().count

        vm.toggle(Category.LOCATIVE, false)
        assertTrue(!locative().enabled)
        assertEquals("disabling a category must not change its count", countBefore, locative().count)
    }
}

private fun <T> CoroutineScope.launchCollect(flow: StateFlow<T>) {
    launch { flow.collect {} }
}

private class FakePreferencesRepository(initial: Set<Category>) : PreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val enabledCategories: StateFlow<Set<Category>> = state.asStateFlow()

    override suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        state.update { if (enabled) it + category else it - category }
    }
}
