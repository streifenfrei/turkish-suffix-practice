package com.example.suffixtrainer.pipeline

import com.example.suffixtrainer.model.Category

/**
 * Entry point for the offline data pipeline that will ingest Tatoeba and run
 * Zemberek morphological analysis to produce `trainer.db`.
 *
 * Stub only — no ingestion or analysis is implemented yet. It just confirms the
 * module builds and can see the shared :core-model contract.
 */
fun main() {
    println("Turkish Suffix Trainer data pipeline — stub.")
    println("No ingestion implemented yet. Known categories: ${Category.entries.size}")
}
