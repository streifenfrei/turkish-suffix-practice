package com.example.suffixtrainer.pipeline.tatoeba

import com.example.suffixtrainer.pipeline.PipelineConfig
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class TatoebaParserTest {

    private val fixtureDir = File("src/test/resources/fixture")

    @Test
    fun reads_sentences_and_audio_from_fixture() {
        val tur = TatoebaParser.readSentences(File(fixtureDir, "tur_sentences.tsv"))
        assertEquals(5, tur.size)
        assertEquals("Eve gidiyorum.", tur[2])

        val audio = TatoebaParser.readAudioIds(File(fixtureDir, "sentences_with_audio.tsv"))
        assertEquals(setOf(2L, 4L), audio)
    }

    @Test
    fun pairs_turkish_with_english_and_flags_audio() {
        val pairs = TatoebaIngest(PipelineConfig(devMode = true, fixtureDir = fixtureDir))
            .loadPairs()
            .associateBy { it.turkishId }

        assertEquals(5, pairs.size)
        assertEquals("I am going home.", pairs.getValue(2).englishText)
        assertTrue(pairs.getValue(2).hasAudio)
        assertFalse(pairs.getValue(1).hasAudio)
        // Unlinked English (id 999) must not leak into any pair.
        assertTrue(pairs.values.none { it.englishText.contains("Unrelated") })
    }
}
