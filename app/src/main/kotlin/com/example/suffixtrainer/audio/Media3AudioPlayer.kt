package com.example.suffixtrainer.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3 (ExoPlayer)-backed [AudioPlayer]. The underlying player is created lazily on the first
 * real playback request, so with the current corpus (all `audioPath == null`) no ExoPlayer is
 * ever instantiated and the app runs without any bundled audio. Audio files are expected under
 * `assets/` and addressed via the `asset:///` scheme.
 */
@Singleton
class Media3AudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AudioPlayer {

    private var player: ExoPlayer? = null

    override fun play(audioPath: String?) {
        if (audioPath.isNullOrBlank()) return // No audio bundled yet — no-op gracefully.
        val exo = player ?: ExoPlayer.Builder(context).build().also { player = it }
        exo.setMediaItem(MediaItem.fromUri("asset:///$audioPath"))
        exo.prepare()
        exo.play()
    }

    override fun release() {
        player?.release()
        player = null
    }
}
