package com.rolex.ytlite.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackInfo(
    val title: String = "",
    val isPlaying: Boolean = false,
    val videoUrl: String = ""
)

/**
 * Process-wide holder so the (visible) WebView in MainActivity and the
 * (headless) WebView inside MusicPlaybackService can share state without a
 * tight coupling between the two classes.
 */
object PlaybackState {
    private val _info = MutableStateFlow(PlaybackInfo())
    val info = _info.asStateFlow()

    fun update(title: String? = null, isPlaying: Boolean? = null, videoUrl: String? = null) {
        _info.value = _info.value.copy(
            title = title ?: _info.value.title,
            isPlaying = isPlaying ?: _info.value.isPlaying,
            videoUrl = videoUrl ?: _info.value.videoUrl
        )
    }
}
