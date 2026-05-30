package com.example.suffixtrainer.pipeline.tatoeba

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * Streams Tatoeba export TSVs. Handles three on-disk forms transparently:
 *  - `*.tsv` / `*.csv` — plain text (dev fixtures)
 *  - `*.tsv.bz2`       — bzip2-compressed text (per-language sentence dumps)
 *  - `*.tar.bz2`       — bzip2 tarball with a single data file (links, audio)
 *
 * Tatoeba TSV has no quoting or embedded tabs, so a plain tab split is safe.
 */
object TatoebaParser {

    /** Opens [file] as a stream of decompressed text lines, calling [block] per line. */
    fun forEachLine(file: File, block: (List<String>) -> Unit) {
        openReader(file).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.isNotEmpty()) block(line.split('\t'))
            }
        }
    }

    /** Reads a sentence dump (id, lang, text) into id → text. */
    fun readSentences(file: File): Map<Long, String> {
        val out = HashMap<Long, String>()
        forEachLine(file) { cols ->
            if (cols.size >= 3) {
                val id = cols[0].toLongOrNull() ?: return@forEachLine
                out[id] = cols[2]
            }
        }
        return out
    }

    /** Reads links (id1, id2), invoking [pair] for each directed link. */
    fun readLinks(file: File, pair: (Long, Long) -> Unit) {
        forEachLine(file) { cols ->
            if (cols.size >= 2) {
                val a = cols[0].toLongOrNull()
                val b = cols[1].toLongOrNull()
                if (a != null && b != null) pair(a, b)
            }
        }
    }

    /** Reads the set of sentence ids that have an audio recording (first column). */
    fun readAudioIds(file: File): Set<Long> {
        val out = HashSet<Long>()
        forEachLine(file) { cols ->
            cols.firstOrNull()?.toLongOrNull()?.let(out::add)
        }
        return out
    }

    private fun openReader(file: File): BufferedReader {
        val name = file.name
        val stream: InputStream = when {
            name.endsWith(".tar.bz2") ->
                TarArchiveInputStream(BZip2CompressorInputStream(file.inputStream().buffered())).apply {
                    // Advance to the first regular file entry.
                    var entry = nextEntry
                    while (entry != null && entry.isDirectory) entry = nextEntry
                    checkNotNull(entry) { "No file entry in tar: $name" }
                }
            name.endsWith(".bz2") ->
                BZip2CompressorInputStream(file.inputStream().buffered())
            else -> file.inputStream().buffered()
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
    }
}
