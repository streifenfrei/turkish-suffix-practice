package com.example.suffixtrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
private val CorrectGreen = androidx.compose.ui.graphics.Color(0xFF66BB6A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        PracticeContent(
            state = state,
            onInput = viewModel::onInputChange,
            onCheck = viewModel::check,
            onNewCard = viewModel::newCard,
            modifier = Modifier.padding(innerPadding),
        )
    }
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

    // One focus requester per blank; the cursor starts in the first blank of each new card.
    val focusRequesters = remember(state.nonce) { List(card.blanks.size) { FocusRequester() } }
    LaunchedEffect(state.nonce) { runCatching { focusRequesters.firstOrNull()?.requestFocus() } }

    // Enter on a blank: jump to the next blank; on the last blank, Check; after Check, new card.
    val onImeAction: (Int) -> Unit = { i ->
        when {
            i < card.blanks.lastIndex -> runCatching { focusRequesters[i + 1].requestFocus() }
            state.checked -> onNewCard()
            else -> onCheck()
        }
    }

    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    Column(
        modifier = modifier
            .fillMaxSize()
            // Centre the content in the space between the top bar and the keyboard.
            .imePadding()
            // A horizontal swipe (either direction) draws a new random card.
            .pointerInput(state.nonce) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(total) > swipeThresholdPx) onNewCard()
                        total = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                )
            }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = card.english,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        TurkishLine(
            state = state,
            card = card,
            focusRequesters = focusRequesters,
            onInput = onInput,
            onImeAction = onImeAction,
        )
    }
}

/** The Turkish line, directly on the background: literal text with editable blanks, wrapping. */
@Composable
private fun TurkishLine(
    state: PracticeUiState,
    card: PracticeCard,
    focusRequesters: List<FocusRequester>,
    onInput: (Int, String) -> Unit,
    onImeAction: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        var blankIndex = 0
        card.segments.forEach { segment ->
            when (segment) {
                is CardSegment.Text -> Text(
                    text = segment.text,
                    style = MaterialTheme.typography.headlineMedium,
                )

                is CardSegment.Blank -> {
                    val i = blankIndex++
                    BlankCell(
                        answer = segment.answer,
                        input = state.inputs.getOrElse(i) { "" },
                        checked = state.checked,
                        correct = state.results.getOrElse(i) { false },
                        focusRequester = focusRequesters.getOrNull(i),
                        imeAction = if (i == card.blanks.lastIndex) ImeAction.Done else ImeAction.Next,
                        onInput = { onInput(i, it) },
                        onImeAction = { onImeAction(i) },
                    )
                }
            }
        }
    }
}

/**
 * An inline blank that hugs its content, so a filled-in blank reads exactly like the surrounding
 * sentence. While empty it shows a small underline as a slot marker; once typed it just blends in
 * (its colour turns green/red after checking). The correct suffix is shown small above when wrong.
 */
@Composable
private fun BlankCell(
    answer: String,
    input: String,
    checked: Boolean,
    correct: Boolean,
    focusRequester: FocusRequester?,
    imeAction: ImeAction,
    onInput: (String) -> Unit,
    onImeAction: () -> Unit,
) {
    val textColor = when {
        !checked -> MaterialTheme.colorScheme.onSurface
        correct -> CorrectGreen
        else -> MaterialTheme.colorScheme.error
    }
    val underlineColor = if (checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.headlineMedium
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (checked && !correct) {
            Text(text = answer, style = MaterialTheme.typography.labelMedium, color = CorrectGreen)
        }
        // BasicTextField fills its parent's width by default, so size it to content with an
        // invisible sizer Text: an empty blank is just a small slot; a filled one hugs its text.
        // A faint brighter background marks the slot against the page.
        Box(
            modifier = (if (input.isEmpty()) Modifier.widthIn(min = 32.dp) else Modifier)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Reserve a few dp past the text for the cursor, so the field never has to scroll
            // (which would push the first letters left, under the previous word).
            Text(text = input, style = style, modifier = Modifier.alpha(0f).padding(end = 4.dp))
            BasicTextField(
                value = input,
                // Stays editable so the keyboard remains up for the Enter-to-advance flow; the
                // ViewModel ignores input once checked, so the shown answer can't change.
                onValueChange = onInput,
                singleLine = true,
                // Left-aligned so the suffix stays anchored to the preceding stem as it grows.
                textStyle = style.copy(color = textColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() },
                ),
                modifier = Modifier
                    .matchParentSize()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .drawBehind {
                        if (input.isEmpty()) {
                            val y = size.height
                            drawLine(
                                underlineColor,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                        }
                    },
            )
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
