package io.github.gonzsanz.cobalttv.core.playback

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * One ExoPlayer shared by the live preview and the full screen player, so the
 * channel keeps playing when the user opens it full screen and again when they
 * come back to the list. URLs are never logged: they can contain credentials.
 */
class PlayerHolder(context: Context) {

    private val appContext = context.applicationContext
    private var lastUrl: String? = null

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        setHandleAudioBecomingNoisy(true)
    }

    /** Identifier of the item currently loaded (channel id or URL). */
    var currentKey by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set
    var error by mutableStateOf<PlaybackException?>(null)
        private set

    private var resumeOnStart = false

    init {
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                }

                override fun onPlayerError(playerError: PlaybackException) {
                    Log.e(TAG, "No se pudo reproducir el contenido (${playerError.errorCodeName})")
                    error = playerError
                }
            },
        )
    }

    /** Starts [url] unless [key] is already loaded and healthy. */
    fun play(url: String, key: String = url) {
        if (key == currentKey && error == null && playbackState != Player.STATE_IDLE) {
            player.playWhenReady = true
            return
        }
        load(url, key)
    }

    fun reload() {
        val url = lastUrl ?: return
        load(url, currentKey ?: url)
    }

    private fun load(url: String, key: String) {
        error = null
        currentKey = key
        lastUrl = url
        player.stop()
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentKey = null
        lastUrl = null
        error = null
    }

    fun onAppStop() {
        resumeOnStart = player.playWhenReady
        player.pause()
    }

    fun onAppStart() {
        if (resumeOnStart) player.play()
    }

    fun release() {
        player.release()
    }

    private companion object {
        const val TAG = "CobaltPlayer"
    }
}
