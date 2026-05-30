package com.example.suffixtrainer.pipeline

import java.io.File

/**
 * All tunables for one pipeline run. Defaults make a bare
 * `./gradlew :datapipeline:run` produce a curated db from cached input.
 */
data class PipelineConfig(
    // Paths are relative to the :datapipeline module dir — the working directory
    // of both the `run` and `test` Gradle tasks (e.g. datapipeline/build/output).
    /** Directory for cached raw Tatoeba downloads (gitignored, survives clean). */
    val cacheDir: File = File(".tatoeba-cache"),
    /** Output directory for trainer.db and reports. */
    val outputDir: File = File("build/output"),
    /**
     * Optional Room-exported schema JSON (createSql + identityHash). When absent,
     * the writer falls back to structural DDL and warns. See SchemaProvider.
     */
    val schemaJson: File = File("src/main/resources/schema/AppDatabase_1.json"),
    /** Schema version the emitted db is stamped with (must match :app AppDatabase). */
    val schemaVersion: Int = 1,

    /** Curated corpus target — stop once this many usable cards are collected. */
    val maxSentences: Int = 1200,
    /** Drop sentences longer than this many characters. */
    val maxChars: Int = 80,
    /** Drop sentences with more than this many word tokens. */
    val maxTokens: Int = 12,

    /**
     * Dev mode: read uncompressed fixture TSVs from [fixtureDir] instead of
     * downloading the full Tatoeba dumps. Used for fast end-to-end iteration.
     */
    val devMode: Boolean = false,
    val fixtureDir: File = File("src/test/resources/fixture"),
) {
    companion object {
        /** Parses CLI args: `--max N`, `--dev`, `--cache DIR`, `--out DIR`, `--schema FILE`. */
        fun fromArgs(args: Array<String>): PipelineConfig {
            var cfg = PipelineConfig()
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--max" -> cfg = cfg.copy(maxSentences = args[++i].toInt())
                    "--dev" -> cfg = cfg.copy(devMode = true)
                    "--cache" -> cfg = cfg.copy(cacheDir = File(args[++i]))
                    "--out" -> cfg = cfg.copy(outputDir = File(args[++i]))
                    "--schema" -> cfg = cfg.copy(schemaJson = File(args[++i]))
                    else -> error("Unknown argument: ${args[i]}")
                }
                i++
            }
            return cfg
        }
    }
}
