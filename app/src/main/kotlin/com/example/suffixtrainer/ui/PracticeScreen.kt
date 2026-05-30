package com.example.suffixtrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.suffixtrainer.domain.Card as PracticeCard
import com.example.suffixtrainer.domain.CardSegment
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.ui.practice.PracticeUiState
import com.example.suffixtrainer.ui.practice.PracticeViewModel
import com.example.suffixtrainer.ui.theme.TurkishSuffixPracticeTheme

private const val BLANK_PLACEHOLDER = "___"

@Composable
fun PracticeScreen(
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeContent(
        state = state,
        onPrev = viewModel::prev,
        onNext = viewModel::next,
        onReveal = viewModel::toggleReveal,
        onPlay = viewModel::playAudio,
        modifier = modifier,
    )
}

@Composable
private fun PracticeContent(
    state: PracticeUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReveal: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.current
    if (card == null) {
        EmptyDeck(modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${state.position} / ${state.total}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = card.english,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = turkishLine(card, state.revealed, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("Play audio", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onReveal) {
            Icon(
                imageVector = if (state.revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
            )
            Text(
                text = if (state.revealed) "Hide" else "Reveal",
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onPrev,
                enabled = state.hasPrev,
                modifier = Modifier.weight(1f),
            ) { Text("Previous") }
            Button(
                onClick = onNext,
                enabled = state.hasNext,
                modifier = Modifier.weight(1f),
            ) { Text("Next") }
        }
    }
}

/**
 * Renders the Turkish line inline: literal segments as-is, blanked suffixes as a placeholder
 * (or the answer when [revealed]), styled in [accent] with an underline.
 */
private fun turkishLine(card: PracticeCard, revealed: Boolean, accent: Color): AnnotatedString =
    buildAnnotatedString {
        for (segment in card.segments) {
            when (segment) {
                is CardSegment.Text -> append(segment.text)
                is CardSegment.Blank -> withStyle(
                    SpanStyle(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(if (revealed) segment.answer else BLANK_PLACEHOLDER)
                }
            }
        }
    }

@Composable
private fun EmptyDeck(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No cards match the enabled categories.\nEnable some in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PracticeScreenPreview() {
    val sampleCard = PracticeCard(
        sentenceId = 1,
        english = "I stayed at home",
        segments = listOf(
            CardSegment.Text("Ev"),
            CardSegment.Blank("de", Category.LOCATIVE),
            CardSegment.Text(" kal"),
            CardSegment.Blank("dı", Category.PAST_DEFINITE),
            CardSegment.Text("m."),
        ),
        audioPath = null,
    )
    TurkishSuffixPracticeTheme {
        PracticeContent(
            state = PracticeUiState(deck = listOf(sampleCard), index = 0, revealed = false),
            onPrev = {},
            onNext = {},
            onReveal = {},
            onPlay = {},
        )
    }
}
