package com.example.suffixtrainer.ui

import com.example.suffixtrainer.model.Category

/** Human-readable label for a [Category], with its canonical example morpheme. */
fun Category.displayLabel(): String = when (this) {
    Category.NOMINATIVE -> "Nominative"
    Category.ACCUSATIVE -> "Accusative (-ı)"
    Category.DATIVE -> "Dative (-e)"
    Category.LOCATIVE -> "Locative (-de)"
    Category.ABLATIVE -> "Ablative (-den)"
    Category.GENITIVE -> "Genitive (-in)"
    Category.INSTRUMENTAL -> "Instrumental (-le)"
    Category.PRESENT_CONTINUOUS -> "Present continuous (-iyor)"
    Category.AORIST -> "Aorist (-ir)"
    Category.PAST_DEFINITE -> "Past definite (-di)"
    Category.PAST_INFERENTIAL -> "Past inferential (-miş)"
    Category.FUTURE -> "Future (-ecek)"
    Category.CONDITIONAL -> "Conditional (-se)"
    Category.NECESSITATIVE -> "Necessitative (-meli)"
    Category.OPTATIVE -> "Optative (-e)"
}

/** The case categories, in the order shown in Settings. */
val CASE_CATEGORIES: List<Category> = listOf(
    Category.NOMINATIVE,
    Category.ACCUSATIVE,
    Category.DATIVE,
    Category.LOCATIVE,
    Category.ABLATIVE,
    Category.GENITIVE,
    Category.INSTRUMENTAL,
)

/** The tense/mood categories, in the order shown in Settings. */
val TENSE_CATEGORIES: List<Category> = listOf(
    Category.PRESENT_CONTINUOUS,
    Category.AORIST,
    Category.PAST_DEFINITE,
    Category.PAST_INFERENTIAL,
    Category.FUTURE,
    Category.CONDITIONAL,
    Category.NECESSITATIVE,
    Category.OPTATIVE,
)
