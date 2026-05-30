package com.example.suffixtrainer.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs

/** Green used to mark a correct answer (and the shown solution); reads on the dark theme. */
private val CorrectGreen = Color(0xFF66BB6A)

@Composable
fun PracticeScreen(
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeContent(
        state = state,
        onInput = viewModel::onInputChange,
        onCheck = viewModel::check,
        onNewCard = viewModel::newCard,
        modifier = modifier,
    )
}

@Composable
private fun PracticeContent(
    state: PracticeUiState,
    onInput: (Int, String) -> Unit,
    onCheck: () -> Unit,
    onNewCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.card
    if (card == null) {
        EmptyDeck(modifier)
        return
    }

    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    Column(
        modifier = modifier
            .fillMaxSize()
            // A horizontal swipe (either direction) draws a new random card.
            .pointerInput(card.sentenceId) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(total) > swipeThresholdPx) onNewCard()
                        total = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
                Spacer(Modifier.height(20.dp))
                TurkishLine(state = state, card = card, onInput = onInput)
            }
        }

        Spacer(Modifier.height(28.dp))

        if (!state.checked) {
            Button(onClick = onCheck) { Text("Check") }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = "Swipe for a new card",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The Turkish line: literal text segments interleaved with editable blank fields, wrapping. */
@Composable
private fun TurkishLine(
    state: PracticeUiState,
    card: PracticeCard,
    onInput: (Int, String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.Start,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        var blankIndex = 0
        card.segments.forEach { segment ->
            when (segment) {
                is CardSegment.Text -> Text(
                    text = segment.text,
                    style = MaterialTheme.typography.headlineSmall,
                )

                is CardSegment.Blank -> {
                    val i = blankIndex++
                    BlankCell(
                        answer = segment.answer,
                        input = state.inputs.getOrElse(i) { "" },
                        checked = state.checked,
                        correct = state.results.getOrElse(i) { false },
                        onInput = { onInput(i, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlankCell(
    answer: String,
    input: String,
    checked: Boolean,
    correct: Boolean,
    onInput: (String) -> Unit,
) {
    val color = when {
        !checked -> MaterialTheme.colorScheme.primary
        correct -> CorrectGreen
        else -> MaterialTheme.colorScheme.error
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // The correct suffix, shown above only when this blank was answered wrong.
        if (checked && !correct) {
            Text(
                text = answer,
                style = MaterialTheme.typography.labelSmall,
                color = CorrectGreen,
            )
        }
        BasicTextField(
            value = input,
            onValueChange = onInput,
            readOnly = checked,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = color,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .widthIn(min = 56.dp)
                .drawBehind {
                    val y = size.height
                    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (input.isEmpty() && !checked) {
                        Text(
                            text = "___",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun EmptyDeck(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No cards for the selected categories — adjust in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

private val SAMPLE_CARD = PracticeCard(
    sentenceId = 1,
    english = "I stayed at home",
    segments = listOf(
        CardSegment.Text("Ev"),
        CardSegment.Blank("de", Category.LOCATIVE),
        CardSegment.Text(" kal"),
        CardSegment.Blank("dı", Category.PAST_DEFINITE),
        CardSegment.Text("m."),
    ),
)

@Preview(showBackground = true, name = "Practice — typing")
@Composable
private fun PracticeScreenPreview() {
    TurkishSuffixPracticeTheme {
        PracticeContent(
            state = PracticeUiState(card = SAMPLE_CARD, inputs = listOf("de", "")),
            onInput = { _, _ -> },
            onCheck = {},
            onNewCard = {},
        )
    }
}

@Preview(showBackground = true, name = "Practice — checked")
@Composable
private fun PracticeScreenCheckedPreview() {
    TurkishSuffixPracticeTheme {
        PracticeContent(
            state = PracticeUiState(
                card = SAMPLE_CARD,
                inputs = listOf("de", "xx"),
                checked = true,
                results = listOf(true, false),
            ),
            onInput = { _, _ -> },
            onCheck = {},
            onNewCard = {},
        )
    }
}
