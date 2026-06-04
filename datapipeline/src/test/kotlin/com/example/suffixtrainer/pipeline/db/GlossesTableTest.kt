package com.example.suffixtrainer.pipeline.db

import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class GlossesTableTest {

    private fun tempDbFile(): File =
        File.createTempFile("trainer-gloss-test", ".db").also { it.deleteOnExit() }

    private fun write(schema: RoomSchema, glosses: Map<String, String>): File {
        val db = tempDbFile()
        DbWriter(schema).write(
            db,
            sentences = listOf(Sentence(1, "Evde kal.", "Stay home.", null, 1)),
            tokens = listOf(Token(1, 1, 0, "Evde", "ev", 0, 4)),
            suffixes = listOf(Suffix(1, 1, "-de", Category.LOCATIVE, 2, 4)),
            glosses = glosses,
        )
        return db
    }

    @Test
    fun writes_and_round_trips_glosses() {
        val db = write(
            SchemaProvider.resolve(File("does-not-exist.json"), fallbackVersion = 1)
                .copy(identityHash = "abc123"),
            glosses = mapOf("ev" to "house", "kal" to "stay"),
        )
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT gloss FROM glosses WHERE word = 'ev'").use { rs ->
                    assertTrue(rs.next(), "gloss row missing")
                    assertEquals("house", rs.getString(1))
                }
                // Identity hash is unaffected by the extra (non-Room) glosses table.
                st.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42").use { rs ->
                    rs.next(); assertEquals("abc123", rs.getString(1))
                }
            }
        }
    }

    @Test
    fun glosses_table_exists_even_when_empty() {
        val db = write(SchemaProvider.resolve(File("nope.json"), 1), glosses = emptyMap())
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM glosses").use { rs ->
                    rs.next(); assertEquals(0, rs.getInt(1))
                }
                st.executeQuery("SELECT gloss FROM glosses WHERE word = 'ev'").use { rs ->
                    assertNull(if (rs.next()) rs.getString(1) else null)
                }
            }
        }
    }
}
