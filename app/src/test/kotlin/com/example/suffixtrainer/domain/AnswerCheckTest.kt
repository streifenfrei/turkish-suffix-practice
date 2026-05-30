package com.example.suffixtrainer.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    @Test
    fun `exact match is correct`() {
        assertTrue(isSuffixCorrect("de", "de"))
    }

    @Test
    fun `match ignores case and surrounding whitespace`() {
        assertTrue(isSuffixCorrect("DE", "de"))
        assertTrue(isSuffixCorrect("  de ", "de"))
        assertTrue(isSuffixCorrect("İYOR", "iyor")) // Turkish dotted İ lowercases to i
    }

    @Test
    fun `turkish dotless and dotted i are distinct`() {
        // Capital "I" lowercases to dotless "ı" in Turkish, so it must not match dotted "i".
        assertFalse(isSuffixCorrect("I", "i"))
        assertTrue(isSuffixCorrect("I", "ı"))
    }

    @Test
    fun `different suffix is incorrect`() {
        assertFalse(isSuffixCorrect("da", "de"))
        assertFalse(isSuffixCorrect("", "de"))
    }
}
