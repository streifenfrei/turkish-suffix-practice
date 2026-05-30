package com.example.suffixtrainer.pipeline.db

import java.io.File
import org.json.JSONObject

/**
 * The DDL + identity needed to build a Room-compatible SQLite file.
 *
 * [identityHash] is non-null only when sourced from Room's exported schema JSON;
 * in that case [DbWriter] writes `room_master_table` so the app opens the db via
 * `createFromAsset` without a schema-mismatch crash.
 */
data class RoomSchema(
    val version: Int,
    val identityHash: String?,
    /** CREATE TABLE / CREATE INDEX statements in execution order. */
    val ddl: List<String>,
    val source: String,
)

/**
 * Produces a [RoomSchema], preferring Room's exported JSON (the source of truth)
 * and falling back to hand-authored DDL that mirrors what Room 2.8 generates for
 * the :core-model entities.
 *
 * The fallback exists so the pipeline is runnable today: :app currently has
 * `exportSchema = false` and no committed schema JSON. A fallback-built db is
 * structurally correct but lacks `room_master_table`, so it will NOT pass Room's
 * runtime identity check until the JSON is wired (see project plan / handoff).
 */
object SchemaProvider {

    private const val TABLE_NAME = "\${TABLE_NAME}"

    fun resolve(schemaJson: File, fallbackVersion: Int): RoomSchema =
        if (schemaJson.isFile) fromJson(schemaJson) else fallback(fallbackVersion)

    private fun fromJson(file: File): RoomSchema {
        val db = JSONObject(file.readText()).getJSONObject("database")
        val version = db.getInt("version")
        val identityHash = db.optString("identityHash").ifEmpty { null }

        val ddl = mutableListOf<String>()
        val entities = db.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            // Room's createSql already wraps the placeholder in backticks
            // (`${'$'}{TABLE_NAME}`), so substitute the bare table name.
            val table = entity.getString("tableName")
            ddl += entity.getString("createSql").replace(TABLE_NAME, table)
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                ddl += indices.getJSONObject(j).getString("createSql").replace(TABLE_NAME, table)
            }
        }
        val setup = db.optJSONArray("setupQueries")
        if (setup != null) for (i in 0 until setup.length()) ddl += setup.getString(i)

        return RoomSchema(version, identityHash, ddl, "Room schema JSON (${file.name})")
    }

    /** Hand-authored DDL mirroring Room 2.8's output for the :core-model entities. */
    private fun fallback(version: Int): RoomSchema {
        val ddl = listOf(
            "CREATE TABLE IF NOT EXISTS `sentences` (" +
                "`id` INTEGER NOT NULL, `turkishText` TEXT NOT NULL, `englishText` TEXT NOT NULL, " +
                "`audioPath` TEXT, `tatoebaId` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `tokens` (" +
                "`id` INTEGER NOT NULL, `sentenceId` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "`surface` TEXT NOT NULL, `lemma` TEXT NOT NULL, `charStart` INTEGER NOT NULL, " +
                "`charEnd` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`sentenceId`) REFERENCES `sentences`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE INDEX IF NOT EXISTS `index_tokens_sentenceId` ON `tokens` (`sentenceId`)",
            "CREATE TABLE IF NOT EXISTS `suffixes` (" +
                "`id` INTEGER NOT NULL, `tokenId` INTEGER NOT NULL, `morpheme` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, `startInSurface` INTEGER NOT NULL, " +
                "`endInSurface` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`tokenId`) REFERENCES `tokens`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE INDEX IF NOT EXISTS `index_suffixes_tokenId` ON `suffixes` (`tokenId`)",
        )
        return RoomSchema(version, identityHash = null, ddl = ddl, source = "fallback DDL (no schema JSON)")
    }
}
