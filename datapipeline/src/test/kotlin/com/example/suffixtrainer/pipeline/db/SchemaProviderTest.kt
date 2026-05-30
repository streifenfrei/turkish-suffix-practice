package com.example.suffixtrainer.pipeline.db

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class SchemaProviderTest {

    @Test
    fun fallback_used_when_json_absent() {
        val schema = SchemaProvider.resolve(File("nope.json"), fallbackVersion = 1)
        assertNull(schema.identityHash)
        assertEquals(1, schema.version)
        assertTrue(schema.ddl.any { it.contains("CREATE TABLE") && it.contains("sentences") })
        assertTrue(schema.source.contains("fallback"))
    }

    @Test
    fun parses_room_exported_json() {
        val json = File.createTempFile("schema", ".json").apply { deleteOnExit() }
        json.writeText(
            """
            {
              "formatVersion": 1,
              "database": {
                "version": 1,
                "identityHash": "deadbeefcafe",
                "entities": [
                  {
                    "tableName": "sentences",
                    "createSql": "CREATE TABLE IF NOT EXISTS `${'$'}{TABLE_NAME}` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    "indices": [
                      { "name": "idx", "createSql": "CREATE INDEX `idx` ON `${'$'}{TABLE_NAME}` (`id`)" }
                    ]
                  }
                ],
                "setupQueries": [ "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)" ]
              }
            }
            """.trimIndent(),
        )

        val schema = SchemaProvider.resolve(json, fallbackVersion = 99)
        assertEquals(1, schema.version)
        assertEquals("deadbeefcafe", schema.identityHash)
        // ${TABLE_NAME} substituted; index + setup queries included.
        assertTrue(schema.ddl.any { it.contains("CREATE TABLE IF NOT EXISTS `sentences`") })
        assertTrue(schema.ddl.any { it.contains("CREATE INDEX `idx` ON `sentences`") })
        assertTrue(schema.ddl.any { it.contains("room_master_table") })
    }
}
