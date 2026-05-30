package com.example.suffixtrainer.audio

/**
 * Plays a card's pronunciation audio. Kept behind an interface so the UI/ViewModel never touch
 * Media3 directly and so it can be faked in tests. Implementations must no-op gracefully when
 * the path is null/blank (true for all sample cards until real audio is bundled).
 */
interface AudioPlayer {
    fun play(audioPath: String?)

    /** Release underlying resources. Call when the player is no longer needed. */
    fun release()
}
