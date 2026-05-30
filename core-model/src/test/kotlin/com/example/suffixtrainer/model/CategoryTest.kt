package com.example.suffixtrainer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun `category enum has the expected entries in order`() {
        val expected = listOf(
            // Cases
            "NOMINATIVE", "ACCUSATIVE", "DATIVE", "LOCATIVE", "ABLATIVE",
            "GENITIVE", "INSTRUMENTAL",
            // Tense / mood
            "PRESENT_CONTINUOUS", "AORIST", "PAST_DEFINITE", "PAST_INFERENTIAL",
            "FUTURE", "CONDITIONAL", "NECESSITATIVE", "OPTATIVE",
        )
        assertEquals(expected, Category.entries.map { it.name })
    }

    @Test
    fun `category enum has fifteen entries`() {
        assertEquals(15, Category.entries.size)
    }
}
