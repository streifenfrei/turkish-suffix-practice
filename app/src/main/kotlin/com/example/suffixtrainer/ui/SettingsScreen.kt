package com.example.suffixtrainer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.ui.settings.CategoryToggle
import com.example.suffixtrainer.ui.settings.SettingsUiState
import com.example.suffixtrainer.ui.settings.SettingsViewModel
import com.example.suffixtrainer.ui.theme.TurkishSuffixPracticeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingsContent(
            state = state,
            onToggle = viewModel::toggle,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onToggle: (Category, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item { SectionHeader("Cases") }
        items(state.cases, key = { it.category.name }) { toggle ->
            ToggleRow(toggle, onToggle)
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionHeader("Tenses & moods") }
        items(state.tenses, key = { it.category.name }) { toggle ->
            ToggleRow(toggle, onToggle)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    toggle: CategoryToggle,
    onToggle: (Category, Boolean) -> Unit,
) {
    // A category with no cards can't be practiced yet: disable its switch but keep the row
    // (and its "0 cards") visible, just dimmed, so it reads as "exists, no content yet".
    val hasCards = toggle.count > 0
    val contentAlpha = if (hasCards) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = toggle.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${toggle.count} cards",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            modifier = Modifier.padding(end = 12.dp),
        )
        Switch(
            checked = toggle.enabled,
            onCheckedChange = { onToggle(toggle.category, it) },
            enabled = hasCards,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    val sample = SettingsUiState(
        cases = CASE_CATEGORIES.mapIndexed { i, c ->
            CategoryToggle(c, c.displayLabel(), c != Category.NOMINATIVE, count = i * 17)
        },
        tenses = TENSE_CATEGORIES.mapIndexed { i, c ->
            CategoryToggle(c, c.displayLabel(), true, count = 40 - i * 5)
        },
    )
    TurkishSuffixPracticeTheme {
        SettingsContent(state = sample, onToggle = { _, _ -> })
    }
}
