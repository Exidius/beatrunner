package com.barad.beatrunner.service

import androidx.annotation.Nullable
import com.barad.beatrunner.data.MusicDao
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.MediaItemTransitionReason
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MusicEventListener(private val foregroundService: ForegroundService,
                         private val musicDao: MusicDao) : Player.EventListener {

    override fun onMediaItemTransition(@Nullable mediaItem: MediaItem?, @MediaItemTransitionReason reason: Int) {
        if (mediaItem != null && mediaItem.mediaId != "URI") {
            foregroundService.currentMusic.value = (foregroundService.currentPlaylist.value?.find {
                (mediaItem.mediaId.split('/').last()) == it.musicId.toString()
            })
            foregroundService.play()
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady && reason == 1) {
            foregroundService.playbackAllowed = false
        }
        if (playWhenReady && reason == 1) {
            foregroundService.playbackAllowed = true
        }
        super.onPlayWhenReadyChanged(playWhenReady, reason)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
                GlobalScope.launch {
                    musicDao.deleteById(foregroundService.currentMusic.value!!.musicId)
                    val playlist = foregroundService.currentPlaylist.value
                    if (playlist != null) {
                        foregroundService.currentPlaylist.postValue(playlist.filter {
                            x -> x.musicId == foregroundService.currentMusic.value!!.musicId
                        })
                    }
                }
                foregroundService.playNextInPlaylist()
            }
            else -> {}
        }
    }
}