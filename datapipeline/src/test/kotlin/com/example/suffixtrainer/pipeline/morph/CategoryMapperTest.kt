package com.example.suffixtrainer.pipeline.morph

import com.example.suffixtrainer.model.Category
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

/** Pure mapping-table tests — no Zemberek model needed, so these are fast. */
class CategoryMapperTest {

    @Test
    fun maps_case_morphemes() {
        assertEquals(Category.NOMINATIVE, CategoryMapper.categoryFor("Nom"))
        assertEquals(Category.ACCUSATIVE, CategoryMapper.categoryFor("Acc"))
        assertEquals(Category.DATIVE, CategoryMapper.categoryFor("Dat"))
        assertEquals(Category.LOCATIVE, CategoryMapper.categoryFor("Loc"))
        assertEquals(Category.ABLATIVE, CategoryMapper.categoryFor("Abl"))
        assertEquals(Category.GENITIVE, CategoryMapper.categoryFor("Gen"))
        assertEquals(Category.INSTRUMENTAL, CategoryMapper.categoryFor("Ins"))
    }

    @Test
    fun maps_tense_and_mood_morphemes() {
        assertEquals(Category.PRESENT_CONTINUOUS, CategoryMapper.categoryFor("Prog1"))
        assertEquals(Category.PRESENT_CONTINUOUS, CategoryMapper.categoryFor("Prog2"))
        assertEquals(Category.AORIST, CategoryMapper.categoryFor("Aor"))
        assertEquals(Category.PAST_DEFINITE, CategoryMapper.categoryFor("Past"))
        assertEquals(Category.PAST_INFERENTIAL, CategoryMapper.categoryFor("Narr"))
        assertEquals(Category.FUTURE, CategoryMapper.categoryFor("Fut"))
        assertEquals(Category.CONDITIONAL, CategoryMapper.categoryFor("Cond"))
        assertEquals(Category.NECESSITATIVE, CategoryMapper.categoryFor("Neces"))
        assertEquals(Category.OPTATIVE, CategoryMapper.categoryFor("Opt"))
    }

    @Test
    fun untracked_morphemes_map_to_null() {
        // Part of speech, agreement, possessive, derivation — all skipped.
        assertNull(CategoryMapper.categoryFor("Noun"))
        assertNull(CategoryMapper.categoryFor("Verb"))
        assertNull(CategoryMapper.categoryFor("A1sg"))
        assertNull(CategoryMapper.categoryFor("A3sg"))
        assertNull(CategoryMapper.categoryFor("P3sg"))
        assertNull(CategoryMapper.categoryFor("Imp"))
        assertNull(CategoryMapper.categoryFor(""))
    }
}
