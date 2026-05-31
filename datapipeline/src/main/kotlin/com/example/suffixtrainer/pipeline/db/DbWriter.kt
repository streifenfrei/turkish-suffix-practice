package com.example.suffixtrainer.pipeline.db

import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Writes the analyzed corpus to a SQLite file matching the Room schema. Uses the
 * [RoomSchema]'s DDL verbatim, stamps `PRAGMA user_version`, optionally writes
 * `room_master_table` (when an identity hash is known), inserts all rows in one
 * transaction, and VACUUMs.
 */
class DbWriter(private val schema: RoomSchema) {

    fun write(
        outputFile: File,
        sentences: List<Sentence>,
        tokens: List<Token>,
        suffixes: List<Suffix>,
        glosses: Map<String, String> = emptyMap(),
    ) {
        outputFile.parentFile?.mkdirs()
        outputFile.delete()

        DriverManager.getConnection("jdbc:sqlite:${outputFile.absolutePath}").use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                schema.ddl.forEach { st.executeUpdate(it) }
                st.executeUpdate("PRAGMA user_version = ${schema.version}")
            }
            if (schema.identityHash != null) writeMasterTable(conn, schema.identityHash)
            insertSentences(conn, sentences)
            insertTokens(conn, tokens)
            insertSuffixes(conn, suffixes)
            insertGlosses(conn, glosses)
            conn.commit()

            conn.autoCommit = true
            conn.createStatement().use { it.executeUpdate("VACUUM") }
        }
    }

    /**
     * Word translations. A plain SQLite table that is NOT part of the Room schema, so it does not
     * affect the identity hash; the app reads it with `@SkipQueryVerification`. Created even when
     * empty so the app's query never hits a missing table.
     */
    private fun insertGlosses(conn: Connection, glosses: Map<String, String>) {
        conn.createStatement().use {
            it.executeUpdate("CREATE TABLE IF NOT EXISTS glosses (lemma TEXT PRIMARY KEY, gloss TEXT)")
        }
        if (glosses.isEmpty()) return
        conn.prepareStatement("INSERT OR REPLACE INTO glosses (lemma, gloss) VALUES (?, ?)").use { ps ->
            for ((lemma, gloss) in glosses) {
                ps.setString(1, lemma)
                ps.setString(2, gloss)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun writeMasterTable(conn: Connection, identityHash: String) {
        conn.createStatement().use { st ->
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            // Single-quoted literal, matching Room's own master-table format.
            st.executeUpdate(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES(42, '$identityHash')",
            )
        }
    }

    private fun insertSentences(conn: Connection, rows: List<Sentence>) {
        val sql = "INSERT INTO sentences (id, turkishText, englishText, audioPath, tatoebaId) " +
            "VALUES (?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            for (r in rows) {
                ps.setLong(1, r.id)
                ps.setString(2, r.turkishText)
                ps.setString(3, r.englishText)
                if (r.audioPath != null) ps.setString(4, r.audioPath) else ps.setNull(4, java.sql.Types.VARCHAR)
                ps.setLong(5, r.tatoebaId)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertTokens(conn: Connection, rows: List<Token>) {
        val sql = "INSERT INTO tokens (id, sentenceId, position, surface, lemma, charStart, charEnd) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            for (r in rows) {
                ps.setLong(1, r.id)
                ps.setLong(2, r.sentenceId)
                ps.setInt(3, r.position)
                ps.setString(4, r.surface)
                ps.setString(5, r.lemma)
                ps.setInt(6, r.charStart)
                ps.setInt(7, r.charEnd)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertSuffixes(conn: Connection, rows: List<Suffix>) {
        val sql = "INSERT INTO suffixes (id, tokenId, morpheme, category, startInSurface, endInSurface) " +
            "VALUES (?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            for (r in rows) {
                ps.setLong(1, r.id)
                ps.setLong(2, r.tokenId)
                ps.setString(3, r.morpheme)
                ps.setString(4, r.category.name)
                ps.setInt(5, r.startInSurface)
                ps.setInt(6, r.endInSurface)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }
}
