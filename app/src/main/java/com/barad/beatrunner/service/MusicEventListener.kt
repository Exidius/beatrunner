package com.barad.beatrunner.service

import android.hardware.Sensor
import android.util.Log
import androidx.annotation.Nullable
import com.barad.beatrunner.data.MusicDao
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.MediaItemTransitionReason
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MusicEventListener(private val musicService: MusicService,
                         private val musicDao: MusicDao) : Player.EventListener {

    override fun onMediaItemTransition(@Nullable mediaItem: MediaItem?, @MediaItemTransitionReason reason: Int) {
        if (mediaItem != null && mediaItem.mediaId != "URI") {
            musicService.currentMusic.value = (musicService.currentPlaylist.value?.find {
                (mediaItem.mediaId.split('/').last()) == it.musicId.toString()
            })
        }
        musicService.play()
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
                GlobalScope.launch {
                    musicDao.deleteById(musicService.currentMusic.value!!.musicId)
                    val playlist = musicService.currentPlaylist.value
                    if (playlist != null) {
                        musicService.currentPlaylist.postValue(playlist.filter {
                            x -> x.musicId == musicService.currentMusic.value!!.musicId
                        })
                    }
                }
                musicService.playNextInPlaylist()
            }
            else -> {}
        }
    }
}