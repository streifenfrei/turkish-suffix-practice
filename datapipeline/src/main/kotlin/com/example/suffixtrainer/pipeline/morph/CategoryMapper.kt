package com.example.suffixtrainer.pipeline.morph

import com.example.suffixtrainer.model.Category

/**
 * Maps a Zemberek morpheme id (e.g. "Loc", "Past", "Prog1") to our [Category]
 * enum. This is the core, heavily-tested unit of the pipeline.
 *
 * The ids below are Zemberek 0.17.1's stable morpheme ids (`Morpheme.id`),
 * verified against real analyses. Only the case and tense/mood morphemes we care
 * about are mapped; every other id (part of speech, person/number agreement,
 * possessives, derivation markers, …) maps to `null` and is skipped.
 *
 * Note: `Nom` (nominative) is mapped for completeness, but it has a zero-width
 * surface, so [MorphologyAnalyzer] never emits a blankable suffix for it.
 */
object CategoryMapper {

    private val byMorphemeId: Map<String, Category> = mapOf(
        // Cases
        "Nom" to Category.NOMINATIVE,
        "Acc" to Category.ACCUSATIVE,
        "Dat" to Category.DATIVE,
        "Loc" to Category.LOCATIVE,
        "Abl" to Category.ABLATIVE,
        "Gen" to Category.GENITIVE,
        "Ins" to Category.INSTRUMENTAL,
        // Tense / mood
        "Prog1" to Category.PRESENT_CONTINUOUS, // -iyor
        "Prog2" to Category.PRESENT_CONTINUOUS, // -makta
        "Aor" to Category.AORIST,               // -ir / -er
        "Past" to Category.PAST_DEFINITE,       // -di
        "Narr" to Category.PAST_INFERENTIAL,    // -miş
        "Fut" to Category.FUTURE,               // -ecek
        "Cond" to Category.CONDITIONAL,         // -se
        "Neces" to Category.NECESSITATIVE,      // -meli
        "Opt" to Category.OPTATIVE,             // -e
    )

    /** Returns the [Category] for a Zemberek morpheme id, or null if untracked. */
    fun categoryFor(morphemeId: String): Category? = byMorphemeId[morphemeId]
}
