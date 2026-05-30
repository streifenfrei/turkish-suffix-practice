package com.example.suffixtrainer.pipeline.tatoeba

import java.io.BufferedInputStream
import java.io.File
import java.net.URI

/**
 * Downloads Tatoeba export files into a local cache and skips files already
 * present, so re-runs work fully offline. Files are stored compressed exactly as
 * served (.tsv.bz2 / .tar.bz2); decompression happens in [TatoebaParser].
 */
class Downloader(private val cacheDir: File) {

    init {
        cacheDir.mkdirs()
    }

    /** Returns the cached file for [url], downloading it once if missing. */
    fun ensure(url: String, fileName: String): File {
        val target = File(cacheDir, fileName)
        if (target.exists() && target.length() > 0) {
            println("  cache hit: $fileName (${target.length() / 1024} KiB)")
            return target
        }
        println("  downloading: $url")
        val tmp = File(cacheDir, "$fileName.part")
        URI.create(url).toURL().openStream().use { input ->
            BufferedInputStream(input).use { buffered ->
                tmp.outputStream().use { out -> buffered.copyTo(out) }
            }
        }
        check(tmp.renameTo(target)) { "Failed to finalize download $fileName" }
        println("  downloaded: $fileName (${target.length() / 1024} KiB)")
        return target
    }
}
