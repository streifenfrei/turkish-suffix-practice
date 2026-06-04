package com.example.suffixtrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.suffixtrainer.domain.Card as PracticeCard
import com.example.suffixtrainer.domain.CardPiece
import com.example.suffixtrainer.domain.CardSegment
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.ui.practice.PracticeUiState
import com.example.suffixtrainer.ui.practice.PracticeViewModel
import com.example.suffixtrainer.ui.theme.TurkishSuffixPracticeTheme
import kotlinx.coroutines.launch
import java.util.Locale

/** Green used to mark a correct answer (and the shown solution); reads on the dark theme. */
private val CorrectGreen = androidx.compose.ui.graphics.Color(0xFF66BB6A)
private val TR: Locale = Locale.forLanguageTag("tr")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glosses by viewModel.glosses.collectAsStateWithLifecycle()
    val showGlosses by viewModel.showGlosses.collectAsStateWithLifecycle()
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
            glosses = glosses,
            showGlosses = showGlosses,
            onInput = viewModel::onInputChange,
            onCheck = viewModel::check,
            onNext = viewModel::nextCard,
            onPrevious = viewModel::previousCard,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PracticeContent(
    state: PracticeUiState,
    glosses: Map<String, String>,
    showGlosses: Boolean,
    onInput: (Int, String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
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

    // Enter on a blank: jump to the next blank; on the last blank, Check; after Check, next card.
    val onImeAction: (Int) -> Unit = { i ->
        when {
            i < card.blanks.lastIndex -> runCatching { focusRequesters[i + 1].requestFocus() }
            state.checked -> onNext()
            else -> onCheck()
        }
    }

    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    Column(
        modifier = modifier
            .fillMaxSize()
            // Centre the content in the space between the top bar and the keyboard.
            .imePadding()
            // Bidirectional swipe: left advances to the next card, right steps back.
            .pointerInput(state.nonce) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (total <= -swipeThresholdPx) onNext()
                        else if (total >= swipeThresholdPx) onPrevious()
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
            glosses = glosses,
            showGlosses = showGlosses,
            focusRequesters = focusRequesters,
            onInput = onInput,
            onImeAction = onImeAction,
        )
    }
}

/**
 * The Turkish line, directly on the background. The line wraps only at spaces: each word — together
 * with its suffix blank(s) and any attached punctuation — is laid out as a single unbreakable [Row],
 * so a blank never falls onto a different line from the stem it belongs to. Literal word parts are
 * press-and-hold targets that show the word's translation; when [showGlosses] is on, that
 * translation is also shown permanently above each word and the line bottom-aligns so the words
 * still sit on a common baseline.
 */
@Composable
private fun TurkishLine(
    state: PracticeUiState,
    card: PracticeCard,
    glosses: Map<String, String>,
    showGlosses: Boolean,
    focusRequesters: List<FocusRequester>,
    onInput: (Int, String) -> Unit,
    onImeAction: (Int) -> Unit,
) {
    val style = MaterialTheme.typography.headlineMedium
    val rowAlignment = if (showGlosses) Alignment.Bottom else Alignment.CenterVertically
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        itemVerticalAlignment = rowAlignment,
    ) {
        var blankIndex = 0
        wrapUnits(card.pieces).forEach { unit ->
            Row(verticalAlignment = rowAlignment) {
                unit.forEach { piece ->
                    when (piece) {
                        is CardPiece.Separator -> Text(text = piece.text, style = style)

                        is CardPiece.Word -> {
                            val gloss = glosses[piece.surface.lowercase(TR)]
                            // Render this word's literal/blank parts in reading order, advancing the
                            // shared blank index. Defined as a local content lambda so the parts can
                            // be placed either inline or under a permanent gloss without duplication.
                            val parts: @Composable () -> Unit = {
                                piece.parts.forEach { part ->
                                    when (part) {
                                        is CardSegment.Text ->
                                            TooltipWord(part.text, gloss, style)

                                        is CardSegment.Blank -> {
                                            val i = blankIndex++
                                            BlankCell(
                                                answer = part.answer,
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
                            if (showGlosses && !gloss.isNullOrBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = gloss,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) { parts() }
                                }
                            } else {
                                parts()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Group pieces into wrap units that the line may break *between* but never *within*. A unit holds a
 * word with its blanks plus any directly-attached punctuation; a separator that contains whitespace
 * ends the current unit (a trailing space stays inside it so words keep their visual gap). This is
 * what guarantees a suffix blank never wraps away from its stem.
 */
private fun wrapUnits(pieces: List<CardPiece>): List<List<CardPiece>> {
    val units = mutableListOf<MutableList<CardPiece>>()
    var current = mutableListOf<CardPiece>()
    for (piece in pieces) {
        current += piece
        if (piece is CardPiece.Separator && piece.text.any { it.isWhitespace() }) {
            units += current
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) units += current
    return units
}

/**
 * A literal word part. While pressed-and-held it shows a tooltip with the word's full translation,
 * dismissed on release. Words with no bundled gloss render as plain text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipWord(text: String, gloss: String?, style: TextStyle) {
    if (gloss.isNullOrBlank()) {
        Text(text = text, style = style)
        return
    }
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        // Sit well above the anchor so a pressing thumb doesn't cover the translation.
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(
            spacingBetweenTooltipAndAnchor = 24.dp,
        ),
        tooltip = {
            PlainTooltip {
                Text(gloss, style = MaterialTheme.typography.titleMedium)
            }
        },
        state = tooltipState,
        enableUserInput = false, // we drive show/dismiss from the press below
    ) {
        Text(
            text = text,
            style = style,
            modifier = Modifier.pointerInput(gloss) {
                detectTapGestures(
                    onPress = {
                        scope.launch { tooltipState.show() }
                        tryAwaitRelease()
                        tooltipState.dismiss()
                    },
                )
            },
        )
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
    pieces = listOf(
        CardPiece.Word(
            "Evde", "ev",
            listOf(CardSegment.Text("Ev"), CardSegment.Blank("de", Category.LOCATIVE)),
        ),
        CardPiece.Separator(" "),
        CardPiece.Word(
            "kaldım", "kal",
            listOf(
                CardSegment.Text("kal"),
                CardSegment.Blank("dı", Category.PAST_DEFINITE),
                CardSegment.Text("m"),
            ),
        ),
        CardPiece.Separator("."),
    ),
)

private val SAMPLE_GLOSSES = mapOf("evde" to "at home", "kaldım" to "i stayed")

@Preview(showBackground = true, name = "Practice — typing")
@Composable
private fun PracticeScreenPreview() {
    TurkishSuffixPracticeTheme {
        PracticeContent(
            state = PracticeUiState(card = SAMPLE_CARD, inputs = listOf("de", "")),
            glosses = SAMPLE_GLOSSES,
            showGlosses = false,
            onInput = { _, _ -> },
            onCheck = {},
            onNext = {},
            onPrevious = {},
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
            glosses = SAMPLE_GLOSSES,
            showGlosses = true,
            onInput = { _, _ -> },
            onCheck = {},
            onNext = {},
            onPrevious = {},
        )
    }
}
