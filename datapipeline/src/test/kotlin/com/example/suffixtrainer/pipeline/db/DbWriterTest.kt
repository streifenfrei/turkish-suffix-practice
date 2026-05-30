package com.example.suffixtrainer.pipeline.db

import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class DbWriterTest {

    private fun tempDbFile(): File =
        File.createTempFile("trainer-test", ".db").also { it.deleteOnExit() }

    private fun writeSample(schema: RoomSchema): File {
        val db = tempDbFile()
        DbWriter(schema).write(
            db,
            sentences = listOf(
                Sentence(1, "Evde kal.", "Stay home.", null, 1),
                Sentence(2, "Eve gidiyorum.", "I am going home.", "https://tatoeba.org/audio/download/2", 2),
            ),
            tokens = listOf(
                Token(1, 1, 0, "Evde", "ev", 0, 4),
                Token(2, 2, 0, "Eve", "ev", 0, 3),
            ),
            suffixes = listOf(
                Suffix(1, 1, "-de", Category.LOCATIVE, 2, 4),
                Suffix(2, 2, "-e", Category.DATIVE, 2, 3),
            ),
        )
        return db
    }

    @Test
    fun emits_schema_and_round_trips_rows() {
        val schema = SchemaProvider.resolve(File("does-not-exist.json"), fallbackVersion = 1)
        val db = writeSample(schema)

        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                // Tables present.
                val tables = mutableSetOf<String>()
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                    while (rs.next()) tables += rs.getString(1)
                }
                assertTrue(tables.containsAll(setOf("sentences", "tokens", "suffixes")), "tables: $tables")

                // Indices present with Room's naming.
                val indices = mutableSetOf<String>()
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='index'").use { rs ->
                    while (rs.next()) rs.getString(1)?.let(indices::add)
                }
                assertTrue(indices.contains("index_tokens_sentenceId"), "indices: $indices")
                assertTrue(indices.contains("index_suffixes_tokenId"), "indices: $indices")

                // user_version stamped.
                st.executeQuery("PRAGMA user_version").use { rs ->
                    rs.next(); assertEquals(1, rs.getInt(1))
                }

                // Category stored as enum name; nullable audioPath preserved.
                st.executeQuery(
                    "SELECT s.category, t.lemma, sen.audioPath FROM suffixes s " +
                        "JOIN tokens t ON t.id = s.tokenId " +
                        "JOIN sentences sen ON sen.id = t.sentenceId WHERE s.id = 1",
                ).use { rs ->
                    rs.next()
                    assertEquals("LOCATIVE", rs.getString(1))
                    assertEquals("ev", rs.getString(2))
                    assertEquals(null, rs.getString(3))
                }
            }
        }
    }

    @Test
    fun writes_room_master_table_when_identity_hash_present() {
        val schema = SchemaProvider.resolve(File("does-not-exist.json"), fallbackVersion = 1)
            .copy(identityHash = "abc123def456")
        val db = writeSample(schema)

        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42").use { rs ->
                    assertTrue(rs.next(), "room_master_table row missing")
                    assertEquals("abc123def456", rs.getString(1))
                }
            }
        }
    }
}
