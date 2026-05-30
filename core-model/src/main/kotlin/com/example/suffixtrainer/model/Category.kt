package com.example.suffixtrainer.model

/**
 * The grammatical categories a suffix can belong to — the toggle list in
 * Settings. Each entry is independently enableable. This enum is the frozen
 * contract shared by the app and the data pipeline; do not reorder or remove
 * entries without a coordinated schema/data migration.
 */
enum class Category {
    // Cases
    NOMINATIVE,
    ACCUSATIVE,
    DATIVE,
    LOCATIVE,
    ABLATIVE,
    GENITIVE,
    INSTRUMENTAL,

    // Tense / mood
    PRESENT_CONTINUOUS, // -iyor
    AORIST,             // -ir / -er
    PAST_DEFINITE,      // -di
    PAST_INFERENTIAL,   // -miş
    FUTURE,             // -ecek
    CONDITIONAL,        // -se
    NECESSITATIVE,      // -meli
    OPTATIVE,           // -e
}
