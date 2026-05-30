package com.example.suffixtrainer.data

import com.example.suffixtrainer.domain.CardSegment
import com.example.suffixtrainer.domain.renderCard
import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Golden cross-boundary test. Reads the **real** prepackaged `trainer.db` (produced by
 * :datapipeline from Zemberek-computed spans) and runs the rows through the app's pure
 * [renderCard]. This is the only test that exercises the seam between the pipeline's span
 * convention (`startInSurface`/`endInSurface`) and the app's slicing — if either side drifts,
 * the reconstruction / span assertions fail. It runs across every row in the shipped corpus.
 */
class TrainerDbGoldenTest {

    @Test
    fun `every blanked card across the real corpus reconstructs its original Turkish text`() {
        val corpus = loadCorpusFromAsset()
        // Guards against accidentally shipping the 5-row dev fixture instead of the real corpus.
        assertTrue("expected the real corpus (got ${corpus.size} cards)", corpus.size > 50)

        for (data in corpus) {
            val card = renderCard(data, Category.entries.toSet())
                ?: error("'${data.sentence.turkishText}' has no blankable suffix in the db")
            val reconstructed = buildString {
                card.segments.forEach { segment ->
                    when (segment) {
                        is CardSegment.Text -> append(segment.text)
                        is CardSegment.Blank -> append(segment.answer)
                    }
                }
            }
            assertEquals(
                "span mismatch reconstructing '${data.sentence.turkishText}'",
                data.sentence.turkishText,
                reconstructed,
            )
        }
    }

    @Test
    fun `every suffix span in the db points at its morpheme surface`() {
        var checked = 0
        val db = trainerDbFile()
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT t.surface AS surface, s.morpheme AS morpheme, " +
                        "s.startInSurface AS startInSurface, s.endInSurface AS endInSurface " +
                        "FROM suffixes s JOIN tokens t ON t.id = s.tokenId",
                ).use { rs ->
                    while (rs.next()) {
                        val surface = rs.getString("surface")
                        val morpheme = rs.getString("morpheme")
                        val start = rs.getInt("startInSurface")
                        val end = rs.getInt("endInSurface")
                        assertTrue("bad span [$start,$end) on '$surface'", start in 0 until end && end <= surface.length)
                        // Pipeline stores morpheme as "-" + surface.substring(start, end), half-open.
                        assertEquals(
                            "span text mismatch on '$surface'",
                            morpheme.removePrefix("-"),
                            surface.substring(start, end),
                        )
                        checked++
                    }
                }
            }
        }
        assertTrue("expected many suffixes (got $checked)", checked > 50)
    }

    @Test
    fun `known worked example blanks the locative suffix at the right span`() {
        // "Evde kal." is the dev-fixture sentence, not part of the real corpus, so this CardData
        // is built inline — a deterministic worked example of the half-open span rendering.
        val data = CardData(
            sentence = Sentence(1, "Evde kal.", "Stay home.", null, 1),
            tokens = listOf(
                TokenWithSuffixes(
                    token = Token(1, 1, 0, "Evde", "ev", 0, 4),
                    suffixes = listOf(Suffix(1, 1, "-de", Category.LOCATIVE, 2, 4)),
                ),
            ),
        )
        val card = renderCard(data, Category.entries.toSet())!!
        assertEquals(
            listOf(
                CardSegment.Text("Ev"),
                CardSegment.Blank("de", Category.LOCATIVE),
                CardSegment.Text(" kal."),
            ),
            card.segments,
        )
    }

    /** Reads sentences→tokens→suffixes straight out of the shipped asset db via JDBC. */
    private fun loadCorpusFromAsset(): List<CardData> {
        val db = trainerDbFile()
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            val sentences = conn.createStatement().use { st ->
                st.executeQuery("SELECT id, turkishText, englishText, audioPath, tatoebaId FROM sentences ORDER BY id")
                    .use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    Sentence(
                                        id = rs.getLong("id"),
                                        turkishText = rs.getString("turkishText"),
                                        englishText = rs.getString("englishText"),
                                        audioPath = rs.getString("audioPath"),
                                        tatoebaId = rs.getLong("tatoebaId"),
                                    ),
                                )
                            }
                        }
                    }
            }
            return sentences.map { sentence ->
                CardData(sentence = sentence, tokens = loadTokens(conn, sentence.id))
            }
        }
    }

    private fun loadTokens(conn: java.sql.Connection, sentenceId: Long): List<TokenWithSuffixes> {
        val tokens = conn.prepareStatement(
            "SELECT id, sentenceId, position, surface, lemma, charStart, charEnd " +
                "FROM tokens WHERE sentenceId = ? ORDER BY position",
        ).use { ps ->
            ps.setLong(1, sentenceId)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Token(
                                id = rs.getLong("id"),
                                sentenceId = rs.getLong("sentenceId"),
                                position = rs.getInt("position"),
                                surface = rs.getString("surface"),
                                lemma = rs.getString("lemma"),
                                charStart = rs.getInt("charStart"),
                                charEnd = rs.getInt("charEnd"),
                            ),
                        )
                    }
                }
            }
        }
        return tokens.map { token -> TokenWithSuffixes(token, loadSuffixes(conn, token.id)) }
    }

    private fun loadSuffixes(conn: java.sql.Connection, tokenId: Long): List<Suffix> =
        conn.prepareStatement(
            "SELECT id, tokenId, morpheme, category, startInSurface, endInSurface " +
                "FROM suffixes WHERE tokenId = ? ORDER BY startInSurface",
        ).use { ps ->
            ps.setLong(1, tokenId)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Suffix(
                                id = rs.getLong("id"),
                                tokenId = rs.getLong("tokenId"),
                                morpheme = rs.getString("morpheme"),
                                category = Category.valueOf(rs.getString("category")),
                                startInSurface = rs.getInt("startInSurface"),
                                endInSurface = rs.getInt("endInSurface"),
                            ),
                        )
                    }
                }
            }
        }

    /** The :app:test working dir is the module dir, so the asset path resolves relative to it. */
    private fun trainerDbFile(): File {
        val candidates = listOf(
            File("src/main/assets/trainer.db"),
            File("app/src/main/assets/trainer.db"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("trainer.db not found in ${candidates.map { it.absolutePath }}")
    }
}
